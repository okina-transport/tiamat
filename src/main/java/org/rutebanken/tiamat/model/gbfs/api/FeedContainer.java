package org.rutebanken.tiamat.model.gbfs.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class FeedContainer {
    private Map<String, FeedData> data;

    @JsonAnySetter
    public void setData(String key, FeedData value) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(key, value);
    }

    public FeedData getFeedData() {
        return data.values().stream().findFirst().orElse(null);
    }

}
