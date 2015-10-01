#!/bin/bash

# The purpose of this script is to see if the local repository is up-to-date
# with respect to the online repository.

. DynamicDisplays/setupJars.sh

export CLASSPATH=DynamicDisplays/bin:DynamicDisplays/lib/mysql-connector-java-5.1.27.jar:DynamicDisplays/lib/slf4j-api-1.5.8.jar:DynamicDisplays/lib/slf4j-log4j12-1.5.8.jar

# This is my local hash code
myHashCode=`git rev-parse HEAD`

# The newest hash code in the database
both=`java gov.fnal.ppd.dd.util.TranslateGITHashCodeToDate | tail -1`
newestHashCode=`echo $both |  awk '{ print $1 }'`
newestTimeStamp=`echo $both |  awk '{ print $2 }'`

# Now, check if my hash code is equal to the hash code of the newest in the repository

echo $both $myHashCode $newestHashCode

if [ $myHashCode = $newestHashCode ]; then
    echo The local repository is up to date
else
    echo NEED TO UPDATE LOCAL REPOSITORY
fi





