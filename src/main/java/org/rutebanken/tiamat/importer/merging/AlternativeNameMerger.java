package org.rutebanken.tiamat.importer.merging;

import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.model.AlternativeName;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

@Component
public class AlternativeNameMerger {

    private static final String TTS_STOP_NAME_KEY = "Libellé de la synthèse vocale";

    public boolean updateSiteElementAlternativeName(org.rutebanken.tiamat.model.SiteElement incomingElement, org.rutebanken.tiamat.model.SiteElement copy) {
        Optional<String> incomingAltName = getAlternativeName(incomingElement);
        Optional<String> existingAltName = getAlternativeName(copy);
        boolean existingAltNameSameValue = incomingAltName.isPresent() && existingAltName.isPresent()
                && Objects.equals(incomingAltName.get(), existingAltName.get());
        boolean noAltName = incomingAltName.isEmpty() && existingAltName.isEmpty();
        boolean alternativeNameChanged = !(existingAltNameSameValue || noAltName);
        if (alternativeNameChanged) {
            Optional<AlternativeName> newAlternativeName = buildAlternativeName(incomingAltName);
            updateAlternativeName(copy, newAlternativeName);
        }
        return alternativeNameChanged;
    }

    private Optional<String> getAlternativeName(org.rutebanken.tiamat.model.SiteElement incomingElement) {
        Optional<String> alternativeName = Optional.empty();
        if (CollectionUtils.isNotEmpty(incomingElement.getAlternativeNames())) {
            Optional<AlternativeName> matchingElement = incomingElement.getAlternativeNames().stream()
                    .filter(altName -> TTS_STOP_NAME_KEY.equals(altName.getTypeOfName()))
                    .findFirst();
            if (matchingElement.isPresent()) {
                alternativeName = Optional.of(matchingElement.get().getName().getValue());
            }
        }
        return alternativeName;
    }

    private void updateAlternativeName(org.rutebanken.tiamat.model.SiteElement target, Optional<AlternativeName> alternativeName) {
        if (alternativeName.isPresent()) {
            if (CollectionUtils.isNotEmpty(target.getAlternativeNames())) {
                removeExistingAlternativeName(target);
                target.getAlternativeNames().add(alternativeName.get());
            } else {
                target.getAlternativeNames().add(alternativeName.get());
            }
        } else if (CollectionUtils.isNotEmpty(target.getAlternativeNames())) {
            removeExistingAlternativeName(target);
        }
    }

    private void removeExistingAlternativeName(org.rutebanken.tiamat.model.SiteElement target) {
        Iterator<AlternativeName> iterator = target.getAlternativeNames().iterator();
        AlternativeName altName;
        while (iterator.hasNext()) {
            altName = iterator.next();
            if (TTS_STOP_NAME_KEY.equals(altName.getTypeOfName())) {
                iterator.remove();
            }
        }
    }

    private Optional<AlternativeName> buildAlternativeName(Optional<String> incomingAltName) {
        Optional<AlternativeName> newAlternativeName = Optional.empty();
        if (incomingAltName.isPresent()) {
            AlternativeName alternativeName = new AlternativeName();
            EmbeddableMultilingualString alternativeNameValue = new EmbeddableMultilingualString();
            alternativeNameValue.setValue(incomingAltName.get());
            alternativeName.setName(alternativeNameValue);
            alternativeName.setTypeOfName(TTS_STOP_NAME_KEY);
            newAlternativeName = Optional.of(alternativeName);
        }
        return newAlternativeName;
    }
}
