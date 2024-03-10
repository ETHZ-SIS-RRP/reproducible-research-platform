#!/bin/bash

set -e

echo "create-offline-bundle.sh"
echo "$@"

image_name=$1
bundle_name=$4
registry=$5

mkdir -p ${bundle_name}/openbis
mkdir -p ${bundle_name}/results

mkdir ~/.docker
cp /etc/registry-credentials/.dockerconfigjson /home/rrp/.docker/config.json

/regctl registry set --tls=insecure $registry
/regctl -v debug image export ${image_name} ~/${bundle_name}/image.tar

cp -rp /repo repo
cd repo
git config --unset user.name
git config --unset user.email
git config --global --add safe.directory /home/jovyan/project
cd ..

cp -rp repo ${bundle_name}

/transport/bin/download-player-datasets "$2" "$3" ${bundle_name}/repo/.rrp/datasets.yaml ./${bundle_name}/openbis

shift 5

export FULL_IMAGE=$image_name
export IMAGE=$(echo "$image_name" | awk -F/ '{print $NF}')
export TAG=$(date +%Y-%m-%d_%H%M%S)
export COMMAND="$@"
envsubst '${FULL_IMAGE},${IMAGE},${TAG},${COMMAND}'< /play.template > ${bundle_name}/play
envsubst '${FULL_IMAGE},${IMAGE},${TAG},${COMMAND}'< /play.ps1.template > ${bundle_name}/play.ps1
cp /play.bat  ${bundle_name}/play.bat
chmod 755 ${bundle_name}/play

zip -r - ${bundle_name} > /output/${bundle_name}.zip

rv=$?
echo "Complete with code $rv"
exit $rv
