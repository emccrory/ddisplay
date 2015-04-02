#!/bin/bash

export CLASSPATH=.:../bin:../lib/mysql-connector-java-5.1.27.jar:../lib/slf4j-api-1.5.8.jar:../lib/slf4j-log4j12-1.5.8.jar

echo $CLASSPATH
echo $PATH

# feedURL="http://www.cbsnews.com/latest/rss/main cbs.xml"
# feedURL="http://rss.cnn.com/rss/cnn_topstories.rss"
feedURL="http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml"

xmlFile=newsfeed.xml
logFile=newsfeed.log

rm -f temp $xmlFile $logFile

wget -o $logFile -O temp $feedURL # 2>/dev/null

sed 's/>/>\n/g' temp | sed 's/dc://g' | sed 's/atom://g' | sed 's/media://g' > $xmlFile

ls -lt

echo

/usr/bin/java gov.fnal.ppd.dd.news.Channel $xmlFile |tee headlines.txt

cp headlines.txt /var/www/html/newsfeed
