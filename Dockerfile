FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package

FROM openjdk:17-jdk-slim AS final
WORKDIR /app

COPY --from=builder /app/target/WebScraper-1.0-SNAPSHOT.jar /app/app.jar

FROM openjdk:17-jdk-slim AS selenium
USER root
WORKDIR /app

RUN apt-get update && apt-get install -y \
    firefox-esr \
    wget \
    ca-certificates \
    libx11-xcb1 \
    libglib2.0-0 \
    libnss3 \
    libgdk-pixbuf2.0-0 \
    libxcomposite1 \
    libxrandr2 \
    libasound2 \
    libgtk-3-0 \
    && rm -rf /var/lib/apt/lists/*

RUN GECKO_DRIVER_VERSION=v0.32.0 && \
    wget https://github.com/mozilla/geckodriver/releases/download/${GECKO_DRIVER_VERSION}/geckodriver-${GECKO_DRIVER_VERSION}-linux64.tar.gz && \
    tar -xvzf geckodriver-${GECKO_DRIVER_VERSION}-linux64.tar.gz && \
    mv geckodriver /usr/local/bin/ && \
    rm geckodriver-${GECKO_DRIVER_VERSION}-linux64.tar.gz

ENV FIREFOX_BIN=/usr/bin/firefox-esr

COPY --from=final /app/app.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]
