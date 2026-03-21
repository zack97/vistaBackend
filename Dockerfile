From maven:3.9-eclipse-temurin-25 AS build
COPY . .
RUN mvn clean package -DskipTests
FROM eclipse-temurin:25.0.2_10-jre-noble
COPY --from=build /target/*.jar demo.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","demo.jar"]