#!/bin/bash
set -e

echo "Starting build process..."

# Build the application using Gradle
echo "Building JAR..."
./gradlew clean build -x test

# Build the Docker image
echo "Building Docker image dohyunyeon/sayai:v1.7..."
docker build -t dohyunyeon/sayai:v1.7 .

# Push to Docker Hub
echo "Pushing to Docker Hub..."
echo "Make sure you are logged in to Docker Hub (run 'docker login' if not)."
docker push dohyunyeon/sayai:v1.7

echo "Done!"
