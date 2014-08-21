. setupJars.sh

screenNum=0
if [ "$1 X" != " X" ]; then
    screenNum=$1;
fi

java gov.fnal.ppd.signage.display.testing.DisplayAsConnectionToFireFox -screen=$displayNum

