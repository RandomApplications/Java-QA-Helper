::
:: Created by Pico Mitchell (of Free Geek) on 08/23/19
:: For QA Helper
:: Last Updated: 10/03/20
::
:: MIT License
::
:: Copyright (c) 2019 Free Geek
::
:: Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
:: to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
:: and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
::
:: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
::
:: THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
:: FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
:: WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
::


:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::                                                                               ::
::            TO INSTALL QA HELPER, DOUBLE CLICK THIS FILE TO RUN IT             ::
:: IF WINDOWS BLOCKS THE SCRIPT FROM RUNNING, CLICK "MORE INFO" AND "RUN ANYWAY" ::
::                                                                               ::
::         OR, YOU CAN EXECUTE THIS FILE MANUALLY FROM A COMMAND PROMPT          ::
::                                                                               ::
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


@ECHO off

ECHO.
ECHO   Preparing to Install QA Helper...

SET MODE=%1
IF [%MODE%] == [] SET MODE=install

:: Add empty lines for PowerShell progress UI
ECHO.
ECHO.
ECHO.
ECHO.

ECHO   IMPORTANT: Internet Is Required During Installation Process

\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -NoLogo -NoProfile -NonInteractive -Command ^
	[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12, [Net.SecurityProtocolType]::Ssl3;^
	for ($downloadAttempt = 0; $downloadAttempt -lt 5; $downloadAttempt ++) {^
		try {^
			$actuallyInstallScriptContent = Invoke-RestMethod -Uri 'https://apps.freegeek.org/qa-helper/download/actually-install-qa-helper.ps1' -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop;^
			if ($actuallyInstallScriptContent.Contains('qa-helper')) {^
				$actuallyInstallScriptBlock = [ScriptBlock]::Create($actuallyInstallScriptContent);^
				Invoke-Command $actuallyInstallScriptBlock -ArgumentList %MODE% -ErrorAction Stop;^
				break^
			} else {^
				throw 'Invalid Installer Script Contents'^
			}^
		} catch {^
			Write-Host """"`n  ERROR LOADING QA HELPER INSTALLER: $_"""" -ForegroundColor Red;^
			Write-Host """"  IMPORTANT: Internet Is Required During Installation Process"""" -ForegroundColor Red;^
			if ($downloadAttempt -lt 4) {^
				Write-Host """"  Load Installer Attempt $($downloadAttempt + 1) of 5 - TRYING AGAIN..."""" -ForegroundColor Yellow;^
				Start-Sleep ($downloadAttempt + 1)^
			} else {^
				Write-Host """"  Failed to Load QA Helper Installer After 5 Attempts"""" -ForegroundColor Yellow^
			}^
		}^
	}

ECHO.
ECHO   Press Any Key to Close This Window

PAUSE >NUL

EXIT 0
