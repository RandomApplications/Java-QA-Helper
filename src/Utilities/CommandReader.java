/*
 *
 * MIT License
 *
 * Copyright (c) 2018 PCs for People
 * Copyright (c) 2019-2024 Free Geek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package Utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This class reads commands determined by constructor and returns all lines or line containing a given string
 *
 * @author Stefani Monson (of PCs for People) & Pico Mitchell (of Free Geek)
 */
public class CommandReader {

    Scanner reader;
    boolean isWindows = System.getProperty("os.name").startsWith("Windows");

    public CommandReader(String command) {
        initCommandReader((isWindows ? new String[]{"\\Windows\\System32\\cmd.exe", "/c", command} : new String[]{"/bin/sh", "-c", command}));
    }

    public CommandReader(String[] command) {
        initCommandReader(command);
    }

    private void initCommandReader(String[] command) {
        try {
            reader = new Scanner(Runtime.getRuntime().exec(command).getInputStream()).useDelimiter("\n");
        } catch (IOException commandReaderException) {
            reader = null;
            // Ignore Error

            //JOptionPane.showMessageDialog(null, "<html><b>Failed To Perform Command</b><br/><br/>" + Arrays.toString(command) + "</html>", "QA Helper  —  Command Error", JOptionPane.ERROR_MESSAGE);
            //System.out.println(commandReaderException);
        }
    }

    public String getFirstOutputLine() {
        return getFirstOutputLineContaining("");
    }

    public String getFirstOutputLineContaining(String match) {
        if (reader == null) {
            return "";
        }

        boolean getFirstLine = match.isEmpty();

        while (reader.hasNext()) {
            String thisLine = reader.nextLine();

            if (getFirstLine || thisLine.contains(match)) {
                reader.close();
                return thisLine;
            }

        }

        reader.close();
        return "";
    }

    public String getFirstOutputLineNotContaining(String match) {
        if (reader == null) {
            return "";
        }

        boolean getFirstNonEmptyLine = match.isEmpty();

        while (reader.hasNext()) {
            String thisLine = reader.nextLine();

            if ((getFirstNonEmptyLine && !thisLine.isEmpty()) || !thisLine.contains(match)) {
                reader.close();
                return thisLine;
            }

        }

        reader.close();
        return "";
    }

    public String[] getOutputLines() {
        return getOutputLinesContaining(new String[]{""});
    }

    public String[] getOutputLinesContaining(String match) {
        return getOutputLinesContaining(new String[]{match});
    }

    public String[] getOutputLinesContaining(String[] matches) {
        if (reader == null || matches.length == 0) {
            return new String[0];
        }

        ArrayList<String> outputLines = new ArrayList<>();

        boolean matchAllLines = false;
        for (String thisMatch : matches) {
            if (thisMatch.isEmpty()) {
                matchAllLines = true;
                break;
            }
        }

        while (reader.hasNext()) {
            String thisLine = reader.nextLine();

            if (matchAllLines) {
                outputLines.add(thisLine);
            } else {
                for (String thisMatch : matches) {
                    if (thisLine.contains(thisMatch)) {
                        outputLines.add(thisLine);
                        break;
                    }
                }
            }
        }

        reader.close();

        return outputLines.toArray(String[]::new);
    }

    public String[] getOutputLinesNotContaining(String match) {
        return getOutputLinesNotContaining(new String[]{match});
    }

    public String[] getOutputLinesNotContaining(String[] matches) {
        if (reader == null || matches.length == 0) {
            return new String[0];
        }

        ArrayList<String> outputLines = new ArrayList<>();

        boolean matchNonEmptyLines = false;
        for (String thisMatch : matches) {
            if (thisMatch.isEmpty()) {
                matchNonEmptyLines = true;
                break;
            }
        }

        while (reader.hasNext()) {
            String thisLine = reader.nextLine();

            if (!matchNonEmptyLines || (matchNonEmptyLines && !thisLine.isEmpty())) {
                boolean lineContainsMatch = false;

                for (String thisMatch : matches) {
                    if (!thisMatch.isEmpty() && thisLine.contains(thisMatch)) {
                        lineContainsMatch = true;
                        break;
                    }
                }

                if (!lineContainsMatch) {
                    outputLines.add(thisLine);
                }
            }
        }

        reader.close();

        return outputLines.toArray(String[]::new);
    }
}
