set -e
J=app/src/main/java/com/ssc27/tracker
R=app/src/main/res
mkdir -p $J $R/values $R/drawable $R/mipmap-anydpi-v26
mv Data.kt Texts.kt Notify.kt Strings.kt MainActivity.kt Ui.kt $J/
mv AndroidManifest.xml app/src/main/
mv app.build.gradle.kts app/build.gradle.kts
mv themes.xml colors.xml $R/values/
mv ic_launcher_foreground.xml ic_stat.xml ic_launcher_fg.png $R/drawable/
mv ic_launcher.xml $R/mipmap-anydpi-v26/
