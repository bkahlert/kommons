#!/bin/sh
cd "/Users/bkahlert/Development/com.bkahlert/koodies" || exit -1
docker run --name shared-prefix-boot-and-run --rm -i busybox <<HERE-N3P8YUKR
while true; do
    sleep 1
done
HERE-N3P8YUKR
