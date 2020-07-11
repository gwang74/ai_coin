#!/bin/bash
#-------------mac install gun-sed---------------------------------
#1.brew install gnu-sed --with-default-names
#2.vi ~/.zshrc
#export PATH="/usr/local/opt/gnu-sed/libexec/gnubin:$PATH"
#3.source ~/.zshrc 或者新开窗口，让设置生效
#------------------------------------------------

list=`ls src/app/src/main/assets/static/js/*.js`
TARGET_FILE_NAME='dist/js.json'
headStr="\"JSMD5\": {\n"
tailStr="}"
rootdir=`pwd`
versioninfofile="$rootdir/dist/version_info.json"
#echo "\"JSMD5\": {" > "js.json"
for file in $list
do
    md5=`md5sum $file | xargs | cut -f1 -d" "`
    fileName=${file##*/}
    #echo " \""$fileName"\": \""$md5"\"," >> "js.json"
    hash="$hash \""$fileName"\": \""$md5"\",\n\t\t\t"
done
echo $hash
#hash=`echo -e "$headStr$hash$tailStr"`
echo -e "$headStr\t\t\t$hash$tailStr" > $TARGET_FILE_NAME
sed -i ":a;N;s/","\n\t\t\t"}"/\n\t\t\t"}"/" $TARGET_FILE_NAME
cp $TARGET_FILE_NAME testin/
cp -r src/app/src/main/assets dist/update
cd dist/
rm -rf update.zip
zip -r update.zip update
rm -rf update
cp update.zip ../testin/
updatemd5=`md5sum update.zip | xargs | cut -f1 -d" "`
apkmd5=`md5sum mtdex.apk | xargs | cut -f1 -d" "`
echo ${updatemd5}
cd $rootdir
zipVersionCode=$1
echo $zipVersionCode

cat > $versioninfofile <<EOF
{
	"android": {
		"versionCode": 12,
		"versionName": "1.1.5",
		"downLoadUrl": "https://appcdn.jccdex.cn/download/weidex.apk",
		"APKMD5": "$apkmd5",
		"zipVersionCode": $zipVersionCode,
		"zipMD5": "$updatemd5",
		"zipDownLoadUrl": "https://appcdn.jccdex.cn/download/update.zip",
		"isShowAd": true,
		"isMaintenance": false,
		"isForceUp": false,
		"isOptional": false,
		"AdFirst": {
			"start": 20190428,
			"end": 20190428,
			"adUrl": "https://app.jccdex.cn/download/android/ad/",
			"dialogUrl": "https://app.jccdex.cn/download/android/dialog/",
			"isShowDialog": false,
			"isShowAd": false,
			"eventUrl": "",
			"shareTitle": "新年SWTC红包大派送!",
			"shareContent": "在手机上点击这里，跟我一起抢红包，快，别让财运溜走了"
		},
		"AdSecond": {
			"start": 20190518,
			"end": 20191231,
			"adUrl": "https://app.jccdex.cn/download/android/ad/",
			"dialogUrl": "https://app.jccdex.cn/download/android/dialog/",
			"isShowDialog": false,
			"isShowAd": true,
			"eventUrl": "",
			"shareTitle": "新年SWTC红包大派送!",
			"shareContent": "在手机上点击这里，跟我一起抢红包，快，别让财运溜走了"
		},
                `cat $TARGET_FILE_NAME`
	},
	"ios": {
		"versionCode": 1.6,
		"versionName": "1.0.0",
		"downLoadUrl": "itms-services://?action=download-manifest&url=https://appcdn.jccdex.cn/download/weidex.plist",
		"APKMD5": "",
		"zipVersionCode": $zipVersionCode,
		"zipMD5": "$updatemd5",
		"zipDownLoadUrl": "https://appcdn.jccdex.cn/download/update.zip",
		"isShowAd": true,
		"isMaintenance": false,
		"isForceUp": false,
		"isOptional": false,
		"AdFirst": {
			"start": 20190214,
			"end": 20190215,
			"adUrl": "https://app.jccdex.cn/download/ios/ad/",
			"dialogUrl": "https://app.jccdex.cn/download/ios/dialog/",
			"isShowDialog": false,
			"isShowAd": false,
			"eventUrl": "",
			"shareTitle": "新年SWTC红包大派送!",
			"shareContent": "在手机上点击这里，跟我一起抢红包，快，别让财运溜走了"
		},
		"AdSecond": {
			"start": 20190518,
			"end": 20191231,
			"adUrl": "https://app.jccdex.cn/download/ios/ad/",
			"dialogUrl": "https://app.jccdex.cn/download/ios/dialog/",
			"isShowDialog": false,
			"isShowAd": true,
			"eventUrl": "",
			"shareTitle": "新年SWTC红包大派送!",
			"shareContent": "在手机上点击这里，跟我一起抢红包，快，别让财运溜走了"
		},
		"JSMD5": {}
	}

}
EOF
echo 'Waring: update dist/version_info.json ,update file hash and zip hash over!'
bash ./deploy.sh
