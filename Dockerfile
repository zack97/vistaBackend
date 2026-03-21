FROM maven:3.9.6-eclipse-temurin-25 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jdk-alpine
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]