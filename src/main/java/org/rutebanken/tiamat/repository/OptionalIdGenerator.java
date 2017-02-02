package org.rutebanken.tiamat.repository;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.SequenceGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.SQLQueryImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.rutebanken.tiamat.model.EntityStructure;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * Generate ID if not already set.
 * If already set, it would use the same ID again!
 */
public class OptionalIdGenerator extends SequenceStyleGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OptionalIdGenerator.class);

    @Override
    public Serializable generate(SessionImplementor session, Object obj) throws HibernateException {

        if (obj == null) throw new HibernateException(new NullPointerException());

        if(obj instanceof EntityStructure) {
            EntityStructure entityStructure = (EntityStructure) obj;

            logger.trace("{} is instance of entity structre", entityStructure);

            if((entityStructure instanceof StopPlace || entityStructure instanceof Quay)) {
                long desiredId;
                if(entityStructure.getId() == null) {
                    desiredId = 0L;
                } else {
                    desiredId = entityStructure.getId();
                }

                logger.trace("About to retrieve or set id for: {}", entityStructure);

                SessionImpl sessionImpl = (SessionImpl) session;
                String tableName = ((AbstractEntityPersister) (sessionImpl).getSessionFactory().getAllClassMetadata().get(obj.getClass().getCanonicalName())).getTableName();

                String sql = "SELECT generate_next_available_id('" + tableName + "', " + desiredId + ")";

                SQLQuery sqlQuery = sessionImpl.createSQLQuery(sql);
                List list = sqlQuery.list();
                if (list.size() > 0) {
                    long newId = ((Integer) list.get(0)).longValue();
                    logger.trace("Got ID: {} for {}", newId, entityStructure);
                    return newId;
                }

                logger.warn("Could not get next available ID for table_name {}", tableName);
            }
        }
        Serializable id = super.generate(session, obj);
        logger.trace("Generated id {}", id);
        return id;
    }
}