FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package

FROM openjdk:17-jdk-slim AS final
WORKDIR /app

COPY --from=builder /app/target/WebScraper-1.0-SNAPSHOT.jar /app/app.jar

FROM openjdk:17-jdk-slim AS playwright
USER root
WORKDIR /app

RUN apt-get update && apt-get install -y \
    curl \
    ca-certificates \
    libatomic1 \
    libxslt1.1 \
    libwoff1 \
    libevent-2.1-7 \
    libopus0 \
    libflite1 \
    libwebpdemux2 \
    libharfbuzz-icu0 \
    libwebpmux3 \
    libenchant-2-2 \
    libsecret-1-0 \
    libhyphen0 \
    libmanette-0.2-0 \
    libdw1 \
    libegl1 \
    libgudev-1.0-0 \
    libgles2 \
    libx264-dev \
    libglib2.0-0 \
    libnss3 \
    libgdk-pixbuf2.0-0 \
    libgtk-3-0 \
    libx11-xcb1 \
    libxcomposite1 \
    libxrandr2 \
    libasound2 \
    libatk1.0-0 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    fonts-liberation \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

RUN useradd -m -s /bin/bash playwright && \
    mkdir -p /home/playwright/.cache/ms-playwright && \
    chown -R playwright:playwright /home/playwright

WORKDIR /home/playwright
RUN npm init -y && \
    npm install playwright@1.48.2 && \
    npx playwright install --with-deps chromium && \
    chown -R playwright:playwright /home/playwright && \
    chmod -R 755 /home/playwright/.cache/ms-playwright

WORKDIR /app
COPY --from=final /app/app.jar /app/app.jar
RUN chown playwright:playwright /app/app.jar

USER playwright

ENV PLAYWRIGHT_BROWSERS_PATH=/home/playwright/.cache/ms-playwright

CMD ["java", "-jar", "/app/app.jar"]