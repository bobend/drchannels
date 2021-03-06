DR TV channels

An Android TV app that implements a TV Input Service for DR TV to allow its channels to be added to the Android TV Live Channels app.

The input service adds EPG channel data to Live Channels and allows for live playback. Just install the DR channels app, open Live Channels on Android TV and go to Settings->Channel sources and setup DR TV channels as a source.

Also mobile apps for iOS and Android.

Android:
https://www.dropbox.com/s/tw9gpldrwicd3kj/drchannels.apk?dl=1

Android TV:
https://www.dropbox.com/s/a4oc5xmtl5naea7/drchannels-tv-release.apk?dl=1

The DR MU-online API version 1.4 is implemented in its own "drapi" module as a Kotlin multiplatform library so that it can easily be used in other Android and iOS projects.

Create local.properties and set Android SDK dir in it:
`sdk.dir=/Users/path/to/Android/sdk`

Build from the command line:

_Linux/Mac:_
`./gradlew assembleDebug`

_Windows:_
`gradle.bat assembleDebug`

_Build iOS framework before opening XCode project:_
`./gradlew :logic:packForXcode`
