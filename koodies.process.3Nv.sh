#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.Lifecycle.IsRunning.should_return_true_on_running_container --rm -i busybox <<HERE-0DQA51VH
while true; do
echo \"looping\"
sleep 1
done
HERE-0DQA51VH
