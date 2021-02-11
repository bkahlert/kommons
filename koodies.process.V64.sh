#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.Lifecycle.should_start_docker_and_process_output --rm -i busybox <<HERE-G03C2LJK
while true; do
echo \"looping\"
sleep 1
done
HERE-G03C2LJK
