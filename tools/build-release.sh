#!/bin/sh

APK=./build/apk/altidroid-release-unsigned.apk
OUT=altidroid_aligned.apk

./gradlew assembleRelease || exit 1
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore release.keystore \
  $APK altidroid_release || exit 1
$ANDROID_HOME/tools/zipalign -f 4 $APK $OUT
