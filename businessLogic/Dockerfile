FROM maven:3.9-eclipse-temurin-17 as builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY --from=builder /build/target/*.jar businessLogic-0.0.1-SNAPSHOT.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "businessLogic-0.0.1-SNAPSHOT.jar"]