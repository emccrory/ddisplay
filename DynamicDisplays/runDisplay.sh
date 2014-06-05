. setupJars.sh

displayNum=8
if [ "$1 X" != " X" ]; then
    displayNum=$1;
fi

# java gov.fnal.ppd.signage.display.attic.DisplayAsStandaloneBrowser-display=$displayNum
java gov.fnal.ppd.signage.display.testing.DisplayAsConnectionToFireFox -display=$displayNum

