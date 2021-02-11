#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.Lifecycle.should_start_docker_and_process_input --rm -i busybox <<HERE-Q0RDYRIP
echo test
HERE-Q0RDYRIP
