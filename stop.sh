#!/usr/bin/env bash
zookeeper_nodes=${DCOS_ZK_NODES:-3}
master_nodes=${DCOS_MASTER_NODES:-3}
slave_nodes=${DCOS_SLAVE_NODES:-3}

docker rm -f $(seq -f "zk-%g" 1 $zookeeper_nodes) $(seq -f "master-%g" 1 $master_nodes) $(seq -f "slave-%g" 1 $slave_nodes) mesosdns repo scheduler haproxy mitmproxy &> /dev/null
docker network rm mesos &> /dev/null
rm -rf target/mesos

