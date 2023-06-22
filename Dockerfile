FROM openjdk:17-jdk-slim

#ENV BOT_KEY=6007801894:AAFRWD7ADVQyUUBQ0QS0axl-KsIIG2krJy8
#ENV BOT_NAME=carworkerbot
#ENV GOOGLE_APPLICATION_CREDENTIALS=/app/credentials.json
#ENV LOG_SPREEDSHEED_ID=1_jD4KgAkzQ25EdgvHzsonGmP7y-6wWGhssaOH1F8QNU
#ENV SPRING_PROFILES_ACTIVE=test
#ENV BOT_URI=${BOT_URI}

WORKDIR /app

#COPY ./target/*.jar .

#CMD java $JAVA_OPTS -jar *.jar
COPY  ./build/libs/*.jar .
COPY  ./build/resources/main/credentials.json .

ENTRYPOINT ["java","-jar","car-calculator-bot-0.0.1-SNAPSHOT.jar"]

EXPOSE 8080