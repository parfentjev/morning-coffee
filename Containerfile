FROM docker.io/library/maven:3-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY src src
RUN mvn clean package -ntp -q

FROM docker.io/library/maven:3-eclipse-temurin-25-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
