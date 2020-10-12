#!/bin/bash

. setupJars.sh

d=$(date +%F)
log="../../log/messagingClient_${d}_$$.log"

{
    echo date
    java -Xmx512m gov.fnal.ppd.dd.chat.MessagingClientGUI
} > "$log" 2>&1

