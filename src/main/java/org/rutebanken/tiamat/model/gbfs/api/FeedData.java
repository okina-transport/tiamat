package org.rutebanken.tiamat.model.gbfs.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.rutebanken.tiamat.model.gbfs.GbfsFeedItem;

import java.util.List;

public class FeedData {
    @JsonProperty("feeds")
    private List<GbfsFeedItem> feeds;

    public List<GbfsFeedItem> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<GbfsFeedItem> feeds) {
        this.feeds = feeds;
    }
}
