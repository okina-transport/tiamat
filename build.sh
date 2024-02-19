#!/usr/bin/env bash

echo Building docker image

# Back
VERSION=$(mvn -q \
    -Dexec.executable=echo \
    -Dexec.args='${project.version}' \
    --non-recursive \
    exec:exec)
IMAGE_NAME=registry.okina.fr/mosaic/tiamat:"${VERSION}"

echo version:${VERSION}
echo targetFile:target/tiamat-${VERSION}.jar

#mvn spring-boot:build-image -Dspring-boot.build-image.imageName="${IMAGE_NAME}" -Dfile.encoding=UTF-8
docker build -t "${IMAGE_NAME}" --build-arg JAR_FILE=target/tiamat-${VERSION}.jar .
docker push "${IMAGE_NAME}"
