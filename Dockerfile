FROM openjdk:11-jre
ARG JAR_FILE


EXPOSE 8585

COPY ${JAR_FILE} tiamat.jar
ENTRYPOINT ["java","-jar","tiamat.jar"]