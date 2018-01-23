android_binary(
    name = "sqrl",
    custom_package = "org.ea.sqrl",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest = "src/main/AndroidManifest.xml",
    resource_files = glob(["src/main/res/**"]),
    multidex = "native",
    dex_shards = 10,
    deps = [
        "@androidsdk//com.android.support:appcompat-v7-25.3.1",
        "@androidsdk//com.android.support:design-25.3.1",
        "@androidsdk//com.android.support.constraint:constraint-layout-1.0.2"
    ]
)
