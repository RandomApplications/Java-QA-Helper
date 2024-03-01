#!/bin/bash
# shellcheck enable=add-default-case,avoid-nullary-conditions,check-unassigned-uppercase,deprecate-which,quote-safe-variables,require-double-brackets

#
# Created by Pico Mitchell (of Free Geek) on 03/22/19
# For QA Helper
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


while true; do
    clear

    read -rp '
Are you sure you want undo "oem-config-prepare" so that this system can boot back into the "OEM" user account?

If you choose to proceed, you will be prompted to enter this computers admin password.

This computer will be shut down after the process has completed successfully.

To proceed, type "yes" and press RETURN. To cancel, type anything else and press RETURN: ' confirm_return_value
    
    if [[ -n "${confirm_return_value}" ]]; then
        confirm_return_value="${confirm_return_value,,}"
        break
    fi
done

if [[ "${confirm_return_value}" == 'yes' ]]; then
    clear

    echo '
UNDOING OEM-CONFIG-PREPARE
'

    # Undo everything done in the oem-config-prepare source: https://github.com/linuxmint/ubiquity/blob/master/bin/oem-config-prepare
    sudo systemctl disable oem-config.service
    sudo systemctl disable oem-config.target
    sudo systemctl set-default graphical.target

    sudo rm -rf '/lib/systemd/system/oem-config.service' '/lib/systemd/system/oem-config.target'

    sudo apt purge -y oem-config-gtk # Do not autoremove, we just want to purge so that the next command will recreate the oem-config-prepare-gtk.desktop file in the start menu.
    sudo apt install --no-install-recommends -y oem-config-gtk # The internet is not required for this re-install.

    # Add line to QA Helper log that "oem-config-prepare" was undone.
    if [[ ! -d '/usr/local/share/build-info' ]]; then
        mkdir '/usr/local/share/build-info' # This folder (and the "qa-helper-log.txt" file) may or may not already exist depending on whether or not anything else was previously verified in QA Helper.
    fi
    echo "Undo: oem-config-prepare - $(date '+%m/%d/%Y %T')" >> '/usr/local/share/build-info/qa-helper-log.txt'

    echo '
FINSIHED UNDOING OEM-CONFIG-PREPARE

THIS COMPUTER WILL NOW SHUT DOWN
'

    sleep 2

    shutdown now
else
    clear

    echo '
CANCELED UNDO-OEM-CONFIG-PREPARE : You did not type "yes" so "oem-config-prepare" will not be undone.
'
fi
