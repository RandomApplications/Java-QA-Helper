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

# THIS IS SO FIXED SIZED addGap()'s ARE SCALED CORRECTLY ON HiDPI LINUX

PATH='/usr/bin:/bin:/usr/sbin:/sbin'

cd "${BASH_SOURCE[0]%/*}/../src/GUI" || exit 1

if grep 'addGap(12, 12, 12)' './'*'.java'; then
    sed -i '' 's/addGap(12, 12, 12)/addGap(UIScale.scale(12), UIScale.scale(12), UIScale.scale(12))/' './'*'.java'
fi

if grep 'addGap(18, 18, 18)' './'*'.java'; then
    sed -i '' 's/addGap(18, 18, 18)/addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))/' './'*'.java'
fi

if grep 'addGap(26, 26, 26)' './'*'.java'; then
    sed -i '' 's/addGap(26, 26, 26)/addGap(UIScale.scale(26), UIScale.scale(26), UIScale.scale(26))/' './'*'.java'
fi

if grep 'addGap(18, 18, Short.MAX_VALUE)' './'*'.java'; then
    sed -i '' 's/addGap(18, 18, Short.MAX_VALUE)/addGap(UIScale.scale(18), UIScale.scale(18), Short.MAX_VALUE)/' './'*'.java'
fi

echo -e '\nDone UIScaling Fixed addGaps\n\n'
