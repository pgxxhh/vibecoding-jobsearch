FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV JAVA_OPTS="-Xms256m -Xmx768m"
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["/bin/sh","-c","java $JAVA_OPTS -jar /app/app.jar \
  --spring.datasource.url=${DB_URL:-jdbc:h2:file:/data/h2/vibejobs;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE} \
  --spring.datasource.username=${DB_USER:-sa} \
  --spring.datasource.password=${DB_PASSWORD:-} \
  --spring.datasource.driver-class-name=${SPRING_DATASOURCE_DRIVER_CLASS_NAME:-org.h2.Driver} \
  --spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:-update} \
  --server.port=${SERVER_PORT:-8080}"]
