. setupJars.sh

d=`date +%F`

{
    echo date
    java -Xmx1024m gov.fnal.ppd.dd.chat.MessagingServerGUI
} > ../../log/messagingServer_${d}_$$.log 2>&1

