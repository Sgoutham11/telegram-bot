FROM eclipse-temurin:21-jre-alpine

WORKDIR /telegram-bot-buddy

COPY telegram-bot-buddy.jar telegram-bot-buddy.jar

EXPOSE 7072

ENTRYPOINT ["java","-Dspring.profiles.active=docker","-jar","telegram-bot-buddy.jar"]