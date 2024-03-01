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
					echo '  CANCELING BUILD TO DOWNLOAD NEWER NETBEANS'
					open "https://netbeans.apache.org/$(curl -m 5 -sfL 'https://netbeans.apache.org' | xmllint --html --xpath 'string(//a[@class="button success"]/@href)' - 2> /dev/null)"
					exit 1
				fi
			fi
		else
			echo -e '  FAILED TO GET LATEST NETBEANS VERSION\n'
			afplay /System/Library/Sounds/Basso.aiff
		fi


		echo -e '\nChecking for JDK Update...'
		# Suppress ShellCheck suggestion to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
		# shellcheck disable=SC2012
		INSTALLED_JDK_VERSION="$(ls -t '/Library/Java/JavaVirtualMachines/' | awk -F '-|[.]jdk' '{ print $(NF-1); exit }')"
		readonly INSTALLED_JDK_VERSION
		echo "  Installed JDK Version: ${INSTALLED_JDK_VERSION}"

		LATEST_JDK_VERSION="$(osascript -l 'JavaScript' -e 'run = argv => JSON.parse(argv[0])[0].version_data.openjdk_version' -- "$(curl -m 5 -sf 'https://api.adoptium.net/v3/assets/feature_releases/21/ga')" 2> /dev/null)"
		LATEST_JDK_VERSION="${LATEST_JDK_VERSION%-LTS}"
		readonly LATEST_JDK_VERSION

		if [[ -n "${LATEST_JDK_VERSION}" ]]; then
			echo "     Latest JDK Version: ${LATEST_JDK_VERSION}"

			if [[ "${LATEST_JDK_VERSION}" != "${INSTALLED_JDK_VERSION}" ]]; then
				if osascript -e "display dialog \"JDK version ${LATEST_JDK_VERSION} is now available!

JDK version ${INSTALLED_JDK_VERSION} is currently installed.\" buttons {\"Continue Build with JDK ${INSTALLED_JDK_VERSION}\", \"Download JDK ${LATEST_JDK_VERSION}\"} cancel button 1 default button 2 with title \"Newer JDK Available for QA Helper\" with icon (\"${PROJECT_PATH}/macOS Build Resources/QA Helper.icns\" as POSIX file)" &> /dev/null; then
					echo '  CANCELING BUILD TO DOWNLOAD NEWER JDK'

					open "https://api.adoptium.net/v3/binary/latest/21/ga/mac/$([[ "$(sysctl -in hw.optional.arm64)" == '1' ]] && echo 'aarch64' || echo 'x64')/jdk/hotspot/normal/eclipse"

					exit 1
				fi
			fi
		else
			echo -e '  FAILED TO GET LATEST JDK VERSION\n'
			afplay /System/Library/Sounds/Basso.aiff
		fi


		echo -e '\nChecking for FlatLaf Update...'
		# Suppress ShellCheck suggestion to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
		# shellcheck disable=SC2012
		INSTALLED_FLATLAF_VERSION="$(ls -t "${PROJECT_PATH}/libs/flatlaf-"* | awk -F '-|[.]jar' '{ print $(NF-1); exit }')"
		readonly INSTALLED_FLATLAF_VERSION
		echo "  Installed FlatLaf Version: ${INSTALLED_FLATLAF_VERSION}"

		# The "https://maven-badges.herokuapp.com" URLs seem to fail more frequently lately, maybe because of rate-limiting, so use "--retry 2" to try a total of 3 times with the default "curl" delays which seem to help.
		LATEST_FLATLAF_VERSION="$(curl -m 5 --retry 2 -sfw '%{redirect_url}' -o /dev/null 'https://maven-badges.herokuapp.com/maven-central/com.formdev/flatlaf' | awk -F '/' '{ print $7; exit }')"
		readonly LATEST_FLATLAF_VERSION

		if [[ -n "${LATEST_FLATLAF_VERSION}" ]]; then
			echo "     Latest FlatLaf Version: ${LATEST_FLATLAF_VERSION}"

			if [[ "${LATEST_FLATLAF_VERSION}" != "${INSTALLED_FLATLAF_VERSION}" ]]; then
				if osascript -e "display dialog \"FlatLaf version ${LATEST_FLATLAF_VERSION} is now available!

