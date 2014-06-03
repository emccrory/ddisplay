#!/bin/bash

# Use for a fresh install of a Dynamic Display instance
#
# Original by Elliott McCrory, May 1, 2014
# Updates --------------------------------------------------------------
#   05/29/2014 | P. Riehecky | Update for better Bash coding practices
#   05/30/2014 | E. McCrory  | Remove references to Opera web browser

# Exit this script when something (anything!!!) fails.
set -e

# Exit the script if any variable is undefined
set -u

SERVER="http://mccrory.fnal.gov/DynamicDisplays"
dduser=ddisplay

ddgroup=$(getent group $(getent passwd ${dduser} | cut -d: -f4) | cut -d: -f1)
destination=$(getent passwd ${dduser} | cut -d: -f6)

DDLogs=/var/log/DD
log="${destination}/DynamicDisplays$$.log"

# create a new log file
date > ${log}

echo "Starting Dynamic Displays creation in folder" $destination ".  Log file is " ${log}

echo "Making folders for Dynamic Displays"

# mkdir $DDLogs || { echo $DDLogs " already exists. Continuing ..."; }
# chown $dduser  $DDLogs $DDLogs/*.log
# chgrp $ddgroup $DDLogs $DDLogs/*.log
mkdir -p ${DDLogs}
chown ${dduser}:${ddgroup} $DDLogs $DDLogs/*.log

mkdir ${destination} || { echo ${destination} " already exists. Continuing ..."; }
cd ${destination}

# TODO -- Replace tar.gz fetching and manipulations with git commands (6/3/2014)

echo Getting the software update script
rm -f DDUpdate.sh
wget ${SERVER}/DDUpdate.sh

# TODO - Run this script rather than duplicating the code here.  The problem is that the Update
#        script, as it exists today, also creates and writes to a log file.

echo "Removing existing files"

rm -f all.tar*
rm -rf bin lib

echo "Fetching the files needed for DD"

wget ${SERVER}/all.tar.gz >> ${log} 2>&1

echo "Unzipping and then un-tarring those files"

gzip -dv all.tar.gz >> ${log} 2>&1

tar xvf all.tar >> ${log} 2>&1

ls -l ${destination} | tee -a ${log}

echo "Finished at " `date` | tee -a ${log}
