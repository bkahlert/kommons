#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.should_start_docker --rm -i busybox <<HERE-PQLRFKAG
echo test
HERE-PQLRFKAG
