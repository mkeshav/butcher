FROM openjdk:8-slim as base
LABEL Author="Keshav Murthy"

RUN apt-get update && apt-get install -y gnupg2 apt-transport-https

RUN \
  echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
  apt-key adv --keyserver hkps://keyserver.ubuntu.com:443 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
  apt-get update && \
  apt-get install -y \
    sbt=1.3.2 \
    wget bsdtar \
    ca-certificates

RUN update-ca-certificates

WORKDIR /app

#Create docker container with the dependencies so subsequent builds are faster
ADD build.sbt ./
ADD ./project/PackagingTypeWorkaround.scala ./project/
ADD ./project/plugins.sbt ./project/
RUN sbt update

FROM base as release
ADD . /app

FROM base as sonar
ENV SONAR_SCANNER_VERSION 4.2.0.1873
RUN wget -qO- https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}-linux.zip | bsdtar -xvf - -C /usr/local/
RUN chmod +x /usr/local/sonar-scanner-${SONAR_SCANNER_VERSION}-linux/bin/sonar-scanner
RUN chmod +x /usr/local/sonar-scanner-${SONAR_SCANNER_VERSION}-linux/jre/bin/java
RUN ln -s /usr/local/sonar-scanner-${SONAR_SCANNER_VERSION}-linux/bin/sonar-scanner /usr/bin/sonar-scanner
ADD . /app

# --- dev image
FROM base as dev
RUN apt-get install -y curl python3
RUN update-alternatives --install /usr/bin/python python /usr/bin/python3 10
RUN \
    curl -L https://github.com/scalacenter/bloop/releases/download/v1.3.2/install.py | python
RUN \
    curl -L https://github.com/lihaoyi/Ammonite/releases/download/1.8.2/2.12-1.8.2 > /usr/local/bin/amm && chmod +x /usr/local/bin/amm
ENV PATH="/root/.bloop:${PATH}"
ADD . /app
