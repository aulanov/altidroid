android_resource(
  name = 'res',
  res = 'res',
  assets = 'assets',
  package = 'org.openskydive.altidroid',
  visibility = [
    'PUBLIC',
  ],
)

android_library(
  name = 'activity',
  srcs = glob(['src/**/*.java']),
  deps = [ 
    ':res',
    '//libs:android-support',
    '//protobuf-src/com/google/protobuf:protobuf',
  ],
  visibility = [ 'PUBLIC' ],
)

android_binary(
  name = 'altidroid',
  manifest = 'AndroidManifest.xml',
  target = 'Google Inc.:Google APIs:17',
  keystore_properties = 'debug.keystore.properties',
  deps = [
    ':activity',
  ],
)

apk_genrule(
  name = 'altidroid_aligned',
  apk = ':altidroid',
  deps = [],
  srcs = [],
  cmd = '$ANDROID_HOME/tools/zipalign -f 4 $APK $OUT',
  out = 'altidroid_aligned.apk',
)

android_binary(
  name = 'altidroid_release',
  manifest = 'AndroidManifest.xml',
  target = 'Google Inc.:Google APIs:17',
  # build-release.sh resigns this using the proper release key.
  keystore_properties = 'debug.keystore.properties',
  package_type = 'release',
  proguard_config = 'proguard.cfg',
  deps = [
    ':activity',
  ],
)

project_config(
  src_target = ':src',
)
