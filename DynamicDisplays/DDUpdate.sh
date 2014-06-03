#!/bin/bash

# Use for updating the Java code and shell scripts for a Dynamic Display instance
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
ddgroup=$(getent group $(getent passwd ${dduser}) | cut -d: -f4) | cut -d: -f1)
destination=$(getent passwd ${dduser} | cut -d: -f6)
log="${destination}/DynamicDisplays$$.log"

# create a new log file
date > ${log}

echo "Starting Dynamic Displays update in folder" ${destination} ".  Log file is " ${log}

cd ${destination}

echo "Removing existing files"

rm -f all.tar*
rm -rf bin lib

echo "Fetching the files needed for DD"

wget ${SERVER}/all.tar.gz >> ${log} 2>&1

echo "Unzipping and un-tarring those files"

gzip -dv all.tar.gz >> ${log} 2>&1
tar  xvf all.tar    >> ${log} 2>&1

chown -R ${dduser}:${ddgroup} *

ls -l ${destination} | tee -a ${log}

echo "Finished at " `date` ". Log file is " ${log} | tee -a ${log}
