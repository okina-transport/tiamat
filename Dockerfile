FROM openjdk:11-jre
ADD target/tiamat-*-SNAPSHOT.jar tiamat.jar


RUN apt-get update && apt-get install -y locales


RUN echo "fr_FR.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen fr_FR.UTF-8 && \
    dpkg-reconfigure locales && \
    /usr/sbin/update-locale LANG=fr_FR.UTF-8

ENV LC_ALL fr_FR.UTF-8

EXPOSE 8777
CMD java $JAVA_OPTIONS -jar /tiamat.jar