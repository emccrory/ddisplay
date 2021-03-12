#!/bin/bash
#
# Shell script to orchestrate giving this computer a public/private key pair
#

. ./setupJars.sh

DBUSER=insertpublickey
DBNAME=xoc_prd
CLIENTNAME=$(hostname | sed 's/.fnal.gov//g' | sed 's/.local//g' )

echo "The system name from the operating system for this computer is " $CLIENTNAME
java gov.fnal.ppd.security.CheckHostName
echo java -Dddisplay.dbserver=vip-mariadb-prd.fnal.gov:3309 -Dddisplay.dbname=$DBNAME gov.fnal.ppd.security.GenerateNewKeyPair "$CLIENTNAME\\ selector\\ 00" "$DBUSER"
     java -Dddisplay.dbserver=vip-mariadb-prd.fnal.gov:3309 -Dddisplay.dbname=$DBNAME gov.fnal.ppd.security.GenerateNewKeyPair "$CLIENTNAME\\ selector\\ 00" "$DBUSER"

# mv ./*.key ~/keystore

