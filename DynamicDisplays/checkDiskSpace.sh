#!/bin/bash
# Check the disk space remaining on /home.  Put up a dialog box (using zenity(1) if there is not enough space.
#
# Arguments:
#  (1) - String to attach to the error dialog (Optional)
#
# Exit codes:
#  0  - Everything is OK - there is enough disk space
# 255 - There is not enough disk space!
#

export minimumDiskSpace=3 #GB
export ONEGIG=1048576

# ASSUME that df returns "kilobytes remaining" in column 4
GB=$(($(df | grep /home | grep -v home2 | awk '{ print $4 }')/ONEGIG))

if [ "$GB" -lt "$minimumDiskSpace" ]; then
    echo "$0: Insufficient disk space, $GB GB, to run the Dynamic Displays software.  Log files for this application and for the system need at least $minimumDiskSpace GB."
    echo Here is the df command.
    df 
    echo
    echo This situation is unexpected.  Exiting.
    text="\n  Insufficient Disk Space,\n    $GB GB, to run the\nDynamic Display Software\n"
    ( 
	for i in {1..100..2}
	do
	    echo "$i"
	    sleep 1;
	done
    ) |
    zenity --auto-close --percentage=0 --progress --width=800 --title="Dynamic Displays Software Fatal Error - $1" --text="<span font-family=\"sans\" font-weight=\"900\" font-size=\"40000\">$text</span>"
    exit 255;
fi
