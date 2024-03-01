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
# [PROJECT FOLDER]/macOS Build Resources/Universal Binary Parts/Java [JAVA VERSION] [ALTERNATE ARCHITECTURE] App Binaries

PATH='/usr/bin:/bin:/usr/sbin:/sbin'

TMPDIR="$([[ -d "${TMPDIR}" && -w "${TMPDIR}" ]] && echo "${TMPDIR%/}" || echo '/private/tmp')" # Make sure "TMPDIR" is always set and that it DOES NOT have a trailing slash for consistency regardless of the current environment.

is_apple_silicon="$([[ "$(arch)" == 'arm'* ]] && echo 'true' || echo 'false')" # Use "arch" (which will return show i386 under Rosetta on Apple Silicon) to be able to get the Intel binaries when on Apple Silicon.

script_title="CREATING $($is_apple_silicon && echo 'APPLE SILICON' || echo 'INTEL') BINARIES TO CREATE UNIVERSAL BINARY WHEN BUILDING ON $($is_apple_silicon && echo 'INTEL' || echo 'APPLE SILICON')"
echo "${script_title}"

temp_folder_path="${TMPDIR}/qa-helper-$($is_apple_silicon && echo 'apple-silicon' || echo 'intel')-mac-app-java-binaries"

rm -rf "${temp_folder_path}"
mkdir -p "${temp_folder_path}"
open "${temp_folder_path}"

jdk_download_url="$(curl -m 5 -sfw '%{redirect_url}' "https://api.adoptium.net/v3/binary/latest/21/ga/mac/$($is_apple_silicon && echo 'aarch64' || echo 'x64')/jdk/hotspot/normal/eclipse")"
jdk_archive_filename="${jdk_download_url##*/}"
echo -e "\nDOWNLOADING \"${jdk_download_url}\"..."
curl --connect-timeout 5 --progress-bar -fL "${jdk_download_url}" -o "${temp_folder_path}/${jdk_archive_filename}" || exit 1

echo -e "\nUNARCHIVING \"${jdk_archive_filename}\"..."
tar -xzf "${temp_folder_path}/${jdk_archive_filename}" -C "${temp_folder_path}" || exit 1
rm -f "${temp_folder_path}/${jdk_archive_filename}"

qa_helper_jar_download_url='https://apps.freegeek.org/qa-helper/download/QAHelper-jar.zip'
echo -e "\nDOWNLOADING \"${qa_helper_jar_download_url}\"..."
curl --connect-timeout 5 --progress-bar -fL "${qa_helper_jar_download_url}" -o "${temp_folder_path}/QAHelper-jar.zip" || exit 1

echo -e '\nUNARCHIVING "QAHelper-jar.zip"...'
ditto -xk --noqtn "${temp_folder_path}/QAHelper-jar.zip" "${temp_folder_path}/QA Helper JAR" || exit 1
rm -f "${temp_folder_path}/QAHelper-jar.zip"

# Suppress ShellCheck suggestion to not use "ls | grep" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
# shellcheck disable=SC2010
jdk_path="${temp_folder_path}/$(ls -t "${temp_folder_path}" | grep -xm 1 'jdk-.*')"

echo -e '\nCREATING APP...'
"${jdk_path}/Contents/Home/bin/jpackage" \
	--type 'app-image' \
	--verbose \
	--name 'QA Helper' \
	--input "${temp_folder_path}/QA Helper JAR" \
	--main-jar 'QA_Helper.jar' \
	--runtime-image "${jdk_path}" \
	--dest "${temp_folder_path}" || exit 1

# Move "runtime" to "Frameworks/Java.runtime" to match actual QA Helper structure changes.
mkdir "${temp_folder_path}/QA Helper.app/Contents/Frameworks"
mv "${temp_folder_path}/QA Helper.app/Contents/runtime" "${temp_folder_path}/QA Helper.app/Contents/Frameworks/Java.runtime"

echo -e '\nREMOVING ALL EXCEPT BINARIES FROM APP...'

while IFS='' read -rd '' this_app_file_path; do
	if [[ "$(file "${this_app_file_path}")" != *'Mach-O 64-bit'* ]]; then
		echo "Deleting Non-Binary File (and Empty Parent Folders) From App: ${this_app_file_path/${temp_folder_path}\//}"
		if rm -f "${this_app_file_path}"; then
			rmdir -p "${this_app_file_path%/*}" 2> /dev/null
		else
			exit 1
		fi
	fi
done < <(find "${temp_folder_path}/QA Helper.app" -type f -print0)

touch "${temp_folder_path}/QA Helper.app"

rm -rf "${temp_folder_path}/QA Helper JAR"
rm -rf "${jdk_path}"

open -R "${temp_folder_path}/QA Helper.app"

echo -e "\nDONE ${script_title}\n"
