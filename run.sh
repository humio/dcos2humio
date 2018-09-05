#!/usr/bin/env bash

zookeeper_nodes=${DCOS_ZK_NODES:-3}
master_nodes=${DCOS_MASTER_NODES:-3}
slave_nodes=${DCOS_SLAVE_NODES:-3}
zk_version=${DCOS_ZK_VERSION:-"3.4"}
mesos_version=${DCOS_MESOS_VERSION:-"1.5.0"}

zookeeper_network="172.18.1."
master_network="172.18.2."
slave_network="172.18.3."
tools_network="172.18.4."

./stop.sh

docker network create --subnet 172.18.0.0/16 mesos > /dev/null

### ZOOKEEPER NODES ####################################################################################################
echo -n "Deploy Zookeeper "
for node in $(seq 1 $zookeeper_nodes); do

ZOO_SERVERS=$(for n in $(seq 1 $zookeeper_nodes); do echo -n "server.${n}=${zookeeper_network}${n}:2888:3888 " | sed "s/${zookeeper_network}${node}/0.0.0.0/g"; done)
docker run -d --restart=always --net=mesos --ip=${zookeeper_network}${node} \
  -e ZOO_MY_ID=${node} \
  -e ZOO_SERVERS="${ZOO_SERVERS}" \
  --expose=2181 --expose=2888 --expose=3888 \
  --name="zk-${node}" "zookeeper:${zk_version}" > /dev/null
echo -n "."
done
echo "DONE"
### ZOOKEEPER NODES ####################################################################################################

MESOS_ZK=zk://$(jot -s "," -w "${zookeeper_network}%g:2181" - 1 $zookeeper_nodes)/mesos

### MASTER NODES #######################################################################################################
echo -n "Deploy Masters   "
for node in $(seq 1 $master_nodes); do

docker run -d --restart=always --net=mesos --ip=${master_network}${node} \
  -e MESOS_ZK=${MESOS_ZK} \
  -e MESOS_CLUSTER=dcos-demo \
  -e MESOS_ADVERTISE_IP=${master_network}${node} \
  -e MESOS_HOSTNAME_LOOKUP=false \
  --expose=5050 \
  --name="master-${node}" "mesosphere/mesos-master:${mesos_version}" > /dev/null
echo -n "."
done
echo "DONE"
### MASTER NODES #######################################################################################################

### MESOS-DNS ##########################################################################################################
echo -n "Deploy Mesos-dns "
mkdir -p target/mesos/mesosdns/{etc,logs}
cat << EOF > target/mesos/mesosdns/etc/config.json
{
  "zk": "${MESOS_ZK}"
}
EOF
docker run --name="mesosdns" -d \
  -p 8123:8123 \
  --net=mesos --ip=${tools_network}21 \
  -v "$(pwd)/target/mesos/mesosdns/etc/config.json:/config.json" \
  -v "$(pwd)/target/mesos/mesosdns/logs:/tmp" \
  mesosphere/mesos-dns:v0.6.0 /usr/bin/mesos-dns -v=2 -config=/config.json > /dev/null

echo "   DONE"
### MESOS-DNS ##########################################################################################################

### SLAVE NODES ########################################################################################################
echo -n "Deploy Slaves    "
for node in $(seq 1 $slave_nodes); do

docker run -d --net=mesos --ip=${slave_network}${node} \
  -e MESOS_MASTER=${MESOS_ZK} \
  -e MESOS_WORK_DIR=/var/tmp/mesos \
  -e MESOS_SYSTEMD_ENABLE_SUPPORT=false \
  -v /sys:/sys \
  --name="slave-${node}" "mesosphere/mesos-slave:${mesos_version}" > /dev/null
echo -n "."
done
echo "DONE"
### SLAVE NODES ########################################################################################################

### SCHEDULER ##########################################################################################################
echo -n "Deploy Scheduler "
docker run --name="repo" --net=mesos --ip=${tools_network}10 -d --expose=8085 -v $(pwd):/usr/share/nginx/html nginx > /dev/null
echo -n "."

docker run --name="scheduler" -d -p 5005:5005 --restart=always \
  --net=mesos --ip=${tools_network}30 \
  --dns=${tools_network}21 \
  -e HOST=${tools_network}10 \
  -e HUMIO_HOST=https://cloud.humio.com \
  -e HUMIO_DATASPACE=${DCOS_HUMIO_DATASPACE:-dcosagentdemo} \
  -e HUMIO_GLOBALFIELDS=sourcehost=$(hostname) \
  -e HUMIO_INGESTTOKEN=${DCOS_HUMIO_INGESTTOKEN} \
  -e MESOS_ZOOKEEPER_SERVER=$(jot -s "," -w "${zookeeper_network}%g:2181" - 1 $zookeeper_nodes) \
  --expose=5005 \
  -v $(pwd)/scheduler/target/:/target \
  "mesosphere/mesos:${mesos_version}" java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 -jar /target/scheduler-1.0-SNAPSHOT.jar > /dev/null
echo ". DONE"
### SCHEDULER ##########################################################################################################

### HAPROXY ############################################################################################################
echo -n "Deploy HAProxy   "
mkdir -p target/mesos/humio_lb/etc
cat << EOF > target/mesos/humio_lb/etc/haproxy.cfg
frontend mesoshttp
    bind *:5050
    mode http
    default_backend masters

backend masters
    mode http
    balance source
    option forwardfor
    http-request set-header X-Forwarded-Port %[dst_port]
    http-request add-header X-Forwarded-Proto https if { ssl_fc }
    server leader master.mesos:5050
EOF
#$(for n in $(seq 1 $master_nodes); do echo "    server master${n} ${master_network}${n}:5050"; done)

docker run --name="haproxy" -d -p 5050:5050 \
  --net=mesos --ip=${tools_network}20 --dns=${tools_network}21 \
  --expose=5050 \
  -v $PWD/target/mesos/humio_lb/etc/haproxy.cfg:/usr/local/etc/haproxy/haproxy.cfg \
  "haproxy" > /dev/null

docker run --name="mitmproxy" -d -p 8888:8080 \
  --net=mesos --ip=${tools_network}22 --dns=${tools_network}21 \
  "mitmproxy/mitmproxy" mitmdump &> /dev/null

echo "   DONE"
### HAPROXY ############################################################################################################

