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
# 跟随 MC 版本的分支。26.3 分支需先在上游仓库建好，否则此处会直接失败——
# 这是刻意的:默认分支仍是 26.2(fabric.mod.json 要求 minecraft ~26.2),
# 构建进 26.3 镜像只会在加载时才报错,不如在构建期暴露。
ARG JUSTBACKUP_REF=26.3
RUN git clone -b "$JUSTBACKUP_REF" https://github.com/saltedfishclub/justbackup /mod
RUN cd /mod && \
    ./gradlew build && \
    rm -f /mod/build/libs/*sources.jar /mod/build/libs/*dev.jar && \
    cp /mod/build/libs/justbackup-*.jar /justbackup.jar

FROM base AS carpet-builder
# 同上。carpet 的 depends 写的是 minecraft >26.2,能装上但 mixin 是按旧版编译的,
# 跑在 26.3 上会在 mixin apply 阶段炸,所以同样必须切到 26.3 分支。
# ⚠️ 注意:26.3 分支目前只是合了上游 master,而上游自己还停在 26.3-snapshot-9
# (对 26.3 正式版有 100+ 处编译错误)。等 gnembon 跟上 26.3 正式版、我们再合一次
# 之前,这一层产出的 carpet 装进 26.3 服务器大概率仍然会崩。
ARG CARPET_REF=26.3
RUN git clone -b "$CARPET_REF" https://github.com/saltedfishclub/fabric-carpet /mod
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