#!/bin/sh

JLDIR=/usr/share/java
CP1=$JLDIR/libbzdev-base.jar:$JLDIR/libbzdev-desktop.jar
CP2=$CP1:$JLDIR/libbzdev-graphics.jar:$JLDIR/libosgbatik.jar
CP=$CP2:$JLDIR/core.jar:$JLDIR/javase.jar

java -Dqrl.cmd="$0" \
     -classpath /usr/share/qrlauncher/qrlauncher.jar:$CP QRLauncher "$@"
