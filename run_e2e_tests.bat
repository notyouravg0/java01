@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo    GROUP 10 CAR RENTAL SYSTEM - AUTOMATED E2E TEST RUNNER LAUNCHER
echo ======================================================================

:: 1. Progressive JDK Discovery
set "JAVAC_CMD="
set "JAVA_CMD="

:: Check Local Bundled JDK
if exist "%~dp0jdk\bin\javac.exe" (
    set "JAVAC_CMD=%~dp0jdk\bin\javac.exe"
    set "JAVA_CMD=%~dp0jdk\bin\java.exe"
    echo [INFO] Located bundled JDK at: %~dp0jdk
    goto :jdk_found
)

:: Check JAVA_HOME
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
        echo [INFO] Located JDK via JAVA_HOME: %JAVA_HOME%
        goto :jdk_found
    )
)

:: Check System PATH
where javac >nul 2>&1
if !ERRORLEVEL! equ 0 (
    where java >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        set "JAVAC_CMD=javac"
        set "JAVA_CMD=java"
        echo [INFO] Located javac and java in system PATH
        goto :jdk_found
    )
)

:: Search Standard Windows Installation Paths
for /d %%D in ("%ProgramFiles%\Java\jdk*" "%ProgramFiles%\Eclipse Adoptium\jdk*" "%ProgramFiles%\Amazon Corretto\jdk*" "%ProgramFiles%\Zulu\zulu*" "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk*" "%ProgramFiles(x86)%\Java\jdk*") do (
    if exist "%%~fD\bin\javac.exe" (
        set "JAVAC_CMD=%%~fD\bin\javac.exe"
        set "JAVA_CMD=%%~fD\bin\java.exe"
        echo [INFO] Discovered JDK installation at: %%~fD
        goto :jdk_found
    )
)

:jdk_not_found
echo.
echo ======================================================================
echo [ERROR] JDK - Java Development Kit - was not found on this system!
echo.
echo In order to compile and run the E2E Acceptance Test Suite, a standard
echo Java SE JDK 8 or higher is required.
echo.
echo Recommended Options:
echo   1. Install Eclipse Adoptium Temurin OpenJDK from https://adoptium.net/
echo   2. Set your JAVA_HOME environment variable to your JDK path:
echo      set JAVA_HOME=C:\Path\To\Your\JDK
echo      set PATH=%%JAVA_HOME%%\bin;%%PATH%%
echo ======================================================================
exit /b 1

:jdk_found
echo [INFO] Using Java Compiler: "%JAVAC_CMD%"
echo [INFO] Using Java Runtime : "%JAVA_CMD%"
"%JAVA_CMD%" -version

:: 2. Ensure bin/ directory exists
if not exist "bin" (
    mkdir "bin"
    echo [INFO] Created bin/ compilation directory.
)

:: 3. Collect Java source files for compilation
echo [INFO] Discovering source files to compile...
set "SOURCES_LIST=%TEMP%\e2e_sources_%RANDOM%.txt"
if exist "%SOURCES_LIST%" del "%SOURCES_LIST%"

dir /s /b "src\*.java" "test\carrental\e2e\*.java" > "%SOURCES_LIST%" 2>nul

:: 4. Compile sources into bin/
echo [INFO] Compiling Group 10 application and test sources...
"%JAVAC_CMD%" -cp ".;lib/*;src;bin" -d bin @"%SOURCES_LIST%"
set COMPILE_STATUS=!ERRORLEVEL!
if exist "%SOURCES_LIST%" del "%SOURCES_LIST%"

if !COMPILE_STATUS! neq 0 (
    echo.
    echo ======================================================================
    echo [ERROR] Compilation failed with exit code !COMPILE_STATUS!!
    echo Please resolve the compiler errors above before re-running tests.
    echo ======================================================================
    exit /b !COMPILE_STATUS!
)

echo [INFO] Compilation successful!
echo.

:: 5. Execute E2ETestRunner
echo ======================================================================
echo Launching carrental.e2e.E2ETestRunner...
echo ======================================================================
"%JAVA_CMD%" -cp "bin;lib/*;src;test" carrental.e2e.E2ETestRunner %*
set TEST_STATUS=!ERRORLEVEL!

exit /b !TEST_STATUS!
