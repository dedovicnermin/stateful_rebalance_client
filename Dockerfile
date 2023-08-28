FROM maven:3.9.3-amazoncorretto-11 AS packager
ADD ./pom.xml pom.xml
ADD ./src src
RUN mvn clean install package -DskipTests





FROM openjdk:11.0.16-jre-buster

# add user
ARG APPLICATION_USER=nermdev
RUN useradd --no-create-home -u 1000 $APPLICATION_USER

# config working directory
RUN mkdir /app && \
    chown -R $APPLICATION_USER /app

USER 1000
COPY --from=packager --chown=1000:1000 target/stateful-rebalance-client-jar-with-dependencies.jar /app/app.jar
COPY --from=packager --chown=1000:1000 target/classes/log4j.properties /app/log4j.properties
COPY --chown=1000:1000 ./exporter/jmx_prometheus_javaagent-0.19.0.jar /app/jmx_prometheus_javaagent.jar
COPY --chown=1000:1000 ./exporter/kafka_client.yml /app/kafka_client.yml

ENV JAVA_TOOL_OPTIONS "-Dcom.sun.management.jmxremote.ssl=false \
 -Dcom.sun.management.jmxremote.authenticate=false \
 -Dcom.sun.management.jmxremote.port=7203 \
 -Dcom.sun.management.jmxremote.rmi.port=7203 \
 -Dcom.sun.management.jmxremote.host=0.0.0.0 \
 -Djava.rmi.server.hostname=${NODE_NAME} \
 -javaagent:/app/jmx_prometheus_javaagent.jar=7778:/app/kafka_client.yml"

WORKDIR /app

ENV JMX_PORT=7203
ENV EXPORTER_PORT=7778
EXPOSE 8080
EXPOSE 7203
EXPOSE 7778

ARG CONFIG=/mnt/application/application.properties
ENTRYPOINT ["/jre/bin/java", "-jar", "/app/app.jar"]


