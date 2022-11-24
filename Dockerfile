FROM openjdk:11-jre
# ### Locale support de_DE and timezone CET ###
USER root
RUN localedef -i fr_FR -f UTF-8 fr_FR.UTF-8
RUN echo "LANG=\"fr_FR.UTF-8\"" > /etc/locale.conf
RUN ln -s -f /usr/share/zoneinfo/CET /etc/localtime
USER jboss
ENV LANG fr_FR.UTF-8
ENV LANGUAGE fr_FR.UTF-8
ENV LC_ALL fr_FR.UTF-8
### Locale Support END ###

ADD target/tiamat-*-SNAPSHOT.jar tiamat.jar


EXPOSE 8777
CMD java $JAVA_OPTIONS -jar /tiamat.jar