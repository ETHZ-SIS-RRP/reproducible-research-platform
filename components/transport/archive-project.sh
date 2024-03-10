#!/bin/bash
mkdir data
export registry=$1
export image=$2
export tool=$3

mkdir ~/.docker
cp /etc/registry-credentials/.dockerconfigjson /home/rrp/.docker/config.json

shift 3
/regctl registry set --tls=insecure $registry
/regctl -v debug image export $registry/$image ~/data/image.tar

cp -rp /repo repo
cd repo
git config --unset user.name
git config --unset user.email
cd ..

tar cvf ~/data/repository.tar repo

echo $tool > ~/data/repo2docker-version
echo $image > ~/data/image-name

/transport/bin/create-dataset "$@"
rv=$?
echo "Complete with code $rv"
exit $rv
