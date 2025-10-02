FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy

# Install Chrome and dependencies for JavaScript crawler
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libappindicator3-1 \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libdrm2 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libu2f-udev \
    libvulkan1 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxkbcommon0 \
    libxrandr2 \
    libxss1 \
    libxtst6 \
    xdg-utils \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/googlechrome-linux-keyring.gpg \
    && sh -c 'echo "deb [arch=amd64 signed-by=/usr/share/keyrings/googlechrome-linux-keyring.gpg] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Create a non-root user for running Chrome (security best practice)
RUN groupadd --gid 1000 spring \
    && useradd --uid 1000 --gid spring --shell /bin/bash --create-home spring

WORKDIR /app

# Set environment variables for headless Chrome and JVM tuning
ENV CHROME_BIN=/usr/bin/google-chrome-stable
ENV DISPLAY=:99
ENV JAVA_OPTS="-Xms512m -Xmx1400m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/ -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -Djava.awt.headless=true -Dspring.jpa.show-sql=false -Dlogging.level.org.hibernate.SQL=WARN -Dfile.encoding=UTF-8"

# Copy application jar
COPY --from=build /app/target/*.jar /app/app.jar

# Change ownership to spring user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

EXPOSE 8080
ENTRYPOINT ["/bin/sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
