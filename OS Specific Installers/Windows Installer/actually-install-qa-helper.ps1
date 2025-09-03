#
# Created by Pico Mitchell (of Free Geek) on 08/23/19
# For QA Helper
# Last Updated: 8/25/25
#
# MIT License
#
# Copyright (c) 2019 Free Geek
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
# to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

param(
	[Parameter(Position = 0)]
	[String]$Mode
)

$ProgressPreference = 'SilentlyContinue' # Not showing progress makes "Invoke-WebRequest" downloads MUCH faster: https://stackoverflow.com/a/43477248

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12, [Net.SecurityProtocolType]::Ssl3

$Mode = $Mode.ToLower()
$forceUpdate = $false
$uninstall = $false
$reinstall = $false

$downloadURL = 'https://apps.freegeek.org/qa-helper/download'

Write-Host "`n  Successfully Loaded QA Helper Installer" -ForegroundColor Green

if (($Mode -eq 'test') -or ($Mode -eq 'testing')) {
	$forceUpdate = $true
	if (Test-Connection 'tools.freegeek.org' -Count 1 -Quiet) {
		$downloadURL = 'http://tools.freegeek.org/qa-helper/download'
		Write-Host "`n  MODE SET: Install Latest TEST Version" -ForegroundColor Yellow
	} else {
		Write-Host "`n  TEST MODE NOT SET - Local Free Geek Network Required - SETTING UPDATE MODE INSTEAD" -ForegroundColor Yellow
	}
} elseif ($Mode -eq 'update') {
	$forceUpdate = $true
	Write-Host "`n  MODE SET: Update to Latest Live Version" -ForegroundColor Yellow
} elseif ($Mode -eq 'uninstall') {
	$uninstall = $true
	Write-Host "`n  MODE SET: Uninstall" -ForegroundColor Yellow
} elseif ($Mode -eq 'reinstall') {
	$reinstall = $true
	Write-Host "`n  MODE SET: Re-Install" -ForegroundColor Yellow
}

if ((-not $uninstall) -and (-not (Test-Connection 'apps.freegeek.org' -Count 1 -Quiet))) {
	throw 'Failed to Connect to "apps.freegeek.org" Within Installer'
}

Start-Sleep 2 # Sleep for a couple seconds to be able to see last results before clearing screen.

$installPath = '\Install\QA Helper'
$desktopPath = [Environment]::GetFolderPath('Desktop')

