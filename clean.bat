@echo off
REM Annytunes - Complete cleanup of temporary files, build outputs, .gradle and .idea
echo ====================================================
echo             Annytunes - Workspace Clean             
echo ====================================================
echo.

echo [1/4] Stopping Gradle daemons...
call gradlew.bat --stop 2>nul

echo [2/4] Removing build directories...
if exist "build" rmdir /s /q "build" 2>nul
if exist "app\build" rmdir /s /q "app\build" 2>nul
if exist "flutter\build" rmdir /s /q "flutter\build" 2>nul

echo [3/4] Removing .gradle and .idea directories...
if exist ".gradle" rmdir /s /q ".gradle" 2>nul
if exist ".idea" rmdir /s /q ".idea" 2>nul

echo [4/4] Removing residual cache and iml files...
del /f /q *.iml 2>nul
del /f /q app\*.iml 2>nul

echo.
echo ====================================================
echo   Workspace cleaned successfully!
echo   Removed directories: .gradle, .idea, build, app/build
echo ====================================================
echo.
