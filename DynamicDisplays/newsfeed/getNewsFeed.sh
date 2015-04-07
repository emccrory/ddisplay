#!/bin/bash
. /home/mccrory/.bashrc

export CLASSPATH=.:../bin:../lib/mysql-connector-java-5.1.27.jar:../lib/slf4j-api-1.5.8.jar:../lib/slf4j-log4j12-1.5.8.jar
export PATH=/usr/krb5/bin:/usr/krb5/bin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/usr/bin:/bin:/usr/games:/usr/local/sbin:/usr/sbin:/sbin:/home/mccrory/bin

# feedURL="http://www.cbsnews.com/latest/rss/main cbs.xml"
# feedURL="http://rss.cnn.com/rss/cnn_topstories.rss"
newsURL="http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml"
weatherURL="http://w1.weather.gov/xml/current_obs/KDPA.rss"

xmlNewsFile=newsfeed.xml
logNewsFile=newsfeed.log

xmlWeatherFile=weatherfeed.xml
logWeatherFile=weatherfeed.log

rm -f temp tempN tempW $xmlNewsFile $logNewsFile $xmlWeatherFile $logWeatherFile

/usr/bin/wget -o $logNewsFile -O tempN $newsURL # 2>/dev/null

/bin/sed 's/>/>\n/g' tempN | sed 's-<br/>--g' | sed 's/dc://g' | sed 's/atom://g' | sed 's/media://g' | sed 's/, left,//g' | sed 's/, right,//g' | sed 's/, center,//g' | sed 's/&lt;img .*&gt;//g' > $xmlNewsFile

/usr/bin/java gov.fnal.ppd.dd.news.Channel $xmlNewsFile > headlines.txt

# ---------------------------------------------------------------------------------------
# The weather feed has a lot of blank lines in it.  Some of this is to remove them

/usr/bin/wget -o $logWeatherFile -O tempW $weatherURL # 2>/dev/null

/bin/sed 's/>/>\n/g' tempW |  sed 's-<br/>--g' | sed 's/dc://g' | sed 's/atom://g' | sed 's/media://g' | sed 's/, left,//g' | sed 's/, right,//g' | sed 's/, center,//g' | /bin/sed 's///g' > $xmlWeatherFile

/usr/bin/java gov.fnal.ppd.dd.news.Channel $xmlWeatherFile > weather.txt

# --------------------------------------------------------------------------------------

cat headlines.txt weather.txt > /var/www/html/newsfeed/headlines.txt

