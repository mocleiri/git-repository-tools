#!/bin/bash

JAR_FILE=~/svn-history-tools-0.0.1-SNAPSHOT.jar 
PIPE=/tmp/filter-dump-files.pipe
DELETE=$1

# delete any existing filtered dump files
if test -n "$DELETE"
then
    
    echo "rm -f *-filtered.dump"
    rm -f *-filtered.dump

fi


echo "rm -f \"$PIPE\""

rm -f "$PIPE"

mkfifo $PIPE

echo "exec java -jar $JAR_FILE < $PIPE > java.log &"
$(java -Xmx1024m -jar $JAR_FILE < $PIPE > java.log) &

JAVA_PID=$!

JOIN_WORK=$(ls -l *-join.dat | awk '{print $9}')

for WORK in $JOIN_WORK
do

    TARGET_REV=$(echo $WORK | cut -d- -f1)

    FILTERED_DUMP="$TARGET_REV-filtered.dump"

    
    if test -f $FILTERED_DUMP
    then
        echo "$FILTERED_DUMP exists, skipping"
        continue
    fi

    echo "Processing $WORK" 
    echo "$WORK" >> $PIPE
    
done

# hope to close the pipe
cat /dev/null >> $PIPE

echo "wait $JAVA_PID"
wait $JAVA_PID
