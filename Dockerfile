FROM openjdk:11-jre
RUN localedef -i fr_FR -c -f UTF-8 -A /usr/share/locale/locale.alias fr_FR.UTF-8
ENV LANG fr_FR.utf8

ADD target/tiamat-*-SNAPSHOT.jar tiamat.jar


EXPOSE 8777
CMD java $JAVA_OPTIONS -jar /tiamat.jar