FROM openjdk:21
WORKDIR /app

COPY build/libs/*.jar navik-backend-crawler.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/navik-backend-crawler.jar", "--spring.profiles.active=dev"]