#!/bin/bash

# Start a java class that only starts Firefox, i.e., without Selenium.  This gives the user an electronic display, but
#  1. It is not full screen (the user has to go in there and hit the F11 key)
#  2. There is no emergency messaging display
#  3. Some of the other "eye candy" on the display does not exist.

# It would be appropriate to use this script for a lot of the displays since they almost never get changed. 

# Set up log file 
ddHome=$HOME/src
node=$(uname -n)
adminNode="ad130482.fnal.gov"
adminWorkspace="/home/mccrory/git-ddisplay"
if [ "$node" = "$adminNode" ]; then
    ddHome=$adminWorkspace
fi

log="$ddHome/log/display.log"

if [ -e "$log" ] ; then
    # Rename the existing log file with time stamp of the first access (creation time)
    # This command pipe Assumes A LOT!  So it will probably be brittle
   suffix=$(stat $log | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g')

   mv "$log" "$ddHome/log/display_$suffix.log"
   gzip      "$ddHome/log/display_$suffix.log" &
fi

touch $log

{
    java gov.fnal.ppd.dd.display.client.simplified.DisplayAsSimpleBrowser &

    # TODO - make Firefox go full screen (Kiosk mode).  It works rather well if you first start Firefox, 
    # then hit F11, then exit Firefox - it remembers this state most of the time in Scientific Linux.
} >> $log 2>&1
