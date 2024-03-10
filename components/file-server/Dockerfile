FROM ubuntu:22.04

RUN groupadd -g 1000 rrp
RUN useradd -d /home/rrp -m -s/bin/bash -u 1000 -N -g rrp -G tty rrp

RUN apt clean
RUN apt update --fix-missing
RUN apt install -y apache2 gettext-base

RUN cp /etc/apache2/apache2.conf /etc/apache2/apache2.conf.original

COPY . /

RUN mkdir -p /httpd-rundir
RUN chown -R rrp:rrp /httpd-rundir
RUN chmod -R 700 /httpd-rundir

RUN ln -s /home/rrp/config.json /frontend/config.json

USER rrp
CMD /create-config-json && rm -rf /httpd-rundir/* && exec apache2 -DFOREGROUND
