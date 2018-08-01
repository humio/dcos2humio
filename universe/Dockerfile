FROM nginx:1.15

COPY universe/target /usr/share/nginx/html

RUN rm -f /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/example_ssl.conf
COPY universe/universe.marathon.mesos.conf /etc/nginx/conf.d/
