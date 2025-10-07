FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy

# 安装 Chromium 运行所需系统库（与 Playwright 校验一致）
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    libglib2.0-0 \
    libnss3 \
    libnspr4 \
    libdbus-1-3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libatspi2.0-0 \
    libx11-6 \
    libxcomposite1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libxcb1 \
    libxkbcommon0 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2 \
 && rm -rf /var/lib/apt/lists/*

# Create a non-root user for running the application
RUN groupadd --gid 1000 spring \
    && useradd --uid 1000 --gid spring --shell /bin/bash --create-home spring

WORKDIR /app

# Set environment variables for JVM tuning (optimized for 2GB machine)
ENV JAVA_OPTS="-Xms256m -Xmx1200m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/ -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -Djava.awt.headless=true -Dspring.jpa.show-sql=false -Dlogging.level.org.hibernate.SQL=WARN -Dfile.encoding=UTF-8"

# Copy application jar
COPY --from=build /app/target/*.jar /app/app.jar

# Change ownership to spring user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

EXPOSE 8080
ENTRYPOINT ["/bin/sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
