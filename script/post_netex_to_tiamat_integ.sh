#!/usr/bin/env bash

#GET TOKEN
RESULT=`curl --data "grant_type=password&client_id=Tiamat&username=okina&password=5o7u6DDv2xYF" https://auth.okina.fr/auth/realms/rutebanken/protocol/openid-connect/token`;
TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`;
echo ${TOKEN};

#POST
curl -XPOST -H"Content-Type: application/xml" -H"authorization: bearer $TOKEN" -d@$1  https://entur.okina.fr/services/stop_places/netex?importType=INITIAL