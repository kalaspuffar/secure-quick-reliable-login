# Secure Quick Reliable Login

This repository is an implementation for SQRL (Secure Quick Reliable Login) on android.

***This repository is in active development***

## Usage

***TODO***

## Introduction

Before you begin using SQRL to login to websites, your SQRL private identity must be created. You only need one, probably for life, because it reveals NOTHING about you, and it's highly secure. It's just a very long (77-digit) random number.

From then on, whenever you login with SQRL to a website, your private identity is used to generate another 77-digit number for that one website. Every website you visit sees you as a different number, yet every time you return to the same site, that site's unique number is regenerated.

This allows you to be uniquely and permanently identified, yet completely anonymous.

Since you never need to use an eMail address or a password, you never give a website your actual identity to protect. If the website's SQRL identities are ever stolen, not only would the stolen identities only be valid for that one website, but SQRL's cryptography prevents impersonation using stolen identities.

This is as good as it sounds. It's what we've been waiting for.

## Installation

The client is available on google play. Following the link below you will be redirected to the store page.
[Secure Quick Reliable Login](https://play.google.com/store/apps/details?id=org.ea.sqrl)

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

