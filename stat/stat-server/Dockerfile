FROM amazoncorretto:21
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY target/*.jar stat-server.jar
ENTRYPOINT ["java", "-jar", "/stat-server.jar"]