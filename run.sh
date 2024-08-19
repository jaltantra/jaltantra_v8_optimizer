#!/bin/bash

./mvnw clean
./mvnw package
java -jar target/JaltantraLoopSB-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev  > /dev/null 2>&1