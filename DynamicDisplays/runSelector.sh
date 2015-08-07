. setupJars.sh

window="true"
public="false"

if [ "$1 X" = "FULL X" ]; then
    window="false"
    if [ "$2 X" = "PUBLIC X" ]; then
	public="true";
    fi
fi

if [ "$1 X" = "XOC X" ]; then
   public='false';
   if [ "$2 X" = "WINDOW X" ]; then
       window="true"
   fi
fi

d=`date +%F`

#java -Dddisplay.selector.inwindow=$window \
#     -Dddisplay.selector.public=$public \
#     -Dddisplay.dbserver=$databaseServer \
#     -Dddisplay.dbusername=$databaseUsername \
#     -Dddisplay.dbpassword=$databasePassword \
#     -Dddisplay.virtualdisplaynumbers=TRUE \
#     -Xmx512m  gov.fnal.ppd.dd.MakeChannelSelector > ../../log/selector_${d}_$$.log 2>&1  &

java -Dddisplay.selector.inwindow=$window \
     -Dddisplay.selector.public=$public \
     -Dddisplay.virtualdisplaynumbers=TRUE \
     -Xmx512m  gov.fnal.ppd.dd.MakeChannelSelector > ../../log/selector_${d}_$$.log 2>&1  &
