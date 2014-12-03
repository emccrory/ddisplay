#!/bin/bash

. setupJars.sh

if java -Xmx512m gov.fnal.ppd.dd.chat.MessagingServerTest; then
    echo Messaging server is there;
else
    echo Messaging server is not present;
fi

