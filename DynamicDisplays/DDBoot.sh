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

DDLogs=${destination}/log/
log="${destination}/DynamicDisplays$$.log"

# create a new log file
date > ${log}

echo "Starting Dynamic Displays creation in folder" ${destination}".  Log file is " ${log}

echo "Making folders for Dynamic Displays"

mkdir -p ${destination} ${DDLogs}

cd ${destination}


# TODO -- Replace tar.gz fetching and manipulations with git commands (6/3/2014)
#
# This format doesn't require any special redmine rights
# git clone http://cdcvs.fnal.gov/projects/roc-dynamicdisplays/ .

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

# on SL6, tar is smart enough to automatically gzip -d in memory
tar xvf all.tar.gz >> ${log} 2>&1

ls -l ${destination} | tee -a ${log}

# ensure ownership is correct on all files
chown -Rh ${dduser}:${ddgroup} ${DDLogs} ${destination}

echo "Finished at " `date` | tee -a ${log}
