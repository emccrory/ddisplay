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

java -Dsignage.selector.inwindow=$window -Dsignage.selector.public=$public -Xmx512m  gov.fnal.ppd.ChannelSelector
