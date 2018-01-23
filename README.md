# Secure Quick Reliable Login

This repository is an implementation for SQRL (Secure Quick Reliable Login) on android.

***This repository is in active development and haven't reached first viable implentation yet***

## Usage

***TODO***

## Introduction

Before you begin using SQRL to login to websites, your SQRL private identity must be created. You only need one, probably for life, because it reveals NOTHING about you, and it's highly secure. It's just a very long (77-digit) random number.

From then on, whenever you login with SQRL to a website, your private identity is used to generate another 77-digit number for that one website. Every website you visit sees you as a different number, yet every time you return to the same site, that site's unique number is regenerated.

This allows you to be uniquely and permanently identified, yet completely anonymous.

Since you never need to use an eMail address or a password, you never give a website your actual identity to protect. If the website's SQRL identities are ever stolen, not only would the stolen identities only be valid for that one website, but SQRL's cryptography prevents impersonation using stolen identities.

This is as good as it sounds. It's what we've been waiting for.

## Installation

***TODO***

## Build from source

There is three different ways to build this project.

#### Android studio
First of you can install android studio, import the project and run / build / release it using the graphical interface.

#### Gradle

You can build using gradle, the executibles are not supplied with this repository so in order to do this you need android studio to generate these files.
But after the project is setup you may use the command below to build your release using the Android Studio gradle implementation from the command line.

```
./gradlew assembleRelease
```

#### Bazel

Last way to build the project is to use bazel. First you need to install or build bazel which is easy on Linux and Mac but can be more of a hazzle on windows.

First you need to setup your workspace inside of the project directory. In order to find your api level you can look into the [sdk path]/platforms to see which versions are installed. The build_tools_version can be found in the [sdk path]/build-tools.

```
android_sdk_repository(
    name = "androidsdk",
    path = "[your sdk path]",
    api_level = 27,
    build_tools_version = "27.0.3"
)
```

Building the appliction you supply //[application dir]:[application name]. You also need to set the python path to python version 2. The android build system in bazel don't support version 3 as of writing this.

```
bazel build //app:sqrl --python_path=/usr/bin/python2
```

Installning the application on a device you may run the mobile-install command. Supplying the arguments -s and serial to adb in order to install.
```
bazel mobile-install --adb_arg=-s --adb_arg=<SERIAL> --start_app //app:sqrl
```

After the first install you may run incremental install in order to improve the development time.
```
bazel mobile-install --incremental --adb_arg=-s --adb_arg=<SERIAL> --start_app //app:sqrl
```
