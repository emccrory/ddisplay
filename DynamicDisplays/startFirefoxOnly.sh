#!/bin/bash

# Start Firefox without Java and without Selenium.  This gives the user an electronic display, but
#  1. It is not full screen (the user has to go in there and hit the F11 key)
#  2. There is no control from a ChannelSelector
#  3. The default channel cannot be a list of channels or a YouTube video (unless showing the video only once is OK)

# It may be appropriate to use this script for a lot of the displays since they almost never get changed. The
# displays that use this could be marked in the database somehow. There are plenty of unused fields in the Display
# table, for example, "Type" could be repurposed to say "Dynamic" or "Static", and "Static" would do this script.

# The way that might work is to create a simple messaging client.  First it will use this scrupt to launch Firefox.  
# Then it will listend for channel changes;  when it gets one, it will kill the Firefox process, and then 
# re-launch Firefox again with the new URL.

# But this method will not:
#  -> Show lists of channels
#  -> Show a YouTube video (YouTube does not let you do an "auto-repeat" directly)

# To be determined ...

defaultURL=$1

if [ "X $1" = "X " ]; then
    . setupJars.sh

    defaultURL=`java gov.fnal.ppd.dd.db.GetDefaultContentForDisplay | grep DEFAULTCONTENT | awk '{ print $2 }'`
fi

/usr/bin/firefox $defaultURL &

echo $!

# TODO - make Firefox go full screen (Kiosk mode).  It works rather wekll if you first start Firefox, 
# then hit F11, then exit Firefox - it remembers this state most of the time in Scientific Linux.
