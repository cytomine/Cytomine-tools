#
# Copyright (c) 2009-2020. Authors: see NOTICE file.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM cytomine/java8:v1.2.0

MAINTAINER Cytomine Team "support@cytomine.be"

# Miniconda Python environment
RUN wget https://repo.continuum.io/miniconda/Miniconda3-4.7.12.1-Linux-x86_64.sh
RUN bash Miniconda3-4.7.12.1-Linux-x86_64.sh -b

ENV PATH /root/miniconda3/bin:$PATH 

RUN apt-get -y update && apt-get install -y \
      apt-transport-https \
      ca-certificates \
      curl \
      git \
      g++ \
      language-pack-en-base \
      libglib2.0-0 \
      libsm6 \
      libxext6 \
      libxrender1 \
      iptables \
      make \
      zip

RUN dpkg-reconfigure locales

# Cytomine-python-client

RUN mkdir -p /root/Cytomine/ && cd /root/Cytomine/ && git clone https://github.com/cytomine/Cytomine-python-client.git && cd Cytomine-python-client/ && git checkout v2.1.1
RUN cd /root/Cytomine/Cytomine-python-client/ && python setup.py build && python setup.py install


# Groovy Installation
RUN rm /bin/sh && ln -s /bin/bash /bin/sh
ENV CURL_CA_BUNDLE /etc/ssl/certs/ca-certificates.crt
RUN cd /tmp/ && curl http://get.sdkman.io | bash
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh && sdk install groovy 2.4.4 --yes"


RUN mkdir -p /software_router && cd /software_router && wget https://github.com/cytomine/Cytomine-software-router/releases/download/v1.1.1/Cytomine-software-router.jar -O Cytomine-software-router.jar
RUN cd /software_router && wget https://github.com/cytomine/Cytomine-java-client/releases/download/v1.9.9/cytomine-java-client.jar -O cytomine-java-client.jar

# Fix path
ENV GROOVY_HOME /root/.sdkman/candidates/groovy/current
ENV GROOVY_PATH /root/.sdkman/candidates/groovy/current/bin/groovy

RUN mkdir -p /software_router/algo

RUN touch /tmp/addHosts.sh
ADD deploy.sh /tmp/deploy.sh
RUN chmod +x /tmp/deploy.sh

ENTRYPOINT ["/tmp/deploy.sh"]
