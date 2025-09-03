#!/bin/bash
# shellcheck enable=add-default-case,avoid-nullary-conditions,check-unassigned-uppercase,deprecate-which,quote-safe-variables,require-double-brackets

#
# Created by Pico Mitchell (of Free Geek) on 02/22/19
# For QA Helper
# Last Updated: 7/21/25
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


DOWNLOAD_URL='https://apps.freegeek.org/qa-helper/download'
TEST_MODE=false
FORCE_UPDATE=false
UNINSTALL=false
REINSTALL=false


if [[ -z "${MODE}" ]]; then # MODE can be inherited from calling script. But if it doesn't exist, check for first arg and use that instead.
    MODE="$1"
fi
MODE="$(echo "${MODE}" | tr '[:upper:]' '[:lower:]')" # Can't use string substition here in case it runs on bash 3 on macOS.
readonly MODE

echo 'RUNNING INSTALL QA HELPER'


if [[ "${MODE}" == 'test' || "${MODE}" == 'testing' ]]; then
    FORCE_UPDATE=true
    if ping 'tools.freegeek.org' -W 2 -c 1 &> /dev/null; then
        DOWNLOAD_URL='http://tools.freegeek.org/qa-helper/download'
        TEST_MODE=true
        echo 'MODE SET: Install Latest TEST Version'
    else
        echo 'TEST MODE NOT SET - Local Free Geek Network Required - SETTING UPDATE MODE INSTEAD'
    fi
elif [[ "${MODE}" == 'update' ]]; then
    FORCE_UPDATE=true
    echo 'MODE SET: Update to Latest Live Version'
elif [[ "${MODE}" == 'uninstall' ]]; then
    UNINSTALL=true
    echo 'MODE SET: Uninstall'
elif [[ "${MODE}" == 'reinstall' ]]; then
    REINSTALL=true
    echo 'MODE SET: Re-Install'
fi


readonly DOWNLOAD_URL TEST_MODE FORCE_UPDATE UNINSTALL REINSTALL


if ! $UNINSTALL && ! ping 'apps.freegeek.org' -W 10 -c 1 &> /dev/null; then
    echo -e '\n\nFAILED TO INSTALL QA HELPER: INTERNET IS REQUIRED\n'
elif [[ "$(uname)" == 'Darwin' ]]; then # Installer for macOS (not actually used for macOS deployment, was just easy to add for convenience)
    darwin_major_version="$(uname -r | cut -d '.' -f 1)" # 15 = 10.11, 16 = 10.12, 17 = 10.13, 18 = 10.14, 19 = 10.15, 20 = 11.0, 21 = 12.0, 22 = 13.0, 23 = 14.0, etc.
    
    TMPDIR="$([[ -d "${TMPDIR}" && -w "${TMPDIR}" ]] && echo "${TMPDIR%/}/" || echo '/private/tmp/')" # Make sure "TMPDIR" is always set and that it always has a trailing slash for consistency regardless of the current environment.
    
    qa_helper_mac_zip_name='QAHelper-mac-universal.zip'
    if (( darwin_major_version <= 15 )); then # Must install older version on OS X 10.11 El Capitan and older because newer versions are built with Java 17 and newer which only support macOS 10.12 Sierra and newer (and can be made a Universal Binary).
        qa_helper_mac_zip_name='QAHelper-mac-ElCapitan.zip'
    fi
    
    install_dir="${HOME}/Applications"

    if $UNINSTALL || $REINSTALL; then
        echo -e '\n\nUNINSTALLING QA HELPER...\n'

        osascript -e 'tell application id "org.freegeek.QA-Helper" to quit' &> /dev/null

        rm -f "${TMPDIR}${qa_helper_mac_zip_name}"
        rm -rf "${install_dir}/QA Helper.app"
        rm -f "${HOME}/Desktop/QA Helper.app"

        echo -e 'FINISHED UNINSTALLING QA HELPER'
    fi

    if ! $UNINSTALL; then
        if $FORCE_UPDATE || [[ ! -e "${install_dir}/QA Helper.app" ]]; then
            echo -e '\n\nINSTALLING QA HELPER...\n'

            osascript -e 'tell application id "org.freegeek.QA-Helper" to quit' &> /dev/null

            for (( app_download_attempt = 0; app_download_attempt < 5; app_download_attempt ++ )); do
                rm -f "${TMPDIR}${qa_helper_mac_zip_name}"
                echo 'DOWNLOADING QA HELPER:'
                curl --connect-timeout 5 --progress-bar -fL "${DOWNLOAD_URL}/${qa_helper_mac_zip_name}" -o "${TMPDIR}${qa_helper_mac_zip_name}"

                if [[ -e "${TMPDIR}${qa_helper_mac_zip_name}" ]]; then
                    if [[ ! -d "${install_dir}" ]]; then
                        mkdir -p "${install_dir}"
                    fi

                    echo -e '\nUNARCHIVING QA HELPER AND MOVING TO INSTALL LOCATION:'
                    rm -rf "${install_dir}/QA Helper.app"
                    ditto -xk --noqtn "${TMPDIR}${qa_helper_mac_zip_name}" "${install_dir}"
                    rm -f "${TMPDIR}${qa_helper_mac_zip_name}"

                    if [[ -e "${install_dir}/QA Helper.app" ]]; then
                        touch "${install_dir}/QA Helper.app"
                        break
                    fi
                fi
            done

            if [[ -e "${install_dir}/QA Helper.app" ]]; then
                rm -rf "${HOME}/Desktop/QA Helper.app"
                ln -s "${install_dir}/QA Helper.app" "${HOME}/Desktop/QA Helper.app"

                echo -e '\n\nQA HELPER IS INSTALLED: YOU CAN LAUNCH QA HELPER FROM THE DESKTOP'
            else
                echo -e '\n\nFAILED TO INSTALL QA HELPER: QA HELPER DOWNLOAD FAILED'
            fi
        else
            echo -e '\n\nQA HELPER IS ALREADY INSTALLED'
        fi
    fi
