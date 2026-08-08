FROM azul-zulu:25-jdk-alpine AS base
RUN apk update && apk add curl jq git
RUN curl -fsSLo /mc-server-runner.tar.gz https://github.com/itzg/mc-server-runner/releases/download/1.15.1/mc-server-runner_1.15.1_linux_amd64.tar.gz && \
    tar -xzf /mc-server-runner.tar.gz -C / && \
    rm /mc-server-runner.tar.gz && mv /mc-server-runner /usr/bin/mc-server-runner
RUN curl -fsSLo /packwiz-installer.jar https://github.com/packwiz/packwiz-installer-bootstrap/releases/download/v0.0.3/packwiz-installer-bootstrap.jar && \
    curl -fsSLo /fabric-installer.jar https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.1/fabric-installer-1.1.1.jar
COPY --from=hengyunabc/arthas:4.1.1-no-jdk /opt/arthas /opt/arthas

FROM base AS sfcraft-builder
COPY . /mod
RUN  cd /mod && \
    ./gradlew shadowJar && \
    cp /mod/build/libs/sfcraft-*-all.jar /sfcraft.jar

FROM base AS justbackup-builder
RUN git clone https://github.com/saltedfishclub/justbackup /mod
RUN cd /mod && \
    ./gradlew build && \
    rm -f /mod/build/libs/*sources.jar /mod/build/libs/*dev.jar && \
    cp /mod/build/libs/justbackup-1.0.0.jar /justbackup.jar

FROM base AS carpet-builder
RUN git clone https://github.com/saltedfishclub/fabric-carpet /mod
RUN cd /mod && \
    ./gradlew build && \
    rm -f /mod/build/libs/*sources.jar /mod/build/libs/*dev.jar && \
    cp /mod/build/libs/*.jar /fabric-carpet.jar

FROM base
WORKDIR /deployment
COPY server-deploy/pack /deployment/pack
RUN MC_VERSION="$(grep '^minecraft' /deployment/pack/pack.toml | head -n1 | cut -d'"' -f2)" && \
    LOADER_VERSION="$(grep '^fabric' /deployment/pack/pack.toml | head -n1 | cut -d'"' -f2)" && \
    if [ -z "$MC_VERSION" ] || [ -z "$LOADER_VERSION" ]; then \
        echo "failed to read minecraft/fabric version from pack.toml" >&2; exit 1; \
    fi && \
    echo "Installing Fabric server (MC=$MC_VERSION, loader=$LOADER_VERSION)" && \
    java -jar /fabric-installer.jar server -mcversion "$MC_VERSION" -loader "$LOADER_VERSION" -downloadMinecraft
RUN java -jar /packwiz-installer.jar -g -s server /deployment/pack/pack.toml
COPY server-deploy/overrides /deployment
COPY --from=sfcraft-builder /sfcraft.jar /deployment/mods/sfcraft.jar
COPY --from=justbackup-builder /justbackup.jar /deployment/mods/justbackup.jar
COPY --from=carpet-builder /fabric-carpet.jar /deployment/mods/fabric-carpet.jar
VOLUME ["/deployment/config","/deployment/world","/deployment/logs","/deployment/crash-reports", "/deployment/squaremap", "/deployment/backups"]
ENTRYPOINT ["mc-server-runner", "/deployment/run.sh"]