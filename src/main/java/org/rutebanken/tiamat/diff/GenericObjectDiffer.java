package org.rutebanken.tiamat.diff;

import javassist.util.proxy.MethodHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GenericObjectDiffer {

    private static final int MAX_DEPTH = 10;

    private static final Logger logger = LoggerFactory.getLogger(GenericObjectDiffer.class);

    public List<Difference> compareObjects(Object oldObject, Object newObject, Set<String> identifiers, Set<String> ingoreFields, Set<Class> onlyEqualCheckTypes) throws IllegalAccessException {
        return compareObjects(null, oldObject, newObject, identifiers, ingoreFields, onlyEqualCheckTypes, 0);
    }

    public List<Difference> compareObjects(String property, Object oldObject, Object newObject, Set<String> identifiers, Set<String> ignoreFields, Set<Class> onlyEqualCheckTypes, int depth) throws IllegalAccessException {
        RecursiveStatus recursiveStatus = new RecursiveStatus();
        recursiveStatus.depth = depth;
        recursiveStatus.ignoreFields = ignoreFields;
        recursiveStatus.identifiers = identifiers;
        recursiveStatus.onlyDoEqualsCheck = onlyEqualCheckTypes;

        return compareObjects(property, oldObject, newObject, recursiveStatus);
    }


    public List<Difference> compareObjects(String property, Object oldObject, Object newObject, RecursiveStatus recursiveStatus) throws IllegalAccessException {

        List<Difference> differences = new ArrayList<>();

        if(recursiveStatus.depth > MAX_DEPTH) {
            logger.debug("Reached max depth of {}", MAX_DEPTH);
            return differences;
        }
        recursiveStatus.depth++;

        Class clazz = oldObject.getClass();

        Field[] fields = getAllFields(clazz, recursiveStatus.ignoreFields);

        if (property == null) {
            property = oldObject.getClass().getSimpleName();
        }

        for (Field field : fields) {

            try {

                if (field.getType().isAssignableFrom(MethodHandler.class)) {
                    logger.debug("Ignoring field {} as its assignable from {}", field, MethodHandler.class);
                    continue;
                }

                field.setAccessible(true);

                Object oldValue = field.get(oldObject);
                Object newValue = field.get(newObject);

                if (oldValue == null && newValue == null) {
                    continue;
                }

                if (oldValue == null && newValue != null || oldValue != null && newValue == null) {
                    differences.add(new Difference(property + '.' + field.getName(), oldValue, newValue));
                    continue;
                }

                if (Collection.class.isAssignableFrom(field.getType())) {
                    compareCollection(property + '.' + field.getName(), (Collection) oldValue, (Collection) newValue, differences, recursiveStatus);
                    continue;

                } else if (Map.class.isAssignableFrom(field.getType())) {
                    String mapPropertyName = property + "." + field.getName();
                    compareMap((Map) oldValue, (Map) newValue, differences, false, mapPropertyName, recursiveStatus);
                    continue;
                }

                if (oldValue == newValue) {
                    continue;
                }

                if (oldValue.equals(newValue)) {
                    continue;
                }

                String childProperty = property + '.' + field.getName();

                if (isPrimitive(oldValue)) {
                    differences.add(new Difference(childProperty, oldValue, newValue));
                } else if(recursiveStatus.onlyDoEqualsCheck.stream().anyMatch(type -> type.isAssignableFrom(oldValue.getClass()))) {
                    if(!oldValue.equals(newValue)) {
                        differences.add(new Difference(childProperty, oldValue, newValue));
                    }
                } else {
                    differences.addAll(compareObjects(property + '.' + field.getName(), oldValue, newValue, recursiveStatus));
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new RuntimeException("Could not compare property " + property
                        + ", field '" + field + ". ex:\""
                        + e.getMessage() + "\". old object "
                        + oldObject + " new object " + newObject, e);
            }
        }

        return differences;
    }

    public boolean isPrimitive(Object value) {
        return value instanceof Number || value instanceof String || value instanceof Boolean;
    }

    public void compareMap(Map<?, ?> map1, Map<?, ?> map2, List<Difference> differences, boolean reverse, String mapPropertyName, RecursiveStatus recursiveStatus) throws IllegalAccessException {

        Map<?, ?> leftMap;
        Map<?, ?> rightMap;

        if (reverse) {
            leftMap = map2;
            rightMap = map1;
        } else {
            leftMap = map1;
            rightMap = map2;
        }

        for (Object leftMapKey : leftMap.keySet()) {

            Object leftMapValue = leftMap.get(leftMapKey);

            if (!rightMap.containsKey(leftMapKey)) {
                logger.debug("right map does not contain key {}", leftMapKey);

                differences.add(new Difference(mapPropertyName + "{" + leftMapKey + "}", leftMapValue, null));

            } else if (rightMap.containsKey(leftMapKey)) {

                logger.debug("right map contain key {}", leftMapKey);

                String childProperty = mapPropertyName + "{" + leftMapKey + "}";
                differences.addAll(compareObjects(childProperty, leftMapValue, rightMap.get(leftMapKey), recursiveStatus));
            }
        }
    }

    public void compareCollection(final String propertyName, Collection oldCollection, Collection newCollection, List<Difference> differences, RecursiveStatus recursiveStatus) throws IllegalAccessException {

        if (oldCollection == null && newCollection == null) {
            return;
        }

        if (oldCollection == null && newCollection != null) {
            differences.add(new Difference(propertyName, null, newCollection.size()));
        } else if (oldCollection != null && newCollection == null) {
            differences.add(new Difference(propertyName, oldCollection.size(), null));
        } else if (oldCollection.isEmpty() && newCollection.isEmpty()) {
            return;
        } else  {
            Set<Object> ignoreIdentifiers = new HashSet<>();
            compareCollectionItems(propertyName, oldCollection, newCollection, differences, ignoreIdentifiers, false, recursiveStatus);
            compareCollectionItems(propertyName, newCollection, oldCollection, differences, ignoreIdentifiers, true, recursiveStatus);

        }
    }

    public void compareCollectionItems(String propertyName, Collection collectionLeft, Collection collectionRight, List<Difference> differences, Set<Object> ignoreIdentifiers, boolean reverse, RecursiveStatus recursiveStatus) throws IllegalAccessException {

        for (Object itemLeft : collectionLeft) {

            Object itemLeftIdentifier;
            // Get identifierField for left item.
            Field identifierField = identifierField(recursiveStatus.identifiers, getAllFields(itemLeft.getClass(), recursiveStatus.ignoreFields));
            if (identifierField != null) {

                itemLeftIdentifier = identifierField.get(itemLeft);
                if (ignoreIdentifiers.contains(itemLeftIdentifier)) {
                    continue;
                }

            } else {
                itemLeftIdentifier = null;
            }

            boolean foundMatchOnId = false;
            for (Object itemRight : collectionRight) {
                if (identifierField != null && itemLeftIdentifier != null) {
                    Object itemRightIdentifier = identifierField.get(itemRight);
                    if (itemLeftIdentifier.equals(itemRightIdentifier)) {

                        String newProperty = propertyName + "[" + itemRightIdentifier + "]";
                        ignoreIdentifiers.add(itemLeftIdentifier);
                        differences.addAll(compareObjects(newProperty, itemLeft, itemRight, recursiveStatus));
                        foundMatchOnId = true;
                        break;
                    }
                }
            }

            if (!foundMatchOnId) {
                if (reverse && !collectionRight.contains(itemLeft)) {
                    differences.add(new Difference(DiffType.COLLECTION_ADD, propertyName + "[]", null, itemLeft));
                    break;

                } else if (!collectionRight.contains(itemLeft)) {
                    differences.add(new Difference(DiffType.COLLECTION_REMOVE, propertyName + "[]", itemLeft, null));
                }
            }
        }

    }

    private Field[] getAllFields(Class clazz, Set<String> ignoreFields) {
        List<Field> fields = new ArrayList<>();
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        if (clazz.getSuperclass() != null) {
            fields.addAll(Arrays.asList(getAllFields(clazz.getSuperclass(), ignoreFields)));
        }
        return fields.stream()
                .filter(field -> !ignoreFields.contains(field.getName()))
                .collect(Collectors.toList()).toArray(new Field[]{});
    }

    public String diffListToString(List<Difference> differences) {
        return differences.stream().map(difference -> difference.toString()).collect(Collectors.joining("\n", "\n", "\n"));
    }

    private Field identifierField(Set<String> identifiers, Field[] fields) {
        return Stream.of(fields)
                .filter(field -> identifiers.contains(field.getName()))
                .peek(identifierField -> identifierField.setAccessible(true))
                .findFirst()
                .orElse(null);
    }

    private static class RecursiveStatus {

        /**
         * Current depth of recursive progression
         */
        public int depth;

        /**
         * Common field names to ignore for all objects
         */
        public Set<String> ignoreFields;


        /**
         * Fields to be treated as identifiers in collections (if they apply for type)
         */
        public Set<String> identifiers;

        /**
         * Do not compare these types recursively. Only check the equals method.
         */
        public Set<Class> onlyDoEqualsCheck;
    }
}
