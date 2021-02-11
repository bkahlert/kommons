#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name shared-prefix-boot-and-run --rm -i busybox <<HERE-FCMFRW1G
while true; do
    sleep 1
done
HERE-FCMFRW1G
