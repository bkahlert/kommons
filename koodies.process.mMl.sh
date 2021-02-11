#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.Lifecycle.IsRunning.should_return_false_on_not_yet_started_container_container --rm -i busybox <<HERE-FY1M7HCN
while true; do
echo \"looping\"
sleep 1
done
HERE-FY1M7HCN
