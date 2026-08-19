FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY app.jar app.jar

EXPOSE 7072

ENTRYPOINT ["java","-Dspring.profiles.active=docker","-jar","app.jar"]