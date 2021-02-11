#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.Lifecycle.should_start_docker_and_pass_arguments --rm -i busybox <<HERE-WX2NIMHD
echo test
HERE-WX2NIMHD
