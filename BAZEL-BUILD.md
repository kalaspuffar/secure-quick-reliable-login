#### Bazel

*** KEEPING THE BAZEL BUILD WORKING DURING DEVELOPMENT ESPECIALLY WHEN ADDING NATIVE C LIBRARIES AREN'T A PRIORITY ***

Last way to build the project is to use bazel. First you need to install or build bazel which is easy on Linux and Mac but can be more of a hazzle on windows.

Remember to build a project with bazel in windows you use three slashes instead of two to signify the root. eg ///app:sqrl

First you need to setup your workspace inside of the project directory. In order to find your api level you can look into the [sdk path]/platforms to see which versions are installed. The build_tools_version can be found in the [sdk path]/build-tools.

```
load("@bazel_tools//tools/build_defs/repo:maven_rules.bzl", "maven_aar")
maven_aar(
  name = "qrcodereaderview",
  artifact = "com.dlazaro66.qrcodereaderview:qrcodereaderview:2.0.2",
)
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


[link](sqrl://www.grc.com/sqrl/?nut=ThisIsABadNut)