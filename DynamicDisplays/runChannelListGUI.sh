#!/bin/bash

cd ~/src/roc-dynamicdisplays/DynamicDisplays || exit

. setupEnvironment.sh

./runVersionInformation.sh

d=$(date +%F)

java -Dddisplay.selector.inwindow=false \
     -Xmx1024m  gov.fnal.ppd.dd.channel.list.ChannelListGUI > "../../log/channelListGUI_${d}_$$.log" 2>&1  &
