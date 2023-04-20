FROM eclipse-temurin:17-jdk-alpine

ENV APP=app
ENV WORKDIR=/${APP}
ENV JAR=@project.build.finalName@.jar
ENV EXECUTABLE=run.sh
ENV JVM_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"

RUN apk -U -a upgrade
RUN apk add dos2unix
RUN adduser -D -h ${WORKDIR} -s /bin/bash ${APP}

COPY ${JAR} ${WORKDIR}
COPY ${EXECUTABLE} ${WORKDIR}

RUN dos2unix ${WORKDIR}/${EXECUTABLE}; \
    chmod 755 ${WORKDIR}/${EXECUTABLE}; \
    chown -R ${APP}:users ${WORKDIR}
USER ${APP}

EXPOSE 8080
EXPOSE 8081

CMD ${WORKDIR}/${EXECUTABLE}
