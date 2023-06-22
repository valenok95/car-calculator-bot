FROM gradle:latest AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

RUN gradle clean bootJar --no-daemon

FROM openjdk:17-jdk-slim AS prod
WORKDIR /app

COPY --from=build /home/gradle/src/build/libs/*.jar /app/bot.jar
COPY --from=build /home/gradle/src/build/resources/main/credentials.json /app/credentials.json

ENTRYPOINT ["java","-jar","bot.jar"]

#ENV BOT_KEY=6007801894:AAFRWD7ADVQyUUBQ0QS0axl-KsIIG2krJy8
#ENV BOT_NAME=carworkerbot
#ENV GOOGLE_APPLICATION_CREDENTIALS=/app/credentials.json
#ENV LOG_SPREEDSHEED_ID=1_jD4KgAkzQ25EdgvHzsonGmP7y-6wWGhssaOH1F8QNU
#ENV SPRING_PROFILES_ACTIVE=test
#ENV BOT_URI=https://fluffy-radios-design.loca.lt