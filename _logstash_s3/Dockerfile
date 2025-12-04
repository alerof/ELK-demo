FROM docker.elastic.co/logstash/logstash:8.17.0

LABEL maintainer=alaerof@gmail.com

RUN bin/logstash-plugin install logstash-output-s3

USER root

EXPOSE 5044
EXPOSE 5000
EXPOSE 9600