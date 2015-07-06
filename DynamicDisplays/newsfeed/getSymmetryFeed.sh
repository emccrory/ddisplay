#!/bin/bash


(
  # Wait for lock on $HOME/lock/.temperatureScript.exclusivelock (file desriptor 200) for 2 seconds
  flock -x -w 2 200 || exit 1

  . /home/mccrory/.bashrc
  
  export CLASSPATH=.:../bin:../lib/mysql-connector-java-5.1.27.jar:../lib/slf4j-api-1.5.8.jar:../lib/slf4j-log4j12-1.5.8.jar
  export PATH=/usr/krb5/bin:/usr/krb5/bin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/usr/bin:/bin:/usr/games:/usr/local/sbin:/usr/sbin:/sbin:/home/mccrory/bin
  
  newsURL="http://www.interactions.org/index.rss"
  
  xmlNewsFile=symfeed.xml
  logNewsFile=symfeed.log
  
  rm -f temp tempN tempW $xmlNewsFile $logNewsFile
  
  /usr/bin/wget -o $logNewsFile -O tempN $newsURL # 2>/dev/null
  
  /bin/sed 's/>/>\n/g' tempN | sed 's-<br/>--g' | sed 's/dc://g' | sed 's/atom://g' | sed 's/media://g' | sed 's/, left,//g' | sed 's/, right,//g' | sed 's/, center,//g' | sed 's/&lt;img .*&gt;//g' > $xmlNewsFile
  
  /usr/bin/java gov.fnal.ppd.dd.xml.news.Channel $xmlNewsFile 10 | head -n 15 | sed 's/null//g' | sed 's/--//g' | sed 's/  //g' > symmetry.txt
  
# --------------------------------------------------------------------------------------
  
  cp symmetry.txt /var/www/html/newsfeed

  #  if [ -e /web/sites/dynamicdisplays.fnal.gov/htdocs/newsfeed ]; then
  if /usr/krb5/bin/klist > /tmp/k ; then 
      cp symmetry.txt /web/sites/dynamicdisplays.fnal.gov/htdocs/newsfeed
  else
      echo There seems to be no kerberos ticket, so we cannot copy the newsfeed file to the web directory
  fi
  
) 200>$HOME/lock/.symmetryFeedScript.exclusivelock

rm -f $HOME/lock/.symmetryFeedScript.exclusivelock




