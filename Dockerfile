# Étape 1 : build avec JDK 25 + Maven
FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app

# Installer Maven
RUN apk add --no-cache maven git bash

# Copier le projet
COPY . .

# Build du projet
RUN mvn clean package -DskipTests

# Étape 2 : conteneur runtime léger
FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app

COPY --from=0 /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]