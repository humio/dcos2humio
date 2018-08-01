FROM nginx:1.15-alpine

COPY universe/target /etc/nginx/html

RUN rm -f /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/example_ssl.conf
COPY universe/universe.marathon.mesos.conf /etc/nginx/conf.d/
