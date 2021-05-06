#!/bin/bash

workingDirectory=$(pwd)
if [ "$1 X" = "SKIP X" ]; then
    shift;
else
    cd ../../log || exit
    
    if [ -e /usr/bin/xterm ]; then
    # SLF
	/usr/bin/xterm -geometry 200x30 -fa 'Monospace' -fs 12 &
    elif [ -e /opt/X11/bin/xterm ]; then
    # Mac OS
	/opt/X11/bin/xterm &
    fi
fi

cd "$workingDirectory" || exit

./runVersionInformation.sh

. setupEnvironment.sh

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

# Set up log file 
ddHome=../..
node=$(uname -n)
if [ "$node" = "$adminNode" ]; then
    ddHome=$adminWorkspace
fi

log="$ddHome/log/selector.log"

if [ -e "$log" ] ; then
    # Rename the existing log file with time stamp of the first access (creation time)
    # This command pipe Assumes A LOT!  So it will probably be brittle
   suffix=$(stat $log | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g')

   mv "$log" "$ddHome/log/selector_$suffix.log"
   gzip      "$ddHome/log/selector_$suffix.log" &
fi

{
    while {
	java -Dddisplay.selector.inwindow=$window \
	     -Dddisplay.selector.public=$public \
	     -Dddisplay.virtualdisplaynumbers=TRUE \
	     -Xmx2048m  gov.fnal.ppd.dd.MakeChannelSelector
	test $? -eq 255
    }
    do
	echo ""
	echo ""
	echo ""
	echo "$(date) Controller program exited because user requested a refresh"
	# Maybe there is a new version of the software here.  This "cd" should put us in the right place
	cd "$workingDirectory" || exit
	sleep 1
	echo "     ..."
	echo "$(date) Starting the Channel Selector again now."
	echo ""
	echo ""
	echo ""
    done
} > $log 2>&1 &
