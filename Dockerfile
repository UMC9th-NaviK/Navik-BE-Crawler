FROM openjdk:21-jdk-slim
WORKDIR /app

# 크롬
RUN apt-get update && apt-get install -y wget gnupg curl \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list \
    && apt-get update && apt-get install -y google-chrome-stable \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

COPY build/libs/*.jar navik-backend-crawler.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "navik-backend-crawler.jar", "--spring.profiles.active=prod"]