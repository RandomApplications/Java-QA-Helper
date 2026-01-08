#!/bin/bash
# shellcheck enable=add-default-case,avoid-nullary-conditions,check-unassigned-uppercase,deprecate-which,quote-safe-variables,require-double-brackets

#
# Created by Pico Mitchell (of Free Geek)
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

PATH='/usr/bin:/bin:/usr/sbin:/sbin'

PROJECT_PATH="$(cd "${BASH_SOURCE[0]%/*}/.." &> /dev/null && pwd -P)"
readonly PROJECT_PATH

BUILD_VERSION="$(head -1 "${PROJECT_PATH}/src/Resources/qa-helper-version.txt")"
readonly BUILD_VERSION

TMPDIR="$([[ -d "${TMPDIR}" && -w "${TMPDIR}" ]] && echo "${TMPDIR%/}" || echo '/private/tmp')" # Make sure "TMPDIR" is always set and that it DOES NOT have a trailing slash for consistency regardless of the current environment.

if [[ "${BUILD_VERSION}" != *'-0' ]]; then # Only check for updates when building a release version
	if [[ "$(uname)" == 'Darwin' ]]; then # Only run these update checks on macOS
		echo -e '\nChecking for NetBeans Update...'
		# Suppress ShellCheck suggestion to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
		# shellcheck disable=SC2012
		INSTALLED_NETBEANS_VERSION="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' '/Applications/Apache NetBeans.app/Contents/Info.plist')"
		readonly INSTALLED_NETBEANS_VERSION
		echo "  Installed NetBeans Version: ${INSTALLED_NETBEANS_VERSION}"

		LATEST_NETBEANS_VERSION="$(curl -m 5 -sfL 'https://netbeans.apache.org/front/main/' | xmllint --html --xpath 'string(//h1)' - 2> /dev/null)"
		LATEST_NETBEANS_VERSION="${LATEST_NETBEANS_VERSION//[^0-9.]/}"
		readonly LATEST_NETBEANS_VERSION

		if [[ -n "${LATEST_NETBEANS_VERSION}" ]]; then
			echo "     Latest NetBeans Version: ${LATEST_NETBEANS_VERSION}"

			if [[ "${LATEST_NETBEANS_VERSION}" != "${INSTALLED_NETBEANS_VERSION}" ]]; then
				if osascript -e "display dialog \"NetBeans version ${LATEST_NETBEANS_VERSION} is now available!

NetBeans version ${INSTALLED_NETBEANS_VERSION} is currently installed.\" buttons {\"Continue Build with NetBeans ${INSTALLED_NETBEANS_VERSION}\", \"Download NetBeans ${LATEST_NETBEANS_VERSION}\"} cancel button 1 default button 2 with title \"Newer NetBeans Available\" with icon (\"${PROJECT_PATH}/macOS Build Resources/QA Helper.icns\" as POSIX file)" &> /dev/null; then
					echo '  OPENING DOWNLOAD NEWER NETBEANS LINK'
					open "https://netbeans.apache.org/$(curl -m 5 -sfL 'https://netbeans.apache.org' | xmllint --html --xpath 'string(//a[@class="button success"]/@href)' - 2> /dev/null)"
				fi
			fi
		else
			echo -e '  FAILED TO GET LATEST NETBEANS VERSION'
			afplay /System/Library/Sounds/Basso.aiff
		fi


		echo -e '\nChecking for JDK Update...'
		declare -a jdk_versions_and_paths=()
		while IFS='' read -rd '' this_jdk_java_binary; do
			this_jdk_version="$("${this_jdk_java_binary}" --version 2>&1 | awk -F '[( )]' '($(NF-2) == "build") { print $(NF-1) }')"
			this_jdk_version="${this_jdk_version%%-*}"
			jdk_versions_and_paths+=( "${this_jdk_version}:${this_jdk_java_binary/\/Contents\/Home\/bin\/java/}" )
		done < <(find '/Library/Java/JavaVirtualMachines/' -type f -perm +111 -name 'java' -print0)
		installed_latest_jdk_version_and_path="$(printf '%s\n' "${jdk_versions_and_paths[@]}" | sort -rV | head -1)"

		readonly INSTALLED_JDK_VERSION="${installed_latest_jdk_version_and_path%%:*}"
		if [[ -n "${INSTALLED_JDK_VERSION}" ]]; then
			readonly INSTALLED_JDK_MAJOR_VERSION="${INSTALLED_JDK_VERSION%%.*}"
			echo "  Installed JDK Version: ${INSTALLED_JDK_VERSION}"

			LATEST_JDK_VERSION="$(osascript -l 'JavaScript' -e 'run = argv => JSON.parse(argv[0])[0].version_data.openjdk_version' -- "$(curl -m 5 -sf "https://api.adoptium.net/v3/assets/feature_releases/${INSTALLED_JDK_MAJOR_VERSION}/ga")" 2> /dev/null)"
			LATEST_JDK_VERSION="${LATEST_JDK_VERSION%-LTS}"
			readonly LATEST_JDK_VERSION

			if [[ -n "${LATEST_JDK_VERSION}" ]]; then
				echo "     Latest JDK Version: ${LATEST_JDK_VERSION}"

				if [[ "${LATEST_JDK_VERSION}" != "${INSTALLED_JDK_VERSION}" ]]; then
					osascript -e "display dialog \"JDK version ${LATEST_JDK_VERSION} is now available!

JDK version ${INSTALLED_JDK_VERSION} is currently installed.\" buttons {\"Continue Build\"} default button 1 with title \"Newer JDK Available\" with icon (\"${PROJECT_PATH}/macOS Build Resources/QA Helper.icns\" as POSIX file)" &> /dev/null
				fi
			else
				echo -e '  FAILED TO GET LATEST JDK VERSION'
				afplay /System/Library/Sounds/Basso.aiff
			fi
		else
			echo -e '  FAILED TO GET INSTALLED JDK VERSION'
			afplay /System/Library/Sounds/Basso.aiff
		fi


		echo -e '\nChecking for FlatLaf Update...'
		# Suppress ShellCheck suggestion to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
		# shellcheck disable=SC2012
		INSTALLED_FLATLAF_VERSION="$(ls -t "${PROJECT_PATH}/libs/flatlaf-"* | awk -F '-|[.]jar' '{ print $(NF-1); exit }')"
		readonly INSTALLED_FLATLAF_VERSION
		echo "  Installed FlatLaf Version: ${INSTALLED_FLATLAF_VERSION}"

		# The "https://maven-badges.sml.io" URLs seem to fail more frequently lately, maybe because of rate-limiting, so use "--retry 2" to try a total of 3 times with the default "curl" delays which seem to help.
		LATEST_FLATLAF_VERSION="$(curl -m 5 --retry 2 -sfw '%{redirect_url}' -o /dev/null 'https://maven-badges.sml.io/maven-central/com.formdev/flatlaf' | awk -F '/' '{ print $7; exit }')"
		readonly LATEST_FLATLAF_VERSION

		if [[ -n "${LATEST_FLATLAF_VERSION}" ]]; then
			echo "     Latest FlatLaf Version: ${LATEST_FLATLAF_VERSION}"

			if [[ "${LATEST_FLATLAF_VERSION}" != "${INSTALLED_FLATLAF_VERSION}" ]]; then
				if osascript -e "display dialog \"FlatLaf version ${LATEST_FLATLAF_VERSION} is now available!

FlatLaf version ${INSTALLED_FLATLAF_VERSION} is currently installed.\" buttons {\"Continue Build with FlatLaf ${INSTALLED_FLATLAF_VERSION}\", \"Download FlatLaf ${LATEST_FLATLAF_VERSION}\"} cancel button 1 default button 2 with title \"Newer FlatLaf Available for QA Helper\" with icon (\"${PROJECT_PATH}/macOS Build Resources/QA Helper.icns\" as POSIX file)" &> /dev/null; then
					echo '  OPENING DOWNLOAD NEWER FLATLAF LINK'
					open 'https://maven-badges.sml.io/maven-central/com.formdev/flatlaf'
				fi
			fi
		else
			echo -e '  FAILED TO GET LATEST FLATLAF VERSION'
			afplay /System/Library/Sounds/Basso.aiff
		fi

		echo -e '\nChecking for org.json Update...'
		# Suppress ShellCheck suggestion to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
		# shellcheck disable=SC2012
		INSTALLED_ORGJSON_VERSION="$(ls -t "${PROJECT_PATH}/libs/json-"* | awk -F '-|[.]jar' '{ print $(NF-1); exit }')"
		readonly INSTALLED_ORGJSON_VERSION
		echo "  Installed org.json Version: ${INSTALLED_ORGJSON_VERSION}"

		if [[ -n "${LATEST_FLATLAF_VERSION}" ]]; then
			# The "https://maven-badges.sml.io" URLs seem to fail more frequently lately, maybe because of rate-limiting, so use "--retry 2" to try a total of 3 times with the default "curl" delays which seem to help.
			LATEST_ORGJSON_VERSION="$(curl -m 5 --retry 2 -sfw '%{redirect_url}' -o /dev/null 'https://maven-badges.sml.io/maven-central/org.json/json' | awk -F '/' '{ print $7; exit }')"
			readonly LATEST_ORGJSON_VERSION

			if [[ -n "${LATEST_ORGJSON_VERSION}" ]]; then
				echo "     Latest org.json Version: ${LATEST_ORGJSON_VERSION}"

				if [[ "${LATEST_ORGJSON_VERSION}" != "${INSTALLED_ORGJSON_VERSION}" ]]; then
					if osascript -e "display dialog \"org.json version ${LATEST_ORGJSON_VERSION} is now available!

org.json version ${INSTALLED_ORGJSON_VERSION} is currently installed.\" buttons {\"Continue Build with org.json ${INSTALLED_ORGJSON_VERSION}\", \"Download org.json ${LATEST_ORGJSON_VERSION}\"} cancel button 1 default button 2 with title \"Newer org.json Available for QA Helper\" with icon (\"${PROJECT_PATH}/macOS Build Resources/QA Helper.icns\" as POSIX file)" &> /dev/null; then
						echo '  OPENING DOWNLOAD NEWER ORG.JSON LINK'
						open 'https://maven-badges.sml.io/maven-central/org.json/json'
					fi
				fi
			else
				echo -e '  FAILED TO GET LATEST ORG.JSON VERSION'
				afplay /System/Library/Sounds/Basso.aiff
			fi
		else
			echo '  NOT CHECKING ORG.JSON VERSION BECAUSE RETRIEVING LATEST FLATLAF VERSION FROM SAME SOURCE FAILED'
		fi

		echo -e '\nChecking for Apache Commons Text Libraries Update...'
		# Suppress ShellCheck suggestions to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
		# shellcheck disable=SC2012
		INSTALLED_COMMONS_TEXT_VERSION="$(ls -t "${PROJECT_PATH}/libs/commons-text-"* | awk -F '-|[.]jar' '{ print $(NF-1); exit }')"
		readonly INSTALLED_COMMONS_TEXT_VERSION
		# shellcheck disable=SC2012
		INSTALLED_COMMONS_LANG_VERSION="$(ls -t "${PROJECT_PATH}/libs/commons-lang3-"* | awk -F '-|[.]jar' '{ print $(NF-1); exit }')"
		readonly INSTALLED_COMMONS_LANG_VERSION
		echo "  Installed Apache Commons Text Libraries Versions: ${INSTALLED_COMMONS_TEXT_VERSION}, ${INSTALLED_COMMONS_LANG_VERSION}"

		if [[ -n "${LATEST_FLATLAF_VERSION}" ]]; then
			LATEST_COMMONS_TEXT_VERSION="$(curl -m 5 --retry 2 -sfw '%{redirect_url}' -o /dev/null 'https://maven-badges.sml.io/maven-central/org.apache.commons/commons-text' | awk -F '/' '{ print $7; exit }')"
			readonly LATEST_COMMONS_TEXT_VERSION
			LATEST_COMMONS_LANG_VERSION="$(curl -m 5 --retry 2 -sfw '%{redirect_url}' -o /dev/null 'https://maven-badges.sml.io/maven-central/org.apache.commons/commons-lang3' | awk -F '/' '{ print $7; exit }')"
			readonly LATEST_COMMONS_LANG_VERSION

			if [[ -n "${LATEST_COMMONS_TEXT_VERSION}" && -n "${LATEST_COMMONS_LANG_VERSION}" ]]; then
				echo "     Latest Apache Commons Text Libraries Versions: ${LATEST_COMMONS_TEXT_VERSION}, ${LATEST_COMMONS_LANG_VERSION}"

				if [[ "${LATEST_COMMONS_TEXT_VERSION}" != "${INSTALLED_COMMONS_TEXT_VERSION}" || "${LATEST_COMMONS_LANG_VERSION}" != "${INSTALLED_COMMONS_LANG_VERSION}" ]]; then
					if osascript -e "display dialog \"Apache Commons Text Libraries versions ${LATEST_COMMONS_TEXT_VERSION}, ${LATEST_COMMONS_LANG_VERSION} are now available!

Apache Commons Text Libraries versions ${INSTALLED_COMMONS_TEXT_VERSION}, ${INSTALLED_COMMONS_LANG_VERSION} are currently installed.\" buttons {\"Continue Build with Current Apache Commons Text Libraries\", \"Download Latest Apache Commons Text Libraries\"} cancel button 1 default button 2 with title \"Newer Apache Commons Text Libraries Available for QA Helper\" with icon (\"${PROJECT_PATH}/macOS Build Resources/QA Helper.icns\" as POSIX file)" &> /dev/null; then
						echo '  OPENING DOWNLOAD NEWER APACHE COMMONS TEXT LIBS LINKS'

						if [[ "${LATEST_COMMONS_TEXT_VERSION}" != "${INSTALLED_COMMONS_TEXT_VERSION}" ]]; then
							open 'https://maven-badges.sml.io/maven-central/org.apache.commons/commons-text'
						fi

						if [[ "${LATEST_COMMONS_LANG_VERSION}" != "${INSTALLED_COMMONS_LANG_VERSION}" ]]; then
							open 'https://maven-badges.sml.io/maven-central/org.apache.commons/commons-lang3'
						fi
					fi
				fi
			else
				echo "  FAILED TO GET ALL LATEST APACHE COMMONS TEXT LIBS VERSIONS (${LATEST_COMMONS_TEXT_VERSION:-N/A}, ${LATEST_COMMONS_LANG_VERSION:-N/A})"
				afplay /System/Library/Sounds/Basso.aiff
			fi
		else
			echo '  NOT CHECKING ALL LATEST APACHE COMMONS TEXT LIBS VERSIONS BECAUSE RETRIEVING LATEST FLATLAF VERSION FROM SAME SOURCE FAILED'
		fi


		echo -e '\nChecking for HDSentinel for Linux Update...'
		# Suppress ShellCheck suggestion to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
		# shellcheck disable=SC2012
		INCLUDED_HDSENTINEL_LINUX_VERSION="$(ls -t "${PROJECT_PATH}/src/Resources/hdsentinel-"*'-x64' | awk -F '-' '{ print $(NF-1); exit }')"
		INCLUDED_HDSENTINEL_LINUX_VERSION=${INCLUDED_HDSENTINEL_LINUX_VERSION//[^0-9]/}
		if [[ "${INCLUDED_HDSENTINEL_LINUX_VERSION}" == '0'* ]]; then
			INCLUDED_HDSENTINEL_LINUX_VERSION="${INCLUDED_HDSENTINEL_LINUX_VERSION/0/0.}"
		fi
		readonly INCLUDED_HDSENTINEL_LINUX_VERSION
		echo "  Included HDSentinel for Linux Version: ${INCLUDED_HDSENTINEL_LINUX_VERSION}"

		LATEST_HDSENTINEL_LINUX_VERSION="$(curl -m 5 -sfL 'https://www.hdsentinel.com/hard_disk_sentinel_linux.php' | xmllint --html --xpath 'string(//h3[text()="Updates"]/following-sibling::p/b)' - 2> /dev/null)"
		readonly LATEST_HDSENTINEL_LINUX_VERSION

		if [[ -n "${LATEST_HDSENTINEL_LINUX_VERSION}" ]]; then
			echo "    Latest HDSentinel for Linux Version: ${LATEST_HDSENTINEL_LINUX_VERSION}"

			if [[ "${LATEST_HDSENTINEL_LINUX_VERSION}" != "${INCLUDED_HDSENTINEL_LINUX_VERSION}" ]]; then
				if osascript -e "display dialog \"HDSentinel for Linux version ${LATEST_HDSENTINEL_LINUX_VERSION} is now available!

HDSentinel for Linux version ${INCLUDED_HDSENTINEL_LINUX_VERSION} is currently included.\" buttons {\"Continue Build with HDSentinel ${INCLUDED_HDSENTINEL_LINUX_VERSION}\", \"Download HDSentinel ${LATEST_HDSENTINEL_LINUX_VERSION}\"} cancel button 1 default button 2 with title \"Newer HDSentinel Available\" with icon (\"${PROJECT_PATH}/macOS Build Resources/QA Helper.icns\" as POSIX file)" &> /dev/null; then
					echo '  OPENING DOWNLOAD NEWER HDSENTINEL LINK'
					open 'https://www.hdsentinel.com/hard_disk_sentinel_linux.php'
				fi
			fi
		else
			echo -e '  FAILED TO GET LATEST HDSENTINEL VERSION'
			afplay /System/Library/Sounds/Basso.aiff
		fi
	fi


	echo -e '\nDownloading Latest PCI IDs...'
	declare -i PREVIOUS_PCI_IDS_LINE_COUNT
	PREVIOUS_PCI_IDS_LINE_COUNT="$({ wc -l "${PROJECT_PATH}/src/Resources/pci.ids" 2> /dev/null || echo '0'; } | awk '{ print $1; exit }')"
	readonly PREVIOUS_PCI_IDS_LINE_COUNT

	rm -f "${TMPDIR}/qa-helper_pci.ids.bz2"
	rm -f "${TMPDIR}/qa-helper_pci.ids"

	curl -m 5 -sfL 'https://pci-ids.ucw.cz/v2.2/pci.ids.bz2' -o "${TMPDIR}/qa-helper_pci.ids.bz2"
	bunzip2 "${TMPDIR}/qa-helper_pci.ids.bz2"
	declare -i NEW_PCI_IDS_LINE_COUNT
	NEW_PCI_IDS_LINE_COUNT="$({ wc -l "${TMPDIR}/qa-helper_pci.ids" 2> /dev/null || echo '0'; } | awk '{ print $1; exit }')"
	readonly NEW_PCI_IDS_LINE_COUNT

	if (( NEW_PCI_IDS_LINE_COUNT >= PREVIOUS_PCI_IDS_LINE_COUNT )); then
		mv -f "${TMPDIR}/qa-helper_pci.ids" "${PROJECT_PATH}/src/Resources/pci.ids"
		echo "  Downloaded PCI IDs $(grep '^#	Version: ' "${PROJECT_PATH}/src/Resources/pci.ids" | cut -c 12-) into '[PROJECT FOLDER]/src/Resources/pci.ids'"
	else
		rm -f "${TMPDIR}/qa-helper_pci.ids"
		echo "  NEW pci.is LINE COUNT NOT GREATER THAN OR EQUAL TO PREVIOUS (${NEW_PCI_IDS_LINE_COUNT} < ${PREVIOUS_PCI_IDS_LINE_COUNT})"
		afplay '/System/Library/Sounds/Basso.aiff'
	fi

	echo -e '\nDownloading Latest USB IDs...'
	declare -i PREVIOUS_USB_IDS_LINE_COUNT
	PREVIOUS_USB_IDS_LINE_COUNT="$({ wc -l "${PROJECT_PATH}/src/Resources/usb.ids" 2> /dev/null || echo '0'; } | awk '{ print $1; exit }')"
	readonly PREVIOUS_USB_IDS_LINE_COUNT

	rm -f "${TMPDIR}/qa-helper_usb.ids.bz2"
	rm -f "${TMPDIR}/qa-helper_usb.ids"

	curl -m 5 -sfL 'https://usb-ids.gowdy.us/usb.ids.bz2' -o "${TMPDIR}/qa-helper_usb.ids.bz2" || curl -m 5 -sfL 'http://www.linux-usb.org/usb.ids.bz2' -o "${TMPDIR}/qa-helper_usb.ids.bz2"
	bunzip2 "${TMPDIR}/qa-helper_usb.ids.bz2"
	declare -i NEW_USB_IDS_LINE_COUNT
	NEW_USB_IDS_LINE_COUNT="$({ wc -l "${TMPDIR}/qa-helper_usb.ids" 2> /dev/null || echo 0; } | awk '{ print $1; exit }')"
	readonly NEW_USB_IDS_LINE_COUNT

	if (( NEW_USB_IDS_LINE_COUNT >= PREVIOUS_USB_IDS_LINE_COUNT )); then
		mv -f "${TMPDIR}/qa-helper_usb.ids" "${PROJECT_PATH}/src/Resources/usb.ids"
		echo "  Downloaded USB IDs $(grep '^# Version: ' "${PROJECT_PATH}/src/Resources/usb.ids" | cut -c 12-) into '[PROJECT FOLDER]/src/Resources/usb.ids'"
	else
		rm -f "${TMPDIR}/qa-helper_usb.ids"
		echo "  NEW usb.is LINE COUNT NOT GREATER THAN OR EQUAL TO PREVIOUS (${NEW_USB_IDS_LINE_COUNT} < ${PREVIOUS_USB_IDS_LINE_COUNT})"
		afplay '/System/Library/Sounds/Basso.aiff'
	fi
else
	echo -e '\nSkipping Online Update Checks when Building Testing Version'
fi

echo -e '\nCopying Latest Keyboard_Test.jar...'
keyboard_test_jar_path="${PROJECT_PATH}/../../Keyboard Test/dist/Keyboard_Test.jar"
if [[ -f "${keyboard_test_jar_path}" ]]; then
	cp -f "${keyboard_test_jar_path}" "${PROJECT_PATH}/src/Resources/Keyboard_Test.jar"
	echo "  Copied Keyboard_Test.jar $(unzip -p "${PROJECT_PATH}/src/Resources/Keyboard_Test.jar" '*/keyboard-test-version.txt' | head -1) into '[PROJECT FOLDER]/src/Resources/Keyboard_Test.jar'"
else
	echo "  DID NOT FIND Latest Keyboard_Test.jar"
fi

echo -e '\nDone Checking for Updates\n\n'
