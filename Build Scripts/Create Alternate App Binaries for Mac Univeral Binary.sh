#!/bin/bash
# shellcheck enable=add-default-case,avoid-nullary-conditions,check-unassigned-uppercase,deprecate-which,quote-safe-variables,require-double-brackets

#
# Created by Pico Mitchell (of Free Geek)
#
# MIT License
#
# Copyright (c) 2021 Free Geek
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

# THIS SCRIPT MUST BE RUN ON THE ALTERNATE ARCHITECTURE TO WHAT YOU ARE BUILDING ON
# If you are building on an Intel Mac, run this script on Apple Silicon Mac to get the binaries needed to make a Universal Binary.
# If you are building on an Apple Silicon Mac, run this string under Rosetta using "arch -x86_64 bash script.sh" (or on an Intel Mac) to get the binaries needed to make a Universal Binary.

# AFTER YOU CREATE THE ALTERNATE APP BINARIES, COPY THAT STRIPPED APP BINARIES TO THE FOLLOWING FOLDER ON THE BUILD MAC:
# /Users/Shared/Mac Deployment/QA Helper Universal Binary Parts/Java [JAVA VERSION] [ALTERNATE ARCHITECTURE] App Binaries

PATH='/usr/bin:/bin:/usr/sbin:/sbin'

PROJECT_PATH="$(cd "${BASH_SOURCE[0]%/*}/.." &> /dev/null && pwd -P)"
readonly PROJECT_PATH

TMPDIR="$([[ -d "${TMPDIR}" && -w "${TMPDIR}" ]] && echo "${TMPDIR%/}" || echo '/private/tmp')" # Make sure "TMPDIR" is always set and that it DOES NOT have a trailing slash for consistency regardless of the current environment.

is_apple_silicon="$([[ "$(arch)" == 'arm'* ]] && echo 'true' || echo 'false')" # Use "arch" (which will return show i386 under Rosetta on Apple Silicon) to be able to get the Intel binaries when on Apple Silicon.

script_title="CREATING $($is_apple_silicon && echo 'APPLE SILICON' || echo 'INTEL') BINARIES TO CREATE UNIVERSAL BINARY WHEN BUILDING ON $($is_apple_silicon && echo 'INTEL' || echo 'APPLE SILICON')"
echo "${script_title}"

univeral_binary_parts_base_path='/Users/Shared/Mac Deployment/QA Helper Universal Binary Parts'
if [[ ! -d "${univeral_binary_parts_base_path}" ]]; then
	mkdir -p "${univeral_binary_parts_base_path}"
fi

jdk_major_version='21'

rm -rf "${univeral_binary_parts_base_path}/Java ${jdk_major_version}."*

jdk_full_version="$(osascript -l 'JavaScript' -e 'run = argv => JSON.parse(argv[0])[0].version_data.openjdk_version' -- "$(curl -m 5 -sf "https://api.adoptium.net/v3/assets/feature_releases/${jdk_major_version}/ga")" 2> /dev/null)"
jdk_full_version="${jdk_full_version%-LTS}"

if [[ -z "${jdk_full_version}" ]]; then
	>&2 echo -e "\n!!! FAILED TO RETRIEVE LATEST FULL VERSION FOR JDK ${jdk_major_version} !!!"
	afplay '/System/Library/Sounds/Basso.aiff'
	exit 1
fi

alternate_app_binaries_for_universal_binary_name="Java ${jdk_full_version} $($is_apple_silicon && echo 'Apple Silicon' || echo 'Intel') App Binaries"
alternate_app_binaries_for_universal_binary_path="${univeral_binary_parts_base_path}/${alternate_app_binaries_for_universal_binary_name}"
mkdir -p "${alternate_app_binaries_for_universal_binary_path}"

if [[ $1 != '--no-reveal' ]]; then
	open -R "${alternate_app_binaries_for_universal_binary_path}"
fi

jdk_download_url="$(curl -m 5 -sfw '%{redirect_url}' "https://api.adoptium.net/v3/binary/latest/${jdk_major_version}/ga/mac/$($is_apple_silicon && echo 'aarch64' || echo 'x64')/jdk/hotspot/normal/eclipse")"
jdk_archive_filename="${jdk_download_url##*/}"
echo -e "\nDOWNLOADING \"${jdk_download_url}\"..."
rm -rf "${TMPDIR:?}/${jdk_archive_filename}"
curl --connect-timeout 5 --progress-bar -fL "${jdk_download_url}" -o "${TMPDIR}/${jdk_archive_filename}" || exit 1

