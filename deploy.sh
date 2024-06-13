#!/bin/bash

sudo apt-get install jq

host_name=$(jq -r '.host_name' deploy_config.json)
host_ip=$(jq -r '.remote_ip' deploy_config.json)
host_folder=$(jq -r '.remote_folder' deploy_config.json)

echo $host_name
echo $host_ip
echo $host_folder


echo "Building Jaltantra"
./mvnw package
echo "Build finished"

echo "Transfering the jar file to deployment machine"
scp target/JaltantraLoopSB-0.0.1-SNAPSHOT.jar $host_name@$host_ip:$host_folder
echo "Transferd the jar file to deployment machine"


COMMAND="nohup java -jar $host_folder/JaltantraLoopSB-0.0.1-SNAPSHOT.jar --spring.profiles.active=deploy  > /dev/null 2>&1"

ssh $host_name@$host_ip $COMMAND


