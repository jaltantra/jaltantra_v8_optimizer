Filling the deploy_config.json
host_name: The name of the host to which the deployment will be made.
Example: "deploy"

remote_ip: The IP address of the remote server where the deployment will occur.
Example: "10.129.6.131"

remote_folder: The directory path on the remote server where the python scripts will be stored 
Example: "/home/deploy/Jaltantra_v2_3_0_0"

Running the deploy script after filling the config file
bash deploy.sh

Filling the monitor_config.json
host_ip:The IP address of the host server.
Example: "localhost"

host_port: The port number on which the host server will listen.
Example: "8099"

application_context: The context path of the application.
Example: "jaltantra_loop_dev_v7"

solver_directory: he directory path on the remote server where the python scripts will be stored 
Example: "/home/hkshenoy/Desktop/Jaltantra_loop/JalTantra-Code-and-Scripts/NetworkResults/"

sender_email: The email address of the sender used for notifications.
Example: "22m0759@iitb.ac.in"

sender_token:The authentication token for the sender's email.
Example: "6a853780b90d30aac01a7d6d48b36a0c"

receiver_email_list: A list of email addresses that will receive notifications.
Example: ["22m0759@iitb.ac.in", "22m0796@iitb.ac.in"]

Running the monitoring script after filling config
bash monitor_app.sh monitor_config.json



