FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY target/*.jar stat-server.jar
ENTRYPOINT ["java", "-jar", "/stat-server.jar"]