#!/bin/bash

cd ~/src/roc-dynamicdisplays/DynamicDisplays

. setupJars.sh

d=`date +%F`

{
    /bin/date
    java -Xmx1024m gov.fnal.ppd.dd.chat.MessagingServerGUI
} > ../../log/messagingServer_${d}_$$.log 2>&1

