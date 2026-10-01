FROM docker.io/library/amazoncorretto:27-alpine AS build
RUN apk add --no-cache maven
WORKDIR /app
COPY pom.xml pom.xml
COPY src src
RUN mvn clean package -ntp -q

FROM docker.io/library/amazoncorretto:27-alpine
WORKDIR /app
COPY --from=build /app/target/morning-coffee.jar app.jar
RUN addgroup -S -g 10001 morningcoffee \
    && adduser -S -D -H -u 10001 -G morningcoffee morningcoffee
USER morningcoffee:morningcoffee
ENTRYPOINT ["java", "-jar", "app.jar"]
