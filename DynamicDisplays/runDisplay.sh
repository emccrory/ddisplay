. setupJars.sh

displayNum=8
if [ "$1 X" != " X" ]; then
    displayNum=$1;
fi

# java gov.fnal.ppd.signage.display.testing.DisplayAsStandaloneBrowser-display=$displayNum
java gov.fnal.ppd.signage.display.testing.DisplayAsConnectionToFirefox -display=$displayNum

