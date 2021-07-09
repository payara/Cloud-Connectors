#!/usr/bin/env bash

VER=0.6.1-SNAPSHOT
RAR=../AmazonSQSRAR/target/amazon-sqs-rar-${VER}.rar
JAR=../AmazonSQSExample/target/amazon-sqs-example-${VER}.jar
MICRO=payara-micro-5.2021.1.jar
REG=docker.brandprotection.net
IMAGE_NAME=sqsexample

cd ../AmazonSQSJCAAPI || exit 1
mvn clean install

cd ../AmazonSQSRAR || exit 1
mvn clean install

cd ../AmazonSQSExample || exit 1
mvn clean install

cd ../docker || exit 1

[[ -f $RAR ]] && [[ -f $JAR ]] && [[ -f $MICRO ]] || { echo "files missing"; exit 1; }

rm -rf ./tmp
mkdir ./tmp
cp $RAR $JAR ./tmp/

echo "Building the docker image..."
docker build -t ${REG}/${IMAGE_NAME}:latest . || { echo "Failed to build image."; exit 1; }
