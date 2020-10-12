#!/bin/bash

# Kill the exiting bash scripts that are in the src/log directory
tempPIDs=temp$$
x=$(pgrep -af bash | grep -v cron | awk '{ print $2 }')
pwdx "$x" 2>/dev/null | grep /log | awk '{ print $1 }' | sed 's/://g' > "$tempPIDs"
if [ -s "$tempPIDs" ]; then
    echo "Killing these PIDs $(cat $tempPIDs)"
    while read -r "tempPIDs" i; do
	kill "$i"
    done
fi






