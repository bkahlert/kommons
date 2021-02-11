#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.should_start_docker --rm -i busybox <<HERE-INHB0TDO
echo test
HERE-INHB0TDO