FlatLaf version ${INSTALLED_FLATLAF_VERSION} is currently installed.\" buttons {\"Continue Build with FlatLaf ${INSTALLED_FLATLAF_VERSION}\", \"Download FlatLaf ${LATEST_FLATLAF_VERSION}\"} cancel button 1 default button 2 with title \"Newer FlatLaf Available for QA Helper\" with icon (\"${PROJECT_PATH}/macOS Build Resources/QA Helper.icns\" as POSIX file)" &> /dev/null; then
					echo '  CANCELING BUILD TO DOWNLOAD NEWER FLATLAF'
					open 'https://maven-badges.herokuapp.com/maven-central/com.formdev/flatlaf'
					exit 1
				fi
			fi
		else
			echo -e '  FAILED TO GET LATEST FLATLAF VERSION\n'
			afplay /System/Library/Sounds/Basso.aiff
		fi

		echo -e '\nChecking for SOAP Libraries Update...'
		# Suppress ShellCheck suggestions to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
		# shellcheck disable=SC2012
		INSTALLED_SOAP_LIB_JAK_ACT_VERSION="$(ls -t "${PROJECT_PATH}/libs/jakarta.activation-"* | awk -F '-|[.]jar' '{ print $(NF-1); exit }')"
		readonly INSTALLED_SOAP_LIB_JAK_ACT_VERSION
		# shellcheck disable=SC2012
		INSTALLED_SOAP_LIB_JAK_XML_VERSION="$(ls -t "${PROJECT_PATH}/libs/jakarta.xml.soap-api-"* | awk -F '-|[.]jar' '{ print $(NF-1); exit }')"
		readonly INSTALLED_SOAP_LIB_JAK_XML_VERSION
		# shellcheck disable=SC2012
		INSTALLED_SOAP_LIB_SAJ_IMP_VERSION="$(ls -t "${PROJECT_PATH}/libs/saaj-impl-"* | awk -F '-|[.]jar' '{ print $(NF-1); exit }')"
		readonly INSTALLED_SOAP_LIB_SAJ_IMP_VERSION
		# shellcheck disable=SC2012
		INSTALLED_SOAP_LIB_STX_EX_VERSION="$(ls -t "${PROJECT_PATH}/libs/stax-ex-"* | awk -F '-|[.]jar' '{ print $(NF-1); exit }')"
		readonly INSTALLED_SOAP_LIB_STX_EX_VERSION
		echo "  Installed SOAP Libraries Versions: ${INSTALLED_SOAP_LIB_JAK_ACT_VERSION}, ${INSTALLED_SOAP_LIB_JAK_XML_VERSION}, ${INSTALLED_SOAP_LIB_SAJ_IMP_VERSION}, ${INSTALLED_SOAP_LIB_STX_EX_VERSION}"

		LATEST_SOAP_LIB_JAK_ACT_VERSION="$(curl -m 5 --retry 2 -sfw '%{redirect_url}' -o /dev/null 'https://maven-badges.herokuapp.com/maven-central/com.sun.activation/jakarta.activation' | awk -F '/' '{ print $7; exit }')"
		readonly LATEST_SOAP_LIB_JAK_ACT_VERSION
		LATEST_SOAP_LIB_JAK_XML_VERSION="$(curl -m 5 --retry 2 -sfw '%{redirect_url}' -o /dev/null 'https://maven-badges.herokuapp.com/maven-central/jakarta.xml.soap/jakarta.xml.soap-api' | awk -F '/' '{ print $7; exit }')"
		readonly LATEST_SOAP_LIB_JAK_XML_VERSION
		LATEST_SOAP_LIB_SAJ_IMP_VERSION="$(curl -m 5 --retry 2 -sfw '%{redirect_url}' -o /dev/null 'https://maven-badges.herokuapp.com/maven-central/com.sun.xml.messaging.saaj/saaj-impl' | awk -F '/' '{ print $7; exit }')"
		readonly LATEST_SOAP_LIB_SAJ_IMP_VERSION
		LATEST_SOAP_LIB_STX_EX_VERSION="$(curl -m 5 --retry 2 -sfw '%{redirect_url}' -o /dev/null 'https://maven-badges.herokuapp.com/maven-central/org.jvnet.staxex/stax-ex' | awk -F '/' '{ print $7; exit }')"
		readonly LATEST_SOAP_LIB_STX_EX_VERSION
		

		if [[ -n "${LATEST_SOAP_LIB_JAK_ACT_VERSION}" && -n "${LATEST_SOAP_LIB_JAK_XML_VERSION}" && -n "${LATEST_SOAP_LIB_SAJ_IMP_VERSION}" && -n "${LATEST_SOAP_LIB_STX_EX_VERSION}" ]]; then
			echo "     Latest SOAP Libraries Versions: ${LATEST_SOAP_LIB_JAK_ACT_VERSION}, ${LATEST_SOAP_LIB_JAK_XML_VERSION}, ${LATEST_SOAP_LIB_SAJ_IMP_VERSION}, ${LATEST_SOAP_LIB_STX_EX_VERSION}"

			if [[ "${LATEST_SOAP_LIB_JAK_ACT_VERSION}" != "${INSTALLED_SOAP_LIB_JAK_ACT_VERSION}" || "${LATEST_SOAP_LIB_JAK_XML_VERSION}" != "${INSTALLED_SOAP_LIB_JAK_XML_VERSION}" ||
				"${LATEST_SOAP_LIB_SAJ_IMP_VERSION}" != "${INSTALLED_SOAP_LIB_SAJ_IMP_VERSION}" || "${LATEST_SOAP_LIB_STX_EX_VERSION}" != "${INSTALLED_SOAP_LIB_STX_EX_VERSION}" ]]; then
				if osascript -e "display dialog \"SOAP Libraries versions ${LATEST_SOAP_LIB_JAK_ACT_VERSION}, ${LATEST_SOAP_LIB_JAK_XML_VERSION}, ${LATEST_SOAP_LIB_SAJ_IMP_VERSION}, ${LATEST_SOAP_LIB_STX_EX_VERSION} are now available!

