cd /Applications/Adept
./title.sh "Adept"
javafx -Xmx800m -classpath "lib/*" com.sri.tasklearning.ui.core.adept.AdeptUI
osascript -e 'tell application "Terminal"' -e 'close front window' -e 'end tell'
