@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

set "PROJECT_ROOT=%SCRIPT_DIR%"
if exist "%SCRIPT_DIR%\..\..\src\compose_pilot" (
    set "PROJECT_ROOT=%SCRIPT_DIR%\..\.."
)

set "PYTHONPATH=%PROJECT_ROOT%\src;%PROJECT_ROOT%\.venv\Lib\site-packages;%PYTHONPATH%"
set "PYTHONIOENCODING=utf-8"

set "PYTHON_EXE=python"
if exist "%LOCALAPPDATA%\Python\bin\python.exe" (
    set "PYTHON_EXE=%LOCALAPPDATA%\Python\bin\python.exe"
)

"%PYTHON_EXE%" -m compose_pilot %*
exit /b %ERRORLEVEL%
