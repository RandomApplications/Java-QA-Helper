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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * This class reads commands determined by constructor and returns all lines or line containing a given string
 *
 * @author Stefani Monson (of PCs for People) & Pico Mitchell (of Free Geek)
 */
public class CommandReader {

    private BufferedReader commandReader;
    private final boolean isWindows = System.getProperty("os.name").startsWith("Windows");

    public CommandReader(String commandString) {
        initCommandReader((isWindows ? new String[]{"\\Windows\\System32\\cmd.exe", "/c", commandString} : new String[]{"/bin/sh", "-c", commandString}));
    }

    public CommandReader(String[] commandArray) {
        initCommandReader(commandArray);
    }

    private void initCommandReader(String[] commandArray) {
        try {
            commandReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(commandArray).getInputStream()));
        } catch (IOException commandReaderException) {
            commandReader = null;
            // Ignore Error

            //JOptionPane.showMessageDialog(null, "<html><b>Failed To Perform Command</b><br/><br/>" + Arrays.toString(command) + "</html>", "QA Helper  —  Command Error", JOptionPane.ERROR_MESSAGE);
            //System.out.println("commandReaderException: " + commandReaderException);
        }
    }

    public String getFirstOutputLine() {
        return getFirstOutputLineContaining("");
    }

    public String getFirstOutputLineContaining(String match) {
        if (commandReader == null) {
            return "";
        }

        boolean getFirstLine = match.isEmpty();

        String outputLine = "";

        try {
            String thisLine;
            while ((thisLine = commandReader.readLine()) != null) {
                if (getFirstLine || thisLine.contains(match)) {
                    outputLine = thisLine;
                    break;
                }
            }

            commandReader.close();
        } catch (IOException readLineException) {

        }

        return outputLine;
    }

    public String getFirstOutputLineNotContaining(String match) {
        if (commandReader == null) {
            return "";
        }

        boolean getFirstNonEmptyLine = match.isEmpty();

        String outputLine = "";

        try {
            String thisLine;
            while ((thisLine = commandReader.readLine()) != null) {
                if ((getFirstNonEmptyLine && !thisLine.isEmpty()) || !thisLine.contains(match)) {
                    outputLine = thisLine;
                    break;
                }
            }

            commandReader.close();
        } catch (IOException readLineException) {

        }

        return outputLine;
    }

    public String[] getOutputLines() {
        return getOutputLinesContaining(new String[]{""});
    }

    public String getOutputLinesAsString() {
        return String.join("\n", getOutputLines());
    }

    public String[] getOutputLinesContaining(String match) {
        return getOutputLinesContaining(new String[]{match});
    }

    public String[] getOutputLinesContaining(String[] matches) {
        if ((commandReader == null) || (matches.length == 0)) {
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

        try {
            String thisLine;
            while ((thisLine = commandReader.readLine()) != null) {
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

            commandReader.close();
        } catch (IOException readLineException) {

        }

        return outputLines.toArray(String[]::new);
    }

    public String[] getOutputLinesNotContaining(String match) {
        return getOutputLinesNotContaining(new String[]{match});
    }

    public String[] getOutputLinesNotContaining(String[] matches) {
        if ((commandReader == null) || (matches.length == 0)) {
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

        try {
            String thisLine;
            while ((thisLine = commandReader.readLine()) != null) {
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

            commandReader.close();
        } catch (IOException readLineException) {

        }

        return outputLines.toArray(String[]::new);
    }
}
