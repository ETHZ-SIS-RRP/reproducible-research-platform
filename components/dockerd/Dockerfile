FROM ubuntu:22.04
ARG TARGETARCH

RUN groupadd -g 1000 docker
RUN useradd -d /home/rrp -m -s/bin/bash -u 1000 -N -g docker rrp

RUN apt clean
RUN apt update --fix-missing
RUN mkdir -p /etc/ssl/certs/java

RUN apt install -y wget kmod uidmap iptables ca-certificates libdevmapper1.02.1 slirp4netns iproute2 fuse-overlayfs dbus-user-session dnsutils --no-install-recommends

RUN wget -q https://download.docker.com/linux/ubuntu/dists/jammy/pool/stable/$TARGETARCH/containerd.io_1.6.6-1_$TARGETARCH.deb
RUN wget -q https://download.docker.com/linux/ubuntu/dists/jammy/pool/stable/$TARGETARCH/docker-ce_20.10.17~3-0~ubuntu-jammy_$TARGETARCH.deb
RUN wget -q https://download.docker.com/linux/ubuntu/dists/jammy/pool/stable/$TARGETARCH/docker-ce-cli_20.10.17~3-0~ubuntu-jammy_$TARGETARCH.deb
RUN wget -q https://download.docker.com/linux/ubuntu/dists/jammy/pool/stable/$TARGETARCH/docker-ce-rootless-extras_20.10.17~3-0~ubuntu-jammy_$TARGETARCH.deb

RUN dpkg -i ./containerd.io_1.6.6-1_$TARGETARCH.deb
RUN dpkg -i ./docker-ce-cli_20.10.17~3-0~ubuntu-jammy_$TARGETARCH.deb
RUN dpkg -i ./docker-ce_20.10.17~3-0~ubuntu-jammy_$TARGETARCH.deb
RUN dpkg -i ./docker-ce-rootless-extras_20.10.17~3-0~ubuntu-jammy_$TARGETARCH.deb

RUN mkdir -p /opt
RUN chmod -R 777 /opt

USER rrp
WORKDIR /home/rrp

RUN /usr/bin/dockerd-rootless-setuptool.sh --skip-iptables install

RUN echo "export XDG_RUNTIME_DIR=/home/rrp/.docker/run" >> /home/rrp/.bashrc
RUN echo "export PATH=/usr/bin:$PATH" >> /home/rrp/.bashrc
RUN echo "export DOCKER_HOST=unix:///home/rrp/.docker/run/docker.sock" >> /home/rrp/.bashrc
RUN echo "export DOCKERD_ROOTLESS_ROOTLESSKIT_SLIRP4NETNS_SECCOMP=false" >> /home/rrp/.bashrc

RUN mkdir -p /home/rrp/.config/docker/
RUN mkdir -p /home/rrp/.docker/

CMD cp /etc/registry-credentials/.dockerconfigjson /home/rrp/.docker/config.json && \
    DOCKERD_ROOTLESS_ROOTLESSKIT_SLIRP4NETNS_SECCOMP=false \
    XDG_RUNTIME_DIR=/home/rrp/.docker/run \
    DOCKER_HOST=unix:///home/rrp/.docker/run/docker.sock \
    dockerd-rootless.sh \
    --exec-root /tmp \
    --insecure-registry $registry & while ! test -f /home/rrp/.docker/run/done; do echo 'Waiting for the agent pod to finish...'; sleep 5; done && exit 0
