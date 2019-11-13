#!/bin/bash

# Kill the exiting bash scripts that are in the src/log directory
tempPIDs=temp$$
pwdx `ps -aef | grep bash | grep -v grep | grep -v cron | awk '{ print $2 }'` 2>/dev/null | grep /log | awk '{ print $1 }' | sed 's/://g' > $tempPIDs
if [ -s $tempPIDs ]; then
    echo Killing these PIDs
    cat $tempPIDs
    kill `cat $tempPIDs`
fi






