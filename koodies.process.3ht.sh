#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name shared-prefix-boot-and-run-program-in-user-session --rm -i busybox <<HERE-2ZPMDDLM
while true; do
    sleep 1
done
HERE-2ZPMDDLM
