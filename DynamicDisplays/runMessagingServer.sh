. setupJars.sh

d=`date +%F`

{
    echo date
    java gov.fnal.ppd.chat.MessagingServerGUI
} > ../log/messagingServer_${d}_$!.log 2>&1

