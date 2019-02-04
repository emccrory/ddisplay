#!/bin/bash

cd ~/src/roc-dynamicdisplays/DynamicDisplays

. setupJars.sh

d=`date +%F`

cd ../../log

if [ -e messagingServer.log ]; then
    # Rename the existing messagingServer.log with time stamp of the first access (creation time)
    suffix=`stat messagingServer.log | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g'`
    # This command pipe Assumes A LOT!  So it will probably be brittle

    mv messagingServer.log messagingServer_$suffix.log
fi

# This compression might take some time, so push it into the background
gzip messagingServer_*.log &

cd ../roc-dynamicdisplays/DynamicDisplays

{
    /bin/date
    java -Xmx1024m gov.fnal.ppd.dd.chat.MessagingServerGUI
} > ../../log/messagingServer.log 2>&1

