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


read -rp '
Are you sure you want undo "oem-config-prepare" / "Prepare for Shipping to End User" so that this computer can boot back into the "OEM" user account?

This computer will be reboot after the process has completed successfully.

Press ENTER to continue, or type CONTROL+C to cancel.

'

echo -e 'UNDOING OEM-CONFIG-PREPARE...\n'

echo 'freegeek' | sudo -vS # Run "sudo -v" with no command to pre-cache the authorization for subsequent commands requiring "sudo" with the standard "freegeek" password. If there is a different password it will be prompted.

# Undo everything done in the oem-config-prepare source: https://github.com/linuxmint/ubiquity/blob/master/bin/oem-config-prepare
sudo systemctl set-default graphical.target || exit 1
sudo systemctl disable oem-config.target || exit 1
sudo systemctl disable oem-config.service || exit 1

sudo rm -rf '/lib/systemd/system/oem-config.target' '/lib/systemd/system/oem-config.service' || exit 1

echo 'autologin-user=oem' | sudo tee -a '/etc/lightdm/lightdm.conf' > /dev/null # Re-enable auto-login.

# Add line to QA Helper log that "oem-config-prepare" was undone.
if [[ ! -d '/usr/local/share/build-info' ]]; then
	mkdir '/usr/local/share/build-info' # This folder (and the "qa-helper-log.txt" file) may or may not already exist depending on whether or not anything else was previously verified in QA Helper.
fi
echo "Undo: oem-config-prepare - $(date '+%m/%d/%Y %T')" >> '/usr/local/share/build-info/qa-helper-log.txt'

echo '
FINISHED UNDOING OEM-CONFIG-PREPARE

THIS COMPUTER WILL NOW REBOOT
'

sleep 2

systemctl reboot
