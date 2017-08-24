package org.rutebanken.tiamat.netex.mapping.mapper;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.tiamat.importer.KeyValueListAppender;
import org.rutebanken.tiamat.model.DataManagedObjectStructure;
import org.rutebanken.tiamat.model.tag.Tag;
import org.rutebanken.tiamat.repository.TagRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TagKeyValuesMapperTest {

    private TagRepository tagRepository = mock(TagRepository.class);
    private TagKeyValuesMapper tagKeyValuesMapper = new TagKeyValuesMapper(tagRepository);

    @Test
    public void mapTagsToProperties() throws Exception {

        Tag tag = new Tag();
        tag.setCreated(Instant.now());
        tag.setName("name");
        tag.setCreatedBy("also me");
        tag.setIdreference("NSR:StopPlace:1");
        tag.setRemovedBy("me");
        tag.setRemoved(Instant.now());
        tag.setComment("comment");

        Set<Tag> tags = Sets.newHashSet(tag);
        when(tagRepository.findByIdReference("NSR:StopPlace:1")).thenReturn(tags);

        StopPlace stopPlace = new StopPlace();
        stopPlace.withKeyList(new KeyListStructure());
        tagKeyValuesMapper.mapTagsToProperties("NSR:StopPlace:1", stopPlace);



        Map<String, String> flattened = stopPlace.getKeyList().getKeyValue().stream().collect(Collectors.toMap(KeyValueStructure::getKey, KeyValueStructure::getValue));

        assertThat(flattened).containsKeys(
                "TAG-0-name",
                "TAG-0-createdBy",
                "TAG-0-created",
                "TAG-0-removed",
                "TAG-0-removedBy",
                "TAG-0-comment",
                "TAG-0-idReference");

        assertThat(flattened.get("TAG-0-idReference")).isEqualTo(tag.getIdReference());
    }


    @Test
    public void mapPropertiesToTag() throws Exception {

        KeyListStructure keyListStructure = new KeyListStructure();
        keyListStructure.getKeyValue().add(new KeyValueStructure().withKey("TAG-0-name").withValue("name"));
        keyListStructure.getKeyValue().add(new KeyValueStructure().withKey("TAG-0-created").withValue(String.valueOf(Instant.now().toEpochMilli())));
        keyListStructure.getKeyValue().add(new KeyValueStructure().withKey("TAG-0-idReference").withValue("NSR:StopPlace:1"));
        keyListStructure.getKeyValue().add(new KeyValueStructure().withKey("TAG-1-name").withValue("name 2"));
        keyListStructure.getKeyValue().add(new KeyValueStructure().withKey("TAG-1-idReference").withValue("NSR:StopPlace:2"));

        Set<Tag> tags = tagKeyValuesMapper.mapPropertiesToTag(keyListStructure);

        assertThat(tags).hasSize(2);

    }

    @Test
    public void mapForthAndBack() throws Exception {
        Tag tag = new Tag();
        tag.setCreated(Instant.now());
        tag.setName("name");
        tag.setCreatedBy("also me");
        tag.setIdreference("NSR:StopPlace:1");
        tag.setRemovedBy("me");
        tag.setRemoved(Instant.now());
        tag.setComment("comment");

        Set<Tag> tags = Sets.newHashSet(tag);
        when(tagRepository.findByIdReference("NSR:StopPlace:1")).thenReturn(tags);

        StopPlace stopPlace = new StopPlace();
        stopPlace.withKeyList(new KeyListStructure());
        tagKeyValuesMapper.mapTagsToProperties("NSR:StopPlace:1", stopPlace);


        Set<Tag> actual = tagKeyValuesMapper.mapPropertiesToTag(stopPlace.getKeyList());

        assertThat(actual.iterator().next()).isEqualTo(tags.iterator().next());
    }

}