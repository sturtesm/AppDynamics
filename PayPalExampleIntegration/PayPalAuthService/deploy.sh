#!/bin/sh


TOP=`dirname $0`
SOURCE=${TOP}/target/PayPalAuthService-0.0.1-SNAPSHOT.war
JBOSS_HOME=/Users/steve.sturtevant/Tools/jboss-as-7.1.1.Final
DEPLOYMENTS=${JBOSS_HOME}/standalone/deployments

#clean out the existing deployment
rm -rf ${TOP}/target

#re-package the app
mvn package


rm -rf ${DEPLOYMENTS}/PayPalAuthService*
sleep 3
cp ${SOURCE} ${DEPLOYMENTS}/PayPalAuthService.war

