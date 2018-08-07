#!/bin/bash
. ./setupJars.sh

DBUSER=insertpublickey
DBNAME=xoc_prd
CLIENTNAME=`hostname | sed 's/.fnal.gov//g'`

java -Dddisplay.dbserver=vip-mariadb-prd.fnal.gov:3309 -Dddisplay.dbname=$DBNAME \
     gov.fnal.ppd.security.GenerateNewKeyPair \"$CLIENTNAME\\ selector\\ 00\" $DBUSER

mv *.key ~/keystore
# ls -l ~/keystore
