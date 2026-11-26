set -e
J=app/src/main/java/com/ssc27/tracker
R=app/src/main/res
mkdir -p $J $R/values $R/drawable $R/mipmap-anydpi-v26
mv Data.kt Texts.kt Notify.kt Strings.kt MainActivity.kt Ui.kt $J/
mv AndroidManifest.xml app/src/main/
mv app.build.gradle.kts app/build.gradle.kts
mv themes.xml colors.xml $R/values/
mv ic_launcher_foreground.xml ic_stat.xml $R/drawable/
mv ic_launcher.xml $R/mipmap-anydpi-v26/
if [ -f logo.b64 ]; then
  base64 -d logo.b64 > $R/drawable/ic_launcher_fg.jpg
  cat > $R/values/colors.xml <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="icon_bg">#3A8D7B</color>
</resources>
XML
  cat > $R/mipmap-anydpi-v26/ic_launcher.xml <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/icon_bg" />
    <foreground android:drawable="@drawable/ic_launcher_fg" />
</adaptive-icon>
XML
fi
