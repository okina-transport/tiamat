FROM openjdk:11-jre
USER root
RUN apt-get install -y locales
RUN locale-gen fr_FR.UTF-8
ENV LANG='fr_FR.UTF-8' LANGUAGE='fr_FR:fr' LC_ALL='fr_FR.UTF-8'

ADD target/tiamat-*-SNAPSHOT.jar tiamat.jar


EXPOSE 8777
CMD java $JAVA_OPTIONS -jar /tiamat.jar