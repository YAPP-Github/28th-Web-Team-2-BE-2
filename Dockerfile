FROM eclipse-temurin:25-jre-jammy

WORKDIR /app
COPY --chown=10001:10001 api/build/libs/app.jar /app/app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
