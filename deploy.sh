#!/bin/bash
#ssh afd@jc_root > /dev/null 2>&1 << eeooff
localUpdateHash=`md5sum dist/update.zip  |cut -d ' ' -f 1`
remoteUpdateHash=`ssh afd@jc_root "cd sync/mobile_web/download;md5sum update.zip |cut -d ' ' -f 1;exit"`

#update update.zip
if [[ $localUpdateHash == $remoteUpdateHash ]]; then
    echo "remote is latest ,do not upgrade update.zip"
else
    echo "uploading update.zip ......"
    rsync -rv dist/update.zip afd@jc_root:sync/mobile_web/download/
echo "upzip remote static file"
ssh afd@jc_root > /dev/null 2>&1 << eeooff
cd ~/sync/mobile_web/download
unzip -o update.zip -d ../
cd ..
cp -r update/static .
cp update/index.html .
exit
eeooff
fi

#update version_info
    echo "uploading version_info.json ......"
    rsync -rv dist/version_info.json afd@jc_root:sync/mobile_web/download/


#update weidex.apk
    echo "uploading weidex.apk ......"
    rsync -rv dist/weidex.apk afd@jc_root:sync/mobile_web/download/


#update weidex.ipa

    echo "uploading weidex.ipa ......"
    rsync -rv dist/weidex.ipa afd@jc_root:sync/mobile_web/download/

#update weidex.plist
    echo "uploading weidex.plist ......"
    rsync -rv dist/weidex.plist afd@jc_root:sync/mobile_web/download/
# updata img
echo "uploading android ios img"
rsync -rv dis/download/android afd@jc_root:sync/mobile_web/download/
rsync -rv dis/download/ios afd@jc_root:sync/mobile_web/download/


echo "update success!!!!!!!!!!!!"

