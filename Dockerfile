FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY mvnw ./
COPY .mvn/ .mvn/
COPY pom.xml ./
COPY src/ src/

RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/backend-*.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]
