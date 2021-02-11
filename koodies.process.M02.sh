#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name shared-prefix-boot-and-run-program-in-user-session --rm -i busybox <<HERE-9CCPV1JY
while true; do
    sleep 1
done
HERE-9CCPV1JY
