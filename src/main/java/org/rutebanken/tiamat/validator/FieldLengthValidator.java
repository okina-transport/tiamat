package org.rutebanken.tiamat.validator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.rutebanken.tiamat.general.FieldTooLongException;
import org.rutebanken.tiamat.general.ImportErrorLimit;
import org.rutebanken.tiamat.model.DataManagedObjectStructure;
import org.rutebanken.tiamat.model.EntityStructure;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks, before saving an entity graph, that no text value exceeds the size of its database column.
 * <p>
 * The graph is walked with the Hibernate mapping (entity, embedded, element collection and association attributes),
 * so that every persisted text field is checked, and the maximum lengths are read from the database schema itself.
 */
@Component
public class FieldLengthValidator {

    private static final Logger logger = LoggerFactory.getLogger(FieldLengthValidator.class);

    private static final String COLUMN_LENGTHS_QUERY = """
            SELECT table_name, column_name, character_maximum_length
            FROM information_schema.columns
            WHERE table_schema = current_schema() AND character_maximum_length IS NOT NULL
            """;

    /**
     * A text value longer than its database column
     *
     * @param entity    type of the identified entity holding the field (ex: Parking, ParkingArea)
     * @param entityIds candidate NeTEx ids of this entity: its netexId, then its "imported-id" key values
     *                  (the id of the imported file is one of them)
     * @param field     path of the field from this entity, ex: Name, PostalAddress/Street
     * @param length    length of the value, in characters
     * @param maxLength size of the database column
     */
    public record Violation(String entity, List<String> entityIds, String field, int length, int maxLength) {
    }

    private record Owner(String entity, List<String> entityIds) {
    }

    private final EntityManagerFactory entityManagerFactory;

    private volatile Map<String, Integer> columnMaxLengths;

