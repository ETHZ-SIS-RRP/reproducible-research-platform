FROM ubuntu:22.04

RUN groupadd -g 1000 rrp
RUN useradd -d /home/rrp -m -s/bin/bash -u 1000 -N -g rrp rrp

RUN apt clean && apt update --fix-missing
RUN apt install -y ca-certificates curl gnupg
RUN install -m 0755 -d /etc/apt/keyrings
RUN curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
ENV NODE_MAJOR=20
RUN echo \
    "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_$NODE_MAJOR.x nodistro main" | \
    tee /etc/apt/sources.list.d/nodesource.list
RUN apt update
RUN apt install -y nodejs

COPY ./src/proxy.js /home/rrp/proxy.js

USER rrp
WORKDIR /home/rrp
RUN npm install http-proxy@1.18.1 crypto-js@4.1.1
CMD node /home/rrp/proxy.js
