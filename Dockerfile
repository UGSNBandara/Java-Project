FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Install Maven manually
RUN apt-get update && apt-get install -y maven

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
COPY .env.example.docker .env.docker
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "test"]