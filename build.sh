#!/usr/bin/env bash

echo Building docker image

VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | sed -r "s/\x1B\[([0-9]{1,3}(;[0-9]{1,2};?)?)?[mGK]//g")

echo "version:${VERSION}"
echo "targetFile:target/tiamat-${VERSION}.jar"

#mvn spring-boot:build-image -Dspring-boot.build-image.imageName="${IMAGE_NAME}" -Dfile.encoding=UTF-8
docker build -t "${IMAGE_NAME}" --build-arg JAR_FILE=target/tiamat-"${VERSION}".jar .
docker push "${IMAGE_NAME}"
