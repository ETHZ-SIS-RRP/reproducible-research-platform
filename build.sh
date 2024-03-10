#!/bin/bash

if [ $# -eq 0 ]
then
  echo "usage: ./build.sh <registry>"
  echo "example: ./build.sh sissource.ethz.ch:5005/sis/rrp/reproducible-research-platform"
  echo "         ./build.sh dev-cluster.local:5000/sis/rrp"
  exit 255
fi

registry=$1
registry_host=$(echo $registry|awk -F'/' '{print $1}')

docker login $registry_host

if [ $? -ne 0 ]
then
  echo "registry authorization failed - aborting"
  exit 255
fi

echo "building to $registry"
echo ""

pushd .
source_dir=$(dirname $0)
cd "$source_dir"
./gradlew -Pregistry=$registry clean build
popd
