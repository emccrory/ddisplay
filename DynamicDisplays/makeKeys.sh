#!/bin/bash
. ./setupJars.sh

DBUSER=insertpublickey
DBNAME=xoc_prd
CLIENTNAME=$(hostname | sed 's/.fnal.gov//g' | sed 's/.local//g' )

echo "Operating system name is " $CLIENTNAME
java gov.fnal.ppd.security.CheckHostName
echo "Echoing the key commands here, rather than executing them..."
echo java -Dddisplay.dbserver=vip-mariadb-prd.fnal.gov:3309 -Dddisplay.dbname=$DBNAME gov.fnal.ppd.security.GenerateNewKeyPair \""$CLIENTNAME"\\ selector\\ 00\" "$DBUSER"

echo mv ./*.key ~/keystore
# ls -l ~/keystore
