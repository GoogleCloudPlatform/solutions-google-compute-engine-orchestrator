#! /bin/bash
# Start jetty when the instance starts

cd /usr/share/jetty
/usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java -jar start.jar &

# Start your application here

EOF
