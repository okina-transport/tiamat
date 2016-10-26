package org.rutebanken.tiamat.importers;

import org.rutebanken.tiamat.model.DataManagedObjectStructure;
import org.rutebanken.tiamat.model.KeyListStructure;
import org.rutebanken.tiamat.model.KeyValueStructure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Append Ids in KeyValue to a comma separated string for a data mangaged object.
 */
@Component
public class KeyValueAppender {
    public boolean appendToOriginalId(String key, DataManagedObjectStructure newObject, DataManagedObjectStructure existingObject) {

        KeyValueStructure existingKeyValue = getOrCreateKeyValue(key, existingObject);
        KeyValueStructure newKeyValue = getOrCreateKeyValue(key, newObject);

        List<String> existingIds = split(existingKeyValue);
        List<String> newIds = split(newKeyValue);

        addNew(newIds, existingIds);
        String newValue = String.join(",", existingIds);

        boolean changed = !newValue.equals(existingKeyValue.getValue());

        existingKeyValue.setValue(newValue);
        return changed;
    }

    public void addNew(List<String> newIds, List<String> existingIds) {
        for (String newId : newIds) {
            if (!existingIds.contains(newId)) {
                existingIds.add(newId);
            }
        }
    }

    public List<String> split(KeyValueStructure keyValue) {
        if (keyValue != null && keyValue.getValue() != null && !keyValue.getValue().isEmpty()) {
            return new ArrayList<>(Arrays.asList(keyValue.getValue().split(",")));
        }
        return new ArrayList<>();
    }

    public KeyValueStructure getOrCreateKeyValue(String key, DataManagedObjectStructure dataObject) {
        KeyListStructure keyList = dataObject.getKeyList();

        if(keyList == null) {
            keyList = new KeyListStructure();
            dataObject.setKeyList(keyList);
        }

        List<KeyValueStructure> keyValues = keyList.getKeyValue();
        for (KeyValueStructure keyValueStructure : keyValues) {
            if (keyValueStructure.getKey().equals(key)) {
                return keyValueStructure;
            }
        }

        KeyValueStructure keyValueStructure = new KeyValueStructure(key, "");
        dataObject.getKeyList().getKeyValue().add(keyValueStructure);

        keyList.getKeyValue().add(keyValueStructure);
        return keyValueStructure;

    }
}