    public FieldLengthValidator(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * @param root a new (not yet persisted) entity, ex: a Parking mapped from an imported file
     * @return the text values of the entity and of its embedded, collection and associated entities that exceed their column size
     */
    public List<Violation> validate(Object root) {
        List<Violation> violations = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visitEntity(root, null, "", visited, violations);
        return violations;
    }

    /**
     * Checks every entity of an imported file before any of them is saved
     *
     * @param entities new (not yet persisted) entities, may be null
     * @throws FieldTooLongException listing every text value exceeding its column size, so that the whole file is rejected
     */
    public void checkAll(Collection<?> entities) {
        if (entities == null) {
            return;
        }
        List<Violation> violations = new ArrayList<>();
        for (Object entity : entities) {
            violations.addAll(validate(entity));
            if (ImportErrorLimit.isExceeded(violations.size())) {
                // too many errors: the other entities are not checked
                break;
            }
        }
        if (!violations.isEmpty()) {
            throw new FieldTooLongException(violations);
        }
    }

    private void visitEntity(Object entity, Owner owner, String path, Set<Object> visited, List<Violation> violations) {
        if (entity == null || !Hibernate.isInitialized(entity) || !visited.add(entity)) {
            return;
        }
        EntityPersister persister = mappingMetamodel().findEntityDescriptor(Hibernate.getClass(entity));
        if (persister == null) {
            return;
        }
        Owner entityOwner = ownerOf(entity);
        Owner currentOwner = entityOwner != null ? entityOwner : owner;
        String currentPath = entityOwner != null ? "" : path;
        persister.forEachAttributeMapping(attribute ->
                visitAttribute(attribute, attribute.getValue(entity), currentOwner, currentPath, visited, violations));
    }

    private void visitAttribute(AttributeMapping attribute, Object value, Owner owner, String path, Set<Object> visited, List<Violation> violations) {
        if (value == null || !Hibernate.isInitialized(value)) {
            return;
        }
        String attributePath = path.isEmpty() ? attribute.getAttributeName() : path + "." + attribute.getAttributeName();
        if (attribute instanceof PluralAttributeMapping pluralAttribute) {
            if (value instanceof Map<?, ?> map) {
                map.forEach((key, element) -> {
                    visitPart(pluralAttribute.getIndexDescriptor(), key, owner, attributePath, visited, violations);
                    visitPart(pluralAttribute.getElementDescriptor(), element, owner, attributePath, visited, violations);
                });
            } else if (value instanceof Collection<?> collection) {
                collection.forEach(element ->
                        visitPart(pluralAttribute.getElementDescriptor(), element, owner, attributePath, visited, violations));
            }
        } else {
            visitPart(attribute, value, owner, attributePath, visited, violations);
        }
    }

    private void visitPart(ModelPart part, Object value, Owner owner, String path, Set<Object> visited, List<Violation> violations) {
        if (value == null) {
            return;
        }
        if (part instanceof EmbeddableValuedModelPart embeddable) {
            embeddable.getEmbeddableTypeDescriptor().forEachAttributeMapping(attribute ->
                    visitAttribute(attribute, attribute.getValue(value), owner, path, visited, violations));
        } else if (part instanceof EntityValuedModelPart) {
            visitEntity(value, owner, path, visited, violations);
        } else if (part instanceof SelectableMapping column && value instanceof String text) {
            checkLength(column, text, owner, path, violations);
        }
    }

    private void checkLength(SelectableMapping column, String text, Owner owner, String path, List<Violation> violations) {
        Integer maxLength = columnMaxLengths().get(columnKey(column.getContainingTableExpression(), column.getSelectionExpression()));
        if (maxLength == null) {
            return;
        }
        int length = text.codePointCount(0, text.length());
        if (length > maxLength) {
            violations.add(new Violation(
                    owner != null ? owner.entity() : null,
                    owner != null ? owner.entityIds() : List.of(),
                    toFieldName(path), length, maxLength));
        }
    }

    /**
     * An entity is reported with its NeTEx id: kept as netexId, or moved to the "imported-id" key values by the mapping
     */
    private static Owner ownerOf(Object entity) {
        List<String> ids = new ArrayList<>();
        if (entity instanceof EntityStructure entityStructure && entityStructure.getNetexId() != null) {
            ids.add(entityStructure.getNetexId());
        }
        if (entity instanceof DataManagedObjectStructure dataManagedObject && dataManagedObject.getKeyValues() != null) {
            Value originalIds = dataManagedObject.getKeyValues().get(NetexIdMapper.ORIGINAL_ID_KEY);
            if (originalIds != null && originalIds.getItems() != null) {
                originalIds.getItems().stream().filter(id -> !ids.contains(id)).forEach(ids::add);
            }
        }
        return ids.isEmpty() ? null : new Owner(Hibernate.getClass(entity).getSimpleName(), List.copyOf(ids));
    }

    /**
     * Converts a mapping attribute path to NeTEx element names: name.value -> Name, postalAddress.street -> PostalAddress/Street
     */
    static String toFieldName(String path) {
        List<String> segments = new ArrayList<>(Arrays.asList(path.split("\\.")));
        if (segments.size() > 1 && segments.getLast().equals("value")) {
            segments.removeLast();
        }
        return segments.stream().map(StringUtils::capitalize).collect(Collectors.joining("/"));
    }

    private MappingMetamodel mappingMetamodel() {
        return entityManagerFactory.unwrap(SessionFactoryImplementor.class).getMappingMetamodel();
    }

    private Map<String, Integer> columnMaxLengths() {
        Map<String, Integer> lengths = columnMaxLengths;
        if (lengths == null) {
            lengths = loadColumnMaxLengths();
            columnMaxLengths = lengths;
        }
        return lengths;
    }

    private Map<String, Integer> loadColumnMaxLengths() {
        Map<String, Integer> lengths = new HashMap<>();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.createNativeQuery(COLUMN_LENGTHS_QUERY).getResultList();
            for (Object[] row : rows) {
                lengths.put(columnKey((String) row[0], (String) row[1]), ((Number) row[2]).intValue());
            }
        } finally {
            entityManager.close();
        }
        logger.info("Loaded the maximum length of {} text columns", lengths.size());
        return Map.copyOf(lengths);
    }

    /**
     * Table and column names as found in the database: unquoted, without schema, lower case
     */
    private static String columnKey(String table, String column) {
        return normalize(table) + "." + normalize(column);
    }

    private static String normalize(String name) {
        String unqualified = name.substring(name.lastIndexOf('.') + 1);
        return StringUtils.strip(unqualified, "\"`").toLowerCase(Locale.ROOT);
    }
}