if ($uninstall -or $reinstall) {
	Clear-Host
	Write-Output "`n  Uninstalling QA Helper..."
	
	try {
		Get-CimInstance Win32_Process -Filter 'Name LIKE "java%.exe" AND CommandLine LIKE "%QA_Helper.jar%"' -ErrorAction Stop | Invoke-CimMethod -Name Terminate -ErrorAction Stop | Out-Null
	} catch {
		Write-Host "    ERROR QUITTING QA HELPER: $_" -ForegroundColor Red
	}
	
	$uninstallSuccessful = $true
	
	try {
		if (Test-Path $installPath) {
			Write-Output "    Deleting `"$installPath`" Folder..."

			Remove-Item $installPath -Recurse -Force -ErrorAction Stop
		} else {
			Write-Host "    The `"$installPath`" Folder Was Already Deleted" -ForegroundColor Yellow
		}
	} catch {
		Write-Host "    ERROR: $_" -ForegroundColor Red
		Write-Host "    ERROR DELETING FOLDER: Failed to delete the `"$installPath`" folder." -ForegroundColor Red

		$uninstallSuccessful = $false
	}
	
	try {
		if (Test-Path '\Install\Launch QA Helper.lnk') {
			Write-Output '    Deleting Shortcut Within "\Install" Folder...'

			Remove-Item '\Install\Launch QA Helper.lnk' -Force -ErrorAction Stop
		} else {
			Write-Host '    Shortcut Within "\Install" Folder Was Already Deleted' -ForegroundColor Yellow
		}
	} catch {
		Write-Host "    ERROR: $_" -ForegroundColor Red
		Write-Host '    ERROR DELETING FOLDER: Failed to delete shortcut within "\Install" folder.' -ForegroundColor Red

		$uninstallSuccessful = $false
	}
	
	try {
		if (Test-Path "$Env:APPDATA\Microsoft\Windows\Start Menu\Programs\QA Helper.lnk") {
			Write-Output '    Deleting Shortcut in User Start Menu...'

			Remove-Item "$Env:APPDATA\Microsoft\Windows\Start Menu\Programs\QA Helper.lnk" -Force -ErrorAction Stop
		} else {
			Write-Host '    Shortcut in User Start Menu Was Already Deleted' -ForegroundColor Yellow
		}
	} catch {
		Write-Host "    ERROR: $_" -ForegroundColor Red
		Write-Host "    ERROR DELETING SHORTCUT: Failed to delete shortcut in User Start Menu." -ForegroundColor Red

		$uninstallSuccessful = $false
	}
	
	try {
		if (Test-Path "$desktopPath\QA Helper.lnk") {
			Write-Output '    Deleting Shortcut on User Desktop...'

			Remove-Item "$desktopPath\QA Helper.lnk" -Force -ErrorAction Stop
		} else {
			Write-Host '    Shortcut on User Desktop Was Already Deleted' -ForegroundColor Yellow
		}
	} catch {
		Write-Host "    ERROR: $_" -ForegroundColor Red
		Write-Host '    ERROR DELETING SHORTCUT: Failed to delete shortcut on User Desktop.' -ForegroundColor Red

		$uninstallSuccessful = $false
	}
	
	try {
		if (Test-Path "$Env:TEMP\QAHelper-jar.zip") {
			Write-Output '    Deleting Temporary QA Helper Appler Download File...'

			Remove-Item "$Env:TEMP\QAHelper-jar.zip" -Force -ErrorAction Stop
		}
		# Do not output anything if temp file doesn't exist.
	} catch {
		Write-Host "    ERROR: $_" -ForegroundColor Red
		Write-Host '    ERROR DELETING TEMP FILE: Failed to delete temporary QA Helper download applet file.' -ForegroundColor Red

		$uninstallSuccessful = $false
	}
	
	try {
		if (Test-Path "$Env:TEMP\qa-helper_java-jre.zip") {
			Write-Output '    Deleting Temporary Java Download File...'

			Remove-Item "$Env:TEMP\qa-helper_java-jre.zip" -Force -ErrorAction Stop
		}
		# Do not output anything if temp file doesn't exist.
	} catch {
		Write-Host "    ERROR: $_" -ForegroundColor Red
		Write-Host '    ERROR DELETING TEMP FILE: Failed to delete temporary Java download file.' -ForegroundColor Red

		$uninstallSuccessful = $false
	}
	
	if ($uninstallSuccessful) {
		Write-Host "`n  Successfully Uninstalled QA Helper" -ForegroundColor Green
	} else {
		Write-Host "`n  Errors Occurred During While Uninstalling QA Helper - SEE ERRORS LISTED ABOVE" -ForegroundColor Red
	}
	
	if ($reinstall) {
		Start-Sleep 2 # Sleep for a couple seconds to be able to see last results before clearing screen.
	}
}

