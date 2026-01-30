#!/bin/bash
./gradlew bootJar
docker build --platform linux/amd64 -t dohyunyeon/sayai .
docker push dohyunyeon/sayai
