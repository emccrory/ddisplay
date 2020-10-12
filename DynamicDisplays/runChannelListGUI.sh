#!/bin/bash

cd ~/src/roc-dynamicdisplays/DynamicDisplays || exit

./runVersionInformation.sh

. setupJars.sh

d=$(date +%F)

java -Dddisplay.selector.inwindow=false \
     -Xmx1024m  gov.fnal.ppd.dd.channel.list.ChannelListGUI > "../../log/channelListGUI_${d}_$$.log" 2>&1  &
