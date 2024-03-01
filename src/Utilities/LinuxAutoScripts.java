/*
 *
 * MIT License
 *
 * Copyright (c) 2019 Free Geek
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * This class run specially located and named scripts to make custom changes to Linux.
 *
 * @author Pico Mitchell (of Free Geek)
 */
public class LinuxAutoScripts {

    public LinuxAutoScripts(String scriptType, String adminPassword) {
        if (System.getProperty("os.name").startsWith("Linux")) {
            try {
                String adminPasswordQuotedForShell = "'" + adminPassword.replace("'", "'\\''") + "'";

                boolean isLinuxUbiquityMode = false;

                if (new File("/proc/cmdline").exists()) {
                    try {
                        List<String> linuxBootArguments = Arrays.asList(String.join(" ", Files.readAllLines(Paths.get("/proc/cmdline"), StandardCharsets.UTF_8)).split(" "));
                        isLinuxUbiquityMode = (linuxBootArguments.contains("automatic-ubiquity") || linuxBootArguments.contains("only-ubiquity"));
                    } catch (IOException getLinuxBootArgsException) {
                        System.out.println("getLinuxBootArgsException: " + getLinuxBootArgsException);
                    }
                }

                if (!isLinuxUbiquityMode) {
                    String launchPath = new File(LinuxAutoScripts.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();

                    if (launchPath.endsWith(".jar")) {
                        String possibleSudo = (!adminPassword.equals("*UNKNOWN*") ? "printf '%s\\n' " + adminPasswordQuotedForShell + " | /usr/bin/sudo -Sk " : "");

                        String launchDirectory = launchPath.substring(0, launchPath.lastIndexOf("/"));
                        String defaultDirectory = System.getProperty("user.home") + "/.local/qa-helper";

                        String autoScriptsDirectory = launchDirectory + "/auto-scripts/";
                        if (!new File(autoScriptsDirectory).exists()) {
                            autoScriptsDirectory = defaultDirectory + "/auto-scripts/";
                        }

                        String autoScriptsToRun = "";

                        String scriptTypeKey = scriptType.toLowerCase().replace(" ", "-").replace("/", "-");

                        if (new File(autoScriptsDirectory).exists()) {
                            String onceAutoScriptPrefix = scriptTypeKey + "+once";

                            String userOnceAutoScript = autoScriptsDirectory + onceAutoScriptPrefix + "+user.sh";
                            String rootOnceAutoScript = autoScriptsDirectory + onceAutoScriptPrefix + "+root.sh";

                            String flagsDirectory = defaultDirectory + "/flags/";
                            String thisOnceAutoScriptsFlag = flagsDirectory + "auto-scripts+did-run=" + onceAutoScriptPrefix + ".flag";
                            String touchDidRunOnceAutoScriptFlag = "";

                            if ((new File(userOnceAutoScript).exists() || new File(rootOnceAutoScript).exists()) && !new File(thisOnceAutoScriptsFlag).exists()) {
                                new File(flagsDirectory).mkdirs();

                                if (!new File(flagsDirectory).exists()) {
                                    Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", possibleSudo + "/bin/mkdir -p '" + flagsDirectory.replace("'", "'\\''") + "'; " + possibleSudo + "/bin/chmod 777 '" + flagsDirectory.replace("'", "'\\''") + "'"});
                                }

                                if (new File(userOnceAutoScript).exists()) {
                                    Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", possibleSudo + "/bin/chmod +x '" + userOnceAutoScript.replace("'", "'\\''") + "'"});
                                    autoScriptsToRun += "echo '\n\nRUNNING ONE-TIME " + scriptType.toUpperCase() + " AUTO-SCRIPT FOR CURRENT USER:\n'; '" + userOnceAutoScript.replace("'", "'\\''") + "'; ";
                                }

                                if (new File(rootOnceAutoScript).exists()) {
                                    Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", possibleSudo + "/bin/chmod +x '" + rootOnceAutoScript.replace("'", "'\\''") + "'"});
                                    autoScriptsToRun += "echo '\n\nRUNNING ONE-TIME " + scriptType.toUpperCase() + " AUTO-SCRIPT FOR ROOT:\n'; " + (possibleSudo.isEmpty() ? "/usr/bin/sudo -p '>>> ENTER ADMIN PASSWORD TO RUN ONE-TIME " + scriptType.toUpperCase() + " AUTO-SCRIPT FOR ROOT: ' " : possibleSudo + "-p ' ' ") + "'" + rootOnceAutoScript.replace("'", "'\\''") + "'; ";
                                }

                                touchDidRunOnceAutoScriptFlag = "touch '" + thisOnceAutoScriptsFlag.replace("'", "'\\''") + "';";
                            }

                            String everyAutoScriptPrefix = scriptTypeKey + "+every";

                            String userEveryAutoScript = autoScriptsDirectory + everyAutoScriptPrefix + "+user.sh";
                            String rootEveryAutoScript = autoScriptsDirectory + everyAutoScriptPrefix + "+root.sh";

                            if (new File(userEveryAutoScript).exists()) {
                                Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", possibleSudo + "/bin/chmod +x '" + userEveryAutoScript.replace("'", "'\\''") + "'"});
                                autoScriptsToRun += "echo '\n\nRUNNING EVERY " + scriptType.toUpperCase() + " AUTO-SCRIPT FOR CURRENT USER:\n'; '" + userEveryAutoScript.replace("'", "'\\''") + "'; ";
                            }

                            if (new File(rootEveryAutoScript).exists()) {
                                Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", possibleSudo + "/bin/chmod +x '" + rootEveryAutoScript.replace("'", "'\\''") + "'"});
                                autoScriptsToRun += "echo '\n\nRUNNING EVERY " + scriptType.toUpperCase() + " AUTO-SCRIPT FOR ROOT:\n'; " + (possibleSudo.isEmpty() ? "/usr/bin/sudo -p '>>> ENTER ADMIN PASSWORD TO RUN EVERY " + scriptType.toUpperCase() + " AUTO-SCRIPT FOR ROOT: ' " : possibleSudo + "-p ' ' ") + "'" + rootEveryAutoScript.replace("'", "'\\''") + "'; ";
                            }

                            if (!autoScriptsToRun.isEmpty()) {
                                boolean isLinuxMATE = new File("/usr/bin/mate-terminal").exists();
                                Runtime.getRuntime().exec(new String[]{"/usr/bin/" + (isLinuxMATE ? "mate" : "gnome") + "-terminal", "--window" + (isLinuxMATE ? "" : "-with-profile-internal-id"), (isLinuxMATE ? "" : "0"), "--title", "QA Helper  —  Running " + scriptType + " Auto-Scripts", "--hide-menubar", "--geometry", "80x25+0+0", "-x", "/bin/bash", "-c", "if /usr/bin/pgrep -fn '/auto-scripts/.*\\+(user|root)\\.sh$' &> /dev/null; then echo '\nWAITING FOR SOME OTHER AUTO-SCRIPTS TO FINISH...\n'; fi; while /usr/bin/pgrep -fn '/auto-scripts/.*\\+(user|root)\\.sh$' &> /dev/null; do /bin/sleep 1; done; echo '\nRUNNING " + scriptType.toUpperCase() + " AUTO-SCRIPTS'; /usr/bin/wmctrl -a 'QA Helper'; " + autoScriptsToRun + "echo '\n\nFINISHED RUNNING " + scriptType.toUpperCase() + " AUTO-SCRIPTS\nTHIS TERMINAL WINDOW WILL CLOSE IN 15 SECONDS - OR PRESS ENTER TO CLOSE NOW'; " + touchDidRunOnceAutoScriptFlag + "read -t 15"});
                            }
                        }
                    }
                }
            } catch (IOException | URISyntaxException autoScriptsException) {
                JOptionPane.showMessageDialog(null, "<html><b>Failed to Run <i>" + scriptType + "</i> Auto-Scripts</b></html>", "QA Helper  —  " + scriptType + " Auto-Scripts Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
