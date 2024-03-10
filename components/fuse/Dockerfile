FROM ubuntu:22.04

RUN groupadd -g 1000 rrp
RUN useradd -d /home/rrp -m -s/bin/bash -u 1000 -N -g rrp rrp

RUN apt update
RUN apt install -y sshfs sshpass

RUN echo "user_allow_other" >> /etc/fuse.conf

COPY . /

CMD /run-fuse.sh