else # Installer for Linux
    install_home="${HOME}"
    install_user="$(id -un)"

    if [[ "${install_user}" == 'root' ]]; then # In case this is being run from our pre-installation environment or as sudo for some other reason
        if [[ -d '/home/oem' ]]; then
            install_home='/home/oem' # Always use oem's home if it exists
            install_user='oem'
        else
            possible_user_for_install="$(who | awk '{ print $1; exit }')" # Get actual user if running as sudo
            if [[ -n "${possible_user_for_install}" && "${possible_user_for_install}" != 'root' && -d "/home/${possible_user_for_install}" ]]; then
                install_home="/home/${possible_user_for_install}"
                install_user="${possible_user_for_install}"
            else
                possible_user_for_install_home="$(ls -t '/home' | head -n 1)" # Get first home folder if still only got root

                if [[ -n "$possible_user_for_install_home" ]]; then
                    install_home="/home/${possible_user_for_install_home}"
                    install_user="${possible_user_for_install_home}"
                fi
            fi
        fi
    fi


    install_dir="${install_home}/.local/qa-helper"


    if $UNINSTALL || $REINSTALL; then
        echo -e '\n\nUNINSTALLING QA HELPER...\n'

        if pgrep -f 'QA_Helper.jar' &> /dev/null; then
            wmctrl -F -c 'QA Helper  —  Loading'
            wmctrl -F -c 'QA Helper'
        fi

        rm -rf "${install_dir}" || sudo rm -rf "${install_dir}"

        rm -f "${install_home}/.config/autostart/qa-helper.desktop" || sudo rm -f "${install_home}/.config/autostart/qa-helper.desktop"
        rm -f "${install_home}/Desktop/qa-helper.desktop" || sudo rm -f "${install_home}/Desktop/qa-helper.desktop"
        rm -f "${install_home}/.local/share/applications/qa-helper.desktop" || sudo rm -f "${install_home}/.local/share/applications/qa-helper.desktop"
        rm -f "${install_home}/.local/bin/qa-helper" || sudo rm -f "${install_home}/.local/bin/qa-helper"

        rm -f '/tmp/qa-helper_java-jre.tar.gz' || sudo rm -rf '/tmp/qa-helper_java-jre.tar.gz'
        rm -f '/tmp/QAHelper-jar.zip' || sudo rm -f '/tmp/QAHelper-jar.zip'
        rm -f '/tmp/launch-qa-helper' || sudo rm -f '/tmp/launch-qa-helper'
        rm -f '/tmp/auto-scripts.zip' || sudo rm -f '/tmp/auto-scripts.zip'
        rm -f '/tmp/qa-helper.desktop' || sudo rm -f '/tmp/qa-helper.desktop'

        echo -e 'FINISHED UNINSTALLING QA HELPER'
    fi


    if ! $UNINSTALL; then
        if [[ ! -d "${install_dir}" ]]; then
            sudo -u "${install_user}" mkdir -p "${install_dir}" || mkdir -p "${install_dir}"
        fi


        if $FORCE_UPDATE || [[ ! -e "${install_dir}/QA_Helper.jar" ]]; then
            echo -e '\n\nINSTALLING QA HELPER JAVA APPLET...\n'

            if pgrep -f 'QA_Helper.jar' &> /dev/null; then
                wmctrl -F -c 'QA Helper  —  Loading'
                wmctrl -F -c 'QA Helper'
            fi

            for (( jar_download_attempt = 0; jar_download_attempt < 5; jar_download_attempt ++ )); do
                rm -f '/tmp/QAHelper-jar.zip' || sudo rm -f '/tmp/QAHelper-jar.zip'
                echo 'DOWNLOADING QA HELPER JAVA APPLET:'
                sudo -u "${install_user}" curl --connect-timeout 5 --progress-bar -fL "${DOWNLOAD_URL}/QAHelper-linux-jar.zip" -o '/tmp/QAHelper-jar.zip' || curl --connect-timeout 5 --progress-bar -fL "${DOWNLOAD_URL}/QAHelper-linux-jar.zip" -o '/tmp/QAHelper-jar.zip'

                if [[ -e '/tmp/QAHelper-jar.zip' ]]; then
                    echo -e '\nUNARCHIVING QA HELPER JAVA APPLET AND MOVING TO INSTALL LOCATION:'
                    rm -f "${install_dir}/QA_Helper.jar" || sudo rm -f "${install_dir}/QA_Helper.jar"
                    sudo -u "${install_user}" unzip -jo '/tmp/QAHelper-jar.zip' 'QA_Helper.jar' -d "${install_dir}" || unzip -jo '/tmp/QAHelper-jar.zip' 'QA_Helper.jar' -d "${install_dir}"
                    rm -f '/tmp/QAHelper-jar.zip' || sudo rm -f '/tmp/QAHelper-jar.zip'

                    if [[ -e "${install_dir}/QA_Helper.jar" ]]; then
                        break
                    fi
                fi
            done
        else
            echo -e '\n\nQA HELPER JAVA APPLET IS ALREADY INSTALLED\n'
        fi


        if [[ -e "${install_dir}/QA_Helper.jar" ]]; then
            if [[ -e "${install_dir}/java-jre/bin/java" ]]; then
                echo -e '\n\nJAVA IS ALREADY INSTALLED\n'
            else
                jdk_version='21.0.8+9'

                echo -e "\n\nINSTALLING JAVA ${jdk_version/_/+}...\n"

                # Check if on local Free Geek network for a faster Java download.
                jdk_download_url="http$(ping 'tools.freegeek.org' -W 2 -c 1 &> /dev/null && echo '://tools' || echo 's://apps').freegeek.org/qa-helper/download/resources/linux/jlink-jre-${jdk_version/+/_}_linux-x64.tar.gz"
                
                for java_download_attempt in {1..5}; do
                    rm -f '/tmp/qa-helper_java-jre.tar.gz' || sudo rm -rf '/tmp/qa-helper_java-jre.tar.gz'
                    echo 'DOWNLOADING JAVA:'

                    sudo -u "${install_user}" curl --connect-timeout 5 --progress-bar -fL "${jdk_download_url}" -o '/tmp/qa-helper_java-jre.tar.gz' || curl --connect-timeout 5 --progress-bar -fL "${jdk_download_url}" -o '/tmp/qa-helper_java-jre.tar.gz'
                    
                    if [[ -e '/tmp/qa-helper_java-jre.tar.gz' ]]; then
                        echo -e '\nUNARCHIVING JAVA AND MOVING TO INSTALL LOCATION'
                        rm -rf "${install_dir}/java-jre" || sudo rm -rf "${install_dir}/java-jre"
                        sudo -u "${install_user}" mkdir "${install_dir}/java-jre" || mkdir "${install_dir}/java-jre"
                        sudo -u "${install_user}" tar -xzf '/tmp/qa-helper_java-jre.tar.gz' -C "${install_dir}/java-jre" --strip-components '1' || tar -xzf '/tmp/qa-helper_java-jre.tar.gz' -C "${install_dir}/java-jre" --strip-components '1'
                        rm -f '/tmp/qa-helper_java-jre.tar.gz' || sudo rm -f '/tmp/qa-helper_java-jre.tar.gz'

                        if [[ -e "${install_dir}/java-jre/bin/java" ]]; then
                            break
                        fi
                    fi
                    
                    if (( "${java_download_attempt}" > 2 )); then # Download Temurin JRE from Adoptium if failed to download condensed custom JLink JRE multiple times (will do 2 download attempts at this location).
                        jdk_download_url='https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jre/hotspot/normal/eclipse'
                    elif (( "${java_download_attempt}" > 1 )); then # Make sure we've tried the apps.freegeek.org location at least once before falling back on Temurin JRE download from Adoptium.
                        jdk_download_url="https://apps.freegeek.org/qa-helper/download/resources/linux/jlink-jre-${jdk_version/+/_}_linux-x64.tar.gz"
                    fi
                done
            fi


            if [[ -e "${install_dir}/java-jre/bin/java" ]]; then
                # Only install qa-helper.config and auto-scripts for OEM user and desktop session is cinnamon or unknown (it will be unknown in pre-install environment).
                if { $TEST_MODE || [[ "${install_user}" == 'oem' ]]; } && { $FORCE_UPDATE || [[ ! -d "${install_dir}/auto-scripts" ]]; } && [[ -z "${DESKTOP_SESSION}" || "${DESKTOP_SESSION}" == 'cinnamon' ]]; then
                    echo -e '\n\nINSTALLING QA HELPER CONFIG FILE...\n'
                    
                    config_download_url="http$(ping 'tools.freegeek.org' -W 2 -c 1 &> /dev/null && echo '://tools' || echo 's://apps').freegeek.org/qa-helper/download/resources/linux/qa-helper.config"
                    
                    for config_download_attempt in {1..5}; do
                        rm -f "${install_dir}/qa-helper.config" || sudo rm -f "${install_dir}/qa-helper.config"
                        echo 'DOWNLOADING QA HELPER CONFIG FILE:'
                        sudo -u "${install_user}" curl --connect-timeout 5 --progress-bar -fL "${config_download_url}" -o "${install_dir}/qa-helper.config" || curl --connect-timeout 5 --progress-bar -fL "${config_download_url}" -o "${install_dir}/qa-helper.config"

                        if [[ -e "${install_dir}/qa-helper.config" && "$(file "${install_dir}/qa-helper.config")" == *'Zip archive data'* ]]; then
                            break
                        fi
                        
                        if (( "${config_download_attempt}" > 2 )); then
                            # Make sure we've tried the apps.freegeek.org location at least twice.
                            config_download_url='https://apps.freegeek.org/qa-helper/download/resources/linux/qa-helper.config'
                        fi
                    done


                    echo -e '\n\nINSTALLING AUTO-SCRIPTS...\n'

                    auto_scripts_download_url="http$(ping tools.freegeek.org -W 2 -c 1 &> /dev/null && echo '://tools' || echo 's://apps').freegeek.org/qa-helper/download/resources/linux/auto-scripts+$($TEST_MODE && echo 'test' || echo 'live').zip"
                    
                    for auto_scripts_download_attempt in {1..5}; do
                        echo 'DOWNLOADING AUTO-SCRIPTS:'
                        rm -f '/tmp/auto-scripts.zip' || sudo rm -f '/tmp/auto-scripts.zip'
                        sudo -u "${install_user}" curl --connect-timeout 5 --progress-bar -fL "${auto_scripts_download_url}" -o '/tmp/auto-scripts.zip' || curl --connect-timeout 5 --progress-bar -fL "${auto_scripts_download_url}" -o '/tmp/auto-scripts.zip'

                        if [[ -e '/tmp/auto-scripts.zip' ]]; then
                            echo -e '\nUNARCHIVING AUTO-SCRIPTS AND MOVING TO INSTALL LOCATION:'
                            rm -rf "${install_dir}/auto-scripts" || sudo rm -rf "${install_dir}/auto-scripts"
                            sudo -u "${install_user}" unzip -jo '/tmp/auto-scripts.zip' '*/*.sh' -x '__MACOSX*' '.*' '*/.*' -d "${install_dir}/auto-scripts" || unzip -jo '/tmp/auto-scripts.zip' '*/*.sh' -x '__MACOSX*' '.*' '*/.*' -d "${install_dir}/auto-scripts"
                            rm -f '/tmp/auto-scripts.zip' || sudo rm -f '/tmp/auto-scripts.zip'

                            if [[ -d "${install_dir}/auto-scripts" ]]; then
                                sudo -u "${install_user}" chmod +x "${install_dir}/auto-scripts/"* || chmod +x "${install_dir}/auto-scripts/"*

                                if $TEST_MODE; then
                                    rm -rf "${install_dir}/flags" || sudo rm -rf "${install_dir}/flags" # Delete flags so auto-scripts run again when testing
                                fi

                                break
                            fi
                        fi
                        
                        if (( "${auto_scripts_download_attempt}" > 2 )); then
                            # Make sure we've tried the apps.freegeek.org location at least twice.
                            auto_scripts_download_url="https://apps.freegeek.org/qa-helper/download/resources/linux/auto-scripts+$($TEST_MODE && echo 'test' || echo 'live').zip"
                        fi
                    done
                fi


                if $FORCE_UPDATE || [[ ! -e "${install_dir}/launch-qa-helper" || ! -e "${install_home}/Desktop/qa-helper.desktop" ]]; then
                    echo -e '\n\nINSTALLING QA HELPER LAUNCHERS...\n'

                    rm -f '/tmp/launch-qa-helper' || sudo rm -f '/tmp/launch-qa-helper'
                    sudo -u "${install_user}" touch '/tmp/launch-qa-helper' || touch '/tmp/launch-qa-helper'
                    launch_qa_helper_source="#!/bin/bash\n\nif [[ -d '${install_dir}' ]]; then\n\tif ! pgrep -f 'QA_Helper.jar' &> /dev/null; then\n\t\t'${install_dir}/java-jre/bin/java' -jar '${install_dir}/QA_Helper.jar'\n\telif [[ \"\$1\" != 'no-focus' ]]; then\n\t\twmctrl -a 'QA Helper'\n\tfi\nfi"
                    echo -e "${launch_qa_helper_source}" | sudo -u "${install_user}" tee '/tmp/launch-qa-helper' > /dev/null || echo -e "${launch_qa_helper_source}" > '/tmp/launch-qa-helper'

                    rm -f "${install_dir}/launch-qa-helper" || sudo rm -f "${install_dir}/launch-qa-helper"
                    sudo -u "${install_user}" mv -f '/tmp/launch-qa-helper' "${install_dir}/launch-qa-helper" || mv -f '/tmp/launch-qa-helper' "${install_dir}/launch-qa-helper"
                    sudo -u "${install_user}" chmod +x "${install_dir}/launch-qa-helper" || chmod +x "${install_dir}/launch-qa-helper"

                    if [[ ! -d "${install_home}/.local/bin" ]]; then
                        sudo -u "${install_user}" mkdir "${install_home}/.local/bin" || mkdir "${install_home}/.local/bin"
                    fi

                    rm -f "${install_home}/.local/bin/qa-helper" || sudo rm -f "${install_home}/.local/bin/qa-helper"
                    sudo -u "${install_user}" ln -s "${install_dir}/launch-qa-helper" "${install_home}/.local/bin/qa-helper" || ln -s "${install_dir}/launch-qa-helper" "${install_home}/.local/bin/qa-helper"

                    rm -f "${install_dir}/qa-helper-icon.png" || sudo rm -f "${install_dir}/qa-helper-icon.png"
                    rm -f "${install_dir}/qa-helper-icon.svg" || sudo rm -f "${install_dir}/qa-helper-icon.svg"
                    sudo -u "${install_user}" unzip -jo "${install_dir}/QA_Helper.jar" '*/qa-helper-icon.svg' -d "${install_dir}" 2> /dev/null || unzip -jo "${install_dir}/QA_Helper.jar" '*/qa-helper-icon.svg' -d "${install_dir}" 2> /dev/null
                    icon_file_path="${install_dir}/qa-helper-icon.svg"
                    
                    if [[ ! -f "${icon_file_path}" ]]; then
                        sudo -u "${install_user}" unzip -jo "${install_dir}/QA_Helper.jar" '*/qa-helper-icon.png' -d "${install_dir}" || unzip -jo "${install_dir}/QA_Helper.jar" '*/qa-helper-icon.png' -d "${install_dir}"
                        icon_file_path="${install_dir}/qa-helper-icon.png"
                    fi
                    
                    rm -f '/tmp/qa-helper.desktop' || sudo rm -f '/tmp/qa-helper.desktop'
                    sudo -u "${install_user}" touch '/tmp/qa-helper.desktop' || touch '/tmp/qa-helper.desktop'
                    qa_helper_desktop_source="[Desktop Entry]\nVersion=1.0\nName=QA Helper\nGenericName=QA Helper\nComment=Launch QA Helper (App Icon is \"Robot Face\" from Twemoji by Twitter licensed under CC-BY 4.0)\nExec=${install_dir}/launch-qa-helper\nIcon=${icon_file_path}\nTerminal=false\nType=Application\nCategories=Utility;Application;"
                    echo -e "${qa_helper_desktop_source}" | sudo -u "${install_user}" tee '/tmp/qa-helper.desktop' > /dev/null || echo -e "${qa_helper_desktop_source}" > '/tmp/qa-helper.desktop'

                    # Only setup autostart for OEM user.
                    if { $TEST_MODE || [[ "${install_user}" == 'oem' ]]; }; then
                        rm -f "${install_home}/.config/autostart/qa-helper.desktop" || sudo rm -f "${install_home}/.config/autostart/qa-helper.desktop"
                        sudo -u "${install_user}" desktop-file-install --dir "${install_home}/.config/autostart/" '/tmp/qa-helper.desktop' || desktop-file-install --dir "${install_home}/.config/autostart/" '/tmp/qa-helper.desktop'
                        echo 'X-GNOME-Autostart-Delay=10' | sudo -u "${install_user}" tee -a "${install_home}/.config/autostart/qa-helper.desktop" > /dev/null || echo 'X-GNOME-Autostart-Delay=10' >> "${install_home}/.config/autostart/qa-helper.desktop"
                        sudo -u "${install_user}" chmod +x "${install_home}/.config/autostart/qa-helper.desktop" || chmod +x "${install_home}/.config/autostart/qa-helper.desktop"
                    fi

                    rm -f "${install_home}/.local/share/applications/qa-helper.desktop" || sudo rm -f "${install_home}/.local/share/applications/qa-helper.desktop"
                    sudo -u "${install_user}" desktop-file-install --dir "${install_home}/.local/share/applications/" '/tmp/qa-helper.desktop' || desktop-file-install --dir "${install_home}/.local/share/applications/" '/tmp/qa-helper.desktop'
                    sudo -u "${install_user}" chmod +x "${install_home}/.local/share/applications/qa-helper.desktop" || chmod +x "${install_home}/.local/share/applications/qa-helper.desktop"

                    rm -f "${install_home}/Desktop/qa-helper.desktop" || sudo rm -f "${install_home}/Desktop/qa-helper.desktop"
                    sudo -u "${install_user}" desktop-file-install --delete-original --dir "${install_home}/Desktop/" '/tmp/qa-helper.desktop' || desktop-file-install --delete-original --dir "${install_home}/Desktop/" '/tmp/qa-helper.desktop'
                    sudo -u "${install_user}" chmod +x "${install_home}/Desktop/qa-helper.desktop" || chmod +x "${install_home}/Desktop/qa-helper.desktop"
                fi

                echo -e '\n\nQA HELPER IS INSTALLED: YOU CAN LAUNCH QA HELPER FROM THE APPS MENU OR DESKTOP'
            else
                echo -e '\n\nFAILED TO INSTALL QA HELPER: JAVA DOWNLOAD FAILED'
            fi
        else
            echo -e '\n\nFAILED TO INSTALL QA HELPER: QA HELPER JAVA APPLET DOWNLOAD FAILED'
        fi
    fi
fi