SOAP Libraries versions ${INSTALLED_SOAP_LIB_JAK_ACT_VERSION}, ${INSTALLED_SOAP_LIB_JAK_XML_VERSION}, ${INSTALLED_SOAP_LIB_SAJ_IMP_VERSION}, ${INSTALLED_SOAP_LIB_STX_EX_VERSION} are currently installed.\" buttons {\"Continue Build with Current SOAPLibraries\", \"Download Latest SOAP Libraries\"} cancel button 1 default button 2 with title \"Newer SOAP Libraries Available for QA Helper\" with icon (\"${PROJECT_PATH}/macOS Build Resources/QA Helper.icns\" as POSIX file)" &> /dev/null; then
					echo '  CANCELING BUILD TO DOWNLOAD NEWER SOAP LIBS'

					if [[ "${LATEST_SOAP_LIB_JAK_ACT_VERSION}" != "${INSTALLED_SOAP_LIB_JAK_ACT_VERSION}" ]]; then
						open 'https://maven-badges.herokuapp.com/maven-central/com.sun.activation/jakarta.activation'
					fi

					if [[ "${LATEST_SOAP_LIB_JAK_XML_VERSION}" != "${INSTALLED_SOAP_LIB_JAK_XML_VERSION}" ]]; then
						open 'https://maven-badges.herokuapp.com/maven-central/jakarta.xml.soap/jakarta.xml.soap-api'
					fi

					if [[ "${LATEST_SOAP_LIB_SAJ_IMP_VERSION}" != "${INSTALLED_SOAP_LIB_SAJ_IMP_VERSION}" ]]; then
						open 'https://maven-badges.herokuapp.com/maven-central/com.sun.xml.messaging.saaj/saaj-impl'
					fi

					if [[ "${LATEST_SOAP_LIB_STX_EX_VERSION}" != "${INSTALLED_SOAP_LIB_STX_EX_VERSION}" ]]; then
						open 'https://maven-badges.herokuapp.com/maven-central/org.jvnet.staxex/stax-ex'
					fi

					exit 1
				fi
			fi
		else
			echo -e "  FAILED TO GET ALL LATEST SOAP LIBS VERSIONS (${LATEST_SOAP_LIB_JAK_ACT_VERSION:-N/A}, ${LATEST_SOAP_LIB_JAK_XML_VERSION:-N/A}, ${LATEST_SOAP_LIB_SAJ_IMP_VERSION:-N/A}, ${LATEST_SOAP_LIB_STX_EX_VERSION:-N/A})\n"
			afplay /System/Library/Sounds/Basso.aiff
		fi


		echo -e '\nChecking for HDSentinel for Linux Update...'
		# Suppress ShellCheck suggestion to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
		# shellcheck disable=SC2012
		INCLUDED_HDSENTINEL_LINUX_VERSION="$(ls -t "${PROJECT_PATH}/src/Resources/hdsentinel-"*'-x64' | awk -F '-' '{ print $2; exit }')"
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
					echo '  CANCELING BUILD TO DOWNLOAD NEWER HDSENTINEL'
					open 'https://www.hdsentinel.com/hard_disk_sentinel_linux.php'
					exit 1
				fi
			fi
		else
			echo -e '  FAILED TO GET LATEST HDSENTINEL VERSION\n'
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

	echo -e '\nDone Checking for Updates\n\n'
else
	echo -e '\nSkipping Checking for Updates when Building Testing Version\n\n'
fi
