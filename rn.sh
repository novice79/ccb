#!/bin/bash
# ssh fs
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
gradle build
scp /data/wisdom_work/ccb/ktor_svr/build/libs/ktor_svr-1.0.jar pf:/data/apps/ccb/
# ssh pf "sudo supervisorctl restart ccb ; sudo supervisorctl status"
ssh pf "sudo supervisorctl stop ccb ; sleep 2 ; sudo supervisorctl start ccb ; sudo supervisorctl status"

