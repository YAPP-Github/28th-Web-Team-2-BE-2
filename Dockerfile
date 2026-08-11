FROM amazoncorretto:25-al2023-headless@sha256:58e0c32b04bd716e680a51b947f1faf254bace4154b5b03f508e8fffe8dc4a82

WORKDIR /app
COPY --chown=10001:10001 api/build/libs/app.jar /app/app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
