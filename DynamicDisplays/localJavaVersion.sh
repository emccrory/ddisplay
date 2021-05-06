#!/bin/bash

#
# Get the version of the software running on each of the display nodes
#
# Note that this script fails if one or more of the nodes in the pssh-hosts file is down.
#

# Prepare to run the Java applications
. setupEnvironment.sh

java gov.fnal.ppd.dd.util.version.VersionInformationLocal
