FROM ubuntu:22.04
ARG TARGETARCH

RUN groupadd -g 1000 rrp
RUN useradd -d /home/rrp -m -s/bin/bash -u 1000 -N -g rrp rrp

RUN apt clean
RUN apt update --fix-missing
RUN mkdir -p /etc/ssl/certs/java

RUN apt install -y ca-certificates-java
RUN apt install -y openjdk-17-jdk wget

COPY ./application /application

USER rrp
WORKDIR /home/rrp
CMD /application/bin/application
