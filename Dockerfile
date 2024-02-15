FROM eclipse-temurin:17.0.9_9-jdk-jammy
ARG JAR_FILE


EXPOSE 8585

COPY ${JAR_FILE} tiamat.jar
CMD ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "tiamat.jar"]
