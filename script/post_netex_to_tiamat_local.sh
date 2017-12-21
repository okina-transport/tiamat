#!/usr/bin/env bash

#GET TOKEN
RESULT=`curl --data "grant_type=password&client_id=Tiamat&username=$1&password=$2" https://auth.okina.fr/auth/realms/rutebanken/protocol/openid-connect/token`;
TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`;
echo ${TOKEN};

# TOPO Places EPCI
curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@"/home/mhicauber/dev/workspaces/NA/netex-utils/kml_to_netex_topo_places/naq_topo_epci.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL" > ../../topographical_places/netex_topo_places.xml


####################################
# COMMUNAUTE AGGLO

#TULLES
curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@"/home/mhicauber/referentials/ch_53/data/1282/export_netexprofile_1282/_TUT_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:241927201"
#
##Grand Poitiers
curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@"/home/mhicauber/referentials/ch_56/data/1309/_GPO_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:200069854"
#
##Bordeaux Métropole Tram
curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@"/home/mhicauber/referentials/ch_54/data/1303/_BME_shared_data_tram.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_TRAM&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:243300316"
#
##Bordeaux Métropole Bus
curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@"/home/mhicauber/referentials/ch_54/data/1306/_BME_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL&forceStopType=ONSTREET_BUS&skipOutput=true&targetTopographicPlaces=OKI:TopographicPlace:243300316"
#
#
#####################################
## DEPARTEMENTS
#
##Transgironde
#curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@"/home/mhicauber/referentials/ch_55/data/1307/_TRA_shared_data.xml"  "http://localhost:8585/services/stop_places/netex?importType=MERGE&forceStopType=ONSTREET_BUS&skipOutput=true"
#
#
#
#
