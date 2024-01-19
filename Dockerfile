# Set of commands to create a docker image
# the instructions to convert our application to a docker image
# Base image
# docker commands 
FROM openjdk:11-jdk-slim as builder

WORKDIR /app
#copies file/directory from src location to destination location
COPY mvnw .
COPY .mvn .mvn 
COPY pom.xml .

#creates a container
RUN chmod +x ./mvnw && ./mvnw -B dependency:go-offline
#builds an image and disposes the container

COPY src src
RUN ./mvnw package -DskipTests

RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

#-----------------------------------------#

FROM openjdk:11.0.13-jre-slim-buster as stage

#argument
ARG DEPENDENCY=/app/target/dependency

# Copy the dependency application file from builder stage artifact
COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=builder ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes /app

# just for development purpose
RUN apt update && apt install -y curl
EXPOSE 8081

ENTRYPOINT ["java", "-cp", "app:app/lib/*", "com.classpathio.order.OrderMicroserviceApplication"]