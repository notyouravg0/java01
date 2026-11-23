@echo off
setlocal

echo ======================================================================
echo    GROUP 10 CAR RENTAL SYSTEM - APPLICATION LAUNCHER
echo ======================================================================
echo.

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

:: 1. Progressive JDK Discovery
set "JAVA_CMD="
set "JAVAC_CMD="

if exist "%SCRIPT_DIR%jdk\bin\java.exe" (
    set "JAVA_CMD=%SCRIPT_DIR%jdk\bin\java.exe"
    set "JAVAC_CMD=%SCRIPT_DIR%jdk\bin\javac.exe"
    echo [INFO] Located bundled JDK at: %SCRIPT_DIR%jdk
    goto java_found
)

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
        set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
        echo [INFO] Using JAVA_HOME: %JAVA_HOME%
        goto java_found
    )
)

for %%D in (
    "C:\Program Files\Java"
    "C:\Program Files\Eclipse Adoptium"
    "C:\Program Files\Microsoft"
    "C:\Program Files\Amazon Corretto"
    "C:\Program Files\Zulu"
) do (
    if exist %%D (
        for /D %%J in (%%~D\jdk*) do (
            if exist "%%J\bin\java.exe" (
                set "JAVA_CMD=%%J\bin\java.exe"
                set "JAVAC_CMD=%%J\bin\javac.exe"
                echo [INFO] Found JDK at: %%J
                goto java_found
            )
        )
    )
)

where java >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "JAVA_CMD=java"
    set "JAVAC_CMD=javac"
    echo [INFO] Using java from PATH
    goto java_found
)

echo [ERROR] Java Development Kit (JDK) not found!
echo Please install JDK 11 or later.
pause
exit /b 1

:java_found
"%JAVA_CMD%" -version 2>&1 | findstr /i "version"
echo.

:: 2. Setup output directories and classpath (bin and lib)
set "BIN_DIR=%SCRIPT_DIR%bin"
set "LIB_DIR=%SCRIPT_DIR%lib"
set "SRC_DIR=%SCRIPT_DIR%src"
set "CLASSPATH=%BIN_DIR%;%LIB_DIR%\*"

if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"

:: 3. Compile sources into bin directory
echo [INFO] Compiling Java sources...
"%JAVAC_CMD%" -cp "%LIB_DIR%\*" -d "%BIN_DIR%" ^
    "%SRC_DIR%\carrental\model\Rentable.java" ^
    "%SRC_DIR%\carrental\model\Vehicle.java" ^
    "%SRC_DIR%\carrental\model\Car.java" ^
    "%SRC_DIR%\carrental\model\Sedan.java" ^
    "%SRC_DIR%\carrental\model\SUV.java" ^
    "%SRC_DIR%\carrental\model\Hatchback.java" ^
    "%SRC_DIR%\carrental\model\Customer.java" ^
    "%SRC_DIR%\carrental\model\Rental.java" ^
    "%SRC_DIR%\carrental\exception\CarUnavailableException.java" ^
    "%SRC_DIR%\carrental\db\DBConnection.java" ^
    "%SRC_DIR%\carrental\db\DBInit.java" ^
    "%SRC_DIR%\carrental\dao\CarDAO.java" ^
    "%SRC_DIR%\carrental\dao\CustomerDAO.java" ^
    "%SRC_DIR%\carrental\dao\RentalDAO.java" ^
    "%SRC_DIR%\carrental\service\RentalService.java" ^
    "%SRC_DIR%\carrental\ui\TricolourTheme.java" ^
    "%SRC_DIR%\carrental\ui\CarInventoryPanel.java" ^
    "%SRC_DIR%\carrental\ui\CustomerBookingPanel.java" ^
    "%SRC_DIR%\carrental\ui\RentCalculationPanel.java" ^
    "%SRC_DIR%\carrental\ui\ReturnInspectionPanel.java" ^
    "%SRC_DIR%\carrental\ui\RentalHistoryPanel.java" ^
    "%SRC_DIR%\carrental\ui\CarRentalApp.java" ^
    "%SRC_DIR%\carrental\Main.java"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)
echo [INFO] Compilation successful.
echo.

:: 4. Launch main application
echo [INFO] Starting Car Rental System GUI...
echo ======================================================================
"%JAVA_CMD%" -cp "%CLASSPATH%" carrental.Main

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Application exited with error code %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [INFO] Application closed normally.
pause
