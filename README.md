# Spark Android App

This is the source repo for the official Spark app for Android, providing the Smart Config and Tinker features, and offering a starting point for your own Android apps to work with your Cores.


## Building
1. Clone the project to your workspace
2. (Optional) Install fonts (see below)
3. Install the TI SmartConfig Library (see below)
4. Open Android Studio and select "Open an existing Android Studio project"
5. Click Browse, select the dir where you cloned the repo, and click OK


## Required Fonts

The Spark app distributed via Google Play uses several typefaces in the Gotham family.
If you have a license to these, you can place the following 4 files in `app/src/main/assets/fonts`.

* gotham_bold.otf
* gotham_book.otf
* gotham_light.otf
* gotham_medium.otf

If these fonts are not available, it will fall back to the default font.


## Required TI SmartConfig Library

You must add smartconfiglib.jar to the `SparkCore/libs` directory.

To get the SmartConfig library, go to the
[CC3000 Wi-Fi Downloads](http://processors.wiki.ti.com/index.php/CC3000_Wi-Fi_Downloads)
page. Search the page for the Android SmartConfig Application. To download, you may have to create an account.
The 'app' will actually be a Windows executable (.exe). Do not be alarmed. Rename to CC3000_Android_App_Setup_v1_1.zip and unzip.
You will find smartconfiglib.jar hiding in:
CC3000_Android_App_Setup_v1_1.zip\InstallerData\Disk1\InstData\Resource1.zip\C_\yael\SmartConfig_CC3xAndroid_Source\SmartConfigCC3X\libs_zg_ia_sf.jar\smartconfiglib.jar

If you're having trouble unzipping the .exe file, try doing it on the command line with the unzip util.

## Key Classes

If you want to know where the action is in the app, look at:
* SimpleSparkApiService: an IntentService which performs the actual HTTP calls to talk to the Spark Cloud
* ApiFacade: A simple interface for making requests and handling responses from the Spark API. If you want to work with the Spark API from Android, this is the place to start. See examples below like nameCore(), digitalWrite(), etc, for templates to work from.
* SparkCoreApp: There are a number of classes which rely on an initialization step during app startup.  All of this happens in SparkCoreApp.onCreate().


## Open Source Licenses

Original code in this repository is licensed by Spark Labs, Inc. under the Apache License, Version 2.0.
See LICENSE for more information.

This app uses several Open Source libraries. See app/libs/licenses for more information.
