#!/bin/bash

tempFile=fermiToday$$.html
makeTodayFile=1
date=`date +%Y-%m-%d`
dateToSearch=`date +"%A, %B %d, %Y" | sed 's/ 0/ /g'`
dateToSearch2=`date +"%A, %b. %d, %Y" | sed 's/ 0/ /g'`
dateToSearch4=`date +"%A, %b.%d, %Y" | sed 's/ 0/ /g'`
dateToSearch3=`echo $dateToSearch2 | sed 's/Sep\./Sept\./g'`

url="http://www.fnal.gov/pub/today"
if [ "$1 X" != " X" ]; then
    makeTodayFile=0;
    date=$1
    Y=`echo $date | awk -F\- '{ print $1 }'`
    y=`echo $Y | sed s/20//`
    m=`echo $date | awk -F\- '{ print $2 }'`
    d=`echo $date | awk -F\- '{ print $3 }'`
    dd=`echo $d | sed s/01/1/| sed s/02/2/| sed s/03/3/| sed s/04/4/| sed s/05/5/| sed s/06/6/| sed s/07/7/| sed s/08/8/| sed s/09/9/`
    # http://www.fnal.gov/pub/today/archive/archive_2013/today13-10-23.html
    url="http://www.fnal.gov/pub/today/archive/archive_$Y/today$y-$m-$d.html";
    echo "Getting photo of the day for " $date " from " $url
    dateToSearch="$dd, $Y";
fi

/usr/bin/wget $url -O $tempFile 2>/dev/null

# -------------------------------------------------------------------------------------------------------
# This IF/THEN/ELSE block is supposed to reject holidays when FT is not published.  The date embedded
# into the HTML source needs to match today's date, which seems to be of the form, "Monday, June 2, 2014"
# (Note the commas, and lack of leading "0" in the month day).
if grep -q "$dateToSearch" $tempFile ; then
    echo We have the right day A > /dev/null ;
else
    if grep -q "$dateToSearch2" $tempFile; then
        echo We found the right day B >/dev/null
    else
	if grep -q "$dateToSearch3" $tempFile; then
	    echo We found the right day C >/dev/null
	else
	    if grep -q "$dateToSearch4" $tempFile; then
		echo We found the right day D >/dev/null
	    else
        # Send an error message to the cron owner to catch problems (if this was done in error)
		echo It seems that todays Fermi Today is not dated $dateToSearch or $dateToSearch2 or $dateToSearch3 or $dateToSearch4
		echo Bye
		exit -1;
	    fi
	fi
    fi
fi
# -------------------------------------------------------------------------------------------------------

MATCH="Fermi Today Headlines"
existingFile=/web/sites/dynamicdisplays.fnal.gov/htdocs/newsfeed/breakingnews.txt

sed 's/\r$//g' $existingFile | grep -B37000 "$MATCH" | grep -v "$MATCH" > newNews.txt

echo "<b>$MATCH</b>" Retrieved `date` >> newNews.txt

# Old Way:
# egrep "<\!--//======="\|"class=\"headline\"" $tempFile | grep -v END | grep -v "\[" | grep -v "= header =" | grep -v "= line" | grep -v "= nav" | grep -v "=============================================" | grep -v "==== Calendar " | grep -v "==== Announcements " | grep -v "==== Archives " | grep -v "========= Info " | ./parseHeadlinesToday.pl >> newNews.txt

# New way
egrep '<td class="header-|class="headline"' $tempFile | grep -v Announcements | grep -v Archives | grep -v wilson_hall_cafe | grep -v "header-info" | ./parseHeadlinesToday2.pl >> newNews.txt

rm -f $tempFile

cp newNews.txt $existingFile

