
[![Stories in Ready](https://badge.waffle.io/couchbase/couchbase-lite-android.png?label=ready&title=Ready)](https://waffle.io/couchbase/couchbase-lite-android)
# Couchbase-Lite-Android #

Couchbase-Lite-Android is a lightweight embedded NoSQL database engine for Android with the built-in ability to sync to Couchbase Server on the backend.  

It is the Android port of [Couchbase Lite iOS](https://github.com/couchbase/couchbase-lite-ios).  

## Architecture

![](http://tleyden-misc.s3.amazonaws.com/couchbase-lite/couchbase-lite-architecture.png)

Couchbase Lite databases are able to sync with eachother via [Sync Gateway](https://github.com/couchbase/sync_gateway/) backed by [Couchbase Server](http://www.couchbase.com/couchbase-server/overview)


## Documentation Overview

* This [README](https://github.com/couchbase/couchbase-lite-android/blob/master/README.md)
* [Official Documentation](http://developer.couchbase.com/mobile/develop/guides/couchbase-lite/index.html) for the latest release
* [Javadocs](http://factory.couchbase.com/view/build/view/mobile_dev/view/android/job/build_cblite_android_100/74/artifact/cblite_android_javadocs_1.0.0-74.zip) for [1.0](https://github.com/couchbase/couchbase-lite-android/tree/1.0.0) release
* [Wiki](https://github.com/couchbase/couchbase-lite-android/wiki)

## Getting started with Couchbase Lite

* Download and run the [GrocerySync](https://github.com/couchbaselabs/GrocerySync-Android) demo application

* Create your own Hello World Couchbase Lite via the [Getting Started](http://developer.couchbase.com/mobile/develop/training/build-first-android-app/index.html) guide.


## Getting the pre-built jars / maven artifacts

### Maven master branch

Maven repo URL: `http://files.couchbase.com/maven2/`

```
<dependency>
  <groupId>com.couchbase.lite</groupId>
  <artifactId>android</artifactId>
  <version>0.0.0-426</version>
</dependency>
```

While `0.0.0-426` was the latest build at the time of writing, it's probably out of date by the time you are reading this. To get the latest build number (eg, the "426" part of the version above), see our [Maven Repo](http://files.couchbase.com/maven2/com/couchbase/lite/couchbase-lite-android/) and look for the highest numbered version that starts with `0.0.0-` and is later than `0.0.0-426`

Here is a [complete gradle file](https://github.com/couchbaselabs/GrocerySync-Android/blob/master/GrocerySync-Android/build.gradle) that uses this maven artifact.

### Latest Maven release

Maven repo URL: `http://files.couchbase.com/maven2/`

```
<dependency>
  <groupId>com.couchbase.lite</groupId>
  <artifactId>android</artifactId>
  <version>${latest_version}</version>
</dependency>
```

Where ${latest_version} should be replaced by something that looks like `1.0.2`.  To find the latest version, check our [Maven Repo](http://files.couchbase.com/maven2/com/couchbase/lite/couchbase-lite-java-core/) directly and look for the latest version, ignoring anything that has a dash after it.  (Eg, ignore items like `1.0.2-239` because they aren't official releases).

### Zipfile that includes jars

For Eclipse and Phonegap users, here are links to the zip file which includes the jars:

* [Master Branch build #426 zipfile](http://factory.couchbase.com/job/build_cblite_android_master-community/13/artifact/couchbase-lite-android-community_0.0.0-426.zip) - to get more recent builds, see [Jenkins CI builds](http://factory.couchbase.com/view/build/view/mobile_dev/view/android/job/build_cblite_android_master/)
* To get the latest released zipfile, go to [the official download site](http://www.couchbase.com/download#cb-mobile) and download the latest release.

## Building Couchbase Lite master branch from source

### Prerequisites

* [Download Android Studio](http://developer.android.com/sdk/installing/studio.html).  Versions 0.8.2 is known to work on the master branch, with the following caveat: when importing, you will probably see [this error](https://code.google.com/p/android/issues/detail?id=74673), but after that you should be able to click the menu bar item "Sync Project with Gradle files" and the project should work after that.  Later versions after 0.8.2 probably work as well, and if you get it to work on a later version please post to the [Couchbase Mobile Google Group](groups.google.com/group/mobile-couchbase/) and mention which version.  Older versions of Couchbase Lite required Android Studio version 0.5.7.

* Under Tools / Android / Android SDK Manager and install "Extras/Google Repository" and "Extras/Android Support Repository" (future versions of Android Studio may make this step unnecessary)

### Clone the git repository

Use Git to clone the Couchbase Lite repository to your local disk: 

```
$ git clone git://github.com/couchbase/couchbase-lite-android.git
$ cd couchbase-lite-android
$ git submodule init && git submodule update
```

### Configure Android Studio SDK location

* `cp local.properties.example local.properties`
* Customize `local.properties` according to your SDK installation directory

### Enable settings.gradle file

* `cp settings.gradle.example settings.gradle`

### Importing Project into Android Studio

You should be able to import the project directly into Android Studio:

* Start Android Studio
* Choose File / Import and choose the settings.gradle file in the couchbase-lite-android directory you cloned earlier
* Hit Finish and wait for all tasks to finish (may take a while)

**Warning** when importing, you will probably see an error like [Wrong offset: 290. Should be in range: 0, 230](https://code.google.com/p/android/issues/detail?id=74673), but after that you should be able to click the menu bar item "Sync Project with Gradle files" and the project should work after that

### Running tests

There are two wiki pages which describe how to run the tests:

* [Running unit tests for couchbase lite android](https://github.com/couchbase/couchbase-lite-android/wiki/Running-unit-tests-for-couchbase-lite-android)  (newer)

* [Running the Test Suite](https://github.com/couchbase/couchbase-lite-android/wiki/Running-the-test-suite) wiki page.

## Example Apps

* [GrocerySync](https://github.com/couchbaselabs/GrocerySync-Android)  
    * Simplest example
* [TodoLite](https://github.com/couchbaselabs/ToDoLite-Android)
    * Facebook auth
    * Replication with channels
    * Image attachments
    * Ability for users to share data
* [LiteServAndroid](https://github.com/couchbaselabs/couchbase-lite-android-liteserv)
    * REST API example

## Project Structure

* [Project Structure](https://github.com/couchbase/couchbase-lite-android/wiki/Project-structure) wiki page that describes the new project structure.

## Requirements

- Android 2.3 Gingerbread (API level 9) and above.

## Getting Help

* [Couchbase Mobile Google Group](http://groups.google.com/group/mobile-couchbase/)
* [File a github issue](https://github.com/couchbase/couchbase-lite-android/issues)

## Credits

[Credits](https://github.com/couchbase/couchbase-lite-android/wiki/Credits)

## License
- Apache License 2.0

[![githalytics.com alpha](https://cruel-carlota.pagodabox.com/bc53967fe3191ba75b4a62c9372d9928 "githalytics.com")](http://githalytics.com/couchbase/couchbase-lite-android)
