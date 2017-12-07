#!/usr/bin/env bash

#GET TOKEN
RESULT=`curl --data "grant_type=password&client_id=Tiamat&username=$1&password=$2" https://auth.okina.fr/auth/realms/rutebanken/protocol/openid-connect/token`;
TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`;
echo ${TOKEN};

#POST
curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@$3  https://entur.okina.fr/services/stop_places/netex?importType=MERGE&forceStopType=TRAM_STATION