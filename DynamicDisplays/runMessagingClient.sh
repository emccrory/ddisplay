. setupJars.sh

d=`date +%F`

{
    echo date
    java -Xmx512m gov.fnal.ppd.chat.MessagingClientGUI
} > ../log/messagingServer_${d}_$$.log 2>&1

