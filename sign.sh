#!/bin/bash

./gradlew clean assembleRelease

zipalign=`find -L /opt/android-sdk -name zipalign | tail -n 1`

bins=app/build/outputs/apk

cp $bins/app-release-unsigned.apk $bins/app-release-unaligned.apk

jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ~/.android/kevinwells.keystore app/build/outputs/apk/app-release-unaligned.apk darxen

$zipalign -v 4 $bins/app-release-unaligned.apk $bins/app-release.apk

ln -sf $bins/app-release.apk app.apk
