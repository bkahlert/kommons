#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.Lifecycle.should_remove_docker_container_after_completion --rm -i busybox <<HERE-7M77DF00
while true; do
echo \"looping\"
sleep 1
done
HERE-7M77DF00
