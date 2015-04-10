#!/bin/bash
. /home/mccrory/.bashrc

export CLASSPATH=.:../bin:../lib/mysql-connector-java-5.1.27.jar:../lib/slf4j-api-1.5.8.jar:../lib/slf4j-log4j12-1.5.8.jar
export PATH=/usr/krb5/bin:/usr/krb5/bin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/usr/bin:/bin:/usr/games:/usr/local/sbin:/usr/sbin:/sbin:/home/mccrory/bin

# Get the weather at the Dupage County Airport.
/usr/bin/wget -o currentConditions.log -O current.xml  http://w1.weather.gov/xml/current_obs/KDPA.xml 

/usr/bin/java gov.fnal.ppd.dd.news.current_observation current.xml > currentTemperature.txt

cp currentTemperature.txt /var/www/html/newsfeed

