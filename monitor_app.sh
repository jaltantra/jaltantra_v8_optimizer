#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: give config file"
    exit 1
fi

JSON_FILE="$1"

if [ ! -f "$JSON_FILE" ]; then
    echo "File not found: $file"
    exit 1
fi
host_ip=$(jq -r '.host_ip' $JSON_FILE)
host_port=$(jq -r '.host_port' $JSON_FILE)
application_context=$(jq -r '.application_context' $JSON_FILE)

SUMMARY_FOLDER=$(jq -r '.solver_directory' $JSON_FILE)
LICENSE_FOLDER=$(jq -r '.license_directory' $JSON_FILE)
sender_email=$(jq -r '.sender_email' $JSON_FILE)
sender_token=$(jq -r '.sender_token' $JSON_FILE)
receiver_email_list=$(jq -r '.receiver_email_list | join(" ")' "$JSON_FILE")


STATUS_FILE="summary.txt"
EMAIL_FILE="email.txt"
WEB_APP_URL=$host_ip:$host_port/$application_context/

set_ssmtp(){

echo "Configuring SSMTP"
SMTP_SERVER="smtp-auth.iitb.ac.in"
SMTP_PORT="587"
USE_STARTTLS="YES"

# Update ssmtp configuration file
sudo tee /etc/ssmtp/ssmtp.conf > /dev/null <<EOF
root=$GMAIL_ADDRESS
mailhub=$SMTP_SERVER:$SMTP_PORT
mailhub=$SMTP_SERVER
hostname=$(hostname)
AuthUser=$sender_email
AuthPass=$sender_token
UseSTARTTLS=$USE_STARTTLS
EOF

# Set proper permissions for the configuration file
sudo chmod 644 /etc/ssmtp/ssmtp.conf
sudo chown root:mail /etc/ssmtp/ssmtp.conf

echo "ssmtp configuration update"
}




check_web_app_status() {
    echo "checking web app staus "$WEB_APP_URL
    CURRENT_TIME=$(date "+%Y-%m-%d %H:%M:%S")
    echo "Check Time: $CURRENT_TIME" > $STATUS_FILE
    
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $WEB_APP_URL)
    if [ $HTTP_STATUS -eq 200 ]; then
        echo "Web application is UP (Status: $HTTP_STATUS)" >> $STATUS_FILE
    else
        echo "Web application is DOWN (Status: $HTTP_STATUS)" >> $STATUS_FILE
    fi
}

check_license(){
	echo "">> $STATUS_FILE
	echo "License Status" >> $STATUS_FILE 
	$LICENSE_FOLDER/ampl_lic netstatus >> $STATUS_FILE
	
}


gather_summary() {
    echo "" >> $STATUS_FILE
    echo "Summary of folder: $SUMMARY_FOLDER" >> $STATUS_FILE
    echo "Total files: $(find $SUMMARY_FOLDER -type f | wc -l)" >> $STATUS_FILE
    echo "Total directories: $(find $SUMMARY_FOLDER -type d | wc -l)" >> $STATUS_FILE
    echo "Total size: $(du -sh $SUMMARY_FOLDER | cut -f1)" >> $STATUS_FILE
    
    
    echo "" >> $STATUS_FILE
    echo "List of directories with creation times and 0_status.txt content:" >> $STATUS_FILE
    
    # Loop through directories and get their creation times and status file content
    while IFS= read -r dir; do
        DIR_NAME=$(basename "$dir")
        CREATION_TIME=$(stat -c %w "$dir" 2>/dev/null || stat -c %y "$dir")
        STATUS_FILE_PATH="$dir/0_status"
        
        if [ -f "$STATUS_FILE_PATH" ]; then
            FIRST_LINE=$(head -n 1 "$STATUS_FILE_PATH")
            echo "$DIR_NAME - $CREATION_TIME - $FIRST_LINE" >> $STATUS_FILE
        else
            echo "$DIR_NAME - $CREATION_TIME - No 0_status.txt file" >> $STATUS_FILE
        fi
    done < <(find $SUMMARY_FOLDER -mindepth 1 -maxdepth 1 -type d)
}

send_email() {
    echo "From: $sender_email" > $EMAIL_FILE
    echo "Subject: Web Application Status and Summary" >> $EMAIL_FILE
    echo "" >> $EMAIL_FILE
    cat $STATUS_FILE >> $EMAIL_FILE
    

    # Send email to each recipient individually

    for recipient in $receiver_email_list;do
        echo "Sending email to: $recipient"
        cat $EMAIL_FILE | sendmail $recipient
    done
}


# Main script execution
set_ssmtp
check_web_app_status
check_license
gather_summary
send_email


#cat $STATUS_FILE
# Cleanup temporary files
rm -f $STATUS_FILE $EMAIL_FILE

echo "Monitoring and email notification completed."
