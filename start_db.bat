@echo off
setlocal

echo ======================================================================
echo    GROUP 10 CAR RENTAL SYSTEM - DATABASE INITIALIZATION LAUNCHER
echo ======================================================================
echo.

set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

set JAVA_CMD=
set JAVAC_CMD=

if exist "%SCRIPT_DIR%jdk\bin\java.exe" (
    set JAVA_CMD=%SCRIPT_DIR%jdk\bin\java.exe
    set JAVAC_CMD=%SCRIPT_DIR%jdk\bin\javac.exe
    echo [INFO] Located bundled JDK at: %SCRIPT_DIR%jdk
    goto run_with_java
)

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set JAVA_CMD=%JAVA_HOME%\bin\java.exe
        set JAVAC_CMD=%JAVA_HOME%\bin\javac.exe
        echo [INFO] Using JAVA_HOME: %JAVA_HOME%
        goto run_with_java
    )
)

where java >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set JAVA_CMD=java
    set JAVAC_CMD=javac
    echo [INFO] Using java from PATH
    goto run_with_java
)

echo [WARN] Java runtime not found. Attempting Python initialization...
set PYTHON_CMD=
where python >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set PYTHON_CMD=python
) else (
    where py >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        set PYTHON_CMD=py
    )
)

if not "%PYTHON_CMD%"=="" (
    echo [INFO] Detected Python runtime: %PYTHON_CMD%
    %PYTHON_CMD% "%SCRIPT_DIR%db\init_db.py" %*
    goto db_done
)

echo [ERROR] Neither Java nor Python was found on this system!
pause
exit /b 1

:run_with_java
set LIB_DIR=%SCRIPT_DIR%lib
set SRC_DIR=%SCRIPT_DIR%src
set OUT_DIR=%SCRIPT_DIR%out
set SQLITE_JAR=%LIB_DIR%\sqlite-jdbc.jar

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

echo [INFO] Compiling DB initializer...
"%JAVAC_CMD%" -cp "%SQLITE_JAR%" -d "%OUT_DIR%" "%SRC_DIR%\carrental\db\DBConnection.java" "%SRC_DIR%\carrental\db\DBInit.java"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] DBInit compilation failed!
    pause
    exit /b 1
)

echo [INFO] Running database setup...
"%JAVA_CMD%" -cp "%OUT_DIR%;%SQLITE_JAR%" carrental.db.DBInit %*

:db_done
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Database setup exited with code %ERRORLEVEL%!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ======================================================================
echo [SUCCESS] SQLite database is ready at: %SCRIPT_DIR%db\car_rental.db
echo You can now launch the application using start_app.bat
echo ======================================================================
echo.
pause
