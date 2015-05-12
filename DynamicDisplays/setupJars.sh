export CLASSPATH=bin:lib/mysql-connector-java-5.1.27.jar:lib/slf4j-api-1.5.8.jar:lib/slf4j-log4j12-1.5.8.jar

# Backup database server is mccrory
export databaseServer="mccrory.fnal.gov"

# Read the database credentials from the secret file
array=()

getArray() {
    i=0
    while read line
    do
	array[i]=$line
	i=$(($i + 1))
    done < $1
}

getArray "$HOME/keystore/credentials.txt"

export databaseServer=${array[0]}:${array[1]}
export databaseName=${array[2]}
export databaseUsername=${array[3]}
export databasePassword=${array[4]}

# echo $databaseServer $databaseUsername $databasePassword 

export PATH=$PATH:/Applications/Firefox.app/Contents/MacOS