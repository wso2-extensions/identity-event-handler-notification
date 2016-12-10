#!/usr/bin/env bash
echo "<<<< Build Email Handler >>>>>>>>>>>>>>"
cd components/email-mgt
mvn clean install

IS_HOME=/home/farazath/Desktop/EmailTemplate/wso2is-5.3.0-SNAPSHOT
IS_PATCH=$IS_HOME/repository/components/patches/patch9999
IS_CORE_PATCHED_JAR=/home/farazath/IS/identity-framework/components/identity-core/org.wso2.carbon.identity.core/target/org.wso2.carbon.identity.core-5.5.0-SNAPSHOT.jar

rm -r $IS_PATCH
mkdir $IS_PATCH

cd /home/farazath/IS/wso2-extensions/identity-event-handler-email

# copy to patch folder
cp components/email-mgt/org.wso2.carbon.email.mgt/target/org.wso2.carbon.email.mgt-1.0.0-SNAPSHOT.jar $IS_PATCH
echo "<<<<<<  Copied Email Mgt JAR >>>>>"
cp components/email-mgt/org.wso2.carbon.email.mgt.ui/target/org.wso2.carbon.email.mgt.ui-1.0.0-SNAPSHOT.jar $IS_PATCH
echo "<<<<<<  Copied Email UI JAR >>>>>"
cp service-stubs/identity/org.wso2.carbon.email.mgt.stub/target/org.wso2.carbon.email.mgt.stub-1.0.0-SNAPSHOT.jar $IS_PATCH
echo "<<<<<<  Copied Email Stub JAR >>>>>"
cp $IS_CORE_PATCHED_JAR $IS_PATCH
echo "<<<<<<  Copied Core patched JAR >>>>>"


echo "<<<<<<  Copied Email JARS >>>>>"

cd $IS_HOME/bin

debug="debug"
if [ "$1" = "$debug" ] ; then
    sh wso2server.sh -debug 5005 -DosgiConsole
else
    sh wso2server.sh
fi


