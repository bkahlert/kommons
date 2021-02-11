#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.Lifecycle.IsRunning.should_stop_started_container --rm -i busybox <<HERE-XN68H8B7
while true; do
echo \"looping\"
sleep 1
done
HERE-XN68H8B7
