#!/bin/bash

# Start a java class that only starts Firefox, i.e., without Selenium.  This gives the user an electronic display, but
#  1. It is not full screen (the user has to go in there and hit the F11 key)
#  2. There is no emergency messaging display
#  3. Some of the other "eye candy" on the display does not exist.

# It would be appropriate to use this script for a lot of the displays since they almost never get changed. 

. setupJars.sh

java gov.fnal.ppd.dd.display.simple.DisplayAsSimpleBrowser &

# TODO - make Firefox go full screen (Kiosk mode).  It works rather well if you first start Firefox, 
# then hit F11, then exit Firefox - it remembers this state most of the time in Scientific Linux.
