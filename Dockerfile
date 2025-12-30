FROM eclipse-temurin:21.0.5_11-jre
ARG JAR_FILE


EXPOSE 8585

COPY ${JAR_FILE} tiamat.jar
CMD ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "tiamat.jar"]
