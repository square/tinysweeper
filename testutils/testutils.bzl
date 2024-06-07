load("@rules_jvm_external//:defs.bzl", "artifact")
load("@rules_kotlin//kotlin:kotlin.bzl", "kt_android_local_test")

def ts_unit_test(
        name,
        srcs = [],
        deps = [],
        resources = [],
        manifest = None,
        test_class = None,
        **kwargs):
    kt_android_local_test(
        name = name,
        srcs = srcs,
        manifest = "//testutils:test_manifest.xml",
        test_class = "com.tinysweeper.tsengine.%s" % name if test_class == None else test_class,
        deps = deps + [
            artifact("org.robolectric:robolectric"),
            "@rules_robolectric//bazel:android-all",
        ],
        **kwargs
    )
