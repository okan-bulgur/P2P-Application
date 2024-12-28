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
COPY src/main/java/app/res/pdf1.pdf /app/source/pdf1.pdf
COPY src/main/java/app/res/pdf2.pdf /app/source/pdf2.pdf
COPY src/main/java/app/res/music1.mp4 /app/source/music1.mp4
COPY src/main/java/app/res/music2.mp3 /app/source/music2.mp3
COPY src/main/java/app/res/video1.mp4 /app/source/video1.mp4
COPY src/main/java/app/res/img1.jpg /app/source/img1.jpg

RUN mkdir -p /app/dest

ENV DISPLAY=host.docker.internal:0.0

CMD ["java", "-jar", "/app/app.jar"]
