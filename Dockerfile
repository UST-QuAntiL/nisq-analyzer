FROM maven:3-jdk-11 as builder
COPY . /tmp/nisq
WORKDIR /tmp/nisq
RUN mvn package -DskipTests && mkdir /build && unzip /tmp/nisq/org.planqk.nisq.analyzer.core/target/org.planqk.nisq.analyzer.core.war -d /build/nisq-analyzer

FROM ubuntu:18.04
LABEL maintainer = "Benjamin Weder <benjamin.weder@iaas.uni-stuttgart.de>"

ARG DOCKERIZE_VERSION=v0.6.1
ARG TOMCAT_VERSION=9.0.8
ARG NISQ_ANALYZER_PORT=5010

ENV POSTGRES_HOSTNAME localhost
ENV POSTGRES_PORT 5060
ENV POSTGRES_USER nisq
ENV POSTGRES_PASSWORD nisq
ENV POSTGRES_DB nisq

ENV NISQ_HOSTNAME: localhost
ENV NISQ_PORT: 5013
ENV NISQ_VERSION: v1.0

ENV MCDA_SERVICES_URL https://webservices.decision-deck.org/soap/

RUN apt-get -qq update && apt-get install -qqy software-properties-common openjdk-11-jdk wget

# setup tomcat
RUN mkdir /usr/local/tomcat
RUN wget --quiet --no-cookies https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz -O /tmp/tomcat.tgz && \
tar xzvf /tmp/tomcat.tgz -C /opt && \
mv /opt/apache-tomcat-${TOMCAT_VERSION} /opt/tomcat && \
rm /tmp/tomcat.tgz && \
sed -i 's/port="8080"/port="'${NISQ_ANALYZER_PORT}'"/g' /opt/tomcat/conf/server.xml
ENV CATALINA_HOME /opt/tomcat
ENV PATH $PATH:$CATALINA_HOME/bin

# install dockerize for configuration templating
RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

RUN rm -rf ${CATALINA_HOME}/webapps/*
COPY --from=builder /build/nisq-analyzer ${CATALINA_HOME}/webapps/nisq-analyzer

EXPOSE ${NISQ_ANALYZER_PORT}

# configure application with template and docker environment variables
ADD .docker/application.properties.tpl ${CATALINA_HOME}/webapps/application.properties.tpl

CMD dockerize -template ${CATALINA_HOME}/webapps/application.properties.tpl:${CATALINA_HOME}/webapps/nisq-analyzer/WEB-INF/classes/application.properties \
    ${CATALINA_HOME}/bin/catalina.sh run
