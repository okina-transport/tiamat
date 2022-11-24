FROM openjdk:11-jre
ADD target/tiamat-*-SNAPSHOT.jar tiamat.jar


RUN apt-get update && apt-get install -y locales


RUN export LC_ALL=fr_FR.UTF-8
RUN export LANG=fr_FR.UTF-8
RUN locale-gen fr_FR.UTF-8

EXPOSE 8777
CMD java $JAVA_OPTIONS -jar /tiamat.jar