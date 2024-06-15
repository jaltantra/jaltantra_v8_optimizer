#!/bin/bash
sudo apt update
#install tmux
sudo apt install tmux -y

#scp ampl from jaltantra server
#scp -r deploy@10.129.6.131:/home/deploy/ampl.linux-intel64/ JalTantra-Code-and-Scripts
# Define the target directory
TARGET_DIR="JalTantra-Code-and-Scripts/ampl.linux-intel64"

# Check if the directory does not exist
if [ ! -d "$TARGET_DIR" ]; then
    # Run the scp command to copy the files
    scp -r deploy@10.129.6.131:/home/deploy/ampl.linux-intel64/ JalTantra-Code-and-Scripts
else
    echo "Directory $TARGET_DIR already exists. Skipping scp."
fi


#change permissions for ampl
chmod 777 -R JalTantra-Code-and-Scripts/ampl.linux-intel64


# Define variables
MINICONDA_INSTALLER="Miniconda3-latest-Linux-x86_64.sh"
ENV_NAME="dev"
MINICONDA_HOME="$HOME/miniconda"

# Download Miniconda installer
wget https://repo.anaconda.com/miniconda/$MINICONDA_INSTALLER

# Run Miniconda installer
bash $MINICONDA_INSTALLER -b -p $MINICONDA_HOME

# Initialize conda (necessary for running conda commands)
source $MINICONDA_HOME/etc/profile.d/conda.sh

# Create 'dev' environment and install Python and 'rich'
conda create -n $ENV_NAME python rich -y

# Activate 'dev' environment
conda init
conda activate $ENV_NAME

# Verify installation
echo "Miniconda and 'dev' environment installed successfully."
echo "Python version installed:"
python --version
echo "'rich' package installed:"
pip show rich | grep -E '^Name|^Version'

# Add Miniconda to PATH and set MINICONDA_HOME
echo "export PATH=\"$MINICONDA_HOME/bin:\$PATH\"" >> ~/.bashrc
echo "export MINICONDA_HOME=\"$MINICONDA_HOME\"" >> ~/.bashrc

# Reload bashrc to apply changes
source ~/.bashrc

# Clean up Miniconda installer
rm $MINICONDA_INSTALLER




echo "Installing MySQL server and open JDK 17"
# sudo apt-get update
sudo apt-get install -y mysql-server
sudo apt install -y openjdk-17-jdk

# Start MySQL service
echo "Starting MySql server"
sudo systemctl start mysql





echo "Creating user dev with password dev on MySQL"
sudo mysql <<MYSQL_SCRIPT
CREATE USER 'dev'@'localhost' IDENTIFIED BY 'dev';
GRANT ALL PRIVILEGES ON *.* TO 'dev'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
exit;
MYSQL_SCRIPT
echo "MySQL user 'dev' created with password 'dev'"


echo "Creating jaltantra_users_database"
sudo mysql -u dev -p'dev' <<MYSQL_SCRIPT
create database jaltantra_users;
exit;
MYSQL_SCRIPT
echo "Created jaltantra_users_database"


current_dir=$(pwd)
properties_file="$current_dir/src/main/resources/application-dev.properties"
echo "Location of properties file "$properties_file


# Read the contents of the application.properties file, filter out lines containing 'spring.datasource.password' and 'solver.root.dir', and assign to modified_contents
modified_contents=$(grep -v -E 'spring\.datasource\.username|spring\.datasource\.password|solver\.root\.dir' $properties_file)

# Append new lines to modified_contents
modified_contents+="\nspring.datasource.username=dev\nspring.datasource.password=dev\nsolver.root.dir=$current_dir/JalTantra-Code-and-Scripts"


# Write the modified contents to a new file
echo -e "$modified_contents" > $properties_file

#./mvnw clean
#./mvnw package
#java -jar target/JaltantraLoopSB-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev  > /dev/null 2>&1

echo "initial setup done"
