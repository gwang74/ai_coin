#!/bin/bash

cd /Users/xdjiang/project/jcc_android
/usr/bin/git pull origin dev dev

localVerionHash=`md5sum testin/version_info.json  |cut -d ' ' -f 1`
remoteVersionHash=`ssh afd@jc_root "cd sync/mobile_web/testin;md5sum version_info.json |cut -d ' ' -f 1;exit"`

#update version_info
if [[ $localVerionHash != $remoteVersionHash ]]; then
    #echo "uploading version_info.json ......"
    scp testin/version_info.json afd@jc_root:sync/mobile_web/testin/
fi


