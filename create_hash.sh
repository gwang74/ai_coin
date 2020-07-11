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
#echo "\"JSMD5\": {" > "js.json"
for file in $list
do
    md5=`md5sum $file | xargs | cut -f1 -d" "`
    fileName=${file##*/}
    #echo " \""$fileName"\": \""$md5"\"," >> "js.json"
    hash="$hash \""$fileName"\": \""$md5"\",\n"
done
hash=`echo -e "$headStr$hash$tailStr"`
echo "$hash" > $TARGET_FILE_NAME
sed -i ":a;N;s/","\n"}"/\n"}"/" $TARGET_FILE_NAME
cp $TARGET_FILE_NAME testin/
cp -r src/app/src/main/assets dist/update
cd dist/
rm -rf update.zip
zip -r update.zip update
rm -rf update
cp update.zip ../testin/
echo `md5sum update.zip | xargs | cut -f1 -d" "`
echo 'Waring:please update dist/version_info.json ,update file hash and zip hash!'
