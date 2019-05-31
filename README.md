# SQRL Login

[![Build Status](https://travis-ci.org/kalaspuffar/secure-quick-reliable-login.svg?branch=master)](https://travis-ci.org/kalaspuffar/secure-quick-reliable-login)

This repository is an implementation for SQRL (Secure Quick Reliable Login) on Android.

## Governance

This is an open source project with an open attitude. The application is meant to be free and
available to anyone. I want this project to be successful. I've put in some of my time, and we
have contributors to this project, but all releases to Google Play are done by me (Daniel Persson).
This is a labor of love for me, so I'm trying to keep this project clean and working.
I recognize that using a product requires trust so for you to understand me perhaps better
and if you are a developer you might find something interesting. Please take a look at my
youtube channel for more of me.

[https://www.youtube.com/c/DanielPersson](https://www.youtube.com/c/DanielPersson)

## Help wanted / appreciated

We are closely nearing a full implementation of the specification. But there is still a lot of things to do.

* Design user interface
* Document usage and code
* Test and verify code
* Security audit
* Bug reporting
* Translation (https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Arabic Status](http://uhash.com/poeditor/ar.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Czech Status](http://uhash.com/poeditor/cs.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Chinese (simplified) Status](http://uhash.com/poeditor/zh-CN.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Dutch Status](http://uhash.com/poeditor/nl.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation English Status](http://uhash.com/poeditor/en.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation French Status](http://uhash.com/poeditor/fr.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation German Status](http://uhash.com/poeditor/de.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Hebrew Status](http://uhash.com/poeditor/he.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Hungarian Status](http://uhash.com/poeditor/hu.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Japanese Status](http://uhash.com/poeditor/ja.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Norwegian Status](http://uhash.com/poeditor/no.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Russian Status](http://uhash.com/poeditor/ru.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Slovenian Status](http://uhash.com/poeditor/sl.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Spanish Status](http://uhash.com/poeditor/es.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)
    * [![Translation Swedish Status](http://uhash.com/poeditor/sv.svg?t=11)](https://poeditor.com/join/project/Jlsa1tqxlx)

Some of these areas are not my speciallity so all help / merge requests are welcome.

Thank you for your time.

## Introduction

Before you begin using SQRL to login to websites, your SQRL private identity must be created. You only need one, probably for life, because it reveals NOTHING about you, and it's highly secure. It's just a very long (77-digit) random number.

From then on, whenever you login with SQRL to a website, your private identity is used to generate another 77-digit number for that one website. Every website you visit sees you as a different number, yet every time you return to the same site, that site's unique number is regenerated.

This allows you to be uniquely and permanently identified, yet completely anonymous.

Since you never need to use an eMail address or a password, you never give a website your actual identity to protect. If the website's SQRL identities are ever stolen, not only would the stolen identities only be valid for that one website, but SQRL's cryptography prevents impersonation using stolen identities.

This is as good as it sounds. It's what we've been waiting for.

## Usage

Follow the install instructions below. When the application is installed you get the choice to either
create or import an existing identity from a textual version or via scanning a QR code.

Follow these instructions in the application in order to get your identity setup and ready for use.
After that you can just visit a site you want to login to either on your phone or other device.

Then you scan the QR code on the page or if you are on the same device you click the provided
"Login with SQRL" link and the application opens up in order to enable you to login.

On the login prompt on your phone you need to verify that the domain your visiting is correct so you
don't fall victim to a man in the middle attack. Then you supply the identity password, the application
will then contact the server in order to verify your identity.

You may be required to create an account on the site if you haven't visited before. Then when ever you
return you repeat the procedure from scanning the QR code or clicking the login link to verify your identity.

If you have any questions you can freely ask them in the SQRL forum at [SQRL forum](https://sqrl.grc.com)
or opening a ticket on the issue tracker here on github.

## Installation

The client is available on google play. Following the link below you will be redirected to the store page.
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=org.ea.sqrl)

We recommend that you download apps from Google Play. You can also get them from other sources.
[<img src="https://f-droid.org/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/org.ea.sqrl/)

## Build from source

There is two different ways to build this project.

#### Android studio
First of you can install android studio, import the project and run / build / release it using the graphical interface.

#### Gradle

You can build using gradle, the executibles are not supplied with this repository so in order to do this you need android studio to generate these files.
But after the project is setup you may use the command below to build your release using the Android Studio gradle implementation from the command line.

```
./gradlew assembleRelease
```

##### Remember:
[System images](https://dl.google.com/android/repository/sys-img/google_apis/sys-img.xml)
