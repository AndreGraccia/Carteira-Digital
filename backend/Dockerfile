# stage-1 : complete build environment
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn clean package -DskipTests


# stage-2 : minimal runtime environment
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN groupadd --system wallia && useradd --system --gid wallia --home-dir /app wallia

# copy jar from the first stage
COPY --from=builder /app/target/*.jar /app/app.jar

RUN chown -R wallia:wallia /app
USER wallia

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
