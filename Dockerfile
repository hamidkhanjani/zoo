FROM eclipse-temurin:21-jre

WORKDIR /app

ARG JAR_FILE=target/zoo-eurail-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=spring

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
