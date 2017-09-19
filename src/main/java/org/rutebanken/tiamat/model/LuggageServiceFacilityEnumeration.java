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

public enum LuggageServiceFacilityEnumeration {

    OTHER("other"),
    LEFT_LUGGAGE("leftLuggage"),
    PORTERAGE("porterage"),
    FREE_TROLLEYS("freeTrolleys"),
    PAID_TROLLEYS("paidTrolleys"),
    COLLECT_AND_DELIVER_TO_STATION("collectAndDeliverToStation"),
    BAGGAGE_CHECK_IN_CHECK_OUT("baggageCheckInCheckOut");
    private final String value;

    LuggageServiceFacilityEnumeration(String v) {
        value = v;
    }

    public static LuggageServiceFacilityEnumeration fromValue(String v) {
        for (LuggageServiceFacilityEnumeration c : LuggageServiceFacilityEnumeration.values()) {
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
