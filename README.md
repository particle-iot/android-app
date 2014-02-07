# Spark Android App

This is the source repo for the official Spark app for Android, providing the Smart Config and Tinker features, and offering a starting point for your own Android apps to work with your Cores.


## Building
1. In Eclipse, go to File --> Import, and under the Android "folder", click "Existing Android Code into workspace", then click Next
2. Click Browse, select the dir where you cloned the repo, and click OK
3. You should now see two projects under the "Projects to Import" header: "SparkCore" and "Fontify".  Click on Finish.
4. In the SparkCore app, create the file ```res/values/local_build.xml``` with the following contents:

```
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <string name="spark_token_creation_credentials">spark:spark</string>
    </resources>
```
_(You could actually put any valid HTTP Basic Auth string where it says ```spark:spark```; these values aren't currently used, but they must be present.)_

After this, you might also need to do a refresh and/or clean on the SparkCore project, because Eclipse. ;-)


## Required Fonts

The Spark app distributed via Google Play uses several typefaces in the Gotham family.
If you have a license to these, you can place the following 4 files in `SparkCore/assets/fonts`.

* gotham_bold.otf
* gotham_book.otf
* gotham_light.otf
* gotham_medium.otf

Otherwise, in order to build a working app, you will need to either modify the app not to look for the fonts or put some other fonts in their place.


## Required TI SmartConfig Library

You must add smartconfiglib.jar to the `SparkCore/libs` directory.

To get the SmartConfig library, go to the
[CC3000 Wi-Fi Downloads](http://processors.wiki.ti.com/index.php/CC3000_Wi-Fi_Downloads)
page. Search the page for the Android SmartConfig Application.
Download and unpack the app, which will require Windows. :-/
You can find smartconfiglib.jar in the libs directory of TI's app.

## Key Classes

If you want to know where the action is in the app, look at:
* SimpleSparkApiService: an IntentService which performs the actual HTTP calls to talk to the Spark Cloud
* ApiFacade: A simple interface for making requests and handling responses from the Spark API. If you want to work with the Spark API from Android, this is the place to start. See examples below like nameCore(), digitalWrite(), etc, for templates to work from.
* SparkCoreApp: There are a number of classes which rely on an initialization step during app startup.  All of this happens in SparkCoreApp.onCreate().


## Open Source Licenses

Original code in this repository is licensed by Spark Labs, Inc. under the Apache License, Version 2.0.
See LICENSE for more information.

This app uses several Open Source libraries. See SparkCore/libs/licenses for more information.
