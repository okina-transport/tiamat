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

package org.rutebanken.tiamat.netex.id;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.PathLinkEnd;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.SiteFrame;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.identification.IdentifiedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class NetexIdHelper {

    // TODO: make it configurable, maybe in ValidPrefixList
    public static final String NSR = "NSR";
    public static final String CITY_INSEE_CODE_RE = "\\d{5}(-\\d{1,2})?";
    public static final String PARKING_ID_RE = String.format("FR[^:]*:%s:Parking:[^:]+(:LOC|:NAP)?", CITY_INSEE_CODE_RE);
    public static final Pattern PARKING_ID_PATTERN = Pattern.compile(PARKING_ID_RE);

    public static final String GENERIC_NETEX_PATTERN = "[^:]+:%s:(?:[^:]+:)*[^:]+(:LOC)?";

    public static final String PARKING_PAN_ID_RE = CITY_INSEE_CODE_RE + "-P-[^:-]+";
    public static final Pattern PARKING_PAN_ID_PATTERN = Pattern.compile(PARKING_PAN_ID_RE);
    private static final Logger logger = LoggerFactory.getLogger(NetexIdHelper.class);
    private static final Pattern NETEX_ID_PATTERN = Pattern.compile("\\w{3}:\\w{3,}:\\w+");
    private final ValidPrefixList validPrefixList;

    @Autowired
    public NetexIdHelper(ValidPrefixList validPrefixList) {
        this.validPrefixList = validPrefixList;
    }

    public static boolean isNetexId(String string) {
        return NETEX_ID_PATTERN.matcher(string).matches();
    }

    public static String determineIdType(IdentifiedEntity identifiedEntity) {
        if (identifiedEntity instanceof StopPlace) {
            return "StopPlace";
        } else if (identifiedEntity instanceof Quay) {
            return "Quay";
        } else if (identifiedEntity instanceof SiteFrame) {
            return "SiteFrame";
        } else if (identifiedEntity instanceof PathLinkEnd) {
            return "PathLinkEnd";
        } else {
            return identifiedEntity.getClass().getSimpleName();
        }
    }

    public static boolean isParkingNetexId(String netexId) {
        return PARKING_ID_PATTERN.matcher(netexId).matches();
    }

    public static boolean isNetexIdOfType(String idToCheck, String netexType){
        String genericPatternWithType = String.format(GENERIC_NETEX_PATTERN, netexType);
        Pattern patternToCheck = Pattern.compile(genericPatternWithType);
        return patternToCheck.matcher(idToCheck).matches();
    }

    /**
     * @param panParkingId parking ID from PAN, format is {insee}-P-{id}
     * @return FR:{insee}:Parking:{id}:NAP
     */
    public static String panParkingIdToNetexParkingId(String panParkingId) {
        var split = panParkingId.split(Pattern.quote("-P-"));
        String insee = split[0];
        String id = split[1];
        return String.format("FR:%s:Parking:%s:NAP", insee, id);
    }

    public static String otherParkingIdToNetexParkingId(String parkingOriginalId, String parkingInsee) {
        return String.format("FR:%s:Parking:%s:LOC", parkingInsee, parkingOriginalId);
    }

    public String getNetexId(String type, long id) {
        return validPrefixList.getValidNetexPrefix() + ":" + type + ":" + id;
    }

    public String getNetexId(IdentifiedEntity identifiedEntity, long id) {
        String type = determineIdType(identifiedEntity);
        return getNetexId(type, id);
    }

    public boolean isNsrId(String netexId) {
        if (!netexId.contains(validPrefixList.getValidNetexPrefix())) {
            logger.debug("The netexId: {} does not start with {}", netexId, validPrefixList.getValidNetexPrefix());
            return false;
        }

        if (StringUtils.countMatches(netexId, ":") != 2) {
            logger.warn("Expected number of colons is two. {}", netexId);
            return false;
        }

        String[] splitId = StringUtils.split(netexId, ":");
        try {
            Long.valueOf(splitId[2]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param netexId Id with long value after last colon.
     * @return long value
     */
    public long extractIdPostfixNumeric(String netexId) {
        try {
            return Long.valueOf(extractIdPostfix(netexId));
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Cannot parse NeTEx ID postfix into numeric valueID: '" + netexId + "'");
        }
    }

    public String extractIdPostfix(String netexId) {
        return netexId.substring(netexId.lastIndexOf(':') + 1).trim();
    }

    public String extractIdType(String netexId) {
        if (isParkingNetexId(netexId)) {
            return "Parking";
        } else if (isNetexIdOfType(netexId, "ParkingArea")) {
            return "ParkingArea";
        }
        try {
            return netexId.substring(netexId.indexOf(':') + 1, netexId.lastIndexOf(':'));
        } catch (StringIndexOutOfBoundsException e) {
            throw new StringIndexOutOfBoundsException("Cannot extract ID type for netexId: " + netexId);
        }
    }

    public String extractIdPrefix(String netexId) {
        if (isParkingNetexId(netexId)) {
            return netexId.split(":Parking:")[0];
        } else if (isNetexIdOfType(netexId, "ParkingArea")) {
            return netexId.split(":ParkingArea:")[0];
        }else if (isNetexIdOfType(netexId, "AccessibilityAssessment")) {
            return netexId.split(":AccessibilityAssessment:")[0];
        }else if (isNetexIdOfType(netexId, "PointOfInterest")){
            return netexId.split(":PointOfInterest:")[0];
        }
        if (StringUtils.countMatches(netexId, ":") != 2) {
            throw new IllegalArgumentException("Number of colons in ID is not two: " + netexId);
        }
        return netexId.substring(0, netexId.indexOf(':'));
    }

}
