#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name DockerProcessTest.should_not_produce_incorrect_empty_lines --rm -i busybox <<HERE-FSKSH0JL
while true; do
echo \"looping\"
sleep 1
done
HERE-FSKSH0JL
