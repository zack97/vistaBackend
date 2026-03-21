# Étape 1 : build avec Maven + JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copier le projet et build
COPY . .
RUN mvn clean package -DskipTests

# Étape 2 : conteneur runtime léger
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]