#!/bin/sh

SEARCH_BASE=./src
JAR_DIR=./lib
JAVACLASSPATH=$JAR_DIR/*:.

# Bing API
# Usage: run.sh <account Key> <precision> <query> 
/usr/bin/java -classpath $JAVACLASSPATH:$SEARCH_BASE Driver "${1}" $2 "${3}" 1 2 0 $SEARCH_BASE
