#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.should_override_toString --rm -i busybox <<HERE-AVZOVQFQ
echo test
HERE-AVZOVQFQ
