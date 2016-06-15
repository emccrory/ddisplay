#!/bin/bash

(
  # Wait for lock on $HOME/lock/.temperatureScript.exclusivelock (file desriptor 200) for 2 seconds
  flock -x -w 2 200 || exit 1

  . /home/mccrory/.bashrc

  export CLASSPATH=.:../bin:../lib/mysql-connector-java-5.1.27.jar:../lib/slf4j-api-1.5.8.jar:../lib/slf4j-log4j12-1.5.8.jar
  export PATH=/usr/krb5/bin:/usr/krb5/bin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/usr/bin:/bin:/usr/games:/usr/local/sbin:/usr/sbin:/sbin:/home/mccrory/bin
  
  # April, 2016 -- this feed seems to have gone cold.
  # newsURL="http://science.sciencemag.org/rss/twis.xml"
  newsURL="http://rss.sciam.com/ScientificAmerican-Global"
  
  xmlNewsFile=sciencefeed.xml
  logNewsFile=sciencefeed.log
  diff="diff -i -E -w -B "

  rm -f tempN $xmlNewsFile $logNewsFile
  
  /usr/bin/wget -o $logNewsFile -O tempN1 $newsURL # 2>/dev/null
  
  /bin/sed 's/>/>\n/g' tempN1 | sed 's-<channel rdf:about.*>-<channel>-g' | sed 's-<item rdf.*>-<item>-g' | sed 's-&amp;nbsp;- -g' | sed 's/&amp;mdash;/ - /g' | sed 's-&nbsp;- -g' | sed 's/&mdash;/ - /g' | sed 's/&ouml;/o/g' | sed 's/&amp;ouml;/o/g' | sed 's/&amp;rsquo;//g'| sed 's/&rsquo;//g' | sed 's/&amp;lsquo;//g'| sed 's/&lsquo;//g' | sed 's-&lt;/div&gt;- -g' | sed 's-&lt;br/&gt;- -g' | sed 's-<em>--g' | sed 's-</em>--g' | sed 's/\&lt;a href=.*&gt;/ /g' | sed 's-<description>.*-<description>-g' | sed 's-<br/>--g' | sed 's/dc://g' | sed 's/atom://g' | sed 's/media://g' | sed 's/, left,//g' | sed 's/, right,//g' | sed 's/, center,//g' | sed 's- – <a href=.*>- \.\.\.-g' | sed 's/&lt;img .*&gt;//g' | sed 's/ type="html"//g' | sed 's-</p>--g' | sed 's-</i>--g'  > tempN2

  grep -v "\<section xmlns:php=" tempN2 | grep -v "Read more on " |  grep -v "credit>" | grep -v "\]\]>" | grep -v "\<rdf:li resource=" | grep -v "rdf:Seq\>" | grep -v "\[Read More\]</a>" | grep -v CDATA > $xmlNewsFile

replaceIt=0
if [ -s $xmlNewsFile ]; then
    if $diff -q $xmlNewsFile old_$xmlNewsFile > /dev/null ; then
	echo The news from Science has not changed > /dev/null
    else
	replaceIt=1;
    fi
else
    replaceIt=1;
fi

if [ $replaceIt -gt 0 ]; then
    echo Installing new Science news > /dev/null
    # $diff $xmlNewsFile old_$xmlNewsFile

    /usr/bin/java gov.fnal.ppd.dd.xml.news.Channel $xmlNewsFile > science.txt
    cp -p $xmlNewsFile old_$xmlNewsFile
    # --------------------------------------------------------------------------------------
    cat science.txt > /var/www/html/newsfeed/science.txt
    if /usr/krb5/bin/klist > /tmp/k ; then 
        cp science.txt /web/sites/dynamicdisplays.fnal.gov/htdocs/newsfeed
	echo yes there is a kerberos ticket for me > /dev/null
    else 
        echo There seems to be no kerberos ticket, so we cannot copy the newsfeed file to the web directory
    fi
fi


) 200>$HOME/lock/.scienceFeedScript.exclusivelock

rm -f $HOME/lock/.scienceFeedScript.exclusivelock
