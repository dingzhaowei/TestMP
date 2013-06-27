@echo off
rem TestMP (Test Management Platform)
rem Copyright 2013 and beyond, Zhaowei Ding.
rem
rem TestMP is free software; you can redistribute it and/or modify it
rem under the terms of the MIT License (MIT).
rem 
rem This software is distributed in the hope that it will be useful,
rem but WITHOUT ANY WARRANTY; without even the implied warranty of
rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
rem Lesser General Public License for more details.

if "%OS%" == "Windows_NT" setlocal

set "CURRENT_DIR=%cd%"
if not "%TESTMP_HOME%" == "" goto hasHome
set "TESTMP_HOME=%CURRENT_DIR%"
if exist "%TESTMP_HOME%\bin\Launcher.class" goto validHome
cd ..
set "TESTMP_HOME=%cd%"
cd "%CURRENT_DIR%"
:hasHome
if exist "%TESTMP_HOME%\bin\Launcher.class" goto validHome
echo The TESTMP_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:validHome

if exist "%TESTMP_HOME%\bin\Launcher.class" goto canRun
echo Cannot find Launcher.class
echo This file is needed to run this program
goto end
:canRun

set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto setArgsDone
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:setArgsDone

cd "%TESTMP_HOME%\bin
java Launcher %CMD_LINE_ARGS%

:end