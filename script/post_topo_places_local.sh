#!/usr/bin/env bash

#GET TOKEN
RESULT=`curl --data "grant_type=password&client_id=Tiamat&username=$1&password=$2" https://auth.okina.fr/auth/realms/rutebanken/protocol/openid-connect/token`;
TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`;
echo ${TOKEN};

#TULLES
curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@"/home/mhicauber/dev/workspaces/NA/topographical_places/naq_topo_epci.xml"  "http://localhost:8585/services/stop_places/netex?importType=INITIAL" > ../../topographical_places/netex_topo_places.xml






