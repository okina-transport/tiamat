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

package org.rutebanken.tiamat.importer.modifier;

import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuayDescriptionPlatformCodeExtractor {

    private static final Logger logger = LoggerFactory.getLogger(QuayDescriptionPlatformCodeExtractor.class);

    private static final int FALLBACK_DESCRIPTION_GROUP = 1;

    private static final int PLATFORM_GROUP = 3;

    private static final int DESCRIPTION_GROUP = 4;

    private final Pattern pattern;

    @Autowired
    public QuayDescriptionPlatformCodeExtractor(@Value("${QuayDescriptionPlatformCodeExtractor.terms:hpl,spor,plattform,pl,plf}") String[] terms) {
        String termsPart = String.join("|", terms);
        pattern = Pattern.compile("(.*)?(" + termsPart + "\\.?)\\s([\\d\\w]+)[\\s\\-.]*(.*)", Pattern.CASE_INSENSITIVE);
        logger.info("Terms: {}. Pattern: {}", terms, pattern);
    }

    public QuayDescriptionPlatformCodeExtractor() {
        this(new String[]{"hpl", "spor", "plattform", "pl", "plf"});
    }


    public StopPlace extractPlatformCodes(StopPlace stopPlace) {
        if(stopPlace.getQuays() != null) {
            stopPlace.getQuays().forEach(q -> extractPlatformCode(q));
        }
        return stopPlace;
    }

    public void extractPlatformCode(Quay quay) {

        if (quay.getDescription() == null) {
            logger.debug("Description is not set for Quay {}, ignoring.", quay);
            return;
        } else if (quay.getName() != null) {
            if (!quay.getName().getValue().isEmpty() && !quay.getName().getValue().equals(quay.getDescription().getValue())) {
                logger.info("Ignoring quay as it does already have a name, and it is not equal to it's description: '{}', {}",
                        quay.getDescription().getValue(), quay);
                return;
            }
        }

        final String description = quay.getDescription().getValue();

        Matcher matcher = pattern.matcher(description);

        if (matcher.matches()) {

            debugLog(matcher);

            if (matcher.groupCount() >= PLATFORM_GROUP) {
                String platformCode = matcher.group(PLATFORM_GROUP);
                logger.info("Setting public code to '{}': {}", platformCode, quay);
                quay.setPublicCode(platformCode);
                setQuayDescriptionFromMatcher(matcher, quay);
            } else {
                logger.info("Description: '{}' matches but group count is not as expected. ", description);
            }

        } else {
            logger.debug("No match in description {}", description);
        }
    }

    private void setQuayDescriptionFromMatcher(Matcher matcher, Quay quay) {
        if (matcher.groupCount() == DESCRIPTION_GROUP) {
            String newDescription = matcher.group(DESCRIPTION_GROUP).trim();
            if (newDescription.isEmpty()) {
                String descriptionBeforePlatformCode = matcher.group(FALLBACK_DESCRIPTION_GROUP).trim();
                if(!descriptionBeforePlatformCode.isEmpty()) {
                    setQuayDescription(descriptionBeforePlatformCode, quay);
                } else {
                    quay.setDescription(null);
                }

            } else {
                setQuayDescription(newDescription, quay);
            }

        } else {
            quay.setDescription(null);
        }
    }

    private void setQuayDescription(String newDescription, Quay quay) {
        logger.info("Setting quay description to '{}' for quay {}", newDescription, quay);
        quay.setDescription(new EmbeddableMultilingualString(newDescription));
    }

    private void debugLog(Matcher matcher) {
        if (logger.isDebugEnabled()) {
            for (int groupId = 0; groupId <= matcher.groupCount(); groupId++) {
                logger.debug("Group ID: {}: {}", groupId, matcher.group(groupId));
            }
        }
    }
}
