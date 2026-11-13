/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.netex.id;

import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;

import com.hazelcast.cp.lock.FencedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;


/**
 * During the implementation of Tiamat was desirable to produce NeTEx IDs for stop places more or less gapless.
 * The reason for this implementation was legacy systems with restrictions of maximum number of digits.
 *
 * This service should only be called for certain ID prefixes. This service only operates with long values.
 *
 * This service is using Hazelcast to avoid generating the same IDs on multiple nodes.
 * There is a hazelcast lock for persisting these IDs from one node to a table in postgres.
 * The reason for not writing the IDs immediately is performance when importing many stops. For instance the initial population of stop places.
 */
@Service
public class GaplessIdGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(GaplessIdGeneratorService.class);

    public static final String REENTRANT_LOCK_PREFIX = "entity_lock_";

    public static final long INITIAL_LAST_ID = 0;
    public static final int LOW_LEVEL_AVAILABLE_IDS = 10;
    public static final int DEFAULT_FETCH_SIZE = 2000;

    public static final int INSERT_CLAIMED_ID_THRESHOLD = 1000;

    private final int fetchSize;

    private final IdGeneratorConnectionPool connectionPool;
    private final HazelcastInstance hazelcastInstance;
    private final GeneratedIdState generatedIdState;

    @Autowired
    public GaplessIdGeneratorService(IdGeneratorConnectionPool connectionPool, HazelcastInstance hazelcastInstance, GeneratedIdState generatedIdState) {
        this.connectionPool = connectionPool;
        this.hazelcastInstance = hazelcastInstance;
        this.generatedIdState = generatedIdState;
        this.fetchSize = DEFAULT_FETCH_SIZE;
    }

    public GaplessIdGeneratorService(IdGeneratorConnectionPool connectionPool, HazelcastInstance hazelcastInstance, GeneratedIdState generatedIdState, int fetchSize) {
        this.connectionPool = connectionPool;
        this.hazelcastInstance = hazelcastInstance;
        this.generatedIdState = generatedIdState;

        if (fetchSize < LOW_LEVEL_AVAILABLE_IDS) {
            logger.warn("Fetch size {} cannot be lower than LOW LEVEL AVAILABLE IDS {}. Setting fetch size to {}", fetchSize, LOW_LEVEL_AVAILABLE_IDS, LOW_LEVEL_AVAILABLE_IDS);
            fetchSize = LOW_LEVEL_AVAILABLE_IDS;
        }

        this.fetchSize = fetchSize;

    }

    /**
     * Get a new ID for a certain entity.
     *
     * @param entityTypeName Name of the entity, for instance StopPlace
     * @return the generated long value
     */
    public long getNextIdForEntity(String entityTypeName) {
        return getNextIdForEntity(entityTypeName, 0);
    }

    /**
     * Claim an ID for a certain entity.
     * If the ID is generated as available, it will be removed.
     * If the claimedId is zero, a fresh generated ID should be removed.
     *
     * @param entityTypeName Name of the entity, for instance StopPlace
     * @param claimedId The claimed ID to remove from list of available IDs
     * @return the claimed ID
     */
    public long getNextIdForEntity(String entityTypeName, long claimedId) {
        final FencedLock lock = hazelcastInstance.getCPSubsystem().getLock(entityLockString(entityTypeName));
        lock.lock();
        try {
            BlockingQueue<Long> availableIds = generatedIdState.getQueueForEntity(entityTypeName);
            ISet<Long> claimedIds = generatedIdState.getClaimedIdListForEntity(entityTypeName);

            if (claimedId > 0) {
                if (availableIds.remove(claimedId)) {
                    logger.trace("Removed claimed ID {} from list of available IDs for entity {}: {}", claimedId, entityTypeName, availableIds.stream().collect(toList()));
                }
                // Add it to make it persisted later
                claimedIds.add(claimedId);
            }

            boolean timeToGenerateAvailableIds = availableIds.size() < LOW_LEVEL_AVAILABLE_IDS;

            if (timeToGenerateAvailableIds || claimedIds.size() > INSERT_CLAIMED_ID_THRESHOLD) {
                writeClaimedIdsAndGenerateNew(entityTypeName, timeToGenerateAvailableIds);
            }

            if (claimedId > 0) {
                logger.trace("Returning claimed ID {}", claimedId);
                return claimedId;
            } else {
                Long remove = availableIds.remove();
                claimedIds.add(remove);
                logger.trace("Returning available ID for {}: {}", entityTypeName, remove);
                return remove;
            }
        } catch (Exception e) {
            throw new IdGeneratorException("Caught exception when generating IDs for entity " + entityTypeName, e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Write claimed IDs and generate new ones.
     */
    private void writeClaimedIdsAndGenerateNew(String entityTypeName, boolean timeToGenerateAvailableIds) {
        executeInTransaction(connection -> {
            BlockingQueue<Long> availableIds = generatedIdState.getQueueForEntity(entityTypeName);
            ISet<Long> claimedIds = generatedIdState.getClaimedIdListForEntity(entityTypeName);

            if (!claimedIds.isEmpty()) {
                // Ignore duplicates because claimed ids could already have been inserted as available IDs previously
                logger.debug("Inserting {} claimed IDs for {}", claimedIds.size(), entityTypeName);
                insertIdsIgnoreDuplicates(entityTypeName, claimedIds, connection);
                claimedIds.destroy();
            }
            if (timeToGenerateAvailableIds) {
                logger.debug("Generating new available IDs for {}", entityTypeName);
                generateNewIds(entityTypeName, availableIds, connection);
            }
        });
    }

    /**
     * Generate new IDs for entity.
     *
     * @param entityTypeName Table to writeClaimedIdsAndGenerateNew IDs for
     * @param availableIds The (empty) queue of available IDs to fill
     * @param connection The connection to use
     */
    private void generateNewIds(String entityTypeName, BlockingQueue<Long> availableIds, Connection connection) throws SQLException, InterruptedException {
        List<Long> retrievedIds = new ArrayList<>();

        while (retrievedIds.isEmpty()) {
            retrievedIds.addAll(retrieveIds(entityTypeName, connection));
        }

        for (long retrievedId : retrievedIds) {
            availableIds.put(retrievedId);
        }
    }


    /**
     * Fetch new IDs when all previously fetched IDs taken.
     * Updates the last ID state.
     *
     * @return list of available IDs for table.
     */
    private List<Long> retrieveIds(String entityTypeName, Connection connection) throws SQLException {
        long lastId = generatedIdState.getLastIdForEntity(entityTypeName);

        List<Long> retrievedIds = selectNextAvailableIds(entityTypeName, lastId, connection);

        logger.trace("Generated for {}: {}", entityTypeName, retrievedIds);

        if (retrievedIds.isEmpty()) {
            generatedIdState.setLastIdForEntity(entityTypeName, lastId + fetchSize);
        } else {

            generatedIdState.setLastIdForEntity(entityTypeName, Collections.max(retrievedIds));
        }
        return retrievedIds;
    }

    /**
     * This inserts a list of IDs ignoring duplicates
     * @param tableName Entity name
     * @param list List of IDs
     * @param connection Connection to use
     */
    private void insertIdsIgnoreDuplicates(String tableName, Set<Long> list, Connection connection) throws SQLException {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("No IDs to insert");
        }

        String sql = "INSERT INTO id_generator(table_name, id_value) " +
                "SELECT ?, ids.id_value FROM unnest(?) AS ids(id_value) " +
                "ON CONFLICT DO NOTHING";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Array idArray = connection.createArrayOf("bigint", list.toArray(new Long[0]));
            try {
                statement.setString(1, tableName);
                statement.setArray(2, idArray);
                int result = statement.executeUpdate();
                logger.trace("Inserted {} ids for {}", result, tableName);
            } finally {
                idArray.free();
            }
        }
    }

    /**
     * Use SQL command genererate_series to be able to find new IDs, without selecting the ones already used.
     * @param tableName The name of the entity.
     * @param lastId The last ID to avoid selecting the same window each time
     * @param connection The connection to use
     * @return A list of fresh IDs to use
     */
    private List<Long> selectNextAvailableIds(String tableName, long lastId, Connection connection) throws SQLException {
        logger.debug("Will fetch new IDs from id_generator table for {}, lastId: {}", tableName, lastId);

        long lastIdPlusOne = lastId + 1;
        long upperBound = lastId + fetchSize;

        String sql = "SELECT * FROM generate_series(?::bigint, ?::bigint) " +
                "EXCEPT (SELECT id_value FROM id_generator WHERE table_name = ?)";

        List<Long> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lastIdPlusOne);
            statement.setLong(2, upperBound);
            statement.setString(3, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(resultSet.getLong(1));
                }
            }
        }

        logger.trace("Got generated values: {}", results);

        Collections.sort(results);
        return results;
    }

    /**
     * Persist claimed IDs to the helper table.
     * Executed in a hazelcast lock, to avoid having multiple instances persisting at the same time.
     * This method is intended to run in a background thread once in a while.
     */
    public void persistClaimedIds() {

        logger.trace("Start to persist claimed IDs if any");
        AtomicInteger persisted = new AtomicInteger();
        generatedIdState.getRegisteredEntityNames().forEach(entityTypeName -> {
            final FencedLock lock = hazelcastInstance.getCPSubsystem().getLock(entityLockString(entityTypeName));
            boolean gotLock;
            final int secondsToWait = 4;

            gotLock = lock.tryLock(secondsToWait, TimeUnit.SECONDS);


            if(gotLock) {
                try {
                    executeInTransaction(connection -> {
                        ISet<Long> claimedIds = generatedIdState.getClaimedIdListForEntity(entityTypeName);
                        if(!claimedIds.isEmpty()) {
                            logger.info("About to write {} claimed IDs to db for {}", claimedIds.size(), entityTypeName);
                            insertIdsIgnoreDuplicates(entityTypeName, claimedIds, connection);
                            persisted.addAndGet(claimedIds.size());
                            claimedIds.destroy();
                        } else {
                            logger.debug("No claimed IDs to insert for {}", entityTypeName);
                        }
                    });
                } catch (Exception e) {
                  logger.warn("Error writing claimed IDs for {} in transaction", entityTypeName, e);
                } finally {
                    lock.unlock();
                }
            }
        });
        if(persisted.get() > 0 ) {
            logger.info("Persisted {} claimed IDs", persisted);
        }
    }

    @FunctionalInterface
    private interface ConnectionWork {
        void execute(Connection connection) throws SQLException, InterruptedException;
    }

    /**
     * Helper method to execute work in a transaction on a connection from the dedicated ID generator pool.
     * The connection is committed on success, rolled back on failure, and always returned to the pool.
     * @param work The work to run inside the transaction
     */
    private void executeInTransaction(ConnectionWork work) {
        try (Connection connection = connectionPool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.execute(connection);
                connection.commit();
            } catch (Exception e) {
                rollbackAndThrow(connection, e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to rollback transaction if possible
     * @param connection The connection holding the transaction to rollback
     * @param e The exception to throw
     */
    private void rollbackAndThrow(Connection connection, Exception e) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            e.addSuppressed(rollbackException);
        }
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        if (e instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException(e);
    }

    /**
     * Helper method to generate the entiry lock string
     * @param entityTypeName the entity name to use, for concurrency needs
     * @return The entity lock string with prefix
     */
    public static String entityLockString(String entityTypeName) {
        return REENTRANT_LOCK_PREFIX + entityTypeName;
    }

}
