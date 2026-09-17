#!/usr/bin/env bash
# 2核2G 演示机：限制 JVM 堆，避免和 MySQL 抢内存
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:--Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=50}"
JAR="${1:-warehouse-back/target/warehouse-back.jar}"
exec java -jar "$JAR"
