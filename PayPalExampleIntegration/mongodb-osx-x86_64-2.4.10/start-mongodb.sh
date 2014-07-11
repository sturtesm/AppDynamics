#!/bin/sh

TOP=`dirname $0`
STDOUT_LOG=${TOP}/mongodb-stdout.log

MONGO_PIDS=`pgrep -f mongod`

if [ "x${MONGO_PIDS}" = "x" ];
then
    echo
    echo ------------------------------------------------------------------------------
    echo
    echo "MongoDB not running on system, starting ${TOP}/bin/mongod now...."
    echo "  STDOUT Logs: ${STDOUT_LOG}"
    echo
    echo ------------------------------------------------------------------------------
    echo

    ${TOP}/bin/mongod 2>&1 > ${STDOUT_LOG}&
else
    echo
    echo ------------------------------------------------------------------------------
    echo
    echo "Won't start MongoDB, Already Running as PID: ${MONGO_PIDS}"
    echo
    echo ------------------------------------------------------------------------------
    echo
fi     
