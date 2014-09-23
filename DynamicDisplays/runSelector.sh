. setupJars.sh

window="false"
public="true"
loc="0"

if [ "$1 X" = "WINDOW X" ]; then
    window="true"
    if [ "$2 X" = "XOC X" ]; then
	public="false";
    fi
fi

if [ "$1 X" = "XOC X" ]; then
   public='false';
   if [ "$2 X" = "WINDOW X" ]; then
       window="true"
   fi
fi

if [ "$3 X" != " X" ]; then
    loc=$3;
fi

d=`date +%F`

java -Dddisplay.selector.inwindow=$window \
     -Dddisplay.selector.public=$public \
     -Dddisplay.selector.location=$loc \
     -Dddisplay.messagingserver=$messagingServer \
     -Dddisplay.dbserver=$databaseServer \
     -Xmx512m  gov.fnal.ppd.ChannelSelector > ../log/selector_${d}_$$.log 2>&1  &
