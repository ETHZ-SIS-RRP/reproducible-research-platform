#!/bin/bash
/transport/bin/create-dataset "$@"
rv=$?
echo "Complete with code $rv"
exit $rv
