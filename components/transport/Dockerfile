FROM ubuntu:22.04
ARG TARGETARCH

RUN groupadd -g 1000 rrp
RUN useradd -d /home/rrp -m -s/bin/bash -u 1000 -N -g rrp rrp

RUN apt clean
RUN apt update --fix-missing
RUN mkdir -p /etc/ssl/certs/java

RUN apt install -y ca-certificates-java
RUN apt install -y openjdk-17-jdk wget git gettext-base jq zip

COPY ./regctl-linux-$TARGETARCH-v0.4.5 /regctl
COPY ./openbis-transport /transport
COPY ./archive-project.sh /archive-project.sh
COPY ./archive-results.sh /archive-results.sh
COPY ./unarchive-project.sh /unarchive-project.sh
COPY ./create-offline-bundle.sh /create-offline-bundle.sh
COPY ./play.template /play.template
COPY ./play.ps1.template /play.ps1.template
COPY ./play.bat /play.bat
COPY ./export-image.sh /export-image.sh

USER rrp
WORKDIR /home/rrp
