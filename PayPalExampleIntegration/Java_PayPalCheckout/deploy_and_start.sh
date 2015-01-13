#!/bin/sh


TOP=`dirname $0`
SOURCE=${TOP}/target/PayPalLoginWebTier-0.0.1-SNAPSHOT.war
TOMCAT=/Users/steve.sturtevant/Tools/apache-tomee-jaxrs-1.6.0

#re-package the app
rm -rf ${TOP}/target
rm -rf ${SOURCE} ${TOMCAT}/webapps/*

mvn package

cp ${SOURCE} ${TOMCAT}/webapps/paypal-online-store.war


