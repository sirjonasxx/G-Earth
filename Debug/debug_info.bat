@echo off

echo --- Java > debug_info.txt
echo [*] Getting Java version
java -version 2>> debug_info.txt

echo --- C++ >> debug_info.txt
echo [*] Getting C++ info
wmic product get name | findstr Redistributable >> debug_info.txt

echo --- Hosts >> debug_info.txt
echo [*] Getting Hosts info
type C:\Windows\System32\Drivers\etc\hosts | findstr /v "^#" | findstr . >> debug_info.txt || echo File empty. >> debug_info.txt

echo --- System type >> debug_info.txt
echo [*] Getting System info
echo %PROCESSOR_ARCHITECTURE% >> debug_info.txt

echo --- G-Earth >> debug_info.txt
echo [*] Opening G-Earth
echo [!] Close the G-Earth window when your problem occur
java -jar ../G-Earth.jar >> debug_info.txt 2> debug_error.txt
