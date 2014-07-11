#!/bin/sh


TOP=`dirname $0`
SOURCE=${TOP}/target/PayPalAuthService-0.0.1-SNAPSHOT.war
JBOSS_HOME=${TOP}/../jboss-as-7.1.1.Final
DEPLOYMENTS=${JBOSS_HOME}/standalone/deployments

#clean out the existing deployment
rm -rf ${TOP}/target

#re-package the app
mvn package

rm -rf ${DEPLOYMENTS}/PayPalAuthService*
sleep 3
cp ${SOURCE} ${DEPLOYMENTS}/PayPalAuthService.war

echo "Deployed ${SOURCE} to ${DEPLOYMENTS}/PayPalAuthService.war..."

JBOSS_PIDS=`pgrep -f jboss-as-7.1.1.Final`

if [ "x${JBOSS_PIDS}" = "x" ]; 
then
    echo ------------------------------------------------------------------------------
    echo
    echo "JBoss not running on system, starting ${JBOSS_HOME}/bin/standalone.sh now..."
    echo
    echo ------------------------------------------------------------------------------
    ${JBOSS_HOME}/bin/standalone.sh
fi

