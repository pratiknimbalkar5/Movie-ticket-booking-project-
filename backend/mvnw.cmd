@IF "%DEBUG%" == "" @ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION

SET ERROR_CODE=0

IF NOT "%JAVA_HOME%" == "" GOTO OkJHome
ECHO Error: JAVA_HOME is not set. >&2
GOTO error

:OkJHome
SET "BASEDIR=%~dp0"
IF "!BASEDIR:~-1!"=="\" SET "BASEDIR=!BASEDIR:~0,-1!"

SET "WRAPPER_DIR=%BASEDIR%\.mvn\wrapper"
SET "WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar"
SET "MAVEN_DIR=%BASEDIR%\.mvn\apache-maven-3.9.9"

IF NOT EXIST "%WRAPPER_DIR%" MKDIR "%WRAPPER_DIR%"

@REM If Maven folder does not exist, download and unzip Maven automatically
IF NOT EXIST "%MAVEN_DIR%\bin\mvn.cmd" (
    ECHO Setting up Apache Maven 3.9.9...
    IF NOT EXIST "%WRAPPER_DIR%\maven.zip" (
        ECHO Downloading Maven...
        C:\Windows\System32\curl.exe -fLo "%WRAPPER_DIR%\maven.zip" "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
    )
    ECHO Extracting Maven...
    powershell -NoProfile -Command "Expand-Archive -Path '%WRAPPER_DIR%\maven.zip' -DestinationPath '%BASEDIR%\.mvn' -Force"
    IF EXIST "%WRAPPER_DIR%\maven.zip" DEL /Q "%WRAPPER_DIR%\maven.zip"
)

IF EXIST "%MAVEN_DIR%\bin\mvn.cmd" (
    CALL "%MAVEN_DIR%\bin\mvn.cmd" %*
    EXIT /B %ERRORLEVEL%
)

ECHO Error: Could not setup Maven.
GOTO error

:error
EXIT /B 1
