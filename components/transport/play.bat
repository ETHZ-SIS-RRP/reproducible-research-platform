@echo off
set "here=%~dp0"
Powershell.exe -executionpolicy remotesigned -File %here%play.ps1
