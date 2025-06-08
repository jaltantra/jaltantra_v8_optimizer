#!/bin/bash

echo "🚀 Building and running Optimizer Microservice..."

# Build the Spring Boot project using Maven wrapper
./mvnw clean package

# Check if JAR file was created
JAR_FILE=$(find target -name '*Optimizer-*.jar' | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ Build failed or JAR not found in target/. Aborting."
    exit 1
fi

# Run the JAR with active profile = dev
echo "▶️ Running $JAR_FILE with --spring.profiles.active=dev"
java -jar "$JAR_FILE" --spring.profiles.active=dev
