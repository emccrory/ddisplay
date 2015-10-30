#!/bin/bash

cd ~/src/log

if [ -e /usr/bin/xterm ]; then
    # SLF
    /usr/bin/xterm -geometry 200x30 -fa 'Monospace' -fs 12 &
elif [ -e /opt/X11/bin/xterm ]; then
    # Mac OS
    /opt/X11/bin/xterm &
fi

cd ~/src/roc-dynamicdisplays/DynamicDisplays

. setupJars.sh

#
# Evaluate if there is a newer version of the software out there
#

if java gov.fnal.ppd.dd.util.version.VersionInformationComparison; then
    echo You are running the latest version of the software
else
    title="New version detected"
    text="There exists a newer version of the software than the version we are about to run.\n\nShall we update to the newest version?";

    if zenity --question --title=$title --text=$text; then 
	# Insert code HERE to fetch the newset version of the software and then restart
	echo At this time, you have to do this update yourself.  Sorry.
    fi

fi

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
