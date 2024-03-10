#!/bin/bash

# image dssurl token datasetid

export image=$1
shift

mkdir ~/.docker
cp /etc/registry-credentials/.dockerconfigjson /home/rrp/.docker/config.json

/transport/bin/download-dataset "$@" .

/regctl registry set --tls=insecure $registry
/regctl -v debug image import $image ~/original/DEFAULT/image.tar
tar xvf ~/original/DEFAULT/repository.tar --strip-components=1 -C /repo

rv=$?
echo "Complete with code $rv"
exit $rv
