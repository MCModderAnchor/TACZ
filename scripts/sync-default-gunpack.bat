@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0sync-default-gunpack.ps1" -Watch %*
