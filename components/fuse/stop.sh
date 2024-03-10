#!/bin/sh
ln -s /proc/mounts /etc/mtab
fusermount -u /openbis/openbis
