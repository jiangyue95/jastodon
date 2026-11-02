# ---- Build ----
FROM eclipse-temurin:21 AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B package -DskipTests

# ---- Run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 jastodon
COPY --from=build /app/target/*.jar app.jar
USER jastodon
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
