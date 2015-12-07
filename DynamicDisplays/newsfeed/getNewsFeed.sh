#!/bin/bash
. /home/mccrory/.bashrc

finalTextFile=breakingnews.txt
interimFile=newNews.txt

MATCH="Fermi Today Headlines"
existingFile=/web/sites/dynamicdisplays.fnal.gov/htdocs/newsfeed/$finalTextFile

sed 's/\r$//g' $existingFile | grep -B37000 "$MATCH" | grep -v "$MATCH" > $interimFile

echo "<b>$MATCH</b>" Retrieved `date` >> $interimFile

export CLASSPATH=.:../bin:../lib/mysql-connector-java-5.1.27.jar:../lib/slf4j-api-1.5.8.jar:../lib/slf4j-log4j12-1.5.8.jar
export PATH=/usr/krb5/bin:/usr/krb5/bin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/usr/bin:/bin:/usr/games:/usr/local/sbin:/usr/sbin:/sbin:/home/mccrory/bin

rssURL="http://news.fnal.gov/rss-faw"
if [ "$1 X" != " X" ]; then
    if [ $1 = "NYT" ]; then
	rssURL="http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml"
    elif [ $1 = "weather" ]; then
	rssURL="http://w1.weather.gov/xml/current_obs/KDPA.rss"
    elif [ $1 = "CBS" ]; then 
	rssURL="http://www.cbsnews.com/latest/rss/main.xml"
    elif [ $i = "CNN" ]; then 
	rssURL="http://rss.cnn.com/rss/cnn_topstories.rss"
    fi
fi

xmlNewsFile=newsfeed.xml
logNewsFile=newsfeed.log
diff="diff -i -E -w -B "

rm -f tempN $xmlNewsFile $logNewsFile

/usr/bin/wget -o $logNewsFile -O tempN $rssURL # 2>/dev/null

/bin/sed 's/>/>\n/g' tempN | grep -v "lastBuildDate" | sed 's-<br/>--g' | sed 's/dc://g' | sed 's/sy://g' | sed 's/content:encoded/comment/g' | sed 's/atom://g' | sed 's/media://g' | sed 's/, left,//g' | sed 's/, right,//g' | sed 's/, center,//g' | sed 's/&lt;img .*&gt;//g' | /bin/sed 's///g'> $xmlNewsFile

if $diff -q $xmlNewsFile old_$xmlNewsFile > /dev/null ; then
  echo The Fermilab news has not changed
else
  echo Installing new Fermilab news

  $diff $xmlNewsFile old_$xmlNewsFile

  /usr/bin/java gov.fnal.ppd.dd.xml.news.Channel $xmlNewsFile >> $interimFile
  cp $interimFile $existingFile
  cp -p $xmlNewsFile old_$xmlNewsFile
fi

