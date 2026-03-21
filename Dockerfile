COPY . .
RUN mvn clean package -DskipTests
EXPOSE 8080