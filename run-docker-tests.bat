@echo off
REM Annytunes - Run tests and builds in Docker Container
echo ====================================================
echo            Annytunes - Docker Test Runner          
echo ====================================================

set TARGET=%1
if "%TARGET%"=="" set TARGET=all

REM Determine whether to run directly or via WSL2
where wsl >nul 2>nul
if %errorlevel% equ 0 (
    wsl -l -v | findstr /i "docker-desktop" | findstr /i "Running" >nul 2>nul
    if %errorlevel% neq 0 (
        echo [INFO] Windows Docker Desktop is not active. Running via WSL2 Docker engine...
        wsl -d Ubuntu --cd "%~dp0" -e ./run-docker-tests.sh %TARGET%
        goto :SUMMARY
    )
)

where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Docker not found in PATH. Please install Docker Desktop.
    exit /b 1
)

docker ps >nul 2>nul
if %errorlevel% equ 0 goto :DOCKER_READY

where wsl >nul 2>nul
if %errorlevel% equ 0 (
    echo [INFO] Windows Docker Desktop not active. Running via WSL Docker engine...
    wsl -d Ubuntu --cd "%~dp0" -e ./run-docker-tests.sh %TARGET%
    goto :SUMMARY
)

echo [ERROR] Docker Desktop is not running or the engine is still initializing.
echo Please open Docker Desktop and wait for "Engine running" status before running tests.
exit /b 1

:DOCKER_READY

echo Running Docker container for target: %TARGET%...
if "%TARGET%"=="clean" (
    echo [INFO] Cleaning Gradle cache, .gradle, .idea, and build folders...
    call clean.bat
    exit /b 0
) else if "%TARGET%"=="unit" (
    docker compose run --rm test-unit
) else if "%TARGET%"=="build" (
    docker compose run --rm build-apk
) else if "%TARGET%"=="release" (
    docker compose run --rm build-release
) else if "%TARGET%"=="instrumented" (
    docker compose run --rm test-instrumented
) else if "%TARGET%"=="all" (
    docker compose run --rm test-all
) else (
    docker compose run --rm test-unit %TARGET%
)
:SUMMARY
echo.
echo ====================================================
echo                  Annytunes - Summary
echo ====================================================
if "%TARGET%"=="all" (
    echo [INFO] Full test suite, debug APK, and release deliverables built successfully!
) else if "%TARGET%"=="unit" (
    echo [INFO] Unit tests completed successfully!
    echo [TIP] Additional available commands:
    echo        - APK Debug:   run-docker-tests.bat build
    echo        - APK Release: run-docker-tests.bat release
    echo        - All:         run-docker-tests.bat all
) else if "%TARGET%"=="build" (
    echo [INFO] Debug APK built successfully!
) else if "%TARGET%"=="release" (
    echo [INFO] Release APK and AAB built successfully!
)
echo.
echo Available files in directory: .\release\
if exist ".\release\Annytunes-release.aab" (
    echo   - AAB Release:           .\release\Annytunes-release.aab
)
if exist ".\release\Annytunes-release.apk" (
    echo   - APK Release:           .\release\Annytunes-release.apk
)
if exist ".\release\Annytunes-debug.apk" (
    echo   - APK Debug:             .\release\Annytunes-debug.apk
)
echo.
echo Available files in directory: .\release\development\
if exist ".\release\development\Annytunes-release.aab" (
    echo   - AAB Release:           .\release\development\Annytunes-release.aab
)
if exist ".\release\development\Annytunes-release.apk" (
    echo   - APK Release:           .\release\development\Annytunes-release.apk
)
if exist ".\release\development\Annytunes-debug.apk" (
    echo   - APK Debug:             .\release\development\Annytunes-debug.apk
)
if exist ".\release\development\reports\" (
    echo   - Test Reports:          .\release\development\reports\
)
echo ====================================================
echo Completed successfully!
