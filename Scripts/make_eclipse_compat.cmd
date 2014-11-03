:: This is based on the shell script with the same name. #!/bin/sh
:: echo off

:: check to make sure we are in the base directory.
if NOT exist BasicSamples (
    echo "*** Error: this script must be run from the base directory of your working copy."
    exit /B 1
)

echo "Making eclipse-compatible projects..."
if NOT exist eclipse_compat mkdir eclipse_compat

echo "Converting each sample"

for %%i in (BeGenerous ButtonClicker TypeANumber CollectAllTheStars2 TrivialQuest TrivialQuest2 SkeletonTbmp SavedGames libraries\BaseGameUtils) do (
    echo "Preparing %%i..."
    if NOT exist  eclipse_compat\%%i mkdir  eclipse_compat\%%i
    copy BasicSamples\%%i\src\main\AndroidManifest.xml eclipse_compat\%%i

::    # copy resources
	if exist eclipse_compat\%%i\res ( rd  /s /q  eclipse_compat\%%i\res)
	xcopy /e /i BasicSamples\%%i\src\main\res\*.* eclipse_compat\%%i\res

::    # copy source files
	if exist eclipse_compat\%%i\src ( rd  /s /q  eclipse_compat\%%i\src)
    	mkdir  eclipse_compat\%%i\src
    	xcopy /e /i BasicSamples\%%i\src\main\java\* eclipse_compat\%%i\src

::    # copy libs
	if exist eclipse_compat\%%i\libs ( rd  /s /q  eclipse_compat\%%i\libs)
    	mkdir eclipse_compat\%%i\libs
    	xcopy /e /i Scripts\eclipse_aux\*.jar eclipse_compat\%%i\libs
    )

echo "Eclipse-compatible folders generated in eclipse_compat\"

