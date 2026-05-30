# Stage 1: Build with Maven
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run with Java
FROM eclipse-temurin:17.0.15_6-jre-alpine  
WORKDIR /app

# Usuario no-root (buena práctica de seguridad)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]


# docker build -t jesusmiguelsanchez/age-detector-be:1.0 .

# docker run -d --name age-detector-be -p 8085:8085 jesusmiguelsanchez/age-detector-be:1.0

# docker push jesusmiguelsanchez/age-detector-be:1.0