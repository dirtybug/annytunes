FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="Annytunes Team"
LABEL description="Annytunes Android Test and Build Container"

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Install essential dependencies and developer tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    bash \
    curl \
    wget \
    unzip \
    git \
    vim \
    nano \
    less \
    procps \
    libpulse0 \
    libgl1 \
    libx11-6 \
    file \
    sed \
    && rm -rf /var/lib/apt/lists/*

# Configure developer convenience aliases
RUN echo 'alias ll="ls -la"' >> /root/.bashrc && \
    echo 'alias test="./gradlew test"' >> /root/.bashrc && \
    echo 'alias build="./gradlew assembleDebug"' >> /root/.bashrc && \
    echo 'alias release="./gradlew assembleRelease"' >> /root/.bashrc && \
    echo 'alias lint="./gradlew lint"' >> /root/.bashrc

# Download and install Android SDK Commandline Tools (version 15859902)
ARG CMDLINE_TOOLS_URL=https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q ${CMDLINE_TOOLS_URL} -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Pre-accept all Android SDK licenses and install required SDK packages (API 35 & 36, build-tools 35.0.0 & 36.0.0)
RUN yes | sdkmanager --licenses && \
    sdkmanager --install \
        "platform-tools" \
        "platforms;android-35" \
        "platforms;android-36" \
        "build-tools;35.0.0" \
        "build-tools;36.0.0"

# Set working directory
WORKDIR /workspace

# Install entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN sed -i 's/\r$//' /usr/local/bin/docker-entrypoint.sh && \
    chmod +x /usr/local/bin/docker-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["unit"]
