#!/bin/bash

set -e

echo "export-image.sh"
echo "$@"
source_registry=$1
source_image=$2
target_image=$3

mkdir ~/.docker
jq -s '.[0] * .[1]'  /etc/internal-registry-credentials/.dockerconfigjson /etc/export-registry-credentials/.dockerconfigjson > /home/rrp/.docker/config.json
/regctl registry set --tls=insecure $source_registry
/regctl -v info image copy $source_image $target_image

rv=$?
echo "Complete with code $rv"
exit $rv
