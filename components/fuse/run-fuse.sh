export SSHPASS="$OPENBIS_TOKEN"
sshpass -e sshfs -p${openbis_port} -oKexAlgorithms=+diffie-hellman-group1-sha1 \?@${openbis_host}: /openbis/openbis -o StrictHostKeyChecking=no -o allow_other -f
