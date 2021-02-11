#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.Lifecycle.IsRunning.should_return_false_on_completed_container --rm -i busybox <<HERE-ML9BNRS8
while true; do
echo \"looping\"
sleep 1
done
HERE-ML9BNRS8
