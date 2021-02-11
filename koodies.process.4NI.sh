#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.Lifecycle.IsRunning.should_stop_started_container --rm -i busybox <<HERE-BU48INVY
while true; do
echo \"looping\"
sleep 1
done
HERE-BU48INVY
