FROM docker.io/library/maven:3-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY src src
RUN mvn clean package -ntp -q

FROM docker.io/library/eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/morning-coffee.jar app.jar
RUN addgroup -S -g 10001 morningcoffee \
    && adduser -S -D -H -u 10001 -G morningcoffee morningcoffee
USER morningcoffee:morningcoffee
ENTRYPOINT ["java", "-jar", "app.jar"]