if (-not $uninstall) {
	Clear-Host
	Write-Output "`n  Installing QA Helper...`n`n`n`n`n" # Add empty lines for PowerShell progress UI
	
	if (-not (Test-Path $installPath)) {
		New-Item -ItemType 'Directory' -Path $installPath | Out-Null
	}
	
	if ($forceUpdate -or (-not (Test-Path "$installPath\QA_Helper.jar"))) {
		Write-Output '  Installing QA Helper Applet:'

		for ($installAppletAttempt = 0; $installAppletAttempt -lt 5; $installAppletAttempt ++) {
			try {
				Write-Output '    Downloading QA Helper Applet...'

				Get-CimInstance Win32_Process -Filter 'Name LIKE "java%.exe" AND CommandLine LIKE "%QA_Helper.jar%"' -ErrorAction Stop | Invoke-CimMethod -Name Terminate -ErrorAction Stop | Out-Null
				
				if (Test-Path "$Env:TEMP\QAHelper-jar.zip") {
					Remove-Item "$Env:TEMP\QAHelper-jar.zip" -Force -ErrorAction Stop
				}

				Invoke-WebRequest "$downloadURL/QAHelper-windows-jar.zip" -OutFile "$Env:TEMP\QAHelper-jar.zip" -ErrorAction Stop

				if (Test-Path "$Env:TEMP\QAHelper-jar.zip") {
					Write-Output '    Unarchiving QA Helper Applet and Moving to Install Location...'
					
					if (Test-Path "$installPath\QA_Helper.jar") {
						Remove-Item "$installPath\QA_Helper.jar" -Force -ErrorAction Stop
					}
					
					Expand-Archive "$Env:TEMP\QAHelper-jar.zip" "$installPath\" -Force -ErrorAction Stop
					
					if (Test-Path "$Env:TEMP\QAHelper-jar.zip") {
						Remove-Item "$Env:TEMP\QAHelper-jar.zip" -Force -ErrorAction Stop
					}
					
					if (Test-Path "$installPath\QA_Helper.jar") {
						Write-Host "`n  Successfully Installed QA Helper Applet" -ForegroundColor Green

						break
					} else {
						throw 'Applet Was Not Installed'
					}
				} else {
					throw 'Applet Was Not Downloaded'
				}
			} catch {
				Write-Host "    ERROR INSTALLING APPLET: $_" -ForegroundColor Red
				Write-Host '    IMPORTANT: Internet Is Required During Installation Process' -ForegroundColor Red
				
				if ($installAppletAttempt -lt 4) {
					Write-Host "    Install Applet Attempt $($installAppletAttempt + 1) of 5 - TRYING AGAIN..." -ForegroundColor Yellow

					Start-Sleep ($installAppletAttempt + 1) # Sleep a little longer after each attempt.
				} else {
					Write-Host '    Failed to Install Applet After 5 Attempts' -ForegroundColor Yellow
				}
			}
		}
	} else {
		Write-Host '  SKIPPING APPLET INSTALLATION: QA Helper Applet Was Already Installed' -ForegroundColor Yellow
	}

	if (Test-Path "$installPath\QA_Helper.jar") {
		if (Test-Path "$installPath\java-jre\bin\javaw.exe") {
			Write-Host "`n`n  SKIPPING JAVA INSTALLATION: Java Was Already Installed" -ForegroundColor Yellow
		} else {
			$jdkVersion = '21.0.8+9'

			Write-Output "`n`n  Installing Java $($jdkVersion.Replace('_', '+')):"
			
			$jdkDownloadURL = "https://apps.freegeek.org/qa-helper/download/resources/windows/jlink-jre-$($jdkVersion.Replace('+', '_'))_windows-x64.zip"
            
			if (Test-Connection 'tools.freegeek.org' -Count 1 -Quiet) { # Check if on local Free Geek network for a faster Java download.
				$jdkDownloadURL = "http://tools.freegeek.org/qa-helper/download/resources/windows/jlink-jre-$($jdkVersion.Replace('+', '_'))_windows-x64.zip"
			}
	
			for ($installJavaAttempt = 0; $installJavaAttempt -lt 5; $installJavaAttempt ++) {
				try {
					Write-Output '    Downloading Java...'

					if (Test-Path "$Env:TEMP\qa-helper_java-jre.zip") {
						Remove-Item "$Env:TEMP\qa-helper_java-jre.zip" -Force -ErrorAction Stop
					}

					Invoke-WebRequest $jdkDownloadURL -OutFile "$Env:TEMP\qa-helper_java-jre.zip" -ErrorAction Stop

					if (Test-Path "$Env:TEMP\qa-helper_java-jre.zip") {
						Write-Output '    Unarchiving Java and Moving to Install Location...'
						
						if (Test-Path "$installPath\java-jre") {
							Remove-Item "$installPath\java-jre" -Recurse -Force -ErrorAction Stop
						}

						if (Test-Path "$installPath\jdk-$($jdkVersion.Replace('_', '+'))") {
							Remove-Item "$installPath\jdk-$($jdkVersion.Replace('_', '+'))" -Recurse -Force -ErrorAction Stop
						}

						if (Test-Path "$installPath\jdk-$($jdkVersion.Replace('_', '+'))-jre") {
							Remove-Item "$installPath\jdk-$($jdkVersion.Replace('_', '+'))-jre" -Recurse -Force -ErrorAction Stop
						}
		
						Expand-Archive "$Env:TEMP\qa-helper_java-jre.zip" "$installPath\" -Force -ErrorAction Stop
						
						if (Test-Path "$installPath\jdk-$($jdkVersion.Replace('_', '+'))-jre") {
							Rename-Item "$installPath\jdk-$($jdkVersion.Replace('_', '+'))-jre" "java-jre" -Force -ErrorAction Stop # Rename folder in case Temurin JRE download from Adoptium.
						}
						
						if (Test-Path "$Env:TEMP\qa-helper_java-jre.zip") {
							Remove-Item "$Env:TEMP\qa-helper_java-jre.zip" -Force -ErrorAction Stop
						}

						if (Test-Path "$installPath\java-jre\bin\javaw.exe") {
							Write-Host "`n  Successfully Installed Java" -ForegroundColor Green

							break
						} else {
							throw 'Java Was Not Installed'
						}
					} else {
						throw 'Java Was Not Downloaded'
					}
				} catch {
					Write-Host "    ERROR INSTALLING JAVA: $_" -ForegroundColor Red
					Write-Host '    IMPORTANT: Internet Is Required During Installation Process' -ForegroundColor Red

					if ($installJavaAttempt -gt 1) { # Download Temurin JRE from Adoptium if failed to download condensed custom JLink JRE multiple times (will do 2 download attempts at this location).
                        $jdkDownloadURL = 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse'
                    } elseif ($installJavaAttempt -gt 0) { # Make sure we've tried the apps.freegeek.org location at least once before falling back on Temurin JRE download from Adoptium.
                        $jdkDownloadURL = "https://apps.freegeek.org/qa-helper/download/resources/windows/jlink-jre-$($jdkVersion.Replace('+', '_'))_windows-x64.zip"
					}

					if ($installJavaAttempt -lt 4) {
						Write-Host "    Install Java Attempt $($installJavaAttempt + 1) of 5 - TRYING AGAIN..." -ForegroundColor Yellow

						Start-Sleep ($installJavaAttempt + 1) # Sleep a little longer after each attempt.
					} else {
						Write-Host '    Failed to Install Java After 5 Attempts' -ForegroundColor Yellow
					}
				}
			}
		}

		if (Test-Path "$installPath\java-jre\bin\javaw.exe") {
			# Don't bother creating any launcher shortcuts in WinPE
			$isWinPE = (((Test-Path '\Windows\System32\startnet.cmd') -Or (Test-Path '\Windows\System32\winpeshl.ini')) -And (Get-ItemProperty 'HKLM:\SYSTEM\Setup').FactoryPreInstallInProgress)
			
			if ((-not $isWinPE) -and ($forceUpdate -or (-not (Test-Path "$installPath\qa-helper-icon.ico")) -or (-not (Test-Path "$installPath\Launch QA Helper.lnk")) -or (-not (Test-Path "\Install\Launch QA Helper.lnk")) -or (-not (Test-Path "$Env:APPDATA\Microsoft\Windows\Start Menu\Programs\QA Helper.lnk")) -or (-not (Test-Path "$desktopPath\QA Helper.lnk")))) {
				Write-Output "`n`n  Creating QA Helper Launcher Shortcuts:"

				$launcherCreationSuccessful = $true

				try {
					Write-Output '    Extracting App Icon From Applet for Shortcuts...'

					if (Test-Path "$installPath\qa-helper-icon.ico") {
						Remove-Item "$installPath\qa-helper-icon.ico" -Force -ErrorAction Stop
					}

					Add-Type -AssemblyName System.IO.Compression.FileSystem
					$qaHelperJar = [System.IO.Compression.ZipFile]::OpenRead("$installPath\QA_Helper.jar")
					$qaHelperJar.Entries | Where-Object { $_.Name -Eq 'qa-helper-icon.ico' } | Select-Object -First 1 { [System.IO.Compression.ZipFileExtensions]::ExtractToFile($_, "$installPath\qa-helper-icon.ico", $true) } | Out-Null
					$qaHelperjar.Dispose()

					if (-not (Test-Path "$installPath\qa-helper-icon.ico")) {
						throw 'App Icon Does Not Exist After Extraction From Applet'
					}
				} catch {
					Write-Host "    ERROR: $_" -ForegroundColor Red
					Write-Host '    ERROR EXTRACTING APP ICON: Failed to extracted app icon from applet for shortcuts.' -ForegroundColor Red

					$launcherCreationSuccessful = $false
				}

				try {
					Write-Output "    Creating Shortcut Within `"$installPath`" Folder..."

					if (Test-Path "$installPath\Launch QA Helper.lnk") {
						Remove-Item "$installPath\Launch QA Helper.lnk" -Force -ErrorAction Stop
					}

					$installPathShortcut = (New-Object -ComObject Wscript.Shell).CreateShortcut("$installPath\Launch QA Helper.lnk")
					$installPathShortcut.TargetPath = "$installPath\java-jre\bin\javaw.exe"
					$installPathShortcut.Arguments = "-jar `"$installPath\QA_Helper.jar`""
					$installPathShortcut.IconLocation = "$installPath\qa-helper-icon.ico"
					$installPathShortcut.Save()
					
					if (-not (Test-Path "$installPath\Launch QA Helper.lnk")) {
						throw "Shortcut Does Not Exist Within `"$installPath`" Folder"
					}
				} catch {
					Write-Host "    ERROR: $_" -ForegroundColor Red
					Write-Host "    ERROR CREATING SHORTCUT: Failed to create shortcut within `"$installPath`" folder." -ForegroundColor Red

					$launcherCreationSuccessful = $false
				}

				try {
					Write-Output '    Creating Shortcut Within "\Install" Folder...'

					if (Test-Path '\Install\Launch QA Helper.lnk') {
						Remove-Item '\Install\Launch QA Helper.lnk' -Force -ErrorAction Stop
					}

					Copy-Item "$installPath\Launch QA Helper.lnk" '\Install\Launch QA Helper.lnk' -Force -ErrorAction Stop
				} catch {
					Write-Host "    ERROR: $_" -ForegroundColor Red
					Write-Host '    ERROR CREATING SHORTCUT: Failed to create shortcut within "\Install" folder.' -ForegroundColor Red

					$launcherCreationSuccessful = $false
				}

				try {
					Write-Output '    Creating Shortcut in User Start Menu...'

					if (Test-Path "$Env:APPDATA\Microsoft\Windows\Start Menu\Programs\QA Helper.lnk") {
						Remove-Item "$Env:APPDATA\Microsoft\Windows\Start Menu\Programs\QA Helper.lnk" -Force -ErrorAction Stop
					}

					if (Test-Path "$Env:APPDATA\Microsoft\Windows\Start Menu\Programs") {
						Copy-Item "$installPath\Launch QA Helper.lnk" "$Env:APPDATA\Microsoft\Windows\Start Menu\Programs\QA Helper.lnk" -Force -ErrorAction Stop
					} else {
						throw "User Start Menu Programs Folder Does Not Exist ($Env:APPDATA\Microsoft\Windows\Start Menu\Programs)"
					}
				} catch {
					Write-Host "    ERROR: $_" -ForegroundColor Red
					Write-Host '    ERROR CREATING SHORTCUT: Failed to create shortcut in User Start Menu.' -ForegroundColor Red

					$launcherCreationSuccessful = $false
				}

				try {
					Write-Output '    Creating Shortcut on User Desktop...'

					if (Test-Path "$desktopPath\QA Helper.lnk") {
						Remove-Item "$desktopPath\QA Helper.lnk" -Force -ErrorAction Stop
					}

					Copy-Item "$installPath\Launch QA Helper.lnk" "$desktopPath\QA Helper.lnk" -Force -ErrorAction Stop
				} catch {
					Write-Host "    ERROR: $_" -ForegroundColor Red
					Write-Host '    ERROR CREATING SHORTCUT: Failed to create shortcut on User Desktop.' -ForegroundColor Red

					$launcherCreationSuccessful = $false
				}

				if ($launcherCreationSuccessful) {
					Write-Host "`n  Successfully Created All QA Helper Launchers" -ForegroundColor Green
				} else {
					Write-Host "`n  Errors Occurred During QA Helper Launcher Creation - SEE ERRORS LISTED ABOVE" -ForegroundColor Red
				}
			} elseif ($isWinPE) {
				Write-Host "`n`n  SKIPPING LAUNCHER CREATION: Launchers Are Not Necessary in Windows Preinstallation Environment" -ForegroundColor Yellow
			} else {
				Write-Host "`n`n  SKIPPING LAUNCHER CREATION: All QA Helper Launchers Already Exist" -ForegroundColor Yellow
			}

			Write-Host "`n`n  FINISHED INSTALLING QA HELPER`n" -ForegroundColor Green
		} else {
			Write-Host "`n`n  INSTALLATION FAILED: Errors Occurred During Java Installation for QA Helper - SEE ERRORS LISTED ABOVE`n" -ForegroundColor Red
		}
	} else {
		Write-Host "`n`n  INSTALLATION FAILED: Errors Occurred During Applet Installation for QA Helper - SEE ERRORS LISTED ABOVE`n" -ForegroundColor Red
	}
}
