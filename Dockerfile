# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy Maven POM and source files
COPY pom.xml .
COPY src ./src

# Build the application (skipping tests)
RUN mvn clean package -DskipTests

# Stage 2: Create a smaller runtime image using OpenJDK 17
FROM openjdk:17-jdk-slim AS final
WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/target/WebScraper-1.0-SNAPSHOT.jar /app/app.jar

# Stage 3: Adding Selenium and Firefox with Java 17
FROM openjdk:17-jdk-slim AS selenium
USER root
WORKDIR /app

# Install necessary dependencies for Firefox and geckodriver
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

# Install geckodriver (Firefox WebDriver)
RUN GECKO_DRIVER_VERSION=v0.32.0 && \
    wget https://github.com/mozilla/geckodriver/releases/download/${GECKO_DRIVER_VERSION}/geckodriver-${GECKO_DRIVER_VERSION}-linux64.tar.gz && \
    tar -xvzf geckodriver-${GECKO_DRIVER_VERSION}-linux64.tar.gz && \
    mv geckodriver /usr/local/bin/ && \
    rm geckodriver-${GECKO_DRIVER_VERSION}-linux64.tar.gz

# Set environment variable for Firefox binary
ENV FIREFOX_BIN=/usr/bin/firefox-esr

# Copy the JAR file from the final stage to the selenium stage
COPY --from=final /app/app.jar /app/app.jar

# Run the JAR file in the Selenium + Firefox environment
CMD ["java", "-jar", "/app/app.jar"]
