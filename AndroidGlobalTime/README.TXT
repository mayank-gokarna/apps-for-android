INTRODUCTION

AndroidGlobalTime is a 3D world clock and may serve as an illustrative
example of how to use the OpenGL ES APIs in your Android applications.  
When you launch it, you'll see a spinning globe showing day and night regions.
Pressing the space bar will overlay an analog clock with the time corresponding
to location you're currently examining. The arrow keys allow you to spin the
Earth and traverse through different time-zones while the clock is displayed.
Pressing the center key in the emulator toggles between a 3D and 2D view of
the earth. Pressing the L key will turn the city lights on or off. To zoom-out
press 2 and to zoom-in press 8.


BUILDING

The project directory is ready for use from either Eclipse or with the
activityCreator.py script (if you want to use something other than
eclipse).

To use Eclipse, install Eclipse, the Android SDK and the Eclipse ADT
plugin (if you haven't already, see
http://code.google.com/android/intro/installing.html for more details)
then start Eclipse, and create a new Android project. Use the
"from existing source" option, and browse to this directory. The
Android project should then be created, and you can browse the source,
build the application and run it in the emulator.

For instructions on how to build the application with activityCreator
Start a command line and change to the AndroidGlobalTime directory, then type
activityCreator.py com.google.android.globaltime.AndroidGlobalTime

(It is assumed you have set up activityCreator.py, ant and emulator
to run from the command line. Please see
http://code.google.com/android/intro/installing.html#otherides
for more details)

It should give some warnings about files already existing, this is
expected. Then type:

ant

and the project should build.

Start the emulator and then install the AndroidGlobalTime.apk file from the
bin directory:

adb install AndroidGlobalTime.apk

You should then be able to run the app.

