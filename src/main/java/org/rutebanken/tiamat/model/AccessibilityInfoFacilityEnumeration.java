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

package org.rutebanken.tiamat.model;

public enum AccessibilityInfoFacilityEnumeration {

    AUDIO_INFORMATION("audioInformation"),
    AUDIO_FOR_HEARING_IMPAIRED("audioForHearingImpaired"),
    VISUAL_DISPLAYS("visualDisplays"),
    DISPLAYS_FOR_VISUALLY_IMPAIRED("displaysForVisuallyImpaired"),
    LARGE_PRINT_TIMETABLES("largePrintTimetables"),
    OTHER("other");
    private final String value;

    AccessibilityInfoFacilityEnumeration(String v) {
        value = v;
    }

    public static AccessibilityInfoFacilityEnumeration fromValue(String v) {
        for (AccessibilityInfoFacilityEnumeration c : AccessibilityInfoFacilityEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public String value() {
        return value;
    }

}
