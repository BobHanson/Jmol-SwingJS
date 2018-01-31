@echo off
echo wrapper script to run xml2po python script on Windows

echo %*
set python_prog=%PYTHON_PATH%\python.exe
"%python_prog%" %*

echo done.