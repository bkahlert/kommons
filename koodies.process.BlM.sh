#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.should_start_docker --rm -i busybox <<HERE-OS5570NN
echo test
HERE-OS5570NN
