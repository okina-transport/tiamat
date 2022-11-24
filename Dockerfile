FROM openjdk:11-jre
ADD target/tiamat-*-SNAPSHOT.jar tiamat.jar

ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

EXPOSE 8777
CMD java $JAVA_OPTIONS -jar /tiamat.jar