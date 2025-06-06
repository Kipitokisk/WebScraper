FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package

FROM openjdk:17-jdk-slim AS selenium

USER root
WORKDIR /app

RUN apt-get update && apt-get install -y \
    firefox-esr \
    wget \
    unzip \
    ca-certificates \
    libx11-xcb1 \
    libglib2.0-0 \
    libnss3 \
    libgdk-pixbuf2.0-0 \
    libxcomposite1 \
    libxrandr2 \
    libasound2 \
    libgtk-3-0 \
    fonts-liberation \
    libappindicator3-1 \
    xdg-utils \
    chromium \
    chromium-driver \
    --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

RUN CHROMEDRIVER_URL="https://storage.googleapis.com/chrome-for-testing-public/120.0.6099.109/linux64/chromedriver-linux64.zip" && \
    wget -O /tmp/chromedriver.zip "$CHROMEDRIVER_URL" && \
    unzip /tmp/chromedriver.zip -d /tmp/ && \
    mv /tmp/chromedriver-linux64/chromedriver /usr/local/bin/ && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/chromedriver*

RUN GECKO_DRIVER_VERSION=v0.32.0 && \
    wget -O /tmp/geckodriver.tar.gz https://github.com/mozilla/geckodriver/releases/download/${GECKO_DRIVER_VERSION}/geckodriver-${GECKO_DRIVER_VERSION}-linux64.tar.gz && \
    tar -xzf /tmp/geckodriver.tar.gz -C /tmp/ && \
    mv /tmp/geckodriver /usr/local/bin/ && \
    chmod +x /usr/local/bin/geckodriver && \
    rm /tmp/geckodriver.tar.gz

COPY --from=builder /app/target/WebScraper-1.0-SNAPSHOT.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]