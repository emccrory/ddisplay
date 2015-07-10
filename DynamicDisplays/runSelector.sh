. setupJars.sh

window="false"
public="true"

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

d=`date +%F`

java -Dddisplay.selector.inwindow=$window \
     -Dddisplay.selector.public=$public \
     -Dddisplay.dbserver=$databaseServer \
     -Dddisplay.dbusername=$databaseUsername \
     -Dddisplay.dbpassword=$databasePassword \
     -Dddisplay.virtualdisplaynumbers=TRUE \
     -Xmx512m  gov.fnal.ppd.dd.MakeChannelSelector > ../../log/selector_${d}_$$.log 2>&1  &
