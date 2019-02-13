#!/bin/bash

cd ~/src/roc-dynamicdisplays/DynamicDisplays

. setupJars.sh

cd ../../log

log=messagingServer.log

if [ -e $log ]; then
    # Rename the existing messagingServer.log with time stamp of the first access (creation time)
    suffix=`stat $log | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g'`
    # This command pipe ASSUMES A LOT!  So expect it to be brittle

    mv $log messagingServer_$suffix.log
fi

# This compression might take some time, so push it into the background
gzip messagingServer_*.log &

cd ../roc-dynamicdisplays/DynamicDisplays

{
    /bin/date
    java -Xmx1024m gov.fnal.ppd.dd.chat.MessagingServerDaemon
} > ../../log/$log 2>&1

