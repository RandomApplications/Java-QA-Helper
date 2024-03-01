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

PATH='/usr/bin:/bin:/usr/sbin:/sbin:/usr/libexec' # Add "/usr/libexec" to PATH for easy access to PlistBuddy. ("export" is not required since PATH is already exported in the environment, therefore modifying it modifies the already exported variable.)
TMPDIR="$([[ -d "${TMPDIR}" && -w "${TMPDIR}" ]] && echo "${TMPDIR%/}" || echo '/private/tmp')" # Make sure "TMPDIR" is always set and that it DOES NOT have a trailing slash for consistency regardless of the current environment.

if [[ "$(uname)" == 'Darwin' ]]; then # Can only compile macOS app when running on macOS
	PROJECT_PATH="$(cd "${BASH_SOURCE[0]%/*}/.." &> /dev/null && pwd -P)"
	readonly PROJECT_PATH

	readonly fgMIB_USERAPPS_PATH="${PROJECT_PATH}/../../MacLand/fgMIB Resources/Prepare OS Package/Package Resources/User/fg-demo/Apps/darwin-all-versions"

	if [[ ! -e "${PROJECT_PATH}/dist/JAR for macOS/QA_Helper.jar" ]]; then
		>&2 echo -e '\n\n!!! JAR for macOS NOT FOUND !!!'
		afplay '/System/Library/Sounds/Basso.aiff'
		exit 1
	fi

	declare -a jdk_base_paths=( '/Library/Java/JavaVirtualMachines' "${PROJECT_PATH}/../Java for El Captian" ) # Can build against multiple versions of Java (to be able to build old versions for El Capitan with JDK 16.0.2 which is the last to support El Capitan)

	for this_jdk_base_path in "${jdk_base_paths[@]}"; do
		# Suppress ShellCheck suggestion to use "find" instead of "ls" since we need "ls -t" to sort by modification date, and this path shouldn't contain non-alphanumeric characters.
		# shellcheck disable=SC2012
		java_version="$(ls -t "${this_jdk_base_path}" | awk -F '-|[.]jdk' '/.jdk$/ { print $2; exit }')"

		if [[ -z "${java_version}" ]]; then
			>&2 echo -e "\n\n!!! JAVA NOT FOUND IN '${this_jdk_base_path}' !!!"
			afplay '/System/Library/Sounds/Basso.aiff'
			exit 2
		fi

		qa_helper_mac_zip_name=''
		if (( ${java_version%%.*} >= 17 )); then
			echo -e '\n\nBUILDING MAC APP FOR SIERRA AND NEWER (UNIVERSAL BINARY)'
			qa_helper_mac_zip_name='QAHelper-mac-universal.zip'
		else
			echo -e '\n\nBUILDING MAC APP FOR EL CAPITAN (INTEL ONLY)'
			qa_helper_mac_zip_name='QAHelper-mac-ElCapitan.zip' # Build with Java 16.0.2 to create an El Capitan version (that will be Intel only).
		fi

		qa_helper_app_id='org.freegeek.QA-Helper'

		osascript -e "tell application id \"${qa_helper_app_id}\" to quit" &> /dev/null

		tccutil reset All "${qa_helper_app_id}" &> /dev/null # Clear all TCC permissions so that we're always re-prompted when testing to be sure that works properly.

		rm -rf "${PROJECT_PATH}/dist/QA Helper.app"
		rm -rf "${PROJECT_PATH}/dist/jlink-jre"

		echo -e "\nBuilding JRE Version ${java_version}..."

		jdk_path="${this_jdk_base_path}/jdk-${java_version}.jdk"

		# jdeps="$("${jdk_path}/Contents/Home/bin/jdeps" --module-path "${PROJECT_PATH}/libs" --multi-release "${java_version%%.*}" --list-deps "${PROJECT_PATH}/dist/JAR for macOS/QA_Helper.jar" | tr -s '[:space:]' ',' | sed -E 's/^,|,$//g')"
		# echo "JDEPS: ${jdeps}" # Should be "java.base,java.datatransfer,java.desktop,java.logging"
		# java.datatransfer is actually included within java.desktop (along with java.prefs and java.xml) so it doesn't actually need to be listed, but that's what jdeps returns.
		jdeps='java.base,java.desktop,java.logging'

		"${jdk_path}/Contents/Home/bin/jlink" \
			--add-modules "${jdeps}" \
			--strip-native-commands \
			--strip-debug \
			--no-man-pages \
			--no-header-files \
			--compress "$( (( ${java_version%%.*} >= 21 )) && echo 'zip-9' || echo '2' )" \
			--output "${PROJECT_PATH}/dist/jlink-jre"

		find "${PROJECT_PATH}/dist/jlink-jre" -name '.DS_Store' -type f -print -delete

		app_version="$(unzip -p "${PROJECT_PATH}/dist/JAR for macOS/QA_Helper.jar" '*/qa-helper-version.txt' | head -1)"
		app_version_for_jpackage=${app_version%-*} # jpackage version strings can consist of only numbers and up to two dots.

		echo -e "\nBuilding QA Helper Version ${app_version}..."

		"${jdk_path}/Contents/Home/bin/jpackage" \
			--type 'app-image' \
			--verbose \
			--name 'QA Helper' \
			--app-version "${app_version_for_jpackage}" \
			--mac-package-identifier "${qa_helper_app_id}" \
			--input "${PROJECT_PATH}/dist/JAR for macOS" \
			--resource-dir "${PROJECT_PATH}/macOS Build Resources" \
			--main-class 'GUI.QAHelper' \
			--main-jar 'QA_Helper.jar' \
			--runtime-image "${PROJECT_PATH}/dist/jlink-jre" \
			--java-options '-Dsun.java2d.metal=false' \
			--dest "${PROJECT_PATH}/dist"

			# NOTES:
			# NOT using "--mac-sign" since we will be manually creating a Univeral binary, which will need to be signed after the Intel and Apple Silicon binaries are merged, so there is no point just signing this single architecture build in advance.
			# Also, we will be setting our own minimal entitlements which will be less than the overzealous entitlements that "--mac-sign" would use (see comment during "codesign" below for more info about the entitlements).

			# NOT enabling Metal "--java-options '-Dsun.java2d.metal=true'" for Java 17 LTS since seems to cause an issue exiting full screen window for screen test (screen stays black after dispose).
			# EXPLICITLY SETTING "--java-options '-Dsun.java2d.metal=false'" for Java 19 AND NEWER (which enable Metal by default https://www.oracle.com/java/technologies/javase/19-relnote-issues.html#JDK-8284378) because it causes an issue exiting full screen window for screen test (screen stays black after dispose): 

		rm -rf "${PROJECT_PATH}/dist/jlink-jre"

		plutil -replace 'CFBundleShortVersionString' -string "${app_version}" "${PROJECT_PATH}/dist/QA Helper.app/Contents/Info.plist"

		# Move "QA Helper.app/Contents/runtime" folder to "QA Helper.app/Contents/Frameworks/Java.runtime" (jpackager and OpenJDK 11 used "PlugIns" folder),
		# so that Notarization doesn't fail with "The signature of the binary is invalid" error. See links below for references:
		# https://developer.apple.com/forums/thread/116831?answerId=361112022#361112022 & https://developer.apple.com/forums/thread/129703?answerId=410259022#410259022
		# https://developer.apple.com/library/archive/technotes/tn2206/_index.html#//apple_ref/doc/uid/DTS40007919-CH1-TNTAG201
		mkdir "${PROJECT_PATH}/dist/QA Helper.app/Contents/Frameworks"
		mv "${PROJECT_PATH}/dist/QA Helper.app/Contents/runtime" "${PROJECT_PATH}/dist/QA Helper.app/Contents/Frameworks/Java.runtime"
		sed -i '' $'2i\\\napp.runtime=$ROOTDIR/Contents/Frameworks/Java.runtime\n' "${PROJECT_PATH}/dist/QA Helper.app/Contents/app/QA Helper.cfg"

		# Move JAR from "QA Helper.app/Contents/app/QA_Helper.jar" to "QA Helper.app/Contents/Java/QA_Helper.jar" to match previous location used by jpackager and OpenJDK 11
		# This is necessary for old versions to be able to check downloaded JAR version when auto-updating.
		mkdir "${PROJECT_PATH}/dist/QA Helper.app/Contents/Java"
		mv "${PROJECT_PATH}/dist/QA Helper.app/Contents/app/QA_Helper.jar" "${PROJECT_PATH}/dist/QA Helper.app/Contents/Java/QA_Helper.jar"
		# Suppress ShellCheck warning about expressions not expanding in single quotes since it is intentional.
		# shellcheck disable=SC2016
		sed -i '' 's|$APPDIR/QA_Helper.jar|$ROOTDIR/Contents/Java/QA_Helper.jar|' "${PROJECT_PATH}/dist/QA Helper.app/Contents/app/QA Helper.cfg"

		should_notarize="$([[ "${app_version}" == *'-0' ]] && echo 'false' || echo 'true')" # DO NOT offer to Notarize for testing builds (which have versions ending in "-0").

		if [[ "${qa_helper_mac_zip_name}" == 'QAHelper-mac-universal.zip' ]]; then
			echo -e "\nMaking QA Helper Version ${app_version} Universal..."

			# Also update app LSMinimumSystemVersion to match JVMMinimumSystemVersion (which should be 10.12.0 for Java 17).
			jvm_minimum_system_version="$(PlistBuddy -c 'Print :JavaVM:JVMMinimumSystemVersion' "${jdk_path}/Contents/Info.plist" 2> /dev/null)"
			if [[ -n "${jvm_minimum_system_version}" ]]; then
				plutil -replace 'LSMinimumSystemVersion' -string "${jvm_minimum_system_version}" "${PROJECT_PATH}/dist/QA Helper.app/Contents/Info.plist"
			fi

			alternate_app_binaries_for_universal_binary_name="Java ${java_version} $([[ "$(sysctl -in hw.optional.arm64)" == '1' ]] && echo 'Intel' || echo 'Apple Silicon') App Binaries"
			alternate_app_binaries_for_universal_binary="${PROJECT_PATH}/macOS Build Resources/Universal Binary Parts/${alternate_app_binaries_for_universal_binary_name}/QA Helper.app" # Get alternate arch folder from what is running.
			if [[ -d "${alternate_app_binaries_for_universal_binary}" ]]; then
				# If building on an Intel Mac, the files within the "alternate_app_binaries_for_universal_binary" folder must be created by running
				# the "Create Alternate App Binaries for Mac Univeral Binary.sh" script (within the "Build Scripts" folder) on an Apple Silicon Mac
				# in advance and then copying the resulting files into the "Java [VERSION] Apple Silicon App Binaries" folder.
				# If building on an Apple Silicon Mac, the "Create Alternate App Binaries for Mac Univeral Binary.sh" script (within the "Build Scripts" folder)
				# can be run in Rosetta on to obtain the necessary Intel files which can be copied into the "Java [VERSION] Intel App Binaries" folder.
				# This is necessary to be able to manually create a Universal app since that capability is not built-in to "jpackage".

				did_delete_non_binary_files=false

				while IFS='' read -rd '' this_app_file_path; do
					this_alternate_app_binaries_for_universal_binary_file_path="${alternate_app_binaries_for_universal_binary}${this_app_file_path#*/dist/QA Helper.app}"
					this_alternate_app_binaries_for_universal_binary_file_arch="$(lipo -archs "${this_alternate_app_binaries_for_universal_binary_file_path}" 2> /dev/null)"

					if [[ -n "${this_alternate_app_binaries_for_universal_binary_file_arch}" ]]; then
						this_app_file_arch="$(lipo -archs "${this_app_file_path}" 2> /dev/null)"

						if [[ " ${this_app_file_arch} " != *" ${this_alternate_app_binaries_for_universal_binary_file_arch} "* ]]; then
							echo "  Adding \"${this_alternate_app_binaries_for_universal_binary_file_arch}\" to \"${this_app_file_arch}\" Binary File to Make Universal: ${this_app_file_path#*/dist/}"
							if ! lipo -create "${this_app_file_path}" "${this_alternate_app_binaries_for_universal_binary_file_path}" -output "${this_app_file_path}"; then
								>&2 echo -e '\n!!! UNIVERSAL BINARY ERROR !!!'
								afplay '/System/Library/Sounds/Basso.aiff'
								exit 3
							fi
						else
							echo "  Binary File \"${this_app_file_arch}\" Already Contains \"${this_alternate_app_binaries_for_universal_binary_file_arch}\": ${this_app_file_path}"
						fi

						# CONFIRM THAT BINARY FILE ENDED UP UNIVERSAL

						this_app_file_arch_info="$(lipo -info "${this_app_file_path}" 2> /dev/null)"

						echo "    Universal Architectures for ${this_app_file_arch_info#*/dist/}"

						if [[ "${this_app_file_arch_info}" == 'Non-fat file:'* ]]; then
							>&2 echo -e '\n!!! UNIVERSAL BINARY ERROR (NON-FAT) !!!'
							afplay '/System/Library/Sounds/Basso.aiff'
							exit 4
						else
							while IFS='' read -rd ' ' this_universal_arch; do
								if [[ -n "${this_universal_arch}" && "${this_app_file_arch_info} " != *" ${this_universal_arch} "* ]]; then
									>&2 echo -e "\n!!! UNIVERSAL BINARY ERROR (MISSING ${this_universal_arch}) !!!"
									afplay '/System/Library/Sounds/Basso.aiff'
									exit 5
								fi
							done <<< "${this_alternate_app_binaries_for_universal_binary_file_arch} ${this_app_file_arch} " # There *could* possibly be other spaces in these arch vars. NOTE: MUST include a trailing/terminating space so that the last last value doesn't get lost by the "while read" loop.
						fi
					elif [[ -e "${this_alternate_app_binaries_for_universal_binary_file_path}" ]]; then
						echo "    Deleting Non-Binary File (and Empty Parent Folders) From Alternate App Binaries For Universal Binary: ${this_app_file_path}"
						if rm "${this_alternate_app_binaries_for_universal_binary_file_path}"; then
							rmdir -p "${this_alternate_app_binaries_for_universal_binary_file_path%/*}" 2> /dev/null
						fi
						did_delete_non_binary_files=true
					fi
				done < <(find "${PROJECT_PATH}/dist/QA Helper.app" -type f -print0)

				if $did_delete_non_binary_files; then
					touch "${alternate_app_binaries_for_universal_binary}"
				fi
			else
				>&2 echo -e "\n!!! MISSING \"${alternate_app_binaries_for_universal_binary_name}\" TO CREATE UNIVERSAL BINARY !!!"
				afplay '/System/Library/Sounds/Basso.aiff'

				should_notarize=false
			fi
		fi

		rm -f "${PROJECT_PATH}/dist/${qa_helper_mac_zip_name}"

		find "${PROJECT_PATH}/dist/QA Helper.app" -name '.DS_Store' -type f -print -delete
		xattr -crs "${PROJECT_PATH}/dist/QA Helper.app" # "codesign" can fail if there are any xattr's (even though there should never be any).

		echo -e "\nCode Signing QA Helper Version ${app_version}..."

		# NOTE: The following code manually signs each executable and compiled code file (such as "dylib" files) within "Java.runtime".
		# "--deep" IS NOT being used since it is deprecated in macOS 13 Ventura (and it does not sign every executable and compiled code file within "QA Helper.app/Contents/Frameworks/Java.runtime/Contents/Home/lib/" anyways).
	
		jre_bundle_id="$(PlistBuddy -c 'Print :CFBundleIdentifier' "${PROJECT_PATH}/dist/QA Helper.app/Contents/Frameworks/Java.runtime/Contents/Info.plist" 2> /dev/null)"
		if [[ "${jre_bundle_id}" != *'QA-Helper'* ]]; then
			jre_bundle_id="${qa_helper_app_id}"
		fi

		while IFS='' read -rd '' this_java_lib_path; do
			if lipo -archs "${this_java_lib_path}" &> /dev/null; then  # "lipo -archs" is used to locate all compiled code since it will not all be set as executable, like the "dylib" files.
				echo "  Code Signing: ${this_java_lib_path#*/dist/}"
				codesign -fs 'Developer ID Application' -o runtime --prefix "${jre_bundle_id}." --strict "${this_java_lib_path}"
			fi
		done < <(find "${PROJECT_PATH}/dist/QA Helper.app/Contents/Frameworks/Java.runtime/Contents/Home/lib" -type f -print0)

		echo '  Code Signing: QA Helper.app/Contents/Frameworks/Java.runtime'
		codesign -fs 'Developer ID Application' -o runtime --strict "${PROJECT_PATH}/dist/QA Helper.app/Contents/Frameworks/Java.runtime"

		# Starting with FlatLat 3.3, there are native libraries within the JAR that must be signed for Notarization to work: https://github.com/JFormDesigner/FlatLaf/releases/tag/3.3 & https://github.com/JFormDesigner/FlatLaf/issues/800
		# If the FlatLaf native libraries are NOT signed, Notarization will fail with an error stating that "The binary is not signed with a valid Developer ID certificate." for "QAHelper-mac-universal-NOTARIZATION-SUBMISSION.zip/QA Helper.app/Contents/Java/QA_Helper.jar/com/formdev/flatlaf/natives/libflatlaf-macos-x86_64.dylib" (and also the "libflatlaf-macos-arm64.dylib" file).
		did_sign_jar_libs=false
		rm -rf "${TMPDIR}/QA_Helper-JAR"
		mkdir -p "${TMPDIR}/QA_Helper-JAR"
		(cd "${TMPDIR}/QA_Helper-JAR" && "${jdk_path}/Contents/Home/bin/jar" -xf "${PROJECT_PATH}/dist/QA Helper.app/Contents/Java/QA_Helper.jar")
		while IFS='' read -rd '' this_jar_lib_path; do
			if lipo -archs "${this_jar_lib_path}" &> /dev/null; then  # "lipo -archs" is used to locate all compiled code since it will not all be set as executable, like the "dylib" files.
				echo "  Code Signing: QA Helper.app/Contents/Java/QA_Helper.jar/${this_jar_lib_path#*/QA_Helper-JAR/}"
				codesign -fs 'Developer ID Application' -o runtime --prefix "${qa_helper_app_id}." --strict "${this_jar_lib_path}"
				did_sign_jar_libs=true
			fi
		done < <(find "${TMPDIR}/QA_Helper-JAR" -type f -print0)
		if $did_sign_jar_libs; then
			(cd "${TMPDIR}/QA_Helper-JAR" && "${jdk_path}/Contents/Home/bin/jar" -cf "${PROJECT_PATH}/dist/QA Helper.app/Contents/Java/QA_Helper.jar" .)
		fi
		rm -rf "${TMPDIR}/QA_Helper-JAR"

		# Hardened Runtime Exception Entitlements: https://developer.apple.com/documentation/security/hardened_runtime
		# NOTE: Through testing, I found that apparently ONLY the parent app needs the Hardened Runtime Exception Entitlements, NOT anything within the "Java.runtime".

		codesign_entitlements_plist_path="${TMPDIR}/QAHelper_codesign_entitlements.plist"
		rm -rf "${codesign_entitlements_plist_path}"

		# Java Default Entitlements:
		# {
		# "com.apple.security.cs.allow-dyld-environment-variables" => 1
		# "com.apple.security.cs.allow-jit" => 1
		# "com.apple.security.cs.allow-unsigned-executable-memory" => 1
		# "com.apple.security.cs.debugger" => 1
		# "com.apple.security.cs.disable-library-validation" => 1
		# "com.apple.security.device.audio-input" => 1
		# }
		# Originally recommended notarization Args:
		# https://bugs.openjdk.java.net/browse/JDK-8223671?focusedCommentId=14282346&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-14282346
		# https://www.joelotter.com/2020/08/14/macos-java-notarization.html
		# https://blog.adoptopenjdk.net/2020/05/a-simple-guide-to-notarizing-your-java-application/

		# BUT, I'm NOT using all the entitlements that are specified above because I found that they aren't needed (through trial-and-error) for this app and the documentation from Apple states many of these are overzealous and should only be used if needed.

		# NOTE: It appears Java 16 for the El Capitan version needs "com.apple.security.cs.allow-unsigned-executable-memory" BUT at least Java 17 & 19 (and presumably newer) for the Univeral version only needs "com.apple.security.cs.allow-jit" (never tested changing this entitlement for Java 18).
		# Through testing, QA Helper with Java 17 & 19 and only "com.apple.security.cs.allow-jit" launched and worked fine. If QA Helper with Java 16 only has "com.apple.security.cs.allow-jit" it will fail to launch, so "com.apple.security.cs.allow-unsigned-executable-memory" is definitely still needed for the El Capitan builds with Java 16.
		PlistBuddy \
			-c "Add :$([[ "${qa_helper_mac_zip_name}" == 'QAHelper-mac-universal.zip' ]] && echo 'com.apple.security.cs.allow-jit' || echo 'com.apple.security.cs.allow-unsigned-executable-memory') bool true" \
			-c 'Add :com.apple.security.device.audio-input bool true' \
			"${codesign_entitlements_plist_path}"
		# NOTE: Apparently DO NOT need "com.apple.security.automation.apple-events" since QuickTime automation is done via "osascript" rather than directly.

		echo '  Code Signing: QA Helper.app'
		codesign -fs 'Developer ID Application' -o runtime --entitlements "${codesign_entitlements_plist_path}" --strict "${PROJECT_PATH}/dist/QA Helper.app"

		rm -f "${codesign_entitlements_plist_path}"

		if $should_notarize && osascript -e 'activate' -e "display alert \"Notarize QA Helper version ${app_version} $([[ "${qa_helper_mac_zip_name}" == 'QAHelper-mac-universal.zip' ]] && echo '(Universal Binary)' || echo 'for El Capitan')?\" buttons {\"No\", \"Yes\"} cancel button 1 default button 2" &> /dev/null; then
			# Setting up "notarytool": https://scriptingosx.com/2021/07/notarize-a-command-line-tool-with-notarytool/ & https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution/customizing_the_notarization_workflow

			qa_helper_mac_zip_path_for_notarization="${PROJECT_PATH}/dist/${qa_helper_mac_zip_name/.zip/-NOTARIZATION-SUBMISSION.zip}"
			rm -rf "${qa_helper_mac_zip_path_for_notarization}"

			echo -e "\nZipping QA Helper Version ${app_version} for Notarization..."
			ditto -ck --keepParent "${PROJECT_PATH}/dist/QA Helper.app" "${qa_helper_mac_zip_path_for_notarization}"

			notarization_submission_log_path="${TMPDIR}/QAHelper_notarization_submission.log"
			rm -rf "${notarization_submission_log_path}"

			echo -e "\nNotarizing QA Helper Version ${app_version}..."
			xcrun notarytool submit "${qa_helper_mac_zip_path_for_notarization}" --keychain-profile 'notarytool App Specific Password' --wait | tee "${notarization_submission_log_path}" # Show live log since it may take a moment AND save to file to extract submission ID from to be able to load full notarization log.
			notarytool_exit_code="$?"
			rm -f "${qa_helper_mac_zip_path_for_notarization}"

			notarization_submission_id="$(awk '($1 == "id:") { print $NF; exit }' "${notarization_submission_log_path}")"
			rm -f "${notarization_submission_log_path}"

			echo 'Notarization Log:'
			xcrun notarytool log "${notarization_submission_id}" --keychain-profile 'notarytool App Specific Password' # Always load and show full notarization log regardless of success or failure (since documentation states there could be warnings).

			if (( notarytool_exit_code != 0 )); then
				>&2 echo -e "\nNOTARIZATION ERROR OCCURRED: EXIT CODE ${notarytool_exit_code} (ALSO SEE ERROR MESSAGES ABOVE)"
				exit 6
			fi

			echo -e "\nStapling Notarization Ticket to QA Helper Version ${app_version}..."
			xcrun stapler staple "${PROJECT_PATH}/dist/QA Helper.app"
			stapler_exit_code="$?"

			if (( stapler_exit_code != 0 )); then
				>&2 echo -e "\nSTAPLING ERROR OCCURRED: EXIT CODE ${stapler_exit_code} (ALSO SEE ERROR MESSAGES ABOVE)"
				exit 7
			fi

			echo -e "\nAssessing Notarized QA Helper Version ${app_version}..."
			spctl_assess_output="$(spctl -avv "${PROJECT_PATH}/dist/QA Helper.app" 2>&1)"
			spctl_assess_exit_code="$?"

			echo "${spctl_assess_output}"

			if ! codesign -vv --deep --strict -R '=notarized' --check-notarization "${PROJECT_PATH}/dist/QA Helper.app" || (( spctl_assess_exit_code != 0 )) || [[ "${spctl_assess_output}" != *$'\nsource=Notarized Developer ID\n'* ]]; then # Double-check that the app got assessed to be signed with "Notarized Developer ID".
				# Verifying notarization with "codesign": https://developer.apple.com/forums/thread/128683?answerId=404727022#404727022 & https://developer.apple.com/forums/thread/130560
				# Information about using "--deep" and "--strict" options during "codesign" verification:
					# https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution/resolving_common_notarization_issues#3087735
					# https://developer.apple.com/library/archive/technotes/tn2206/_index.html#//apple_ref/doc/uid/DTS40007919-CH1-TNTAG211
					# https://developer.apple.com/library/archive/technotes/tn2206/_index.html#//apple_ref/doc/uid/DTS40007919-CH1-TNTAG404
				# The "--deep" option is DEPRECATED in macOS 13 Ventura for SIGNING but I don't think it's deprecated for VERIFYING since verification is where it was always really intended to be used (as explained in the note in the last link in the list above).

				>&2 echo -e "\nASSESSMENT ERROR OCCURRED: EXIT CODE ${spctl_assess_exit_code} (ALSO SEE ERROR MESSAGES ABOVE)"
				exit 8
			fi

			echo -e "\nZipping Notarized QA Helper Version ${app_version}..."
			ditto -ck --keepParent --sequesterRsrc --zlibCompressionLevel 9 "${PROJECT_PATH}/dist/QA Helper.app" "${PROJECT_PATH}/dist/${qa_helper_mac_zip_name}"

			if [[ "${qa_helper_mac_zip_name}" != 'QAHelper-mac-ElCapitan.zip' && -d "${fgMIB_USERAPPS_PATH}" ]]; then
				rm -f "${fgMIB_USERAPPS_PATH}/${qa_helper_mac_zip_name}"
				ditto "${PROJECT_PATH}/dist/${qa_helper_mac_zip_name}" "${fgMIB_USERAPPS_PATH}"
			fi

			echo -e "\nSuccessfully Notarized QA Helper Version ${app_version}!"

			osascript -e 'activate' -e "display alert \"Successfully Notarized & Zipped\nQA Helper Version ${app_version}!\"" &> /dev/null
		fi

		open -na "${PROJECT_PATH}/dist/QA Helper.app"

		if [[ "${app_version}" == *'-0' ]]; then # DO NOT offer to build for El Captian for testing builds (which have versions ending in "-0").
			break
		elif [[ "${qa_helper_mac_zip_name}" == 'QAHelper-mac-universal.zip' ]] && osascript -e 'activate' -e "display alert \"Also build QA Helper version ${app_version} for El Capitan?\" buttons {\"Yes\", \"No\"} cancel button 1 default button 2" &> /dev/null; then
			break
		fi
	done

	rm -rf "${PROJECT_PATH}/dist/JAR for macOS"
fi
