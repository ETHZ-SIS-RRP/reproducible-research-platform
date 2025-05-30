FROM --platform=linux/amd64 ubuntu:22.04

SHELL ["/bin/bash", "-i", "-c"]

ARG repo2docker_version
RUN echo "--- repo2docker $repo2docker_version ---"

RUN groupadd -g 1000 rrp
RUN useradd -d /home/rrp -m -s/bin/bash -u 1000 -N -g rrp rrp

RUN apt update
RUN apt install -y curl dnsutils

# qemu needed to run repo2docker on apple silicon: https://github.com/jupyterhub/repo2docker/issues/1120
RUN apt install -y qemu-user-static binfmt-support

RUN ln -s /docker-run/docker.sock /var/run/docker.sock

USER rrp
WORKDIR /home/rrp

COPY traitlets.config.json /home/rrp/traitlets.config.json

RUN curl https://repo.anaconda.com/miniconda/Miniconda3-py310_22.11.1-1-Linux-x86_64.sh -o ./miniconda.sh

RUN chmod +x ~/miniconda.sh
RUN bash ./miniconda.sh -b -p ./miniconda3
RUN ./miniconda3/bin/conda init
RUN source ./.bashrc && conda install -y ruamel.yaml=0.16.12 && python3 -m pip install jupyter-repo2docker==$repo2docker_version ruamel_yaml

RUN mkdir -p /home/rrp/.docker/

RUN sed -e s/rstudio.com/posit.co/g -i miniconda3/lib/python3.10/site-packages/repo2docker/buildpacks/r.py

CMD source ./.bashrc && while [ ! -e /docker-run/docker.sock ]; do sleep 1; done && \
  cp /etc/registry-credentials/.dockerconfigjson /home/rrp/.docker/config.json && \
  trap "rv=\${PIPESTATUS[0]}; touch /docker-run/done; echo \"\" >> /log/build.log; if [ \"\$rv\" -eq \"0\" ]; then touch /log/build.log.rrp; else echo \"\$rv\" > /log/build.log.rrp; fi" EXIT && \
  jupyter-repo2docker --config traitlets.config.json --no-run --push --debug --image-name $image_name --user-name jovyan --target-repo-dir /home/jovyan/build /repository 2>&1 | tee -a /log/build.log
