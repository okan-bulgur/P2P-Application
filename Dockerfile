FROM openjdk:23-jdk-slim

LABEL author="OkanBulgur"

RUN apt-get update && apt-get install -y \
    xauth \
    x11-apps \
    libxtst6 \
    libxrender1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/P2P-Application-1.0-SNAPSHOT.jar /app/app.jar
COPY src/main/java/app/res/CSE462TP.pdf /app/source/CSE462TP.pdf
COPY src/main/java/app/res/CSE471TP.pdf /app/source/CSE471TP.pdf
RUN mkdir -p /app/dest

ENV DISPLAY=host.docker.internal:0.0

CMD ["java", "-jar", "/app/app.jar"]
