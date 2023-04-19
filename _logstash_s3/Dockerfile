FROM docker.elastic.co/logstash/logstash:7.17.9

LABEL maintainer=WKG-GPO-Embassy-Support@wolterskluwer.com

RUN bin/logstash-plugin install logstash-output-s3

USER root

EXPOSE 5044
EXPOSE 5000
EXPOSE 9600