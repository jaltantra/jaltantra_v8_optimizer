#!/bin/bash

echo "🔧 Starting Optimizer Microservice Setup..."

# ---------------------- Java and Maven Setup -------------------------
# Install OpenJDK 17 if not already installed
if ! command -v java &> /dev/null; then
    echo "📦 Installing OpenJDK 17..."
    sudo apt update
    sudo apt install -y openjdk-17-jdk
else
    echo "✅ Java is already installed."
fi

# Install Maven if not installed
if ! command -v mvn &> /dev/null; then
    echo "📦 Installing Maven..."
    sudo apt install -y maven
else
    echo "✅ Maven is already installed."
fi

# ---------------------- Ampl Setup -----------------------------------
AMPL_DIR="JalTantra-Code-and-Scripts/ampl.linux-intel64"

if [ ! -d "$AMPL_DIR" ]; then
    echo "📦 Copying AMPL from central server..."
    scp -r deploy@10.129.6.131:/home/deploy/ampl.linux-intel64/ JalTantra-Code-and-Scripts
    chmod -R 777 "$AMPL_DIR"
else
    chmod -R 777 "$AMPL_DIR"
    echo "✅ AMPL directory already exists. Skipping SCP."
fi

# ---------------------- Conda & Python Setup --------------------------
MINICONDA_INSTALLER="Miniconda3-latest-Linux-x86_64.sh"
MINICONDA_HOME="$HOME/miniconda"
ENV_NAME="dev"

if [ ! -d "$MINICONDA_HOME" ]; then
    echo "📦 Downloading Miniconda..."
    wget https://repo.anaconda.com/miniconda/$MINICONDA_INSTALLER -O $MINICONDA_INSTALLER
    bash $MINICONDA_INSTALLER -b -p $MINICONDA_HOME
    rm $MINICONDA_INSTALLER
    echo "✅ Miniconda installed."
else
    echo "✅ Miniconda already installed at $MINICONDA_HOME"
fi

# Activate Conda
source "$MINICONDA_HOME/etc/profile.d/conda.sh"
conda init

# Create Conda environment if not exists
if ! conda info --envs | grep -q "$ENV_NAME"; then
    echo "📦 Creating Conda environment: $ENV_NAME"
    conda create -n $ENV_NAME python=3.10 rich -y
else
    echo "✅ Conda environment '$ENV_NAME' already exists."
fi

# Add Miniconda to PATH persistently
echo "export PATH=\"$MINICONDA_HOME/bin:\$PATH\"" >> ~/.bashrc
echo "export MINICONDA_HOME=\"$MINICONDA_HOME\"" >> ~/.bashrc

echo "✅ Python version in '$ENV_NAME':"
conda activate $ENV_NAME
python --version
pip show rich | grep -E 'Name|Version'

# ---------------------- Maven Build -----------------------------------

echo "⚙️ Building Optimizer Microservice..."
mvn clean install

# ---------------------- Spring Config Patch -------------------------

echo "🔧 Patching application-dev.properties..."

current_dir=$(pwd)
properties_file="$current_dir/src/main/resources/application-dev.properties"

if [ ! -f "$properties_file" ]; then
    echo "❌ ERROR: Cannot find application-dev.properties at $properties_file"
    exit 1
fi

# Remove any old hardcoded entries for datasource credentials and solver path
modified_contents=$(grep -v -E 'solver\.root\.dir' "$properties_file")

# Append new dynamic values
modified_contents+="
solver.root.dir=$current_dir/JalTantra-Code-and-Scripts
"

# Write back to file
echo -e "$modified_contents" > "$properties_file"

echo "✅ application-dev.properties updated with environment-specific config."


echo "✅ Setup Complete. Use './run.sh' to start the Optimizer service."
