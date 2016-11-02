#!/bin/bash
. ./setupJars.sh

DBUSER=insertpublickey
DBNAME=xoc_dev
CLIENTNAME=`hostname | sed 's/.fnal.gov//g'`

java -Dddisplay.dbserver=fnalmysqldev.fnal.gov:3311 -Dddisplay.dbname=$DBNAME \
     gov.fnal.ppd.security.GenerateNewKeyPair Public.key private$CLIENTNAME\ selector\ 00.key $CLIENTNAME\ selector\ 00 $DBUSER

mv *.key ~/keystore
# ls -l ~/keystore
