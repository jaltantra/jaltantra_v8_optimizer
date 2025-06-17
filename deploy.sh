#!/bin/bash

set -e

# === CONFIGURATION ===
SERVER_USER="jaltantra_v8_optimizer"
SERVER_IP="10.129.6.131"
REMOTE_DIR="~/optimizer"
JAR_NAME="jaltantra-optimizer-v8.jar"
PROPERTIES_FILE="application-deploy.properties"
DEPLOY_PORT=8430

echo "🚀 Starting optimizer deployment to $SERVER_IP..."

# === STEP 1: Stop any existing optimizer process ===
echo "🛑 Stopping existing optimizer process on server..."
ssh ${SERVER_USER}@${SERVER_IP} "
    PID=\$(ps aux | grep ${JAR_NAME} | grep -v grep | awk '{print \$2}')
    if [ ! -z \"\$PID\" ]; then
        echo \"🔍 Found process ID \$PID. Killing it...\"
        kill -9 \$PID
    else
        echo \"✅ No running optimizer process found.\"
    fi
"

# === STEP 2: Build the JAR ===
echo "🔨 Building JAR..."
./mvnw clean package -DskipTests

# === STEP 3: Copy files to the server ===
echo "📦 Copying JAR and properties to server..."
scp target/${JAR_NAME} ${SERVER_USER}@${SERVER_IP}:${REMOTE_DIR}/
scp src/main/resources/${PROPERTIES_FILE} ${SERVER_USER}@${SERVER_IP}:${REMOTE_DIR}/

# === STEP 4: Start the new optimizer process ===
echo "🚀 Starting optimizer on remote server..."
ssh ${SERVER_USER}@${SERVER_IP} "
    cd ${REMOTE_DIR}
    nohup java -jar -Dspring.config.location=file:${PROPERTIES_FILE} ${JAR_NAME} > optimizer.log 2>&1 &
    sleep 2
    echo \"⏳ Waiting for service to bind to port ${DEPLOY_PORT}...\"
    lsof -i :${DEPLOY_PORT} || echo \"❌ Port ${DEPLOY_PORT} not active yet. Check optimizer.log.\"
"

echo "✅ Deployment completed. You can tail the log using:"
echo "   ssh ${SERVER_USER}@${SERVER_IP} 'tail -f ${REMOTE_DIR}/optimizer.log'"
