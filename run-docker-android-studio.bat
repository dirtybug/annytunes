@echo off
REM Annytunes - Run Android Studio with Native GUI (without VNC) or Docker Web GUI
echo ====================================================
echo    Annytunes - Android Studio GUI Runner (Native)  
echo ====================================================
echo.

set ACTION=%1
if "%ACTION%"=="" set ACTION=start

if "%ACTION%"=="stop" (
    echo [INFO] Stopping Android Studio...
    wsl.exe -d Ubuntu killall -9 studio.sh java 2>nul
    docker compose stop android-studio 2>nul
    echo [INFO] Android Studio stopped successfully!
    exit /b 0
)

if "%ACTION%"=="docker" (
    echo [INFO] Starting Android Studio in Docker (Web noVNC + VNC)...
    docker compose up -d android-studio
    echo [INFO] Android Studio container started.
    echo [INFO] Opening Web Browser interface (noVNC)...
    start http://localhost:6080/vnc.html?autoconnect=true^&resize=remote
    exit /b 0
)

where wsl >nul 2>nul
if %errorlevel% neq 0 (
    echo [INFO] WSL not found, launching via Docker container...
    docker compose up -d android-studio
    start http://localhost:6080/vnc.html?autoconnect=true^&resize=remote
    exit /b 0
)

echo [1/2] Preparing native WSLg graphical environment (without VNC)...
echo [2/2] Opening Android Studio in native Windows window...
start "" wsl.exe -d Ubuntu env ANDROID_HOME=/opt/android-sdk PATH="/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:$PATH" /opt/android-studio/bin/studio.sh /mnt/c/Users/JúlioAndrade/annytunes

echo.
echo ====================================================
echo     Android Studio Opened in Native Window!        
echo ====================================================
echo  - Mode:       Native Windows Graphical Interface (WSLg)
echo  - No VNC:     Direct Windows window (no browser required)
echo  - Project:    Annytunes (/workspace)
echo  - SDK:        /opt/android-sdk (API 36)
echo  - Alt (Web):  run-docker-android-studio.bat docker
echo ====================================================
echo.
