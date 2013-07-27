#!/bin/bash

LOG=$1
PUSH_LOCATION=/sdcard/altidroid/replay.log

adb push $1 $PUSH_LOCATION
adb shell am startservice --user 0 -a com.ulanov.altidroid.FOREGROUND -n org.openskydive.altidroid/.AltidroidService -e mocklog $PUSH_LOCATION
