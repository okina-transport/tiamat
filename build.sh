#!/usr/bin/env bash

echo Building docker image

# Back
VERSION=$(mvn -q \
    -Dexec.executable=echo \
    -Dexec.args='${project.version}' \
    --non-recursive \
    exec:exec)
IMAGE_NAME=registry.okina.fr/mobiiti/tiamat:"${VERSION}"

mvn spring-boot:build-image -Dspring-boot.build-image.imageName="${IMAGE_NAME}"
docker push "${IMAGE_NAME}"