echo -e "\nUNARCHIVING \"${jdk_archive_filename}\"..."
tar -xzf "${TMPDIR}/${jdk_archive_filename}" -C "${alternate_app_binaries_for_universal_binary_path}" || exit 1
rm -f "${TMPDIR}/${jdk_archive_filename}"

if [[ -f "${PROJECT_PATH}/dist/QA_Helper.jar" ]]; then
	echo -e '\nCOPYING "QA_Helper.jar"...'
	ditto "${PROJECT_PATH}/dist/QA_Helper.jar" "${alternate_app_binaries_for_universal_binary_path}/QA Helper JAR/QA_Helper.jar" || exit 1
else
	qa_helper_jar_download_url='https://apps.freegeek.org/qa-helper/download/QAHelper-jar.zip'
	echo -e "\nDOWNLOADING \"${qa_helper_jar_download_url}\"..."
	rm -rf "${TMPDIR}/QAHelper-jar.zip"
	curl --connect-timeout 5 --progress-bar -fL "${qa_helper_jar_download_url}" -o "${TMPDIR}/QAHelper-jar.zip" || exit 1

	echo -e '\nUNARCHIVING "QAHelper-jar.zip"...'
	ditto -xk --noqtn "${TMPDIR}/QAHelper-jar.zip" "${alternate_app_binaries_for_universal_binary_path}/QA Helper JAR" || exit 1
	rm -f "${TMPDIR}/QAHelper-jar.zip"
fi

jdk_path="${alternate_app_binaries_for_universal_binary_path}/jdk-${jdk_full_version}"

echo -e '\nCREATING APP...'
"${jdk_path}/Contents/Home/bin/jpackage" \
	--type 'app-image' \
	--verbose \
	--name 'QA Helper' \
	--input "${alternate_app_binaries_for_universal_binary_path}/QA Helper JAR" \
	--main-jar 'QA_Helper.jar' \
	--runtime-image "${jdk_path}" \
	--dest "${alternate_app_binaries_for_universal_binary_path}" || exit 1

# Move "runtime" to "Frameworks/Java.runtime" to match actual QA Helper structure changes.
mkdir "${alternate_app_binaries_for_universal_binary_path}/QA Helper.app/Contents/Frameworks"
mv "${alternate_app_binaries_for_universal_binary_path}/QA Helper.app/Contents/runtime" "${alternate_app_binaries_for_universal_binary_path}/QA Helper.app/Contents/Frameworks/Java.runtime"

echo -e '\nREMOVING ALL EXCEPT BINARIES FROM APP...'

while IFS='' read -rd '' this_app_file_path; do
	if [[ "$(file "${this_app_file_path}")" != *'Mach-O 64-bit'* ]]; then
		echo "Deleting Non-Binary File (and Empty Parent Folders) From App: ${this_app_file_path/${alternate_app_binaries_for_universal_binary_path}\//}"
		if rm -f "${this_app_file_path}"; then
			rmdir -p "${this_app_file_path%/*}" 2> /dev/null
		else
			exit 1
		fi
	fi
done < <(find "${alternate_app_binaries_for_universal_binary_path}/QA Helper.app" -type f -print0)

touch "${alternate_app_binaries_for_universal_binary_path}/QA Helper.app"

rm -rf "${alternate_app_binaries_for_universal_binary_path}/QA Helper JAR"
rm -rf "${jdk_path}"

if [[ $1 != '--no-reveal' ]]; then
	open -R "${alternate_app_binaries_for_universal_binary_path}/QA Helper.app"
fi

echo -e "\nDONE ${script_title}\n"
