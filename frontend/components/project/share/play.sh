#!/bin/bash

for program in docker
do
  if ! command -v $program &> /dev/null
  then
      echo "$program was not found in \$PATH"
      exit -1
  fi
done

here=$(cd $(dirname "${BASH_SOURCE:-$0}") && pwd)
pushd . >/dev/null
cd $here

if [ ! "$(docker ps -q -f name=rrp-player)" ]
then

  if [ ! -f openbis/.download-complete ]
  then
    download_image=$(cat ./download-image.txt)
    docker run \
        -it \
        --rm \
        --pull always \
        --mount type=bind,source="${here}",target=/home/rrp/host \
        $download_image
  fi

  if [[ $? -ne 0 ]]
  then
    echo "Downloading externally stored data was not successful"
    exit -1
  fi

  mkdir -p results

  image=$(cat ./image.txt)
  command=$(cat ./command.txt)
  docker pull $image

  docker run \
      -d \
      --rm \
      --publish 8888:8888 \
      --mount type=bind,source="${here}"/repo,target=/home/jovyan/project \
      --mount type=bind,source="${here}"/openbis,target=/home/jovyan/openbis \
      --mount type=bind,source="${here}"/results,target=/home/jovyan/results \
      --workdir /home/jovyan \
      --entrypoint /bin/bash \
      --name rrp-player \
      $image \
      -c "$command"

  if [[ $? -ne 0 ]]
  then
    echo "Pulling image $image failed"
    exit -1
  fi

  echo -n "Waiting for the container to start"
  while [[ $(docker exec -it rrp-player cat /proc/net/tcp 2>/dev/null|grep 22B8) = "" ]]; do
    echo -n "."
    sleep 2
  done
  echo ""
fi

if [[ $(uname) == Darwin ]]
  then
    open_cmd=open
  else
    open_cmd=xdg-open
fi

$open_cmd http://localhost:8888/lab

popd >/dev/null
