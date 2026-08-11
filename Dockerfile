FROM eclipse-temurin:25-jre-jammy@sha256:57f9a2046d0ff628c24c0c127bd9d37a57e372064b30fdade9c00683382c1ab3

WORKDIR /app
COPY --chown=10001:10001 api/build/libs/app.jar /app/app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
