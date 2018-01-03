#!/usr/bin/env bash

#GET TOKEN
RESULT=`curl --data "grant_type=password&client_id=Tiamat&username=$1&password=$2" https://auth.okina.fr/auth/realms/rutebanken/protocol/openid-connect/token`;
TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`;
echo "TOKEN : " ${TOKEN};

path_folder='/home/tgonzalez/share/export_naq/';

# TOPO Places EPCI
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"naq_topo_epci.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL"

####################################
# COMMUNAUTE AGGLO

##Grand Poitiers
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_VIT_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:200069854"
#
##Perigueux Agglo
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_PER_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:200040392"
#
##Landes/Couralin
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_COU_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:244000675"
#
##Landes/Yego
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_YEG_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true"
#
##Bordeaux Métropole Tram
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_CUB_shared_data_tram.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_TRAM&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:243300316"
##
##Bordeaux Métropole Bus
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_BME_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:243300316"
#
##TAC Châtellerault
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_CHL_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:248600413"
#
##Cognac
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_COG_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:200070514"
#
##Brive
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_BRI_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:200043172"
#
#
#####################################
#
##Aéroport LR
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_APR_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=MERGE&forceStopType=ONSTREET_BUS&skipOutput=true"
#
#Aix/Fouras
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_FAI_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=FERRY_STOP&skipOutput=true"
#
#BAC Royan
curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_BAC_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=MERGE&forceStopType=FERRY_STOP&skipOutput=true"
#
#
#####################################
## DEPARTEMENTS
#
##Transgironde
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_TRA_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=MERGE&forceStopType=ONSTREET_BUS&skipOutput=true"
#
##CG23 Creuse
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_CRE_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=MERGE&forceStopType=ONSTREET_BUS&skipOutput=true"
#
#Vienne
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_VIE_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=MERGE&forceStopType=ONSTREET_BUS&skipOutput=true"
#
#Landes/XLR
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_LAN_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=MERGE&forceStopType=ONSTREET_BUS&skipOutput=true"
#
#Limousin
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_LIM_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=MERGE&forceStopType=ONSTREET_BUS&skipOutput=true"
#
#Charente-maritime
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_CHA_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=MERGE&forceStopType=ONSTREET_BUS&skipOutput=true"
#
#####################################
#
# SNCF / Intercités
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@${path_folder}"_SNC_shared_data_intercites.xml"  "http://localhost:8585/services/stop_places/netex?importType=MERGE&forceStopType=RAIL_STATION&skipOutput=true"
#
#
#