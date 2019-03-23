#!/bin/bash
# ssh fs
gradle build
scp /data/wisdom_work/ccb/ktor_svr/build/libs/ktor_svr-1.0-all.jar pf:/data/apps/ccb/
# ssh pf "sudo supervisorctl restart ccb ; sudo supervisorctl status"
ssh pf "sudo supervisorctl stop ccb ; sleep 2 ; sudo supervisorctl start ccb ; sudo supervisorctl status"