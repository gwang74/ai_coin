#!/bin/bash
#ssh afd@jc_root > /dev/null 2>&1 << eeooff
localApkHash=`md5sum testin/weidex.apk  |cut -d ' ' -f 1`
remoteApkHash=`ssh afd@jc_root "cd sync/mobile_web/testin;md5sum weidex.apk |cut -d ' ' -f 1;exit"`

localIpaHash=`md5sum testin/weidex.ipa  |cut -d ' ' -f 1`
remoteIpaHash=`ssh afd@jc_root "cd sync/mobile_web/testin;md5sum weidex.ipa |cut -d ' ' -f 1;exit"`


localUpdateHash=`md5sum testin/update.zip  |cut -d ' ' -f 1`
remoteUpdateHash=`ssh afd@jc_root "cd sync/mobile_web/testin;md5sum update.zip |cut -d ' ' -f 1;exit"`

localVerionHash=`md5sum testin/version_info.json  |cut -d ' ' -f 1`
remoteVersionHash=`ssh afd@jc_root "cd sync/mobile_web/testin;md5sum version_info.json |cut -d ' ' -f 1;exit"`

localPlistHash=`md5sum testin/weidex.plist  |cut -d ' ' -f 1`
remotePlistHash=`ssh afd@jc_root "cd sync/mobile_web/testin;md5sum weidex.plist |cut -d ' ' -f 1;exit"`

#update update.zip
if [[ $localUpdateHash == $remoteUpdateHash ]]; then
    echo "remote is latest ,do not upgrade update.zip"
else
    echo "uploading update.zip ......"
    scp testin/update.zip afd@jc_root:sync/mobile_web/testin/
fi

#update version_info
if [[ $localVerionHash == $remoteVersionHash ]]; then
     #scp weidex.apk afd@jc_root:sync/mobile_web/testin/
    echo "remote is latest ,do not upgrade version_info.json"
else
    echo "uploading version_info.json ......"
    scp testin/version_info.json afd@jc_root:sync/mobile_web/testin/
fi

#update weidex.apk
if [[ $localApkHash == $remoteApkHash ]]; then
    echo "remote is latest ,do not upgrade weidex.apk"
else
    echo "uploading weidex.apk ......"
    scp testin/weidex.apk afd@jc_root:sync/mobile_web/testin/
fi

#update weidex.plist
if [[ $localPlistHash == $remotePlistHash ]]; then
    echo "remote is latest ,do not upgrade weidex.plist"
else
    echo "uploading weidex.plist ......"
    scp testin/weidex.plist afd@jc_root:sync/mobile_web/testin/
fi

#update weidex.ipa
if [[ $localIpaHash == $remoteIpaHash ]]; then
    echo "remote is latest ,do not upgrade weidex.ipa"
else
    echo "uploading weidex.ipa ......"
    scp testin/weidex.ipa afd@jc_root:sync/mobile_web/testin/
fi

echo "update success!!!!!!!!!!!!"

