#!/bin/bash

if [ "$1 X" = "SKIP X" ]; then
    shift;
else
    cd ~/src/log
    
    if [ -e /usr/bin/xterm ]; then
    # SLF
	/usr/bin/xterm -geometry 200x30 -fa 'Monospace' -fs 12 &
    elif [ -e /opt/X11/bin/xterm ]; then
    # Mac OS
	/opt/X11/bin/xterm &
    fi
fi

cd ~/src/roc-dynamicdisplays/DynamicDisplays

./runVersionInformation.sh

. setupJars.sh

window="true"
public="false"

if [ "$1 X" = "FULL X" ]; then
    window="false"
    if [ "$2 X" = "PUBLIC X" ]; then
	public="true";
    fi
fi

if [ "$1 X" = "XOC X" ]; then
   public='false';
   if [ "$2 X" = "WINDOW X" ]; then
       window="true"
   fi
fi

d=`date +%F`

java -Dddisplay.selector.inwindow=$window \
     -Dddisplay.selector.public=$public \
     -Dddisplay.virtualdisplaynumbers=TRUE \
     -Xmx1024m  gov.fnal.ppd.dd.MakeChannelSelector > ../../log/selector_${d}_$$.log 2>&1  &
