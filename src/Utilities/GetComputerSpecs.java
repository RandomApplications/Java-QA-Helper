/*
 *
 * MIT License
 *
 * Copyright (c) 2018 PCs for People
 * Copyright (c) 2018-2024 Free Geek
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class gathers information about the computer's specifications.
 *
 * @author Stefani Monson (of PCs for People) & Pico Mitchell (of Free Geek)
 */
public final class GetComputerSpecs {

    /**
     * @param args the command line arguments
     */
    //variable declarations
    private String hdSentinelBinaryTempPath;
    private String pciIDsDbTempPath;
    private String loadSpecsExceptionString = "";

    private String os = "N/A";
    private String fullOS = "N/A";

    private String model = "N/A";
    private String macShortModelName = "N/A";
    private String macModelIdentifier = "";
    private String fullModel = "N/A";
    private String motherboardModel = "N/A";
    private String fullMotherboardModel = "N/A";
    private String chassisType = "N/A";
    private boolean isLaptop = false;
    private boolean hasScreen = false;
    private final ArrayList<String> macScreenFeaturesArray = new ArrayList<>();
    private boolean hasTouchscreen = false;
    private boolean hasCamera = false;
    private boolean possibleCustomDesktopOrBareMotherboard = false;
    private String brand = "N/A";
    private String fullBrand = "N/A";
    private String motherboardBrand = "N/A";
    private String fullMotherboardBrand = "N/A";
    private String serial = "N/A";
    private String fullSerial = "N/A";
    private String motherboardSerial = "N/A";
    private String fullMotherboardSerial = "N/A";
    private String biosUUID = "N/A";
    private String cpu = "N/A";
    private String fullCPU = "N/A";
    private int cpuThreadCount = 0;
    private String ram = "N/A";
    private String fullRAM = "N/A";
    private String motherboardRAM = "N/A";
    private String fullMotherboardRAM = "N/A";
    private String storage = "N/A";
    private final ArrayList<String> storageArray = new ArrayList<>();
    private String storageSerial = "";
    private final ArrayList<String> storageSerialsArray = new ArrayList<>();
    private final ArrayList<String> disksFormattedWithoutPartitionTable = new ArrayList<>();
    private String driveHealth = "N/A";
    private boolean driveHealthWarning = false;
    private boolean driveTrimWarning = false;
    private boolean driveRecalled = false;
    private final ArrayList<String> batteryHealthArray = new ArrayList<>();
    private String powerAdapter = "";
    private final ArrayList<String> batteryHealthWarningArray = new ArrayList<>();
    private final ArrayList<String> batteryHealthErrorArray = new ArrayList<>();
    private String gpu = "N/A";
    private final ArrayList<String> gpuArray = new ArrayList<>();
    private final ArrayList<String> gpuIDsArray = new ArrayList<>();
    private final ArrayList<String> gpuErrorArray = new ArrayList<>();
    private String audio = "N/A";
    private final ArrayList<String> audioArray = new ArrayList<>();
    private final ArrayList<String> audioIDsArray = new ArrayList<>();
    private final ArrayList<String> audioErrorArray = new ArrayList<>();
    private final ArrayList<String> wirelessArray = new ArrayList<>();
    private final ArrayList<String> wirelessErrorArray = new ArrayList<>();
    private String screenSize = "N/A";
    private String fullScreenSize = "N/A";
    private String screenResolution = "";
    private final ArrayList<String> screenErrorArray = new ArrayList<>();
    private String discDrive = "N/A";
    private final ArrayList<String> discDriveArray = new ArrayList<>();
    private final ArrayList<String> discDriveLogicalNames = new ArrayList<>();
    private boolean discDriveCanBurnCDs = false;
    private boolean discDriveCanBurnDVDs = false;
    private boolean hasDiscInDiscDrive = false;
    private boolean keyboardRecalled = false;
    private boolean serialIsMAC = false;

    private final ArrayList<String> ethernetMACarray = new ArrayList<>();

    private String productKeyFromRegistryVbsTempPath;

    public GetComputerSpecs(String adminPassword, boolean isTestMode) {
        reloadSpecs(adminPassword, isTestMode);
    }

    private String cleanComputerBrand(String dirtyComputerBrand) {
        for (String thisStringToRemove : new String[]{",", " inc.", " incorporated", " inc", " ltd.", " ltd", " limited", " co.", " corp.", " corporation", " computers", " computer", " systems", " technology", " electronics", " software international", " gmbh", "(r) client"}) {
            dirtyComputerBrand = dirtyComputerBrand.replaceAll("(?i)" + Pattern.quote(thisStringToRemove), "");
        }

        return dirtyComputerBrand.replace("ASUSTeK", "ASUS").replace("FUJITSU", "Fujitsu").replace("Hewlett-Packard", "HP").replace("INTEL", "Intel").replace("LENOVO", "Lenovo").replace("Micro-Star International", "MSI").replace("MICRO-STAR INTERNATIONAL", "MSI").replace("SAMSUNG", "Samsung").replace("TOSHIBA", "Toshiba").replace("GOOGLE", "Google").trim().replaceAll("\\s{2,}", " ");
    }

    public boolean ignoreSpecsPlaceholders(String rawModel) {
        String modelForComparison = rawModel.toLowerCase().replaceAll("[^a-z0-9]", ""); // Make lowercase and remove all except letters and number to not have to worry about any possible slight variations.

        if (modelForComparison.isEmpty()) {
            return true;
        }

        if (modelForComparison.replaceAll("[0-1]", "").isEmpty()) {
            // Ignore any values that are all 1's and 0's. Such as the Lenovo IdeaPad Z580 with the System Serial of "11000000000000000000000000000001" (while the correct serial is stored with the motherboard).
            // This also ignores some SKUs that are all zeros.
            return true;
        }

        return Arrays.asList(
                "tobefilledbyoem", "notapplicable", // Could be in Any Field
                "systemmanufacturer", // Manufacturers
                "systemversion", "chassisversion", "notspecified", "lenovoproduct", "type1productconfigid", "rev1xx", "rev10", "102a", "0123456789", "invalid", "tbdbyoem", // Versions
                "systemproductname", "unidentifiedsystem", "allseries", "notebook", // Products
                "systemskunumber", "systemskunumberunknown", "systemsku", "all", "sku", "xxx123xaba", "type1sku0", "serenaskua", "asusnotebooksku", "asusultrabook", "undefined", "defaultstring", "badindex", // SKUs
                "systemserialnumber", "chassisserialnumber", "123456789", "ing", "na", "nosn000", // Serials
                "revx0x", "xx", // Motherboard Versions
                "none" // Motherboard Serials
        ).contains(modelForComparison);
    }

    private String cleanCPU(String dirtyCPU) {
        for (String thisStringToReplaceWithSpace : new String[]{"genuine", "authentic", "(r)", "(tm)", "?", " cpu", " apu", " processor"}) {
            dirtyCPU = dirtyCPU.replaceAll("(?i)" + Pattern.quote(thisStringToReplaceWithSpace), " ");
        }

        return dirtyCPU.trim().replaceAll("\\s{2,}", " ");
    }

    private String cleanDeviceBrand(String dirtyDeviceBrand) {
        // See PCI ID Repository for examples of Brands and Models: https://pci-ids.ucw.cz/v2.2/pci.ids
        // PCI ID Repository is used by lshw on Linux and queried manually in Windows when drivers aren't installed.

        String cleanedDeviceBrand = dirtyDeviceBrand;

        if (cleanedDeviceBrand.endsWith(")")) {
            // Parenthesis at the end (in PCI ID Repo) could possibly contain "Wrong ID" or other junk we don't want.
            int openParenthesesLastIndex = cleanedDeviceBrand.lastIndexOf("(");
            if (openParenthesesLastIndex > 0) {
                // Don't delete parenthesis if the whole thing is in parenthesis, such as "(Standard display types)"
                cleanedDeviceBrand = cleanedDeviceBrand.substring(0, openParenthesesLastIndex);
            }
        }

        if (cleanedDeviceBrand.endsWith("]")) {
            // Stuff in sqaure brakets at the end (in PCI ID Repo) is just short or alternate names which we don't need.
            int openSquareBracketLastIndex = cleanedDeviceBrand.lastIndexOf("[");
            if (openSquareBracketLastIndex > 0) {
                // Don't delete brackets if the whole thing is in brackets for some reason.
                cleanedDeviceBrand = cleanedDeviceBrand.substring(0, openSquareBracketLastIndex);
            }
        }

        // "(R)", "®" (\u00AE), "(TM)", and "™" (\u2122) could exist when brand is coming from Windows driver info rather than PCI ID Repo
        for (String thisStringToRemove : new String[]{"(r)", "\u00AE", "(tm)", "\u2122", "?", ",", " inc.", " incorporated", " inc", " ltd.", " ltd", " limited", " co.", " corp.", " corporation", " computers", " computer", " systems", " semiconductors", " semiconductor", " and subsidiaries"}) {
            cleanedDeviceBrand = cleanedDeviceBrand.replaceAll("(?i)" + Pattern.quote(thisStringToRemove), "");
        }

        return cleanedDeviceBrand.replace("Advanced Micro Devices", "AMD").trim().replaceAll("\\s{2,}", " ");
    }

    private String cleanDeviceModel(String dirtyDeviceModel) {
        String cleanedDeviceModel = dirtyDeviceModel;

        boolean didContainHDMIaudio = (cleanedDeviceModel.contains("HDMI Audio"));

        int openSquareBracketLastIndex = cleanedDeviceModel.lastIndexOf("[");
        int closeSquareBracketLastIndex = cleanedDeviceModel.lastIndexOf("]");
        if (openSquareBracketLastIndex > -1 && closeSquareBracketLastIndex > -1) {
            // NVIDIA and AMD contain the "normal" model name inside square brackets in PCI ID Repo
            cleanedDeviceModel = cleanedDeviceModel.substring(openSquareBracketLastIndex + 1, closeSquareBracketLastIndex);
        }

        if (didContainHDMIaudio && !cleanedDeviceModel.contains("HDMI Audio")) {
            // "HDMI Audio" could get cut out when only using what's inside square brackets from PCI ID Repo
            cleanedDeviceModel += " HDMI Audio";
        }

        // "(R)", "®" (\u00AE), "(TM)", "™" (\u2122), and "Device" could exist when model is coming from Windows driver info rather than PCI ID Repo
        return cleanedDeviceModel.replace("(R)", "").replace("\u00AE", "").replace("(TM)", "").replace("\u2122", "").replace("?", "").replace("Generation", "Gen").replace("High Definition", "HD").replace("processor", "CPU").replace("Processor", "CPU").replace("Wireless-", "").replace("Wireless", "").replace("PCIe", "").replace("PCI Express", "").replace("PCI-Express", "").replace(" Controller", "").replace(" Adapter", "").replace(" Device", "").replace(" CODEC", "").replace(" Network", "").replace(" Modem", "").replace("()", "").trim().replaceAll("\\s{2,}", " ");
    }

    public void reloadSpecs(String adminPassword, boolean isTestMode) {
        try {
            String adminPasswordQuotedForShell = "'" + adminPassword.replace("'", "'\\''") + "'";

            if (isTestMode) {
                System.out.println("\n\nReloading Specs in Test Mode");
            }

            loadSpecsExceptionString = "";
            os = "N/A";
            fullOS = "N/A";

            model = "N/A";
            macShortModelName = "N/A";
            macModelIdentifier = "";
            fullModel = "N/A";
            motherboardModel = "N/A";
            fullMotherboardModel = "N/A";
            chassisType = "N/A";
            isLaptop = false;
            hasScreen = false;
            macScreenFeaturesArray.clear();
            hasTouchscreen = false;
            hasCamera = false;
            possibleCustomDesktopOrBareMotherboard = false;
            brand = "N/A";
            fullBrand = "N/A";
            motherboardBrand = "N/A";
            fullMotherboardBrand = "N/A";
            serial = "N/A";
            fullSerial = "N/A";
            motherboardSerial = "N/A";
            fullMotherboardSerial = "N/A";
            biosUUID = "N/A";
            cpu = "N/A";
            fullCPU = "N/A";
            cpuThreadCount = 0;
            ram = "N/A";
            fullRAM = "N/A";
            motherboardRAM = "N/A";
            fullMotherboardRAM = "N/A";
            storage = "N/A";
            storageArray.clear();
            storageSerial = "";
            storageSerialsArray.clear();
            disksFormattedWithoutPartitionTable.clear();
            driveHealth = "N/A";
            driveHealthWarning = false;
            driveTrimWarning = false;
            driveRecalled = false;
            batteryHealthArray.clear();
            powerAdapter = "";
            batteryHealthWarningArray.clear();
            batteryHealthErrorArray.clear();
            gpu = "N/A";
            gpuArray.clear();
            gpuIDsArray.clear();
            gpuErrorArray.clear();
            audio = "N/A";
            audioArray.clear();
            audioIDsArray.clear();
            audioErrorArray.clear();
            wirelessArray.clear();
            wirelessErrorArray.clear();
            screenSize = "N/A";
            fullScreenSize = "N/A";
            screenResolution = "";
            screenErrorArray.clear();
            discDrive = "N/A";
            discDriveArray.clear();
            discDriveLogicalNames.clear();
            discDriveCanBurnCDs = false;
            discDriveCanBurnDVDs = false;
            hasDiscInDiscDrive = false;
            keyboardRecalled = false;
            serialIsMAC = false;

            ethernetMACarray.clear();

            os = System.getProperty("os.name") + " " + System.getProperty("os.version");
            boolean isLinux = (os.startsWith("Linux"));
            boolean isMacOS = (os.startsWith("Mac OS X") || os.startsWith("macOS"));
            boolean isWindows = (os.startsWith("Windows"));

            if (isMacOS) {
                String actualMacOSversion = new CommandReader(new String[]{"/usr/bin/sw_vers", "-productVersion"}).getFirstOutputLine(); // Java os.version does not show the full version on Big Sur (11.5.2 shows as just 11.5).

                if (actualMacOSversion.isEmpty()) {
                    actualMacOSversion = System.getProperty("os.version"); // This should never be needed, just just in case.
                }

                String[] versionParts = actualMacOSversion.replaceAll("[^0-9.]", "").split("\\.");

                if (versionParts.length >= 2 && versionParts[0].equals("10")) {
                    int minorVersionPart = Integer.parseInt(versionParts[1]);

                    if (minorVersionPart >= 16) {
                        // Early macOS 11 betas identify as 10.16, also Java os.version built before Big Sur SDK will get 10.16 as version for compatibility.
                        int actualMajorVersion = minorVersionPart - 5; // 10.16 is actually 11.0
                        os = "macOS " + actualMajorVersion + ((versionParts.length == 3) ? "." + versionParts[2] : "");
                        // Marketing names for macOS 11 an newer will be added below.
                    } else if (minorVersionPart >= 12) {
                        os = "macOS " + actualMacOSversion;

                        switch (minorVersionPart) {
                            case 12:
                                os += " Sierra";
                                break;
                            case 13:
                                os += " High Sierra";
                                break;
                            case 14:
                                os += " Mojave";
                                break;
                            case 15:
                                os += " Catalina";
                                break;
                            default:
                                break;
                        }
                    } else if (minorVersionPart >= 8) {
                        os = "OS X " + actualMacOSversion;

                        switch (minorVersionPart) {
                            case 8:
                                os += " Mountain Lion";
                                break;
                            case 9:
                                os += " Mavericks";
                                break;
                            case 10:
                                os += " Yosemite";
                                break;
                            case 11:
                                os += " El Capitan";
                                break;
                            default:
                                break;
                        }
                    } else {
                        os = "Mac OS X " + actualMacOSversion;
                        // Don't bother adding the marketing names to these very old versions since QA Helper has never even been tested on older than 10.11.
                    }
                } else {
                    os = "macOS " + actualMacOSversion;

                    try {
                        if (Integer.parseInt(versionParts[0]) >= 13) {
                            String macOSrsrVersion = new CommandReader(new String[]{"/usr/bin/sw_vers", "-productVersionExtra"}).getFirstOutputLine();

                            if (macOSrsrVersion.startsWith("(")) {
                                os += " " + macOSrsrVersion;
                            }
                        }
                    } catch (NumberFormatException macOSmajorVersionException) {
                        if (isTestMode) {
                            System.out.println("macOSmajorVersionException: " + macOSmajorVersionException);
                        }
                    }
                }

                if (os.startsWith("macOS 11")) {
                    os += " Big Sur";
                } else if (os.startsWith("macOS 12")) {
                    os += " Monterey";
                } else if (os.startsWith("macOS 13")) {
                    os += " Ventura";
                } else if (os.startsWith("macOS 14")) {
                    os += " Sonoma";
                } else if (os.startsWith("macOS 15")) {
                    os += " Sequoia";
                } else if (os.startsWith("macOS 16") || os.startsWith("macOS 26")) {
                    os += " Tahoe";
                }
            }

            String systemProductName = "N/A";
            String systemProductSKU = "N/A";

            String macModelIdentifierNumber = "";
            String macSerialConfigCode = "";

            String motherboardVersion = "N/A";

            String cpuCurrentSpeedString = "";
            String cpuMaxSpeedString = "";
            String cpuCount = "";
            String cpuCores = "";
            String cpuThreads = "";
            String cpuBrand = "";
            String cpuPandEcoresString = "";

            long ramTotalBytesFromBanks = 0L;
            String maxRAM = "";
            String ramType = "";
            String ramSpeed = "";
            String ramVoltage = "";
            String ramSlots = "";

            HashMap<String, String> batteryDesignVoltages = new HashMap<>();

            boolean isLinuxUbiquityMode = false;

            if (isLinux) {
                // Linux Info

                // Make sure required tools for loading specs are installed and automatically install them if not.
                // PACKAGES FOR REQUIRED TOOLS (IN ORDER OF USAGE):
                //      lsb-release: lsb_release
                //      xinput
                //      dmidecode
                //      util-linux: lsmem, lscpu, lsblk
                //      pciutils: lspci
                //      lshw
                //      hdsentinel (INCLUDED IN JAR - DON'T NEED TO INSTALL)
                //      upower
                //      network-manager: nmcli
                //      modemmanager: mmcli
                //      bluez (for btmgmt)
                //      x11-xserver-utils: xrandr
                //      x11-utils: xdpyinfo
                List<String> requiredLinuxToolsPackages = Arrays.asList("lsb-release", "xinput", "dmidecode", "util-linux", "pciutils", "lshw", "upower", "network-manager", "modemmanager", "bluez", "x11-xserver-utils", "x11-utils");

                ArrayList<String> aptCachePolicyCommand = new ArrayList<>();
                aptCachePolicyCommand.addAll(Arrays.asList("/usr/bin/apt-cache", "policy"));
                aptCachePolicyCommand.addAll(requiredLinuxToolsPackages);
                String[] aptCachePolicyCommandArray = aptCachePolicyCommand.toArray(String[]::new);

                if (!adminPassword.equals("*UNKNOWN*") && (new CommandReader(aptCachePolicyCommandArray).getOutputLinesContaining("Installed: (none)").length > 0)) {
                    if (isTestMode) {
                        System.out.println("installedLinuxToolsLocations:\n" + new CommandReader(aptCachePolicyCommandArray).getOutputLinesAsString());
                    }

                    boolean requiredToolsAreInstalled = false;

                    try {
                        Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "printf '%s\\n' " + adminPasswordQuotedForShell + " | /usr/bin/sudo -Sk /usr/bin/apt-get install --no-install-recommends -qq " + String.join(" ", requiredLinuxToolsPackages)}).waitFor();
                        requiredToolsAreInstalled = (new CommandReader(aptCachePolicyCommandArray).getOutputLinesContaining("Installed: (none)").length == 0);
                    } catch (IOException | InterruptedException installRequiredToolsException) {
                        if (isTestMode) {
                            System.out.println("installRequiredToolsException: " + installRequiredToolsException);
                        }
                    }

                    if (new File("/proc/cmdline").exists()) {
                        try {
                            List<String> linuxBootArguments = Arrays.asList(String.join(" ", Files.readAllLines(Paths.get("/proc/cmdline"))).split(" "));
                            isLinuxUbiquityMode = (linuxBootArguments.contains("automatic-ubiquity") || linuxBootArguments.contains("only-ubiquity"));
                        } catch (IOException getLinuxBootArgsException) {
                            if (isTestMode) {
                                System.out.println("getLinuxBootArgsException: " + getLinuxBootArgsException);
                            }
                        }
                    }

                    if (!requiredToolsAreInstalled && !isLinuxUbiquityMode) {
                        try {
                            File waitForTerminalPIDtempFile = File.createTempFile("qa_helper-wait_for_terminal_pid", ".txt");
                            waitForTerminalPIDtempFile.deleteOnExit();

                            boolean isLinuxMATE = new File("/usr/bin/mate-terminal").exists();
                            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "printf '%s\\n' " + adminPasswordQuotedForShell + " | /usr/bin/sudo -Sk /usr/bin/" + (isLinuxMATE ? "mate" : "gnome") + "-terminal --window" + (isLinuxMATE ? "" : "-with-profile-internal-id '0'") + " --title 'QA Helper  —  Installing Required Tools' --hide-menubar --geometry '80x25+0+0' -x /bin/bash -c 'orig_temp_owner=$(/usr/bin/stat -c %U " + waitForTerminalPIDtempFile.getPath() + "); /bin/chown root " + waitForTerminalPIDtempFile.getPath() + "; echo $$ > " + waitForTerminalPIDtempFile.getPath() + "; /bin/chown ${orig_temp_owner} " + waitForTerminalPIDtempFile.getPath() + "; echo \"\nINSTALLING REQUIRED TOOLS\n\n\"; /usr/local/bin/apt install --no-install-recommends -y " + String.join(" ", requiredLinuxToolsPackages) + "; echo \"\n\nFINISHED INSTALLING REQUIRED TOOLS\nTHIS TERMINAL WINDOW WILL CLOSE IN 5 SECONDS - OR PRESS ENTER TO CLOSE NOW\"; read -t 5; /usr/bin/wmctrl -a \"QA Helper\"'"});

                            String waitForTerminalPID = "";

                            for (int i = 0; i < 10; i++) {
                                if (waitForTerminalPIDtempFile.exists()) {
                                    try {
                                        List<String> waitForTerminalPIDtempFileLines = Files.readAllLines(waitForTerminalPIDtempFile.toPath());

                                        if (!waitForTerminalPIDtempFileLines.isEmpty()) {
                                            waitForTerminalPID = waitForTerminalPIDtempFileLines.get(0);
                                            if (!waitForTerminalPID.isEmpty()) {
                                                break;
                                            }
                                        }
                                    } catch (IOException | IndexOutOfBoundsException getTerminalPIDException) {
                                        if (isTestMode) {
                                            System.out.println("getTerminalPIDException: " + getTerminalPIDException);
                                        }
                                    }
                                }

                                TimeUnit.MILLISECONDS.sleep(500);
                            }

                            if (waitForTerminalPIDtempFile.exists()) {
                                waitForTerminalPIDtempFile.delete();
                            }

                            if (!waitForTerminalPID.isEmpty()) {
                                while (!new CommandReader(new String[]{"/bin/ps", "-p", waitForTerminalPID, "-o", "pid="}).getFirstOutputLine().isEmpty()) {
                                    TimeUnit.MILLISECONDS.sleep(500);
                                }
                            }
                        } catch (IOException | InterruptedException installRequiredToolsViaTerminalException) {
                            if (isTestMode) {
                                System.out.println("installRequiredToolsViaTerminalException: " + installRequiredToolsViaTerminalException);
                            }
                        }
                    }
                }

                // Java os.name and os.version are not flavor specific enough in Linux
                String linuxReleaseDescription = new CommandReader(new String[]{"/usr/bin/lsb_release", "-ds"}).getFirstOutputLine();
                if (!linuxReleaseDescription.isEmpty()) {
                    os = linuxReleaseDescription;
                }

                String linuxReleaseCodename = new CommandReader(new String[]{"/usr/bin/lsb_release", "-cs"}).getFirstOutputLine();
                if (!linuxReleaseCodename.isEmpty() && !os.toLowerCase().contains(linuxReleaseCodename.toLowerCase())) {
                    os += " " + linuxReleaseCodename.substring(0, 1).toUpperCase() + linuxReleaseCodename.substring(1);
                }

                String desktopEnvironment = System.getenv("XDG_CURRENT_DESKTOP");
                if (desktopEnvironment != null) {
                    if (desktopEnvironment.startsWith("X-")) {
                        desktopEnvironment = desktopEnvironment.substring(2);
                    }

                    if (!os.toLowerCase().contains(desktopEnvironment.toLowerCase())) {
                        os += " (" + desktopEnvironment + ")";
                    }
                }

                isLaptop = !new CommandReader(new String[]{"/usr/bin/xinput"}).getFirstOutputLineContaining("AT Translated").isEmpty(); // "AT Translated Set 2 keyboard" is always in internal keyboard which will only exist on laptops (Only checking Chassis Type is not reliable enough).

                // Use dmidecode for Model, Chassis, RAM, and Battery Design Voltage.
                String[] dmidecodeLines = new CommandReader("printf '%s\\n' " + adminPasswordQuotedForShell + " | /usr/bin/sudo -Sk /usr/sbin/dmidecode --type 1,2,3,11,16,17,22").getOutputLines();

                String thisDmidecodeCategoryHandle = "Unknown";
                String thisDmidecodeCategory = "Unknown";

                ArrayList<String> dmidecodeMemoryDeviceLines = new ArrayList<>();

                String possibleMotherboardBrand = "N/A";
                String possibleMotherboardModel = "N/A";
                String possibleMotherboardVersion = "N/A";
                String possibleMotherboardSerial = "N/A";

                boolean thisPhysicalMemoryArrayIsSystemMemory = false;
                ArrayList<String> systemMemoryArrayHandles = new ArrayList<>();
                boolean thisMemoryDeviceIsSystemMemory = false;
                long ramMaxBytes = 0L;

                String thisBatteryManufacturer = "";
                String thisBatteryName = "";

                for (String thisDmidecodeLine : dmidecodeLines) {
                    if (!thisDmidecodeLine.startsWith("\t") && !thisDmidecodeLine.isEmpty()) {
                        if (thisDmidecodeLine.startsWith("Handle ")) {
                            thisDmidecodeCategoryHandle = thisDmidecodeLine.substring(7, thisDmidecodeLine.indexOf(","));
                        } else {
                            thisDmidecodeCategory = thisDmidecodeLine;
                        }

                        if (thisDmidecodeCategory.equals("Physical Memory Array")) {
                            thisPhysicalMemoryArrayIsSystemMemory = false;
                        }

                        continue;
                    }

                    if (thisDmidecodeLine.contains(": ")) {
                        String thisDmidecodeProperty = thisDmidecodeLine.substring(thisDmidecodeLine.indexOf(": ") + 2).replaceAll("^[. /]+", "").replaceAll("[. /]+$", ""); // Trim all leading and trailing spaces, periods, and slashes.

                        if (!thisDmidecodeProperty.isEmpty()) {
                            switch (thisDmidecodeCategory) {
                                case "System Information":
                                    if (!ignoreSpecsPlaceholders(thisDmidecodeProperty)) {
                                        if (thisDmidecodeLine.startsWith("\tManufacturer:")) {
                                            brand = cleanComputerBrand(thisDmidecodeProperty);
                                        } else if (thisDmidecodeLine.startsWith("\tProduct Name:")) {
                                            systemProductName = thisDmidecodeProperty;
                                        } else if (thisDmidecodeLine.startsWith("\tVersion:") && thisDmidecodeProperty.replaceAll("[^A-Za-z0-9]", "").length() > 4) {
                                            model = thisDmidecodeProperty;
                                        } else if (thisDmidecodeLine.startsWith("\tSerial Number:")) {
                                            serial = thisDmidecodeProperty;
                                        } else if (thisDmidecodeLine.startsWith("\tUUID:")) {
                                            biosUUID = thisDmidecodeProperty.toUpperCase();
                                        } else if (thisDmidecodeLine.startsWith("\tSKU Number:") && thisDmidecodeProperty.replaceAll("[^A-Za-z0-9]", "").length() > 1) {
                                            systemProductSKU = thisDmidecodeProperty;
                                        }
                                    }

                                    break;
                                case "Base Board Information":
                                    if (!ignoreSpecsPlaceholders(thisDmidecodeProperty)) {
                                        if (thisDmidecodeLine.startsWith("\tManufacturer:")) {
                                            possibleMotherboardBrand = cleanComputerBrand(thisDmidecodeProperty);
                                        } else if (thisDmidecodeLine.startsWith("\tProduct Name:")) {
                                            possibleMotherboardModel = thisDmidecodeProperty;
                                        } else if (thisDmidecodeLine.startsWith("\tVersion:") && thisDmidecodeProperty.replaceAll("[^A-Za-z0-9]", "").length() > 4) {
                                            possibleMotherboardVersion = thisDmidecodeProperty;
                                        } else if (thisDmidecodeLine.startsWith("\tSerial Number:")) {
                                            possibleMotherboardSerial = thisDmidecodeProperty;
                                        } else if (thisDmidecodeLine.startsWith("\tType:") && thisDmidecodeProperty.equals("Motherboard")) {
                                            motherboardBrand = possibleMotherboardBrand;
                                            motherboardModel = possibleMotherboardModel;
                                            motherboardVersion = possibleMotherboardVersion;
                                            motherboardSerial = possibleMotherboardSerial;
                                        }
                                    }

                                    break;
                                case "Chassis Information":
                                    if (thisDmidecodeLine.startsWith("\tType:")) {
                                        chassisType = thisDmidecodeProperty;
                                        String lowercaseChassisType = chassisType.toLowerCase();
                                        if (lowercaseChassisType.contains("laptop") || lowercaseChassisType.contains("book") || lowercaseChassisType.contains("portable") || lowercaseChassisType.contains("tablet") || lowercaseChassisType.contains("convertable") || lowercaseChassisType.contains("detachable")) {
                                            isLaptop = true; // isLaptop may already be set to true from checking for internal keyboard. Don't ever want the wrong chassis to set it to false.
                                        }
                                        hasScreen = (isLaptop || lowercaseChassisType.replaceAll("[^a-z]", "").contains("allinone"));
                                    }

                                    break;
                                case "OEM Strings":
                                    if (brand.equals("Dell") && (systemProductSKU.equals("N/A") || systemProductSKU.equals(systemProductName)) && thisDmidecodeLine.startsWith("\tString ")
                                            && thisDmidecodeLine.contains("[") && thisDmidecodeLine.endsWith("]") && (thisDmidecodeLine.length() == (thisDmidecodeLine.indexOf("[") + 6))) {
                                        // If Dell and didn't get the actual SKU from "System Information/SKU Number", this should be it.
                                        String possibleSystemProductSKU = thisDmidecodeLine.substring((thisDmidecodeLine.indexOf("[") + 1), (thisDmidecodeLine.indexOf("[") + 5));
                                        if (!possibleSystemProductSKU.equals("0000")) {
                                            systemProductSKU = possibleSystemProductSKU;
                                        }
                                    }

                                    break;
                                case "Physical Memory Array":
                                    if (thisDmidecodeLine.startsWith("\tUse:") && thisDmidecodeProperty.equals("System Memory")) {
                                        thisPhysicalMemoryArrayIsSystemMemory = true;
                                        systemMemoryArrayHandles.add(thisDmidecodeCategoryHandle);
                                    } else if (thisPhysicalMemoryArrayIsSystemMemory) {
                                        if (thisDmidecodeLine.startsWith("\tMaximum Capacity:")) {
                                            try {
                                                if (thisDmidecodeProperty.endsWith(" bytes")) {
                                                    ramMaxBytes += Long.parseLong(thisDmidecodeProperty.substring(0, thisDmidecodeProperty.indexOf(" ")));
                                                } else if (thisDmidecodeProperty.endsWith(" kB")) {
                                                    ramMaxBytes += Long.parseLong(thisDmidecodeProperty.substring(0, thisDmidecodeProperty.indexOf(" "))) * 1024;
                                                } else if (thisDmidecodeProperty.endsWith(" MB")) {
                                                    ramMaxBytes += Long.parseLong(thisDmidecodeProperty.substring(0, thisDmidecodeProperty.indexOf(" "))) * 1024 * 1024;
                                                } else if (thisDmidecodeProperty.endsWith(" GB")) {
                                                    ramMaxBytes += Long.parseLong(thisDmidecodeProperty.substring(0, thisDmidecodeProperty.indexOf(" "))) * 1024 * 1024 * 1024;
                                                } else if (thisDmidecodeProperty.endsWith(" TB")) {
                                                    ramMaxBytes += Long.parseLong(thisDmidecodeProperty.substring(0, thisDmidecodeProperty.indexOf(" "))) * 1024 * 1024 * 1024 * 1024;
                                                } else if (thisDmidecodeProperty.endsWith(" PB")) {
                                                    ramMaxBytes += Long.parseLong(thisDmidecodeProperty.substring(0, thisDmidecodeProperty.indexOf(" "))) * 1024 * 1024 * 1024 * 1024 * 1024;
                                                } else if (thisDmidecodeProperty.endsWith(" EB")) {
                                                    ramMaxBytes += Long.parseLong(thisDmidecodeProperty.substring(0, thisDmidecodeProperty.indexOf(" "))) * 1024 * 1024 * 1024 * 1024 * 1024 * 1024;
                                                } else if (thisDmidecodeProperty.endsWith(" ZB")) {
                                                    ramMaxBytes += Long.parseLong(thisDmidecodeProperty.substring(0, thisDmidecodeProperty.indexOf(" "))) * 1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024;
                                                }
                                            } catch (NumberFormatException ramMaxSizeException) {
                                                if (isTestMode) {
                                                    System.out.println("ramMaxSizeException: " + ramMaxSizeException);
                                                }
                                            }
                                        }
                                    }

                                    break;
                                case "Memory Device":
                                    // Collect "Memory Device" lines to process after the rest of dmidecodeLines because "Physical Memory Array" could come after.
                                    dmidecodeMemoryDeviceLines.add(thisDmidecodeLine);

                                    break;
                                case "Portable Battery":
                                    if (thisDmidecodeLine.startsWith("\tManufacturer:")) {
                                        thisBatteryManufacturer = thisDmidecodeProperty;
                                    } else if (thisDmidecodeLine.startsWith("\tName:")) {
                                        thisBatteryName = thisDmidecodeProperty;
                                    } else if (thisDmidecodeLine.startsWith("\tDesign Voltage:")) {
                                        try {
                                            batteryDesignVoltages.put(thisBatteryManufacturer + " " + thisBatteryName, new DecimalFormat("#.#").format(Double.parseDouble(thisDmidecodeProperty.replaceAll("[^0-9]", "")) / 1000));
                                        } catch (NumberFormatException batteryDesignVoltageException) {
                                            if (isTestMode) {
                                                System.out.println("batteryDesignVoltageException: " + batteryDesignVoltageException);
                                            }
                                        }

                                        thisBatteryManufacturer = "";
                                        thisBatteryName = "";
                                    }

                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }

                // Process "Memory Device" lines after the rest of dmidecodeLines because "Physical Memory Array" could have come after.
                for (String thisDmidecodeMemoryDeviceLine : dmidecodeMemoryDeviceLines) {
                    if (thisDmidecodeMemoryDeviceLine.contains(": ")) {
                        String thisDmidecodeMemoryDeviceProperty = thisDmidecodeMemoryDeviceLine.substring(thisDmidecodeMemoryDeviceLine.indexOf(": ") + 2).trim();

                        if (!thisDmidecodeMemoryDeviceProperty.isEmpty()) {
                            if (thisDmidecodeMemoryDeviceLine.startsWith("\tArray Handle:")) {
                                thisMemoryDeviceIsSystemMemory = systemMemoryArrayHandles.contains(thisDmidecodeMemoryDeviceProperty);
                            } else if (thisMemoryDeviceIsSystemMemory) {
                                if (thisDmidecodeMemoryDeviceLine.startsWith("\tSize:")) {
                                    if (thisDmidecodeMemoryDeviceProperty.equals("No Module Installed")) {
                                        if (ramSlots.isEmpty()) {
                                            ramSlots = "Empty";
                                        } else {
                                            ramSlots += " + Empty";
                                        }
                                    } else {
                                        long thisMemoryBankBytes = 0;

                                        try {
                                            if (thisDmidecodeMemoryDeviceProperty.endsWith(" bytes")) {
                                                thisMemoryBankBytes = Long.parseLong(thisDmidecodeMemoryDeviceProperty.substring(0, thisDmidecodeMemoryDeviceProperty.indexOf(" ")));
                                            } else if (thisDmidecodeMemoryDeviceProperty.endsWith(" kB")) {
                                                thisMemoryBankBytes = Long.parseLong(thisDmidecodeMemoryDeviceProperty.substring(0, thisDmidecodeMemoryDeviceProperty.indexOf(" "))) * 1024;
                                            } else if (thisDmidecodeMemoryDeviceProperty.endsWith(" MB")) {
                                                thisMemoryBankBytes = Long.parseLong(thisDmidecodeMemoryDeviceProperty.substring(0, thisDmidecodeMemoryDeviceProperty.indexOf(" "))) * 1024 * 1024;
                                            } else if (thisDmidecodeMemoryDeviceProperty.endsWith(" GB")) {
                                                thisMemoryBankBytes = Long.parseLong(thisDmidecodeMemoryDeviceProperty.substring(0, thisDmidecodeMemoryDeviceProperty.indexOf(" "))) * 1024 * 1024 * 1024;
                                            } else if (thisDmidecodeMemoryDeviceProperty.endsWith(" TB")) {
                                                thisMemoryBankBytes = Long.parseLong(thisDmidecodeMemoryDeviceProperty.substring(0, thisDmidecodeMemoryDeviceProperty.indexOf(" "))) * 1024 * 1024 * 1024 * 1024;
                                            } else if (thisDmidecodeMemoryDeviceProperty.endsWith(" PB")) {
                                                thisMemoryBankBytes = Long.parseLong(thisDmidecodeMemoryDeviceProperty.substring(0, thisDmidecodeMemoryDeviceProperty.indexOf(" "))) * 1024 * 1024 * 1024 * 1024 * 1024;
                                            } else if (thisDmidecodeMemoryDeviceProperty.endsWith(" EB")) {
                                                thisMemoryBankBytes = Long.parseLong(thisDmidecodeMemoryDeviceProperty.substring(0, thisDmidecodeMemoryDeviceProperty.indexOf(" "))) * 1024 * 1024 * 1024 * 1024 * 1024 * 1024;
                                            } else if (thisDmidecodeMemoryDeviceProperty.endsWith(" ZB")) {
                                                thisMemoryBankBytes = Long.parseLong(thisDmidecodeMemoryDeviceProperty.substring(0, thisDmidecodeMemoryDeviceProperty.indexOf(" "))) * 1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024;
                                            }

                                            if (thisMemoryBankBytes > 0) {
                                                ramTotalBytesFromBanks += thisMemoryBankBytes;

                                                String thisMemoryBankGB = new DecimalFormat("#").format((thisMemoryBankBytes / 1024 / 1024 / 1024)) + " GB";

                                                if (ramSlots.isEmpty()) {
                                                    ramSlots = thisMemoryBankGB;
                                                } else {
                                                    ramSlots += " + " + thisMemoryBankGB;
                                                }
                                            } else {
                                                if (ramSlots.isEmpty()) {
                                                    ramSlots = thisDmidecodeMemoryDeviceProperty;
                                                } else {
                                                    ramSlots += " + " + thisDmidecodeMemoryDeviceProperty;
                                                }
                                            }
                                        } catch (NumberFormatException ramBankSizeException) {
                                            if (isTestMode) {
                                                System.out.println("ramBankSizeException: " + ramBankSizeException);
                                            }
                                        }
                                    }
                                } else if (ramType.isEmpty() && thisDmidecodeMemoryDeviceLine.startsWith("\tType:") && thisDmidecodeMemoryDeviceProperty.contains("DDR")) {
                                    ramType = " " + thisDmidecodeMemoryDeviceProperty;
                                } else if (ramSpeed.isEmpty() && thisDmidecodeMemoryDeviceLine.startsWith("\tSpeed:") && (thisDmidecodeMemoryDeviceProperty.contains(" MT/s") || thisDmidecodeMemoryDeviceProperty.contains(" MHz"))) {
                                    ramSpeed = " @ " + thisDmidecodeMemoryDeviceProperty.replace(" MT/s", " MHz").replace("32 ", "33 ").replace("34 ", "33 ").replace("67 ", "66 "); // MT/s is actually more correct, but MHz is what people are used to. Also, replace any MHz speeds that end with "32" or "34" to "33" and "67" to "66" to account for rounding differences (such as "1332 MHz" vs "1333 MHz" and "2667 MHz" vs "2666 MHz" etc) that would make the software show a slightly different number than what would be on the actual RAM label.
                                } else if (ramVoltage.isEmpty() && thisDmidecodeMemoryDeviceLine.startsWith("\tConfigured Voltage:") && thisDmidecodeMemoryDeviceProperty.contains(" V")) {
                                    ramVoltage = thisDmidecodeMemoryDeviceProperty;
                                }
                            }
                        }
                    }
                }

                // Use lsmem for Total RAM (because dmidecode won't get any RAM info if we don't have admin password and it's more accurate than free and lshw)
                String lsmemTotalOutput = new CommandReader(new String[]{"/usr/bin/lsmem", "-b"}).getFirstOutputLineContaining("Total online memory:");
                if (lsmemTotalOutput.contains(": ")) {
                    String lsmemTotalBytesString = lsmemTotalOutput.substring(lsmemTotalOutput.indexOf(": ") + 2).trim();

                    try {
                        long totalRAMbytes = Long.parseLong(lsmemTotalBytesString);

                        if (ramMaxBytes > 0L && ramMaxBytes >= totalRAMbytes) {
                            maxRAM = " - " + new DecimalFormat("#").format((ramMaxBytes / 1024 / 1024 / 1024)) + " GB Max";
                        }

                        ram = new DecimalFormat("#").format((totalRAMbytes / 1024 / 1024 / 1024)) + " GB";
                    } catch (NumberFormatException ramSizeException) {
                        if (isTestMode) {
                            System.out.println("ramSizeException: " + ramSizeException);
                        }
                    }
                }

                // Use lscpu for Processor info instead of dmidecode or lshw because it always loads full info without admin.
                String cpuThreadsPerCore = "";

                try {
                    // Start up a "yes" process and sleep a half second to get an accurate cpuCurrentSpeedString
                    Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "/usr/bin/yes > /dev/null & /bin/sleep 0.5"}).waitFor();
                } catch (IOException | InterruptedException yesForLscpuException) {
                    if (isTestMode) {
                        System.out.println("yesForLscpuException: " + yesForLscpuException);
                    }
                }

                double cpuCurrentSpeedMHz = -1.0; // Initialize to "-1" to be able to determine if the "CPU MHz:" doesn't exist at all.

                for (int lscpuAttempt = 0; lscpuAttempt < 2; lscpuAttempt++) {
                    // Re-attempt if cpuCurrentSpeedMHz is less that 1000 MHz
                    String[] lscpuLines = new CommandReader(new String[]{"/usr/bin/lscpu"}).getOutputLines();
                    String[] lscpuCoreLines = new CommandReader(new String[]{"/usr/bin/lscpu", "-p=SOCKET,CPU,MHZ,MAXMHZ,CORE"}).getOutputLinesNotContaining("#"); // Ignore header lines that start with "#"

                    for (String thisLscpuLine : lscpuLines) {
                        if (thisLscpuLine.contains(": ")) {
                            String thisLscpuProperty = thisLscpuLine.substring(thisLscpuLine.indexOf(": ") + 2).trim();

                            if (cpuThreadsPerCore.isEmpty() && thisLscpuLine.startsWith("Thread(s) per core:")) {
                                cpuThreadsPerCore = thisLscpuProperty;
                            } else if (cpuCores.isEmpty() && thisLscpuLine.startsWith("Core(s) per socket:")) {
                                cpuCores = thisLscpuProperty;
                            } else if (cpuCount.isEmpty() && thisLscpuLine.startsWith("Socket(s):")) {
                                // TODO: Test this with DUAL PROC system (with and without 2nd proc installed).
                                cpuCount = thisLscpuProperty;
                            } else if (cpuBrand.isEmpty() && thisLscpuLine.startsWith("Vendor ID:")) {
                                cpuBrand = cleanCPU(thisLscpuProperty);
                            } else if (cpu.equals("N/A") && thisLscpuLine.startsWith("Model name:") && !thisLscpuProperty.equals("-")) {
                                cpu = cleanCPU(thisLscpuProperty);
                            } else if (thisLscpuLine.startsWith("CPU MHz:")) {
                                // Update this even if it's already filled because the first time it was below 1000 MHz
                                try {
                                    cpuCurrentSpeedMHz = Double.parseDouble(thisLscpuProperty);
                                } catch (NumberFormatException cpuCurrentSpeedMHzException) {
                                    if (isTestMode) {
                                        System.out.println("cpuCurrentSpeedMHzException: " + cpuCurrentSpeedMHzException);
                                    }
                                }
                            } else if (cpuMaxSpeedString.isEmpty() && thisLscpuLine.startsWith("CPU max MHz:")) {
                                cpuMaxSpeedString = thisLscpuProperty + " MHz";
                            }
                        }
                    }

                    // Starting with 12th Gen Intel processors, there could be Performance (P) and Efficiency (E) cores, so check each core max speeds to get the P+E core breakdown.
                    // Don't determine P cores by hyperthreading since there are some CPUs where the P cores are not hyperthreaded such as: https://ark.intel.com/content/www/us/en/ark/products/226264/intel-celeron-processor-7305-8m-cache-1-10-ghz.html
                    HashMap<String, HashMap<String, ArrayList<String>>> cpuCoresInfo = new HashMap<>();
                    HashMap<String, Integer> cpuCoresCurrentSpeeds = new HashMap<>();
                    for (String thisLscpuCoreLine : lscpuCoreLines) {
                        String[] thisCpuCoreLineParts = thisLscpuCoreLine.split(",");
                        if (thisCpuCoreLineParts.length == 5) {
                            String thisSocketID = thisCpuCoreLineParts[0];
                            if (thisSocketID.equals("0")) { // Only ever examine SOCKET #0 because if there are multiple CPUs they should match and the P+E core counts should be the same.
                                String thisCoreMaxMHzString = thisCpuCoreLineParts[3];
                                if (thisCoreMaxMHzString.isEmpty()) {
                                    thisCoreMaxMHzString = "0";
                                }

                                if (!cpuCoresInfo.containsKey(thisCoreMaxMHzString)) {
                                    cpuCoresInfo.put(thisCoreMaxMHzString, new HashMap<>());
                                }

                                String thisCoreID = thisCpuCoreLineParts[4];

                                if (!cpuCoresInfo.get(thisCoreMaxMHzString).containsKey(thisCoreID)) {
                                    cpuCoresInfo.get(thisCoreMaxMHzString).put(thisCoreID, new ArrayList<>());
                                }

                                cpuCoresInfo.get(thisCoreMaxMHzString).get(thisCoreID).add(thisCpuCoreLineParts[1]);

                                String thisCoreCurrentMHzString = thisCpuCoreLineParts[2];
                                if (thisCoreCurrentMHzString.isEmpty()) {
                                    thisCoreCurrentMHzString = "0";
                                }

                                Integer thisCurrentSpeedCount = 1;
                                if (cpuCoresCurrentSpeeds.containsKey(thisCoreCurrentMHzString)) {
                                    thisCurrentSpeedCount = (cpuCoresCurrentSpeeds.get(thisCoreCurrentMHzString) + 1);
                                }
                                cpuCoresCurrentSpeeds.put(thisCoreCurrentMHzString, thisCurrentSpeedCount);
                            }
                        }
                    }

                    ArrayList<String> cpuCoreMaxSpeedStrings = new ArrayList<>(cpuCoresInfo.keySet());
                    cpuPandEcoresString = "";
                    if (cpuCoreMaxSpeedStrings.size() > 1) {
                        Collections.sort(cpuCoreMaxSpeedStrings, (String thisCoreMaxSpeed, String thatCoreMaxSpeed) -> {
                            try {
                                return Double.compare(Double.parseDouble(thatCoreMaxSpeed), Double.parseDouble(thisCoreMaxSpeed));
                            } catch (NumberFormatException coreMaxSpeedStringToDoubleException) {
                                if (isTestMode) {
                                    System.out.println("coreMaxSpeedStringToDoubleException: " + coreMaxSpeedStringToDoubleException);
                                }

                                return 0;
                            }
                        });

                        for (String thisMaxSpeedString : cpuCoreMaxSpeedStrings) {
                            HashMap<String, ArrayList<String>> thisMaxSpeedCores = cpuCoresInfo.get(thisMaxSpeedString);
                            int thisMaxSpeedCoreCount = thisMaxSpeedCores.keySet().size();
                            if (cpuPandEcoresString.isEmpty()) { // P cores will always be first in the sorted "cpuCoreMaxSpeeds" list.
                                cpuPandEcoresString = thisMaxSpeedCoreCount + "P"; // TODO: Maybe add some indication if P cores are also HT?
                            } else {
                                cpuPandEcoresString += " + " + thisMaxSpeedCoreCount + "E";
                            }

                            if (!thisMaxSpeedString.equals("0")) {
                                try {
                                    String thisMaxGHzString = new DecimalFormat("#.#").format((Double.parseDouble(thisMaxSpeedString) / 1000)) + " GHz Max";
                                    if (!thisMaxGHzString.startsWith("0 ")) {
                                        cpuPandEcoresString += " @ " + thisMaxGHzString;
                                    }
                                } catch (NumberFormatException thisCoreMaxMHzToGHzException) {
                                    if (isTestMode) {
                                        System.out.println("thisCoreMaxMHzToGHzException: " + thisCoreMaxMHzToGHzException);
                                    }
                                }
                            }
                        }
                    }

                    if (cpuCurrentSpeedMHz < 0) { // If "cpuCurrentSpeedMHz" was not set from "lscpu" output, use the "lscpu -e" output to get the most common current speed from each core.
                        String mostCommonCurrentCoreSpeed = "";
                        Integer mostCommonCurrentCoreSpeedCount = 0;
                        for (HashMap.Entry<String, Integer> thisCurrentCoreSpeedAndCount : cpuCoresCurrentSpeeds.entrySet()) {
                            Integer thisCoreCurrentSpeedCount = thisCurrentCoreSpeedAndCount.getValue();

                            if (thisCoreCurrentSpeedCount > mostCommonCurrentCoreSpeedCount) {
                                mostCommonCurrentCoreSpeed = thisCurrentCoreSpeedAndCount.getKey();
                                mostCommonCurrentCoreSpeedCount = thisCoreCurrentSpeedCount;
                            }
                        }

                        try {
                            cpuCurrentSpeedMHz = Double.parseDouble(mostCommonCurrentCoreSpeed);
                        } catch (NumberFormatException mostCommonCurrentCoreSpeedException) {
                            if (isTestMode) {
                                System.out.println("mostCommonCurrentCoreSpeedException: " + mostCommonCurrentCoreSpeedException);
                            }
                        }
                    }

                    if ((cpuCurrentSpeedMHz < 0) || (cpuCurrentSpeedMHz >= 1000)) {
                        break;
                    } else {
                        try {
                            // Start up ANOTHER "yes" process and sleep a half second to get an accurate cpuCurrentSpeedString
                            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "/usr/bin/yes > /dev/null & /bin/sleep 0.5"}).waitFor();
                        } catch (IOException | InterruptedException anotherYesForLscpuException) {
                            if (isTestMode) {
                                System.out.println("anotherYesForLscpuException: " + anotherYesForLscpuException);
                            }
                        }
                    }
                }

                try {
                    Runtime.getRuntime().exec(new String[]{"/usr/bin/killall", "yes"}); // Kill all "yes" processes since we're done with lscpu
                } catch (IOException killallYesForLscpuException) {
                    if (isTestMode) {
                        System.out.println("killallYesForLscpuException: " + killallYesForLscpuException);
                    }
                }

                if (cpuCurrentSpeedMHz > 0) {
                    cpuCurrentSpeedString = cpuCurrentSpeedMHz + " MHz";
                }

                if (!cpuCores.isEmpty() && !cpuThreadsPerCore.isEmpty()) {
                    try {
                        cpuThreads = Integer.toString(Integer.parseInt(cpuCores) * Integer.parseInt(cpuThreadsPerCore));
                    } catch (NumberFormatException cpuThreadsException) {
                        if (isTestMode) {
                            System.out.println("cpuThreadsException: " + cpuThreadsException);
                        }
                    }

                }

                // Use lsblk for Hard Drives (because it loads without admin and detects NVMe drives, which lshw doesn't)
                String[] hardDrives = new CommandReader(new String[]{"/bin/lsblk", "-abdPpo", "NAME,SIZE,TRAN,ROTA,TYPE,RM,RO,SERIAL,VENDOR,MODEL", "-x", "NAME"}).getOutputLines();

                ArrayList<String> internalDriveLogicalNames = new ArrayList<>();

                if (hardDrives.length > 0) {
                    for (String thisHardDrive : hardDrives) {
                        String[] thisHardDriveParts = thisHardDrive.split("\"", -1); // Set -1 limit to include trailing empty elements since it may be possible for the model to be an empty string.
                        // Split lines on double quotes (") to easily extract each value out of each "lsblk" line, which will be like: NAME="/dev/sda" SIZE="1234567890" TRAN="sata" ROTA="0" TYPE="disk" RM="0" RO="0" SERIAL="ABC123" VENDOR="Some Brand" MODEL="Some Model Name"
                        if (thisHardDriveParts.length >= 21) {
                            int thisDriveValueIndex = 1;
                            String thisDriveFullID = thisHardDriveParts[thisDriveValueIndex];
                            thisDriveValueIndex += 2;
                            String thisDriveSizeBytes = thisHardDriveParts[thisDriveValueIndex];
                            thisDriveValueIndex += 2;
                            String thisDriveTransport = thisHardDriveParts[thisDriveValueIndex];
                            thisDriveValueIndex += 2;
                            String thisDriveRotational = thisHardDriveParts[thisDriveValueIndex];
                            thisDriveValueIndex += 2;
                            String thisDriveType = thisHardDriveParts[thisDriveValueIndex];
                            thisDriveValueIndex += 2;
                            String thisDriveRemovable = thisHardDriveParts[thisDriveValueIndex];
                            thisDriveValueIndex += 2;
                            String thisDriveReadOnly = thisHardDriveParts[thisDriveValueIndex];
                            thisDriveValueIndex += 2;
                            String thisDriveSerial = thisHardDriveParts[thisDriveValueIndex].replaceAll("^[. /]+", "").replaceAll("[. /]+$", ""); // Trim all leading and trailing spaces, periods, and slashes.
                            thisDriveValueIndex += 2;
                            String thisDriveBrand = thisHardDriveParts[thisDriveValueIndex];
                            thisDriveValueIndex += 2;

                            // Only list DISKs that have a TRANsport type of SATA or ATA or NVMe or MMC (for eMMC embedded Memory Cards, which is confirmed below).
                            if (thisDriveType.equals("disk") && thisDriveRemovable.equals("0") && thisDriveReadOnly.equals("0") && !thisDriveSizeBytes.equals("") && (thisDriveTransport.endsWith("ata") || thisDriveTransport.equals("nvme") || thisDriveTransport.equals("mmc"))) {
                                try {
                                    double thisDiskBytes = Double.parseDouble(thisDriveSizeBytes);

                                    boolean mmcIsEmbedded = false;
                                    if (thisDriveTransport.equals("mmc")) {
                                        String mmcType = new CommandReader(new String[]{"/bin/udevadm", "info", "--query", "property", "--property", "MMC_TYPE", "--value", "-p", ("/sys/class/block/" + thisDriveFullID.replace("/dev/", ""))}).getFirstOutputLine();
                                        if (mmcType.equals("MMC") || (mmcType.isEmpty() && !new CommandReader(new String[]{"/bin/udevadm", "info", "--query", "symlink", "-p", ("/sys/class/block/" + thisDriveFullID.replace("/dev/", ""))}).getFirstOutputLine().contains("/by-id/mmc-USD_"))) {
                                            // Only show eMMC which should have "MMC_TYPE" of "MMC" rather than "SD" (regular Memory Cards can still show as non-removable from "lsblk" though).
                                            // Or, if "MMC_TYPE" doesn't exist (on older versions of "udevadm"?), eMMC should have some UDEV ID starting with other than "USD_" which would indicate an actual Memory Card.
                                            mmcIsEmbedded = true;
                                        }
                                    }

                                    if ((thisDiskBytes > 1000000) && (!thisDriveTransport.equals("mmc") || mmcIsEmbedded)) {
                                        // 2016/2017 MacBooks can have 8 KB secondary NVMe Namespace (nvme0n2) that we want to ignore, unsure exactly why it exists at all.
                                        // Only show eMMC which should have "MMC_TYPE" of "MMC" rather than "SD" (regular Memory Cards can still show as non-removable from "lsblk" though).

                                        boolean thisDiskIsNVMe = thisDriveTransport.equals("nvme");

                                        double thisDriveGB = (thisDiskBytes / 1000 / 1000 / 1000);
                                        String thisDriveSize;
                                        if (thisDriveGB < 1000) {
                                            thisDriveSize = new DecimalFormat("#").format(thisDriveGB) + " GB";
                                        } else {
                                            thisDriveSize = new DecimalFormat("#").format((thisDriveGB / 1000)) + " TB";
                                        }

                                        String thisDisk = thisDriveSize + " " + (thisDiskIsNVMe ? "NVMe" : (thisDriveTransport.equals("mmc") ? "eMMC" : (thisDriveRotational.equals("0") ? "SSD" : "HDD"))); // Only internal embedded MMC drives will be detected based on conditions above, so display them as "eMMC".

                                        String thisDriveModel = String.join(" ", Arrays.copyOfRange(thisHardDriveParts, thisDriveValueIndex, thisHardDriveParts.length)).trim().replace("_", " ").replace("(", "").replace(")", "").replaceAll("\\s{2,}", " ");
                                        // NOTES:
                                        // - I don't know if it's possible for models to contain double quotes ("), but if they do there will be more elements at the end of the "thisHardDriveParts" that need to be joined back together to re-create the
                                        //   full model name which is what is being done with "Arrays.copyOfRange", but just re-join with spaces to not need to worry about removing a trailing quotation mark since there will always be an final empty element in the array.
                                        //
                                        // - Replace all underscores with spaces since "lsblk" version 2.34 (which shipped with Mint 20.X) seems to include them where spaces should be, but version 2.37.2 which shipped with Mint 21.X properly has spaces instead of underscore.
                                        //   Even though we're currently installing Mint 21.1, still replace them just in case it's still needed for some drive models that I haven't seen in my testing.
                                        //
                                        // - Remove all parens (which shouldn't normally exist anyways) to not break removing models when truncating storage line for PCsCRM.
                                        //
                                        // - Prior to "lsblk" (part of "util-linux") version 2.33, truncated model names would be retrieved from from sysfs ("/sys/block/<name>/device/model"), but the full model name could still be retrieved manually from from "hdparm".
                                        //   But, starting with version 2.33, "lsblk" retrieves full model names from "udev": https://github.com/util-linux/util-linux/blob/master/Documentation/releases/v2.33-ReleaseNotes#L394
                                        //   Mint 20 was the first to ship with "lsblk" version 2.34 while Mint 19.3 shipped with version 2.31.1 which still retrieved truncated drive model names.
                                        //   Since we haven't installed Mint 19.3 for multiple years, just use the model name from "lsblk" since it will always be the full model for our usage.
                                        //   (If other companies using QA Helper are still running Mint 19.X, then truncated model names will still be retrieved on those systems but since Mint 19.X is EOL April 2023 it shouldn't really be getting installed anymore anyways.)

                                        thisDriveBrand = thisDriveBrand.trim().replace("_", " ").replace("(", "").replace(")", "").replaceAll("\\s{2,}", " ");
                                        if (!thisDriveBrand.isEmpty()) {
                                            String thisDriveBrandUppercase = thisDriveBrand.toUpperCase();
                                            if (!thisDriveBrandUppercase.equals("GENERAL") && !thisDriveBrandUppercase.equals("ATA")) { // TODO: Find and ignore other generic VENDOR strings.
                                                if (thisDriveModel.isEmpty()) {
                                                    thisDriveModel = thisDriveBrand;
                                                } else if (!thisDriveModel.toUpperCase().contains(thisDriveBrandUppercase)) {
                                                    thisDriveModel = thisDriveBrand + " " + thisDriveModel;
                                                }
                                            }
                                        }

                                        if (!thisDriveModel.isEmpty()) {
                                            thisDisk += " (" + thisDriveModel + ")";
                                        }

                                        storageArray.add(thisDisk);

                                        if (!thisDriveSerial.isEmpty()) {
                                            storageSerialsArray.add(thisDriveSerial);
                                        }

                                        if (thisDiskIsNVMe && thisDriveFullID.startsWith("/dev/nvme")) {
                                            // This NVMe drive logical name will be like "/dev/nvme0n1", but we need it to just be like "/dev/nvme0" to match against the logical drive names from HD Sentinel.
                                            // So, split on n's and only use index 1 to always get the right identifier even if there isn't a 2nd n portion at the end.
                                            internalDriveLogicalNames.add("/dev/n" + thisDriveFullID.split("n")[1]);
                                        } else {
                                            internalDriveLogicalNames.add(thisDriveFullID);
                                        }
                                    }
                                } catch (NumberFormatException hardDriveSizeException) {
                                    if (isTestMode) {
                                        System.out.println("diskDriveSizeException: " + hardDriveSizeException);
                                    }
                                }
                            }
                        }
                    }
                }

                // Wireless and Ethernet MAC for Linux
                String[] allNetworkDeviceInfo = new CommandReader(new String[]{"/usr/bin/nmcli", "-f", "GENERAL.VENDOR,GENERAL.PRODUCT,GENERAL.HWADDR,GENERAL.TYPE", "device", "show"}).getOutputLines();

                String thisNetworkDeviceBrand = "";
                String thisNetworkDeviceModel = "";
                String thisNetworkDeviceMAC = "";

                for (String thisNetworkDeviceInfoLine : allNetworkDeviceInfo) {
                    if (thisNetworkDeviceInfoLine.startsWith("GENERAL.VENDOR:")) {
                        thisNetworkDeviceBrand = cleanDeviceBrand(thisNetworkDeviceInfoLine.substring(thisNetworkDeviceInfoLine.indexOf(":") + 1).trim()).replace("(", "[").replace(")", "]");
                        if (thisNetworkDeviceBrand.equals("--")) { // "nmcli" will fill empty values with "--" (example: http://polarhome.com/service/man/?qf=nmcli-examples&af=0&tf=2&of=RedHat)
                            thisNetworkDeviceBrand = "";
                        }
                    } else if (thisNetworkDeviceInfoLine.startsWith("GENERAL.PRODUCT:")) {
                        thisNetworkDeviceModel = cleanDeviceModel(thisNetworkDeviceInfoLine.substring(thisNetworkDeviceInfoLine.indexOf(":") + 1).trim()).replace("(", "[").replace(")", "]");
                        if (thisNetworkDeviceModel.equals("--")) { // "nmcli" will fill empty values with "--" (example: http://polarhome.com/service/man/?qf=nmcli-examples&af=0&tf=2&of=RedHat)
                            thisNetworkDeviceModel = "";
                        }
                    } else if (thisNetworkDeviceInfoLine.startsWith("GENERAL.HWADDR:")) {
                        thisNetworkDeviceMAC = thisNetworkDeviceInfoLine.substring(thisNetworkDeviceInfoLine.indexOf(":") + 1).trim().toUpperCase();
                    } else if (thisNetworkDeviceInfoLine.startsWith("GENERAL.TYPE:")) {
                        String thisNetworkDeviceType = thisNetworkDeviceInfoLine.substring(thisNetworkDeviceInfoLine.indexOf(":") + 1).trim().toLowerCase();
                        if (thisNetworkDeviceType.equals("ethernet")) {
                            ethernetMACarray.add(thisNetworkDeviceMAC);
                        } else if (thisNetworkDeviceType.equals("wifi") && !getWireless().contains("Wi-Fi Detected")) {
                            String thisWifiInfo = "Wi-Fi Detected";

                            if (!thisNetworkDeviceModel.isEmpty()) {
                                thisWifiInfo += " (" + ((thisNetworkDeviceBrand.isEmpty() || thisNetworkDeviceModel.startsWith(thisNetworkDeviceBrand)) ? thisNetworkDeviceModel : (thisNetworkDeviceBrand + " " + thisNetworkDeviceModel)) + ")";
                            } else if (!thisNetworkDeviceBrand.isEmpty()) {
                                thisWifiInfo += " (" + thisNetworkDeviceBrand + ")";
                            }

                            wirelessArray.add(thisWifiInfo);
                        }

                        thisNetworkDeviceBrand = "";
                        thisNetworkDeviceModel = "";
                        thisNetworkDeviceMAC = "";
                    }
                }

                // Cellular for Linux
                // NOTE: I have seen some cellular cards not show immediately on boot take a minute or two to be initialized and recognized via "mmcli" (or "nmcli"),
                // and running "mmcli --scan-modems" doesn't seem to help, so "lscpi" will also be used below as a fallback to check for cellular cards.
                String[] cellularDeviceInfo = new CommandReader(new String[]{"/usr/bin/mmcli", "--list-modems"}).getOutputLines();
                for (String thisCellularDeviceInfoLine : cellularDeviceInfo) {
                    thisCellularDeviceInfoLine = thisCellularDeviceInfoLine.trim().replaceAll("\\s{2,}", " ");
                    if (thisCellularDeviceInfoLine.contains("/Modem/")) {
                        // The modem line will be like: /org/freedesktop/ModemManager1/Modem/0 [Modem Manufacturer] Modem Model
                        String modemModelName = thisCellularDeviceInfoLine.substring(thisCellularDeviceInfoLine.indexOf(" ") + 1);
                        if (modemModelName.contains("] ")) { // Remove Modem Manufacturer in square brackets.
                            modemModelName = modemModelName.substring(modemModelName.indexOf("] ") + 2);
                        }

                        modemModelName = cleanDeviceModel(modemModelName);

                        wirelessArray.add("Cellular Detected" + ((modemModelName.isEmpty() || modemModelName.contains("model unknown")) ? "" : " (" + modemModelName + ")"));
                    }
                }

                if (pciIDsDbTempPath == null) {
                    try (InputStream pciIDsDbInputStream = this.getClass().getClassLoader().getResourceAsStream("Resources/pci.ids")) {
                        File pciIDsDbTempFile = File.createTempFile("qa_helper-pci", ".ids");
                        pciIDsDbTempFile.deleteOnExit();

                        try (FileOutputStream pciIDsDbOutputStream = new FileOutputStream(pciIDsDbTempFile)) {
                            byte[] buf = new byte[1024];
                            int bytesRead;

                            while ((bytesRead = pciIDsDbInputStream.read(buf)) > 0) {
                                pciIDsDbOutputStream.write(buf, 0, bytesRead);
                            }

                            pciIDsDbTempPath = pciIDsDbTempFile.getPath();
                        } catch (Exception writePCIidsDbException) {
                            if (isTestMode) {
                                System.out.println("writePCIidsDbException: " + writePCIidsDbException);
                            }
                        }
                    } catch (Exception readPCIidsDbException) {
                        if (isTestMode) {
                            System.out.println("readPCIidsDbException: " + readPCIidsDbException);
                        }
                    }
                }

                // Use "lspci" instead of "lshw" for Audo and GPU because it can load SubClass names while "lshw" doesn't.
                // NOTE: "lscpi" will also be used below as a fallback to check for cellular cards as described above.
                ArrayList<String> lspciCommand = new ArrayList<>();
                lspciCommand.addAll(Arrays.asList("/usr/bin/lspci", "-vnnmm"));
                if (pciIDsDbTempPath != null) {
                    lspciCommand.addAll(Arrays.asList("-i", pciIDsDbTempPath));
                }

                String[] lspciLines = new CommandReader(lspciCommand.toArray(String[]::new)).getOutputLines();

                boolean isLspciGPU = false;
                boolean isLspciAudio = false;
                boolean isLspciWireless = false;
                String thisLspciVendorName = "";
                String thisLspciDeviceName = "";

                for (String thisLspciLine : lspciLines) {
                    if (thisLspciLine.contains(":\t")) {
                        String thisLspciProperty = thisLspciLine.substring(thisLspciLine.indexOf(":\t") + 2).trim();

                        if (thisLspciLine.startsWith("Class:")) {
                            // PCI Classes: https://pci-ids.ucw.cz/read/PD
                            String thisLspciPropertyEndingWithClass = thisLspciProperty.substring(0, thisLspciProperty.length() - 3);
                            if (thisLspciPropertyEndingWithClass.endsWith(" [03")) {
                                // Class: Display controller (03)
                                isLspciGPU = true;
                            } else if (thisLspciProperty.endsWith(" [0401]") || thisLspciProperty.endsWith(" [0403]")) {
                                // Class: Multimedia controller (04) - SubClass: Multimedia audio controller (01) OR Audio device (03)
                                isLspciAudio = true;
                            } else if (thisLspciPropertyEndingWithClass.endsWith(" [02") || thisLspciPropertyEndingWithClass.endsWith(" [0d")) {
                                // Class: Network controller (02)                                
                                // Class: Wireless controller (0d)
                                // Seems like "40" may be the subclass for cellular cards under Wireless "0d", but don't have enough data to know for sure.
                                isLspciWireless = true;
                            }
                        } else if (isLspciGPU || isLspciAudio || isLspciWireless) {
                            if (thisLspciLine.startsWith("Vendor:")) {
                                if (thisLspciProperty.startsWith("Vendor [")) {
                                    thisLspciVendorName = "Vendor " + thisLspciProperty.substring(thisLspciProperty.lastIndexOf("[") + 1, thisLspciProperty.lastIndexOf("]")).toUpperCase(); // Remove square brakets and keep Vendor code at the end when Vendor name not found.
                                } else {
                                    thisLspciVendorName = cleanDeviceBrand(thisLspciProperty.substring(0, thisLspciProperty.lastIndexOf(" ["))); // Cut off the Vendor code the end and clean when name found.
                                }
                            } else if (thisLspciLine.startsWith("Device:")) {
                                if (thisLspciProperty.startsWith("Device [")) { // This means Device was not found. Fall back to displaying Device code instead.
                                    thisLspciDeviceName = "Device " + thisLspciProperty.substring(thisLspciProperty.lastIndexOf("[") + 1, thisLspciProperty.lastIndexOf("]")).toUpperCase(); // Remove square brakets and keep Device code at the end when Device name not found.
                                } else {
                                    thisLspciDeviceName = cleanDeviceModel(thisLspciProperty.substring(0, thisLspciProperty.lastIndexOf(" ["))); // Cut off the Device code the end and clean when name found.
                                }
                            } else if (thisLspciLine.startsWith("SDevice:")) {
                                String thisLspciSubDeviceName = thisLspciDeviceName;

                                if (!thisLspciProperty.startsWith("Device [")) { // Omit the SubDevice when not found.
                                    thisLspciSubDeviceName = cleanDeviceModel(thisLspciProperty.substring(0, thisLspciProperty.lastIndexOf(" ["))); // Cut off the SubDevice code the end and clean when name found.
                                }

                                if (thisLspciDeviceName.contains(thisLspciVendorName) || thisLspciSubDeviceName.contains(thisLspciVendorName)) {
                                    thisLspciVendorName = "";
                                } else {
                                    thisLspciVendorName += " ";
                                }

                                String thisLspciDeviceFullName = thisLspciVendorName + (thisLspciDeviceName.equals(thisLspciSubDeviceName) ? thisLspciDeviceName : thisLspciDeviceName + " [" + thisLspciSubDeviceName + "]");

                                if (isLspciWireless) {
                                    String lowercaseLspciWirelessDeviceName = thisLspciDeviceFullName.toLowerCase();
                                    if (!getWireless().contains("Cellular Detected") && (lowercaseLspciWirelessDeviceName.contains("mobile") || lowercaseLspciWirelessDeviceName.contains("cellular") || lowercaseLspciWirelessDeviceName.contains("wwan") || lowercaseLspciWirelessDeviceName.replace("realtek", "").contains("lte"))) {
                                        wirelessArray.add("Cellular Detected (" + thisLspciDeviceFullName + ")");
                                    }
                                } else if (!thisLspciDeviceFullName.contains("802.11") && !thisLspciDeviceFullName.contains("Wireless Network") && !thisLspciDeviceFullName.contains("Intel Wireless") && !thisLspciDeviceFullName.contains("Centrino") && !thisLspciDeviceFullName.contains("Ethernet") && !thisLspciDeviceFullName.contains("Card Reader") && !thisLspciDeviceFullName.contains("MMC/SD") && !thisLspciDeviceFullName.contains("SDXC/MMC")) {
                                    // Ignore network and card reader devices that are getting caught from these classes and listed with actual GPU or Audio devices.

                                    if (isLspciGPU) {
                                        if (!gpuArray.contains(thisLspciDeviceFullName)) { // TODO: Should this check against getFullGPU() instead of gpuArray?
                                            gpuArray.add(thisLspciDeviceFullName);
                                        }
                                    } else if (isLspciAudio) {
                                        if (!audioArray.contains(thisLspciDeviceFullName)) { // TODO: Should this check against getFullAudio() instead of audioArray?
                                            audioArray.add(thisLspciDeviceFullName);
                                        }
                                    }
                                }

                                isLspciGPU = false;
                                isLspciAudio = false;
                                isLspciWireless = false;
                                thisLspciVendorName = "";
                                thisLspciDeviceName = "";
                            }
                        }
                    }
                }

                // Bluetooth for Linux
                String[] bluetoothInfo = new CommandReader(new String[]{"/usr/bin/btmgmt", "info"}).getOutputLinesContaining(new String[]{" version ", "supported settings:"});

                if (bluetoothInfo.length > 0) {
                    String bluetoothVersion = "";
                    String bluetoothFeatures = "";

                    for (String thisBluetoothInfoLine : bluetoothInfo) {
                        if (thisBluetoothInfoLine.trim().startsWith("addr ") && thisBluetoothInfoLine.contains(" version ")) {
                            String bluetoothHCIversion = thisBluetoothInfoLine.split(" version ")[1].split(" ")[0].replaceAll("[^0-9]", "");

                            if (!bluetoothHCIversion.isEmpty()) {
                                // HCI/LMP version references: https://support.microsoft.com/en-us/help/4524769/windows-10-what-bluetooth-version-is-on-my-device
                                String[] bluetoothHCIversionTranslation = {"1.0b", "1.1", "1.2", "2.0", "2.1", "3.0", "4.0", "4.1", "4.2", "5.0", "5.1", "5.2", "5.3", "5.4", "6.0"};

                                int bluetoothHCIversionInt = Integer.parseInt(bluetoothHCIversion);
                                if (bluetoothHCIversionTranslation.length > bluetoothHCIversionInt) {
                                    bluetoothVersion = " " + bluetoothHCIversionTranslation[bluetoothHCIversionInt] + " ";
                                } else {
                                    bluetoothVersion = " (HCI " + bluetoothHCIversion + ") ";
                                }
                            }
                        } else if (thisBluetoothInfoLine.trim().startsWith("supported settings:") && thisBluetoothInfoLine.contains(" le ")) {
                            bluetoothFeatures = " (Supports BLE)";
                        }
                    }

                    wirelessArray.add("Bluetooth" + bluetoothVersion + "Detected" + bluetoothFeatures);
                }

                // Use lshw for Disc Drives, and to detect Hard Drives formatted without a partition table
                // Must also load Storage class to be able to exclude drives connected via USB.
                // Volume class is loaded to catch Disks formatted without a partition table.
                ArrayList<String> lshwCommand = new ArrayList<>();
                lshwCommand.addAll(Arrays.asList("/usr/bin/lshw", "-xml", "-c", "storage", "-c", "disk", "-c", "volume"));

                // ALSO: Use lshw for systemProductName and ram in case we didn't get them using dmidecode and lsmem. This is useful for ARM procs (such as Raspberry Pi) which don't have dmidecode or lsmem.
                if (ram.equals("N/A")) {
                    lshwCommand.addAll(Arrays.asList("-c", "memory"));
                }
                if (systemProductName.equals("N/A")) {
                    lshwCommand.addAll(Arrays.asList("-c", "system"));
                }

                String lshwOutputXML;
                if (adminPassword.equals("*UNKNOWN*")) {
                    lshwOutputXML = new CommandReader(lshwCommand.toArray(String[]::new)).getOutputLinesAsString();
                } else {
                    lshwOutputXML = new CommandReader("printf '%s\\n' " + adminPasswordQuotedForShell + " | /usr/bin/sudo -Sk " + String.join(" ", lshwCommand)).getOutputLinesAsString();
                }

                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

                NodeList lshwNodeList;
                try {
                    lshwNodeList = documentBuilder.parse(new InputSource(new StringReader(lshwOutputXML))).getElementsByTagName("node");
                } catch (IOException | SAXException xmlParseException) {
                    throw new Exception("LSHW XML PARSE EXCEPTION: " + xmlParseException + "\n\nRAW LSHW OUTPUT:\n" + lshwOutputXML);
                }

                if (lshwNodeList != null && lshwNodeList.getLength() > 0) {
                    for (int i = 0; i < lshwNodeList.getLength(); i++) {
                        Node thisNode = lshwNodeList.item(i);

                        if (thisNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element thisElement = (Element) thisNode;
                            String thisElementClass = thisElement.getAttribute("class");
                            String thisElementID = thisElement.getAttribute("id");
                            if (thisElementID.contains(":")) {
                                thisElementID = thisElementID.substring(0, thisElementID.indexOf(":")); // Some IDs may be like "id:0", "id:1", etc. So lets just use the part before the colon.
                            }
                            if (thisElement.getAttribute("disabled").equals("true")) {
                                // Skip any disabled elements.
                            } else if (thisElementClass.equals("system") && !thisElement.getAttribute("handle").isEmpty()) {
                                if (systemProductName.equals("N/A")) {
                                    // system class is only loaded if systemProductName is N/A (see comment above).
                                    NodeList systemProductNodes = thisElement.getElementsByTagName("product");
                                    if (systemProductNodes != null && systemProductNodes.getLength() > 0 && systemProductNodes.item(0).getParentNode().isSameNode(thisElement)) {
                                        if (!systemProductNodes.item(0).getTextContent().isEmpty() && !ignoreSpecsPlaceholders(systemProductNodes.item(0).getTextContent())) {
                                            String possibleSystemProductName = systemProductNodes.item(0).getTextContent();

                                            String[] possibleSystemProductNameParts = possibleSystemProductName.split(" \\(");

                                            if (possibleSystemProductNameParts.length == 2) {
                                                if (!ignoreSpecsPlaceholders(possibleSystemProductNameParts[0])) {
                                                    systemProductName = possibleSystemProductNameParts[0].replaceAll("^[. /]+", "").replaceAll("[. /]+$", ""); // Trim all leading and trailing spaces, periods, and slashes.
                                                }

                                                String possibleSystemProductSKU = possibleSystemProductNameParts[1].substring(0, possibleSystemProductNameParts[1].length() - 1).replaceAll("^[. /]+", "").replaceAll("[. /]+$", ""); // Trim all leading and trailing spaces, periods, and slashes.

                                                if (!ignoreSpecsPlaceholders(possibleSystemProductSKU)) {
                                                    if (systemProductSKU.equals("N/A")) {
                                                        systemProductSKU = possibleSystemProductSKU;
                                                    } else if (!systemProductSKU.toLowerCase().equals(possibleSystemProductSKU.toLowerCase())) {
                                                        systemProductSKU += " / " + possibleSystemProductSKU;
                                                    }
                                                }
                                            } else {
                                                systemProductName = possibleSystemProductName;
                                            }
                                        }
                                    }
                                }
                            } else if (thisElementClass.equals("memory") && thisElementID.equals("memory")) {
                                if (ram.equals("N/A")) {
                                    // memory class is only loaded if ram is N/A (see comment above).
                                    NodeList memorySizeNodes = thisElement.getElementsByTagName("size"); // This may not exist, but don't bother with checking banks since if lsmem didn't work, then we probably wouldn't get them anyway.
                                    if (memorySizeNodes != null && memorySizeNodes.getLength() > 0 && memorySizeNodes.item(0).getParentNode().isSameNode(thisElement)) {
                                        try {
                                            ram = new DecimalFormat("#").format((Double.parseDouble(memorySizeNodes.item(0).getTextContent()) / 1024 / 1024 / 1024)) + " GB";
                                        } catch (NumberFormatException ramSizeException) {
                                            if (isTestMode) {
                                                System.out.println("ramSizeException: " + ramSizeException);
                                            }
                                        }
                                    }
                                }
                            } else if (thisElementClass.equals("disk") || thisElementClass.equals("volume")) {
                                // Also check for class=volume to be able to detect disks is formatted without a partition table.
                                Node thisParentNode = thisElement.getParentNode();
                                if (thisParentNode.getNodeType() == Node.ELEMENT_NODE) {
                                    if (!((Element) thisParentNode).getAttribute("id").contains("usb")) {
                                        // Ignore any USB devices
                                        if (thisElementID.equals("disk")) {
                                            if (thisElementClass.equals("volume")) {
                                                // If id=disk AND class=volume then this disk is formatted without a partition table.
                                                NodeList logicalNamesNodes = thisElement.getElementsByTagName("logicalname");

                                                if (logicalNamesNodes != null && logicalNamesNodes.getLength() > 0 && logicalNamesNodes.item(0).getParentNode().isSameNode(thisElement)) {
                                                    disksFormattedWithoutPartitionTable.add(logicalNamesNodes.item(0).getTextContent());
                                                }
                                            }
                                        } else if (thisElementID.equals("cdrom")) {
                                            String thisDiscDriveInfo = "";

                                            NodeList discDriveVendorNodes = thisElement.getElementsByTagName("vendor");
                                            if (discDriveVendorNodes != null && discDriveVendorNodes.getLength() > 0 && discDriveVendorNodes.item(0).getParentNode().isSameNode(thisElement)) {
                                                thisDiscDriveInfo = discDriveVendorNodes.item(0).getTextContent();
                                            }

                                            NodeList discDriveProductNodes = thisElement.getElementsByTagName("product");
                                            if (discDriveProductNodes != null && discDriveProductNodes.getLength() > 0 && discDriveProductNodes.item(0).getParentNode().isSameNode(thisElement)) {
                                                if (!thisDiscDriveInfo.isEmpty()) {
                                                    thisDiscDriveInfo += " ";
                                                }

                                                thisDiscDriveInfo += discDriveProductNodes.item(0).getTextContent().trim().replaceAll("\\s{2,}", " ");
                                            }

                                            if (!thisDiscDriveInfo.isEmpty()) {
                                                discDriveArray.add(thisDiscDriveInfo);
                                            }

                                            NodeList cdromLogicalNameNodes = thisElement.getElementsByTagName("logicalname");
                                            if (cdromLogicalNameNodes != null) {
                                                for (int j = 0; j < cdromLogicalNameNodes.getLength(); j++) {
                                                    if (cdromLogicalNameNodes.item(j).getParentNode().isSameNode(thisElement)) {
                                                        String thisCdromLogicalName = cdromLogicalNameNodes.item(j).getTextContent();
                                                        if (thisCdromLogicalName.startsWith("/dev/sr")) {
                                                            discDriveLogicalNames.add(thisCdromLogicalName);
                                                        }
                                                    }
                                                }
                                            }

                                            if (!hasDiscInDiscDrive) {
                                                NodeList cdromSettingNodes = thisElement.getElementsByTagName("setting");
                                                if (cdromSettingNodes != null) {
                                                    for (int j = 0; j < cdromSettingNodes.getLength(); j++) {
                                                        if (cdromSettingNodes.item(j).getParentNode().getParentNode().isSameNode(thisElement)) {
                                                            Node thisCdromSettingNode = cdromSettingNodes.item(j);
                                                            if (thisCdromSettingNode.getNodeType() == Node.ELEMENT_NODE) {
                                                                Element thisCdromSettingElement = (Element) thisCdromSettingNode;
                                                                if (thisCdromSettingElement.getAttribute("id").equals("status")) {
                                                                    String thisCdromStatus = thisCdromSettingElement.getAttribute("value"); // nodisc, open, ready
                                                                    hasDiscInDiscDrive = (thisCdromStatus.equals("ready"));
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (!discDriveCanBurnCDs || !discDriveCanBurnDVDs) {
                                                NodeList cdromCapabilityNodes = thisElement.getElementsByTagName("capability");
                                                if (cdromCapabilityNodes != null) {
                                                    for (int j = 0; j < cdromCapabilityNodes.getLength(); j++) {
                                                        if (cdromCapabilityNodes.item(j).getParentNode().getParentNode().isSameNode(thisElement)) {
                                                            String thisCdromCapability = cdromCapabilityNodes.item(j).getTextContent();
                                                            if (thisCdromCapability.endsWith(" burning")) {
                                                                if (!discDriveCanBurnCDs && thisCdromCapability.startsWith("CD")) {
                                                                    discDriveCanBurnCDs = true;
                                                                } else if (!discDriveCanBurnDVDs && thisCdromCapability.startsWith("DVD")) {
                                                                    discDriveCanBurnDVDs = true;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Drive Health (Hard Disk Sentinel)
                File hdSentinelOutputFile = new File(System.getProperty("java.io.tmpdir"), "qa_helper-hd_sentinel-output.xml");
                if (hdSentinelOutputFile.exists()) {
                    hdSentinelOutputFile.delete();
                }

                if (hdSentinelOutputFile.exists()) {
                    try {
                        Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "printf '%s\\n' " + adminPasswordQuotedForShell + " | /usr/bin/sudo -Sk /bin/rm " + hdSentinelOutputFile.getPath()}).waitFor();
                    } catch (IOException | InterruptedException deleteHDSentinalOutputException) {
                        if (isTestMode) {
                            System.out.println("deleteHDSentinalOutputException: " + deleteHDSentinalOutputException);
                        }
                    }
                }

                if (hdSentinelBinaryTempPath == null) {
                    try (InputStream hdSentinelInputStream = this.getClass().getClassLoader().getResourceAsStream("Resources/hdsentinel-020-x64")) {
                        File hdSentinelBinaryTempFile = File.createTempFile("qa_helper-hd_sentinel", "");
                        hdSentinelBinaryTempFile.deleteOnExit();

                        try (FileOutputStream hdSentinelOutputStream = new FileOutputStream(hdSentinelBinaryTempFile)) {
                            byte[] buf = new byte[1024];
                            int bytesRead;

                            while ((bytesRead = hdSentinelInputStream.read(buf)) > 0) {
                                hdSentinelOutputStream.write(buf, 0, bytesRead);
                            }

                            hdSentinelBinaryTempFile.setExecutable(true);
                            hdSentinelBinaryTempPath = hdSentinelBinaryTempFile.getPath();
                        } catch (Exception writeHDSentinelException) {
                            if (isTestMode) {
                                System.out.println("writeHDSentinelException: " + writeHDSentinelException);
                            }
                        }
                    } catch (Exception readHDSentinelException) {
                        if (isTestMode) {
                            System.out.println("readHDSentinelException: " + readHDSentinelException);
                        }
                    }
                }

                if (hdSentinelBinaryTempPath != null) {
                    String[] hdSentinelOutputLines = new CommandReader("printf '%s\\n' " + adminPasswordQuotedForShell + " | /usr/bin/sudo -Sk /usr/bin/timeout 15 '" + hdSentinelBinaryTempPath.replace("'", "'\\''") + "' -xml -r '" + hdSentinelOutputFile.getPath().replace("'", "'\\''") + "'").getOutputLines();
                    String hdSentinelTextOutput = "";

                    for (String thisHdSentinelOutputLine : hdSentinelOutputLines) {
                        if (!thisHdSentinelOutputLine.isEmpty() && !thisHdSentinelOutputLine.startsWith("Start with -r") && !thisHdSentinelOutputLine.startsWith("Examining hard disk") && !thisHdSentinelOutputLine.startsWith("Report file saved as:") && !thisHdSentinelOutputLine.startsWith("No hard disk devices found.")) {
                            if (hdSentinelTextOutput.isEmpty()) {
                                hdSentinelTextOutput = thisHdSentinelOutputLine;
                            } else if (thisHdSentinelOutputLine.startsWith("HDD Device")) {
                                hdSentinelTextOutput += "\n\n" + thisHdSentinelOutputLine;
                            } else if (thisHdSentinelOutputLine.startsWith("Temperature") || thisHdSentinelOutputLine.startsWith("Highest Temp.")) {
                                hdSentinelTextOutput += "\n" + thisHdSentinelOutputLine.replaceAll("[^A-Za-z0-9 :.]", ""); // Remove ° because it doesn't display correctly in pre-install environment (can't directly replace it because it is considered some different character in the pre-install environment).
                            } else {
                                hdSentinelTextOutput += "\n" + thisHdSentinelOutputLine;
                            }
                        }
                    }

                    if (!hdSentinelTextOutput.contains("HDD Device")) {
                        if (!adminPassword.equals("*UNKNOWN*")) {
                            if (hdSentinelTextOutput.isEmpty()) {
                                driveHealth = "FAILED to load Hard Disk Sentinel for unknown reasons.\n\nThis could mean that some installed drive is bad, or this may have happened because of some unrelated issue.\n\nIt is recommended to test all installed drives using other means before continuing.";
                                driveHealthWarning = true;
                            } else if (storageArray.isEmpty()) {
                                driveHealth = hdSentinelTextOutput + "\n\nNo hard drives installed or detected.";
                            } else {
                                driveHealth = hdSentinelTextOutput + "\n\nIt appears that Hard Disk Sentinel has failed to detect an installed " + (getFullStorage().contains("NVMe") ? "NVMe" : "hard") + " drive.\n\nIf you have other easy means to test the health of the installed drive, it is recommended to do so. If not, then this Drive Health test should be skipped.";
                                driveHealthWarning = true;
                            }
                        }
                    } else {
                        try {
                            NodeList hdSentinelNodeList = documentBuilder.parse(hdSentinelOutputFile.getPath()).getElementsByTagName("Hard_Disk_Device");
                            if (hdSentinelNodeList != null && hdSentinelNodeList.getLength() > 0) {
                                for (int i = 0; i < hdSentinelNodeList.getLength(); i++) {
                                    Node thisNode = hdSentinelNodeList.item(i);
                                    String thisDriveID = thisNode.getTextContent();

                                    if (internalDriveLogicalNames.contains(thisDriveID)) {
                                        hdSentinelTextOutput = hdSentinelTextOutput.replace(": " + thisDriveID + "\n", ": " + thisDriveID + " (INTERNAL)\n");

                                        if (!driveHealthWarning) {
                                            Element parentDriveElement = (Element) thisNode.getParentNode();

                                            NodeList drivePowerOnTimeNodes = parentDriveElement.getElementsByTagName("Power_on_time");
                                            if (drivePowerOnTimeNodes != null && drivePowerOnTimeNodes.getLength() > 0 && drivePowerOnTimeNodes.item(0).getParentNode().isSameNode(parentDriveElement)) {
                                                String drivePowerOnTimeText = drivePowerOnTimeNodes.item(0).getTextContent();

                                                if (!drivePowerOnTimeText.isEmpty() && drivePowerOnTimeText.contains(" days")) {
                                                    try {
                                                        if (Integer.parseInt(drivePowerOnTimeText.substring(0, drivePowerOnTimeText.indexOf(" days"))) >= 2500) {
                                                            driveHealthWarning = true;
                                                        }
                                                    } catch (NumberFormatException drivePowerOnTimeException) {
                                                        if (isTestMode) {
                                                            System.out.println("drivePowerOnTimeException: " + drivePowerOnTimeException);
                                                        }
                                                    }
                                                }
                                            }

                                            NodeList driveEstimatedRemainingLifetimeNodes = parentDriveElement.getElementsByTagName("Estimated_remaining_lifetime");
                                            if (driveEstimatedRemainingLifetimeNodes != null && driveEstimatedRemainingLifetimeNodes.getLength() > 0 && driveEstimatedRemainingLifetimeNodes.item(0).getParentNode().isSameNode(parentDriveElement)) {
                                                String driveEstimatedRemainingLifetimeText = driveEstimatedRemainingLifetimeNodes.item(0).getTextContent();

                                                if (!driveEstimatedRemainingLifetimeText.isEmpty()) {
                                                    if (!driveEstimatedRemainingLifetimeText.contains(" days")) {
                                                        driveHealthWarning = true;
                                                    } else {
                                                        try {
                                                            if (Integer.parseInt(driveEstimatedRemainingLifetimeText.substring(0, driveEstimatedRemainingLifetimeText.indexOf(" days")).replaceAll("[^0-9]", "")) < 400) {
                                                                driveHealthWarning = true;
                                                            }
                                                        } catch (NumberFormatException driveEstimatedRemainingLifetimeException) {
                                                            if (isTestMode) {
                                                                System.out.println("driveEstimatedRemainingLifetimeException: " + driveEstimatedRemainingLifetimeException);
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            NodeList driveDescriptionNodes = parentDriveElement.getElementsByTagName("Description");
                                            if (driveDescriptionNodes != null && driveDescriptionNodes.getLength() > 0 && driveDescriptionNodes.item(0).getParentNode().isSameNode(parentDriveElement)) {
                                                String driveDescriptionText = driveDescriptionNodes.item(0).getTextContent();
                                                if (!driveDescriptionText.isEmpty() && !driveDescriptionText.contains("is PERFECT.")) {
                                                    driveHealthWarning = true;
                                                }
                                            }

                                            NodeList driveTipNodes = parentDriveElement.getElementsByTagName("Tip");
                                            if (driveTipNodes != null && driveTipNodes.getLength() > 0 && driveTipNodes.item(0).getParentNode().isSameNode(parentDriveElement)) {
                                                String driveTipText = driveTipNodes.item(0).getTextContent();
                                                if (!driveTipText.isEmpty() && !driveTipText.equals("No actions needed.")) {
                                                    driveHealthWarning = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (IOException | NumberFormatException | DOMException | SAXException parseHDSentinalException) {
                            if (isTestMode) {
                                System.out.println("parseHDSentinalException: " + parseHDSentinalException);
                            }

                            driveHealthWarning = true;
                        }

                        driveHealth = hdSentinelTextOutput;
                    }

                    if (hdSentinelOutputFile.exists()) {
                        hdSentinelOutputFile.delete();
                    }

                    if (hdSentinelOutputFile.exists()) {
                        try {
                            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "printf '%s\\n' " + adminPasswordQuotedForShell + " | /usr/bin/sudo -Sk /bin/rm '" + hdSentinelOutputFile.getPath().replace("'", "'\\''") + "'"}).waitFor();
                        } catch (IOException | InterruptedException deleteHDSentinalOutputException) {
                            if (isTestMode) {
                                System.out.println("deleteHDSentinalOutputException: " + deleteHDSentinalOutputException);
                            }
                        }
                    }
                }

                // Battery Health for Linux
                String[] allPowerSources = new CommandReader(new String[]{"/usr/bin/upower", "-e"}).getOutputLines();

                for (String thisPowerSource : allPowerSources) {
                    if (!thisPowerSource.endsWith("/DisplayDevice")) { // Ignore the "DisplayDevice": https://upower.freedesktop.org/docs/UPower.html#UPower.GetDisplayDevice
                        String[] thisBatteryInfo = new CommandReader(new String[]{"/usr/bin/upower", "-i", thisPowerSource}).getOutputLines();

                        String thisBatteryVendor = "";
                        String thisBatteryModel = "";
                        String thisBatteryState = "";
                        String thisBatteryEnergy = "";
                        String thisBatteryEnergyFull = "";
                        String thisBatteryEnergyRate = "";
                        String thisBatteryVoltage = "";
                        String thisBatteryChargeCycles = "";
                        String thisBatteryPercentage = "";
                        String thisBatteryCapacity = "";

                        for (String thisBatteryInfoLine : thisBatteryInfo) {
                            if (thisBatteryInfoLine.startsWith("  vendor:")) {
                                thisBatteryVendor = thisBatteryInfoLine.substring(thisBatteryInfoLine.indexOf(":") + 1).trim();
                            } else if (thisBatteryInfoLine.startsWith("  model:")) {
                                thisBatteryModel = thisBatteryInfoLine.substring(thisBatteryInfoLine.indexOf(":") + 1).trim();
                            } else if (thisBatteryInfoLine.startsWith("    state:")) {
                                thisBatteryState = thisBatteryInfoLine.substring(thisBatteryInfoLine.indexOf(":") + 1).trim();
                            } else if (thisBatteryInfoLine.startsWith("    energy:")) {
                                thisBatteryEnergy = thisBatteryInfoLine.substring(thisBatteryInfoLine.indexOf(":") + 1).trim();
                            } else if (thisBatteryInfoLine.startsWith("    energy-full:")) {
                                thisBatteryEnergyFull = thisBatteryInfoLine.substring(thisBatteryInfoLine.indexOf(":") + 1).trim();
                            } else if (thisBatteryInfoLine.startsWith("    energy-rate:")) {
                                thisBatteryEnergyRate = thisBatteryInfoLine.substring(thisBatteryInfoLine.indexOf(":") + 1).trim();
                            } else if (thisBatteryInfoLine.startsWith("    voltage:")) {
                                try {
                                    double batteryVoltageDouble = Double.parseDouble(thisBatteryInfoLine.substring(thisBatteryInfoLine.indexOf(":") + 1).replaceAll("[^0-9.]", ""));
                                    thisBatteryVoltage = new DecimalFormat("#.#").format(batteryVoltageDouble) + " V";

                                    String thisBatteryDesignVoltage = batteryDesignVoltages.get(thisBatteryVendor + " " + thisBatteryModel);

                                    if (thisBatteryDesignVoltage != null) {
                                        double batteryDesignVoltageDouble = Double.parseDouble(thisBatteryDesignVoltage);

                                        if (batteryVoltageDouble < (batteryDesignVoltageDouble - 0.1)) {
                                            batteryHealthWarningArray.add("Low Voltage");
                                            thisBatteryVoltage += " of " + new DecimalFormat("#.#").format(batteryDesignVoltageDouble) + " V";
                                        }
                                    }
                                } catch (NumberFormatException batteryVoltageException) {
                                    thisBatteryVoltage = "";

                                    if (isTestMode) {
                                        System.out.println("batteryVoltageException: " + batteryVoltageException);
                                    }
                                }
                            } else if (thisBatteryInfoLine.startsWith("    charge-cycles:")) {
                                thisBatteryChargeCycles = thisBatteryInfoLine.substring(thisBatteryInfoLine.indexOf(":") + 1).trim();
                            } else if (thisBatteryInfoLine.startsWith("    percentage:")) {
                                thisBatteryPercentage = thisBatteryInfoLine.substring(thisBatteryInfoLine.indexOf(":") + 1).trim();
                            } else if (thisBatteryInfoLine.startsWith("    capacity:")) {
                                try {
                                    double thisBatteryCapacityDouble = Double.parseDouble(thisBatteryInfoLine.substring(thisBatteryInfoLine.indexOf(":") + 1).replaceAll("[^0-9.]", ""));
                                    thisBatteryCapacity = new DecimalFormat("#.#").format(thisBatteryCapacityDouble) + "%";

                                    if (thisBatteryCapacity.startsWith("0") || thisBatteryEnergyFull.startsWith("0")) { // Also make sure "energy-full" is non-zero to not incorrectly trust a "100%" capacity when the "energy-full" is zero. 
                                        thisBatteryCapacity = "NO POWER";
                                        batteryHealthErrorArray.add("No Power");
                                    } else if (thisBatteryCapacityDouble < 40) {
                                        batteryHealthErrorArray.add("Capacity < 40%");
                                    } else if (thisBatteryCapacityDouble < 60) {
                                        batteryHealthWarningArray.add("Capacity < 60%");
                                    } else if (thisBatteryCapacity.equals("100%") && brand.equals("HP")) { // Some HP firmwares list their design capacity as whatever the full charge capacity is always resulting in exactly "100%" capacity. Sometimes updating firmware can correct this.
                                        thisBatteryCapacity = "UNKNOWN CAPACITY";
                                        batteryHealthWarningArray.add("HP Firmware Update Required for Accurate Battery Percentage");
                                    }
                                } catch (NumberFormatException batteryCapacityException) {
                                    thisBatteryCapacity = "";

                                    if (isTestMode) {
                                        System.out.println("batteryCapacityException: " + batteryCapacityException);
                                    }
                                }
                            }
                        }

                        String thisBatteryCycleCount = "";

                        try {
                            int batteryCycleCount = Integer.parseInt(thisBatteryChargeCycles);
                            if (batteryCycleCount > 0) {
                                thisBatteryCycleCount = batteryCycleCount + " Cycle" + ((batteryCycleCount == 1) ? "" : "s");

                                if (batteryCycleCount >= 1000) {
                                    batteryHealthErrorArray.add("Cycles >= 1000");
                                } else if (batteryCycleCount >= 800) {
                                    batteryHealthWarningArray.add("Cycles >= 800");
                                }
                            }
                        } catch (NumberFormatException batteryCycleCountException) {
                            if (isTestMode) {
                                System.out.println("batteryCycleCountException: " + batteryCycleCountException);
                            }
                        }

                        if (thisBatteryEnergy.startsWith("0") && thisBatteryEnergyRate.startsWith("0") && thisBatteryPercentage.startsWith("0")) {
                            // Even if the capacity is not "0%", the battery may still be dead with no energy charging or discharging.
                            // Checking "startsWith("0")" because I have seen dead batteries show "energy" of "0.123 Wh" or "energy-rate" of "0.123 W".

                            if (thisBatteryVoltage.startsWith("0")) { // If voltage is 0, there may be NO BATTERY installed as I've seen some systems list a battery with all zero values when no battery is installed.
                                thisBatteryCapacity = "";
                            } else if (!thisBatteryCapacity.equals("NO POWER")) { // Otherwise, if there is no charge and no energy (charging or draining) then this is probably a DEAD BATTERY.
                                thisBatteryCapacity = "NO POWER";
                                batteryHealthErrorArray.add("No Power");
                            }
                        }

                        if (!thisBatteryCapacity.isEmpty()) {
                            if ((thisBatteryState.equals("charging") || thisBatteryState.equals("pending-charge")) && thisBatteryEnergyRate.startsWith("0") && !thisBatteryPercentage.equals("100%")) {
                                batteryHealthWarningArray.add("Not Charging");
                            }

                            batteryHealthArray.add(thisBatteryCapacity + (thisBatteryVoltage.isEmpty() ? (thisBatteryCycleCount.isEmpty() ? "" : " (" + thisBatteryCycleCount + ")") : " (" + (thisBatteryCycleCount.isEmpty() ? "" : thisBatteryCycleCount + ", ") + thisBatteryVoltage + ")"));
                        }
                    }
                }

                // Touchscreen Info for Linux ("udevadm" is at "/sbin/udevadm" on Mint 19.3 and older, but is at "/usr/bin/udevadm" on at least Mint 20.2 and newer. maybe only after usrmerge).
                hasTouchscreen = !new CommandReader(new String[]{(new File("/usr/bin/udevadm").exists() ? "/usr/bin/udevadm" : "/sbin/udevadm"), "info", "--export-db"}).getFirstOutputLineContaining("ID_INPUT_TOUCHSCREEN=1").isEmpty();

                // Camera Info for Linux
                String[] devContents = new File("/dev").list();
                for (String thisDevName : devContents) {
                    if (thisDevName.startsWith("video")) {
                        hasCamera = true;
                        break;
                    }
                }

                if (hasCamera) {
                    hasScreen = true; // Assume any built-in Camera means there's a built-in screen as well (because some All-in-Ones don't use the correct Chassis Type).
                }

                // Screen Info for Linux
                int screenWidthMM = 0;
                int screenHeightMM = 0;

                String[] xrandrOutput = new CommandReader(new String[]{"/usr/bin/xrandr"}).getOutputLines();

                for (String thisXrandrOutputLine : xrandrOutput) {
                    if (thisXrandrOutputLine.contains(" connected")) {
                        if (!hasScreen) {
                            // hasScreen has already been set to true for the chassis type's Laptop, *book, or All-in-One (which isn't always the Chassis type for actual All-in-One PCs).
                            // Now, lets check for screens connected via LVDS or eDP which is always an internal screen. But, some All-in-Ones use internal HDMI for their screen, so All-in-Ones aren't reliably caught this way.

                            hasScreen = (thisXrandrOutputLine.startsWith("LVDS") || thisXrandrOutputLine.startsWith("eDP"));
                        }

                        if (hasScreen && thisXrandrOutputLine.contains("mm x ") && !thisXrandrOutputLine.contains(" 0mm x 0mm")) {
                            String[] screenInfoParts = thisXrandrOutputLine.split("\\) ");

                            if (screenInfoParts.length == 2) {
                                String[] mmScreenDimensionsParts = screenInfoParts[1].split(" x ");

                                if (mmScreenDimensionsParts.length == 2) {
                                    try {
                                        screenWidthMM = Integer.parseInt(mmScreenDimensionsParts[0].replaceAll("[^0-9]", ""));
                                        screenHeightMM = Integer.parseInt(mmScreenDimensionsParts[1].replaceAll("[^0-9]", ""));
                                    } catch (NumberFormatException xrandrScreenDimensionsException) {
                                        if (isTestMode) {
                                            System.out.println("xrandrScreenDimensionsException: " + xrandrScreenDimensionsException);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (thisXrandrOutputLine.contains("*")) {
                        screenResolution = thisXrandrOutputLine.substring(2, 15).trim();
                        break;
                    }
                }

                if (hasScreen && isLinuxUbiquityMode && (screenWidthMM == 0 || screenHeightMM == 0)) {
                    // xrandr does not get screen dimensions in pre-install environment (because xorg IS NOT running), but xdpyinfo does.
                    // Also, xdpyinfo can give wrong dimensions (because it thinks DPI is 96) when not in pre-install environment (because xorg IS running).
                    // Info about why xdpyinfo is wrong when xorg is running: https://unix.stackexchange.com/questions/75344/how-does-x-server-calculate-dpi
                    // Also: https://askubuntu.com/a/736118 & https://bugs.launchpad.net/ubuntu/+source/xorg-server/+bug/201491 & https://bbs.archlinux.org/viewtopic.php?id=204823
                    // So, DON'T use xdpyinfo when NOT in pre-install environment since it will likely be wrong.

                    String xdpyinfoDimensionsOutput = new CommandReader("/usr/bin/xdpyinfo | /bin/grep 'dimensions:'").getFirstOutputLineContaining("millimeters");

                    if (!xdpyinfoDimensionsOutput.isEmpty()) {
                        String[] mmScreenDimensionsParts = xdpyinfoDimensionsOutput.substring(xdpyinfoDimensionsOutput.indexOf("(") + 1).split("x");

                        if (mmScreenDimensionsParts.length == 2) {
                            try {
                                screenWidthMM = Integer.parseInt(mmScreenDimensionsParts[0].replaceAll("[^0-9]", ""));
                                screenHeightMM = Integer.parseInt(mmScreenDimensionsParts[1].replaceAll("[^0-9]", ""));
                            } catch (NumberFormatException xdpyinfoScreenDimensionsException) {
                                if (isTestMode) {
                                    System.out.println("xdpyinfoScreenDimensionsException: " + xdpyinfoScreenDimensionsException);
                                }
                            }
                        }
                    }
                }

                if (hasScreen && screenWidthMM > 0 && screenHeightMM > 0) {
                    double diagonalScreenSize = (Math.sqrt(Math.pow(screenWidthMM, 2) + Math.pow(screenHeightMM, 2)) / 25.4);

                    screenSize = new DecimalFormat("#.#").format(diagonalScreenSize);

                    if (screenSize.equals("0")) {
                        screenSize = "N/A";
                    } else {
                        if (screenSize.endsWith(".9")) {
                            screenSize = new DecimalFormat("#").format(diagonalScreenSize);
                        }
                        screenSize += "-inch";
                    }
                }
            } else if (isWindows) {
                // Windows Info

                boolean isWindowsPE = ((new File("\\Windows\\System32\\startnet.cmd").exists() || new File("\\Windows\\System32\\winpeshl.ini").exists()) && !new CommandReader(new String[]{"\\Windows\\System32\\reg.exe", "query", "HKLM\\SYSTEM\\Setup", "/v", "FactoryPreInstallInProgress"}).getFirstOutputLineContaining("0x1").isEmpty());

                String windowsBuildNumber = new CommandReader(new String[]{"\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", "(Get-CimInstance Win32_OperatingSystem -Property BuildNumber).BuildNumber"}).getFirstOutputLine();
                if (windowsBuildNumber.equals("22000")) {
                    // On Windows 11 21H2 (Build 22000), the very first "Get-PhysicalDisk" load when OS boots is returning nothing, so always do one load in advance so that the next full specs load always works.
                    // This appears to NO LONGER be an issue on Windows 11 22H2 (Build 22621).

                    try {
                        Runtime.getRuntime().exec(new String[]{"\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", "Get-PhysicalDisk"}).waitFor();
                    } catch (IOException | InterruptedException loadPhysicalDiskException) {
                        if (isTestMode) {
                            System.out.println("loadPhysicalDiskException: " + loadPhysicalDiskException);
                        }
                    }
                }

                // MUCH faster to run all commands in a single PowerShell call than to call PowerShell for each command!
                String[] windowsHardwareInfo = new CommandReader(new String[]{"\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command",
                    "[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding;" // Some values may contain multi-byte characters such as "®" which will be transliterated to "r" (which cannot be easily isolated for removal) unless the console output encoding is explicitly set to UTF-8: https://stackoverflow.com/a/49481797
                    // Win32_OperatingSystem
                    + "Write-Output 'Windows Hardware Info Class = Win32_OperatingSystem';"
                    + "Get-CimInstance Win32_OperatingSystem -Property Caption,OSArchitecture,BuildNumber | Format-List Caption,OSArchitecture,BuildNumber;"
                    // CurrentVersion
                    + "Write-Output 'Windows Hardware Info Class = CurrentVersion';"
                    + "Get-ItemProperty 'HKLM:\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion' -Name ProductName,DisplayVersion,ReleaseId | Format-List ProductName,DisplayVersion,ReleaseId;"
                    // Win32_ComputerSystemProduct
                    + "Write-Output 'Windows Hardware Info Class = Win32_ComputerSystemProduct';"
                    + "Get-CimInstance Win32_ComputerSystemProduct -Property Vendor,Version,Name,IdentifyingNumber,UUID | Format-List Vendor,Version,Name,IdentifyingNumber,UUID;"
                    // Win32_BaseBoard
                    + "Write-Output 'Windows Hardware Info Class = Win32_BaseBoard';"
                    + "Get-CimInstance Win32_BaseBoard -Property Manufacturer,Version,Product,SerialNumber | Format-List Manufacturer,Version,Product,SerialNumber;"
                    // Win32_ComputerSystem
                    + "Write-Output 'Windows Hardware Info Class = Win32_ComputerSystem';"
                    + "Get-CimInstance Win32_ComputerSystem -Property NumberOfProcessors,PCSystemType,TotalPhysicalMemory,SystemSKUNumber | Format-List NumberOfProcessors,PCSystemType,TotalPhysicalMemory,SystemSKUNumber;"
                    // OEMStringArray FROM Win32_ComputerSystem
                    + "Write-Output 'Windows Hardware Info Class = OEMStringArray FROM Win32_ComputerSystem';"
                    + "Get-CimInstance Win32_ComputerSystem -Property OEMStringArray | Select-Object -ExpandProperty OEMStringArray | ForEach-Object { Write-Output \\\"OEMStringArray : $_\\\" };"
                    + (isWindowsPE
                    // Win32_NetworkAdapter
                    ? "Write-Output 'Windows Hardware Info Class = Win32_NetworkAdapter';"
                    + "Get-CimInstance Win32_NetworkAdapter -Filter 'PhysicalAdapter = True' -Property MACAddress,Name,NetConnectionID | Format-List MACAddress,Name,NetConnectionID;"
                    // NetAdapter
                    : "Write-Output 'Windows Hardware Info Class = Get-NetAdapter';" // DO NOT want to use "-Physical" arg here because we want Bluetooth as well.
                    + "Get-NetAdapter | Format-List MacAddress,InterfaceDescription,PhysicalMediaType;")
                    // Win32_SystemEnclosure
                    + "Write-Output 'Windows Hardware Info Class = Win32_SystemEnclosure';"
                    + "Get-CimInstance Win32_SystemEnclosure -Property ChassisTypes | Format-List ChassisTypes;"
                    // Win32_Processor
                    + "Write-Output 'Windows Hardware Info Class = Win32_Processor';" // Don't get CurrentClockSpeed in WinPE because it seems to always be throttled.
                    + "Get-CimInstance Win32_Processor -Property Manufacturer,Name," + (isWindowsPE ? "" : "CurrentClockSpeed,") + "MaxClockSpeed,NumberOfCores,NumberOfLogicalProcessors | Format-List Manufacturer,Name," + (isWindowsPE ? "" : "CurrentClockSpeed,") + "MaxClockSpeed,NumberOfCores,NumberOfLogicalProcessors;"
                    // Win32_PhysicalMemoryArray
                    + "Write-Output 'Windows Hardware Info Class = Win32_PhysicalMemoryArray';" // Filter for only Use EQUALS 3 (System Memory): https://docs.microsoft.com/en-us/windows/win32/cimwin32prov/win32-physicalmemoryarray
                    + "Get-CimInstance Win32_PhysicalMemoryArray -Filter \\\"Use = '3'\\\" -Property MemoryDevices,MaxCapacity | Format-List MemoryDevices,MaxCapacity;"
                    // Win32_PhysicalMemory
                    + "Write-Output 'Windows Hardware Info Class = Win32_PhysicalMemory';" // Filter for only TypeDetail NOT EQUALS 4096 (Non-Volatile): https://docs.microsoft.com/en-us/windows/win32/cimwin32prov/win32-physicalmemory
                    + "Get-CimInstance Win32_PhysicalMemory -Filter \\\"TypeDetail <> '4096'\\\" -Property Tag,Capacity,MemoryType,SMBIOSMemoryType,Speed,MaxVoltage | Format-List Tag,Capacity,MemoryType,SMBIOSMemoryType,Speed,MaxVoltage;"
                    // Get-PhysicalDisk
                    + "Write-Output 'Windows Hardware Info Class = Get-PhysicalDisk';"
                    + "Get-PhysicalDisk | Where-Object { ($_.BusType -eq 'SATA') -or ($_.BusType -eq 'ATA') -or ($_.BusType -eq 'NVMe') -or ($_.BusType -eq 'RAID') -or (($_.BusType -eq 'SD') -and ($_.PhysicalLocation -like 'Integrated*')) } | Format-List HealthStatus,BusType,MediaType,Model,SerialNumber,FruId,Size;"
                    // Win32_VideoController
                    + "Write-Output 'Windows Hardware Info Class = Win32_VideoController';"
                    + "Get-CimInstance Win32_VideoController -Property InstalledDisplayDrivers,AdapterCompatibility,Name,ConfigManagerErrorCode,CurrentHorizontalResolution,CurrentVerticalResolution | Format-List InstalledDisplayDrivers,AdapterCompatibility,Name,ConfigManagerErrorCode,CurrentHorizontalResolution,CurrentVerticalResolution;"
                    // Win32_SoundDevice
                    + "Write-Output 'Windows Hardware Info Class = Win32_SoundDevice';"
                    + "Get-CimInstance Win32_SoundDevice -Property Manufacturer,ProductName,ConfigManagerErrorCode | Format-List Manufacturer,ProductName,ConfigManagerErrorCode;"
                    // Win32_PnPEntity OR Get-PnpDevice
                    // Use Get-PnpDevice in Installed OS (which isn't available in WinPE) since it gets Hidden Devices while Win32_PnPEntity doesn't list Hidden Devices for some reason.
                    // Hidden Devices are needed to get the full actual hardware details to match the Drivers Cache Model Name from WinPE (since Win32_PnPEntity includes all hardware in WinPE when no drivers are installed). This is important when there are multiple audio cards (assuming multiple GPUs as well, but I noticed the issue with a T450s which listed 2 audio cards in WinPE and only 1 after drivers were installed in OS).
                    // Hidden Devices can be viewed with Device Manager by enabling the "Show hidden devices" option in the View menu.
                    + "Write-Output 'Windows Hardware Info Class = " + (isWindowsPE ? "Win32_PnPEntity" : "Get-PnpDevice") + "';"
                    + (isWindowsPE
                    ? "$pnpEntitiesWithVendorsAndDevices = Get-CimInstance Win32_PnPEntity -Filter \\\"DeviceID LIKE '%VEN_%DEV_%' OR DeviceID LIKE '%VID_%PID_%' OR Name LIKE '%touch screen%'\\\" -Property Name,DeviceID,CompatibleID;"
                    : "$pnpEntitiesWithVendorsAndDevices = Get-PnpDevice | Where-Object { ($_.DeviceID -like '*VEN_*DEV_*') -or ($_.DeviceID -like '*VID_*PID_*') -or ($_.Name -like '*touch screen*') };")
                    + "$pnpEntitiesWithVendorsAndDevices | ForEach-Object { foreach ($thisCompatibleID in $_.CompatibleID) { if ($thisCompatibleID.Contains('CC_') -or $thisCompatibleID.Contains('Class_')) { $_ | Add-Member CompatibleIDwithClass $thisCompatibleID; break } } };"
                    + "$pnpEntitiesWithVendorsAndDevices | Format-List ConfigManagerErrorCode,Name,CompatibleIDwithClass,DeviceID;"
                    // Win32_Battery
                    + "Write-Output 'Windows Hardware Info Class = Win32_Battery';" // Some computers (ex. Lenovo G700, Lenovo G50) don't list any info in Win32_PortableBattery and barely any in Win32_Battery, but retrieving DesignVoltage seems to be reliable and the rest can come from the classes below.
                    + "Get-CimInstance Win32_Battery -Property DesignVoltage,EstimatedChargeRemaining | Format-List DesignVoltage,EstimatedChargeRemaining;"
                    // BatteryStatus
                    + "Write-Output 'Windows Hardware Info Class = BatteryStatus';"
                    + "Get-CimInstance BatteryStatus -Namespace ROOT\\WMI -Property ChargeRate,Charging,DischargeRate,Voltage | Format-List ChargeRate,Charging,DischargeRate,Voltage;"
                    // BatteryStaticData
                    + "Write-Output 'Windows Hardware Info Class = BatteryStaticData';" // For some reason, using Get-CimInstance instead of Get-WmiObject for this class always fails.
                    + "Get-WmiObject -Namespace ROOT\\WMI -Query 'SELECT DesignedCapacity FROM BatteryStaticData' | Format-List DesignedCapacity;"
                    // BatteryFullChargedCapacity
                    + "Write-Output 'Windows Hardware Info Class = BatteryFullChargedCapacity';"
                    + "Get-CimInstance BatteryFullChargedCapacity -Namespace ROOT\\WMI -Property FullChargedCapacity | Format-List FullChargedCapacity;"
                    // BatteryCycleCount
                    + "Write-Output 'Windows Hardware Info Class = BatteryCycleCount';"
                    + "Get-CimInstance BatteryCycleCount -Namespace ROOT\\WMI -Property CycleCount | Format-List CycleCount;"
                    // WmiMonitorBasicDisplayParams
                    + "Write-Output 'Windows Hardware Info Class = WmiMonitorBasicDisplayParams';"
                    + "Get-CimInstance WmiMonitorBasicDisplayParams -Namespace ROOT\\WMI -Property MaxHorizontalImageSize,MaxVerticalImageSize | Format-List MaxHorizontalImageSize,MaxVerticalImageSize;"
                    // Win32_CDROMDrive
                    + "Write-Output 'Windows Hardware Info Class = Win32_CDROMDrive';"
                    + "Get-CimInstance Win32_CDROMDrive -Property Name,Drive,CapabilityDescriptions,MediaType,MediaLoaded -Filter \\\"NOT Name LIKE 'Microsoft Virtual%'\\\" | Format-List Name,Drive,CapabilityDescriptions,MediaType,MediaLoaded"
                }).getOutputLines();

                // if (isTestMode) {
                //    System.out.println(String.join("\n", windowsHardwareInfo)); // DEBUG
                //}
                String osBuildNumber = "";

                String thisNetworkAdapterMAC = "";
                String thisNetworkAdapterName = "";

                int ramSlotCount = 0;
                long ramMaxKilobytes = 0L;

                int lastMemorySlotTag = -1;
                int thisMemorySlotTag = -1;
                int displayedRamSlotCount = 0;

                String thisDriveHealthStatus = "";
                String thisDriveBusType = "";
                String thisDriveMediaType = "";
                String thisDriveModel = "";

                String thisGraphicsManufacturer = "";

                String thisAudioManufacturer = "";

                String pnpDeviceName = "";
                String pnpDeviceConfigManagerErrorCode = "";
                String pnpDeviceCompatibleIDwithClass = "";

                ArrayList<String> pnpGPU = new ArrayList<>();
                ArrayList<String> pnpAudio = new ArrayList<>();
                ArrayList<String> pnpWireless = new ArrayList<>();

                ArrayList<String> windowsBatteryDesignVoltages = new ArrayList<>();
                ArrayList<String> windowsBatteryChargePercentages = new ArrayList<>();
                ArrayList<String> windowsBatteryChargeRates = new ArrayList<>();
                ArrayList<String> windowsBatteryChargingStatuses = new ArrayList<>();
                ArrayList<String> windowsBatteryDischargeRates = new ArrayList<>();
                ArrayList<String> windowsBatteryCurrentVoltages = new ArrayList<>();
                ArrayList<String> windowsBatteryDesignCapacities = new ArrayList<>();
                ArrayList<String> windowsBatteryFullChargeCapacities = new ArrayList<>();
                ArrayList<String> windowsBatteryCycleCounts = new ArrayList<>();

                String thisMaxHorizontalImageSizeString = "";

                String thisDiscDriveName = "";
                String thisDiscDriveLetter = "";
                String thisDiscDriveCapabilityDescriptions = "";
                String thisDiscDriveMediaType = "";

                String thisWindowsHardwareClass = "Unknown";

                for (String thisWindowsHardwareInfoLine : windowsHardwareInfo) {
                    if (thisWindowsHardwareInfoLine.startsWith("Windows Hardware Info Class = ")) {
                        thisWindowsHardwareClass = thisWindowsHardwareInfoLine.substring(thisWindowsHardwareInfoLine.indexOf(" = ") + 3);
                        continue;
                    }

                    if (thisWindowsHardwareInfoLine.contains(": ")) {
                        String thisWindowsHardwareInfoProperty = thisWindowsHardwareInfoLine.substring(thisWindowsHardwareInfoLine.indexOf(": ") + 2).replaceAll("^[. /]+", "").replaceAll("[. /]+$", ""); // Trim all leading and trailing spaces, periods, and slashes.

                        if (!thisWindowsHardwareInfoProperty.isEmpty()) {
                            switch (thisWindowsHardwareClass) {
                                case "Win32_OperatingSystem": // Java os.name and os.version are not specific enough in Windows
                                    if (thisWindowsHardwareInfoLine.startsWith("Caption")) {
                                        os = thisWindowsHardwareInfoProperty.replace("Microsoft", "").trim();
                                    } else if (thisWindowsHardwareInfoLine.startsWith("OSArchitecture")) {
                                        os += " (" + thisWindowsHardwareInfoProperty.replace(" Processor", "") + ")";
                                    } else if (thisWindowsHardwareInfoLine.startsWith("BuildNumber")) {
                                        osBuildNumber = thisWindowsHardwareInfoProperty;
                                    }

                                    break;
                                case "CurrentVersion":
                                    if (thisWindowsHardwareInfoLine.startsWith("ProductName") && os.startsWith(" (")) { // To get proper product name in WinPE
                                        os = thisWindowsHardwareInfoProperty.replace(" (TM)", "").replace("Preinstallation Environment", "PE") + os;
                                    } else if (thisWindowsHardwareInfoLine.startsWith("DisplayVersion")) { // DisplayVersion was added in 20H2 for the new YYH# version style instead of the old YYMM style which is stored in ReleaseId.
                                        os += " - Version " + thisWindowsHardwareInfoProperty;
                                    } else if (thisWindowsHardwareInfoLine.startsWith("ReleaseId") && !os.contains(" - Version ")) { // Still check for and use ReleaseId for 2004 and older versions of Windows 10.
                                        os += " - Version " + thisWindowsHardwareInfoProperty;
                                    }

                                    break;
                                case "Win32_ComputerSystemProduct":
                                    if (!ignoreSpecsPlaceholders(thisWindowsHardwareInfoProperty)) {
                                        if (thisWindowsHardwareInfoLine.startsWith("Vendor")) {
                                            brand = cleanComputerBrand(thisWindowsHardwareInfoProperty);
                                        } else if (thisWindowsHardwareInfoLine.startsWith("Version") && thisWindowsHardwareInfoProperty.replaceAll("[^A-Za-z0-9]", "").length() > 4) {
                                            model = thisWindowsHardwareInfoProperty;
                                        } else if (thisWindowsHardwareInfoLine.startsWith("Name")) {
                                            systemProductName = thisWindowsHardwareInfoProperty;
                                        } else if (thisWindowsHardwareInfoLine.startsWith("IdentifyingNumber")) {
                                            serial = thisWindowsHardwareInfoProperty;
                                        } else if (thisWindowsHardwareInfoLine.startsWith("UUID")) {
                                            biosUUID = thisWindowsHardwareInfoProperty.toUpperCase();
                                        }
                                    }

                                    break;
                                case "Win32_BaseBoard":
                                    if (!ignoreSpecsPlaceholders(thisWindowsHardwareInfoProperty)) {
                                        if (thisWindowsHardwareInfoLine.startsWith("Manufacturer")) {
                                            motherboardBrand = cleanComputerBrand(thisWindowsHardwareInfoProperty);
                                        } else if (thisWindowsHardwareInfoLine.startsWith("Version") && thisWindowsHardwareInfoProperty.replaceAll("[^A-Za-z0-9]", "").length() > 4) {
                                            motherboardVersion = thisWindowsHardwareInfoProperty;
                                        } else if (thisWindowsHardwareInfoLine.startsWith("Product")) {
                                            motherboardModel = thisWindowsHardwareInfoProperty;
                                        } else if (thisWindowsHardwareInfoLine.startsWith("SerialNumber")) {
                                            motherboardSerial = thisWindowsHardwareInfoProperty;
                                        }
                                    }

                                    break;
                                case "Win32_ComputerSystem":
                                    if (thisWindowsHardwareInfoLine.startsWith("NumberOfProcessors")) {
                                        cpuCount = thisWindowsHardwareInfoProperty;
                                    } else if (thisWindowsHardwareInfoLine.startsWith("PCSystemType")) {
                                        if (thisWindowsHardwareInfoProperty.equals("2")) {
                                            // https://docs.microsoft.com/en-us/dotnet/api/microsoft.powershell.commands.pcsystemtype?view=powershellsdk-1.1.0
                                            isLaptop = true; // Double check PCSystemType for "Mobile", but the other values are useless when we already have Chassis type.
                                        }
                                    } else if (thisWindowsHardwareInfoLine.startsWith("TotalPhysicalMemory")) {
                                        try {
                                            ram = new DecimalFormat("#").format((Double.parseDouble(thisWindowsHardwareInfoProperty) / 1024 / 1024 / 1024)) + " GB";
                                        } catch (NumberFormatException ramSizeException) {
                                            if (isTestMode) {
                                                System.out.println("ramSizeException: " + ramSizeException);
                                            }
                                        }
                                    } else if (thisWindowsHardwareInfoLine.startsWith("SystemSKUNumber") && !ignoreSpecsPlaceholders(thisWindowsHardwareInfoProperty) && thisWindowsHardwareInfoProperty.replaceAll("[^A-Za-z0-9]", "").length() > 1) {
                                        systemProductSKU = thisWindowsHardwareInfoProperty;
                                    }

                                    break;
                                case "OEMStringArray FROM Win32_ComputerSystem":
                                    if (brand.equals("Dell") && (systemProductSKU.equals("N/A") || systemProductSKU.equals(systemProductName)) && thisWindowsHardwareInfoLine.startsWith("OEMStringArray")
                                            && thisWindowsHardwareInfoProperty.contains("[") && thisWindowsHardwareInfoProperty.endsWith("]") && (thisWindowsHardwareInfoProperty.length() == (thisWindowsHardwareInfoProperty.indexOf("[") + 6))) {
                                        // If Dell and didn't get the actual SKU from "System Information/SKU Number", this should be it.
                                        String possibleSystemProductSKU = thisWindowsHardwareInfoProperty.substring((thisWindowsHardwareInfoProperty.indexOf("[") + 1), (thisWindowsHardwareInfoProperty.indexOf("[") + 5));
                                        if (!possibleSystemProductSKU.equals("0000")) {
                                            systemProductSKU = possibleSystemProductSKU;
                                        }
                                    }

                                    break;
                                case "Get-NetAdapter": // Get-NetAdapter isn't available in WinPE, so Win32_NetworkAdapter and Win32_PnPEntity will be used instead
                                    if (thisWindowsHardwareInfoLine.startsWith("MacAddress")) {
                                        thisNetworkAdapterMAC = thisWindowsHardwareInfoProperty.replace("-", ":");
                                    } else if (thisWindowsHardwareInfoLine.startsWith("InterfaceDescription")) {
                                        thisNetworkAdapterName = cleanDeviceModel(thisWindowsHardwareInfoProperty);
                                    } else if (thisWindowsHardwareInfoLine.startsWith("PhysicalMediaType")) {
                                        if (!thisNetworkAdapterMAC.isEmpty()) {
                                            if (thisWindowsHardwareInfoProperty.equals("802.3")) {
                                                ethernetMACarray.add(thisNetworkAdapterMAC);
                                            } else if (!getWireless().contains("Wi-Fi Detected") && thisWindowsHardwareInfoProperty.equals("Native 802.11")) {
                                                String wiFiWithProtocols = "Wi-Fi Detected (UNKNOWN Protocols - Wi-Fi DISABLED)";
                                                if (!thisNetworkAdapterName.isEmpty()) {
                                                    wiFiWithProtocols = "Wi-Fi Detected (" + thisNetworkAdapterName + ")";
                                                }

                                                String windowsWiFiProtocols = new CommandReader(new String[]{"\\Windows\\System32\\netsh.exe", "wlan", "show", "drivers"}).getFirstOutputLineContaining("Radio types supported");
                                                if (windowsWiFiProtocols.contains("802.11")) {
                                                    ArrayList<String> wifiVersionsAndProtocols = new ArrayList<>();
                                                    String[] wifiProtocolsArray = windowsWiFiProtocols.substring(windowsWiFiProtocols.indexOf(": ") + 2).replace("802.11", "").split(" ");
                                                    for (String thisWiFiProtocol : wifiProtocolsArray) {
                                                        thisWiFiProtocol = thisWiFiProtocol.trim();
                                                        if (!thisWiFiProtocol.isEmpty()) {
                                                            switch (thisWiFiProtocol) {
                                                                case "b":
                                                                    wifiVersionsAndProtocols.add("1/" + thisWiFiProtocol);
                                                                    break;
                                                                case "a":
                                                                    wifiVersionsAndProtocols.add("2/" + thisWiFiProtocol);
                                                                    break;
                                                                case "g":
                                                                    wifiVersionsAndProtocols.add("3/" + thisWiFiProtocol);
                                                                    break;
                                                                case "n":
                                                                    wifiVersionsAndProtocols.add("4/" + thisWiFiProtocol);
                                                                    break;
                                                                case "ac":
                                                                    wifiVersionsAndProtocols.add("5/" + thisWiFiProtocol);
                                                                    break;
                                                                case "ax":
                                                                    wifiVersionsAndProtocols.add("6/" + thisWiFiProtocol);
                                                                    break;
                                                                case "be":
                                                                    wifiVersionsAndProtocols.add("7/" + thisWiFiProtocol);
                                                                    break;
                                                                default:
                                                                    wifiVersionsAndProtocols.add(thisWiFiProtocol);
                                                                    break;
                                                            }
                                                        }
                                                    }
                                                    Collections.sort(wifiVersionsAndProtocols);
                                                    wiFiWithProtocols = "Wi-Fi Detected (Supports " + String.join(", ", wifiVersionsAndProtocols) + ")";
                                                }

                                                wirelessArray.add(0, wiFiWithProtocols);
                                            } else if (!getWireless().contains("Cellular Detected") && thisWindowsHardwareInfoProperty.equals("Wireless WAN")) {
                                                wirelessArray.add("Cellular Detected" + ((thisNetworkAdapterName.isEmpty() || thisNetworkAdapterName.contains("Generic Mobile Broadband")) ? "" : " (" + thisNetworkAdapterName + ")"));
                                            } else if (!getWireless().contains("Bluetooth Detected") && thisWindowsHardwareInfoProperty.equals("BlueTooth")) {
                                                wirelessArray.add("Bluetooth Detected");
                                            }

                                            thisNetworkAdapterMAC = "";
                                            thisNetworkAdapterName = "";
                                        }
                                    }

                                    break;
                                case "Win32_NetworkAdapter": // Win32_NetworkAdapter will be used in WinPE since Get-NetAdapter isn't available.
                                    if (thisWindowsHardwareInfoLine.startsWith("MACAddress")) {
                                        thisNetworkAdapterMAC = thisWindowsHardwareInfoProperty;
                                    } else if (thisWindowsHardwareInfoLine.startsWith("Name")) {
                                        thisNetworkAdapterName = cleanDeviceModel(thisWindowsHardwareInfoProperty);
                                    } else if (thisWindowsHardwareInfoLine.startsWith("NetConnectionID")) {
                                        if (!thisNetworkAdapterMAC.isEmpty()) {
                                            if (thisWindowsHardwareInfoProperty.equals("Ethernet")) {
                                                ethernetMACarray.add(thisNetworkAdapterMAC);
                                            } else if (!getWireless().contains("Wi-Fi Detected") && thisWindowsHardwareInfoProperty.equals("Wi-Fi")) {
                                                // Wi-Fi may not be listed (and Bluetooth will never be listed) in Win32_NetworkAdapter in WinPE (even when not filtering for "PhysicalAdapter = True"),
                                                // so Win32_PnPEntity is also checked for Wi-Fi and Bluetooth in WindowsPE.
                                                wirelessArray.add(0, "Wi-Fi Detected" + (thisNetworkAdapterName.isEmpty() ? "" : " (" + thisNetworkAdapterName + ")"));
                                            }
                                        }

                                        if (!getWireless().contains("Cellular Detected") && thisWindowsHardwareInfoProperty.equals("Cellular")) { // Cellular may not have MAC Address in WinPE, but still want to detect it.
                                            // Win32_PnPEntity is also checked for Cellular in WindowsPE.
                                            wirelessArray.add("Cellular Detected" + ((thisNetworkAdapterName.isEmpty() || thisNetworkAdapterName.contains("Generic Mobile Broadband")) ? "" : " (" + thisNetworkAdapterName + ")"));
                                        }

                                        thisNetworkAdapterMAC = "";
                                        thisNetworkAdapterName = "";
                                    }

                                    break;
                                case "Win32_SystemEnclosure":
                                    if (thisWindowsHardwareInfoLine.startsWith("ChassisTypes")) {
                                        String[] windowsChassisTypes = thisWindowsHardwareInfoProperty.replaceAll("[^0-9,]", "").split(",");

                                        for (String thisWindowsChassisTypeCode : windowsChassisTypes) {
                                            String thisChassisTypeString = "Undefined"; // https://blogs.technet.microsoft.com/brandonlinton/2017/09/15/updated-win32_systemenclosure-chassis-types/

                                            switch (thisWindowsChassisTypeCode) {
                                                case "1":
                                                    thisChassisTypeString = "Other";
                                                    break;
                                                case "2":
                                                    thisChassisTypeString = "Unknown";
                                                    break;
                                                case "3":
                                                    thisChassisTypeString = "Desktop";
                                                    break;
                                                case "4":
                                                    thisChassisTypeString = "Low Profile Desktop";
                                                    break;
                                                case "5":
                                                    thisChassisTypeString = "Pizza Box";
                                                    break;
                                                case "6":
                                                    thisChassisTypeString = "Mini Tower";
                                                    break;
                                                case "7":
                                                    thisChassisTypeString = "Tower";
                                                    break;
                                                case "8":
                                                    thisChassisTypeString = "Portable";
                                                    isLaptop = true;
                                                    hasScreen = true;
                                                    break;
                                                case "9":
                                                    thisChassisTypeString = "Laptop";
                                                    isLaptop = true;
                                                    hasScreen = true;
                                                    break;
                                                case "10":
                                                    thisChassisTypeString = "Notebook";
                                                    isLaptop = true;
                                                    hasScreen = true;
                                                    break;
                                                case "11":
                                                    thisChassisTypeString = "Hand Held";
                                                    break;
                                                case "12":
                                                    thisChassisTypeString = "Docking Station";
                                                    break;
                                                case "13":
                                                    thisChassisTypeString = "All-in-One";
                                                    hasScreen = true;
                                                    break;
                                                case "14":
                                                    thisChassisTypeString = "Sub Notebook";
                                                    isLaptop = true;
                                                    hasScreen = true;
                                                    break;
                                                case "15":
                                                    thisChassisTypeString = "Space-Saving";
                                                    break;
                                                case "16":
                                                    thisChassisTypeString = "Lunch Box";
                                                    break;
                                                case "17":
                                                    thisChassisTypeString = "Main System Chassis";
                                                    break;
                                                case "18":
                                                    thisChassisTypeString = "Expansion Chassis";
                                                    break;
                                                case "19":
                                                    thisChassisTypeString = "SubChassis";
                                                    break;
                                                case "20":
                                                    thisChassisTypeString = "Bus Expansion Chassis";
                                                    break;
                                                case "21":
                                                    thisChassisTypeString = "Peripheral Chassis";
                                                    break;
                                                case "22":
                                                    thisChassisTypeString = "Storage Chassis";
                                                    break;
                                                case "23":
                                                    thisChassisTypeString = "Rack Mount Chassis";
                                                    break;
                                                case "24":
                                                    thisChassisTypeString = "Sealed-Case PC";
                                                    break;
                                                case "25":
                                                    thisChassisTypeString = "Multi-System Chassis";
                                                    break;
                                                case "26":
                                                    thisChassisTypeString = "Compact PCI";
                                                    break;
                                                case "27":
                                                    thisChassisTypeString = "Advanced TCA";
                                                    break;
                                                case "28":
                                                    thisChassisTypeString = "Blade";
                                                    break;
                                                case "29":
                                                    thisChassisTypeString = "Blade Enclosure";
                                                    break;
                                                case "30":
                                                    thisChassisTypeString = "Tablet";
                                                    isLaptop = true;
                                                    hasScreen = true;
                                                    break;
                                                case "31":
                                                    thisChassisTypeString = "Convertible";
                                                    isLaptop = true;
                                                    hasScreen = true;
                                                    break;
                                                case "32":
                                                    thisChassisTypeString = "Detachable";
                                                    isLaptop = true;
                                                    hasScreen = true;
                                                    break;
                                                case "33":
                                                    thisChassisTypeString = "IoT Gateway";
                                                    break;
                                                case "34":
                                                    thisChassisTypeString = "Embedded PC";
                                                    break;
                                                case "35":
                                                    thisChassisTypeString = "Mini PC";
                                                    break;
                                                case "36":
                                                    thisChassisTypeString = "Stick PC";
                                                    break;
                                                default:
                                                    break;
                                            }

                                            if (chassisType.equals("N/A")) {
                                                chassisType = thisChassisTypeString;
                                            } else {
                                                chassisType += " + " + thisChassisTypeString;
                                            }
                                        }
                                    }

                                    break;
                                case "Win32_Processor":
                                    if (thisWindowsHardwareInfoLine.startsWith("Manufacturer")) {
                                        cpuBrand = cleanCPU(thisWindowsHardwareInfoProperty);
                                    } else if (thisWindowsHardwareInfoLine.startsWith("Name")) {
                                        cpu = cleanCPU(thisWindowsHardwareInfoProperty);
                                    } else if (thisWindowsHardwareInfoLine.startsWith("CurrentClockSpeed")) {
                                        cpuCurrentSpeedString = thisWindowsHardwareInfoProperty + " MHz";
                                    } else if (thisWindowsHardwareInfoLine.startsWith("MaxClockSpeed")) {
                                        cpuMaxSpeedString = thisWindowsHardwareInfoProperty + " MHz";
                                    } else if (thisWindowsHardwareInfoLine.startsWith("NumberOfCores")) {
                                        cpuCores = thisWindowsHardwareInfoProperty;
                                    } else if (thisWindowsHardwareInfoLine.startsWith("NumberOfLogicalProcessors")) {
                                        cpuThreads = thisWindowsHardwareInfoProperty;

                                        if (!cpuCores.isEmpty() && !cpuThreads.isEmpty()) {
                                            try {
                                                int cpuCoreCount = Integer.parseInt(cpuCores);
                                                cpuThreadCount = Integer.parseInt(cpuThreads);

                                                if ((cpuThreadCount > cpuCoreCount) && (cpuThreadCount != (cpuCoreCount * 2))) {
                                                    // Starting with 12th Gen Intel processors, there could be Performance (P) and Efficiency (E) cores, which should ideally be determined by each cores max speed, but I can't figure out how to access that on Windows like I can on Linux (via "lscpu -e").
                                                    // So, instead check if there is only hyperthreading on some (not all) cores, and if so the hyperthreaded cores are the P cores and the rest are E cores.
                                                    // BUT, this isn't actually true for all CPUs with P+E cores since there are some CPUs where the P cores are not hyperthreaded such as: https://ark.intel.com/content/www/us/en/ark/products/226264/intel-celeron-processor-7305-8m-cache-1-10-ghz.html
                                                    // Regardless, this is the best I can do for now on Windows, and for CPUs where P cores are not hyperthreaded this code will simply not show a P+E breakdown.
                                                    // This code and info is based on: https://stackoverflow.com/a/74172144

                                                    int pCoresCount = (cpuThreadCount - cpuCoreCount);
                                                    cpuPandEcoresString = pCoresCount + "P + " + (cpuThreadCount - (pCoresCount * 2)) + "E";
                                                }
                                            } catch (NumberFormatException getPandEcoresException) {
                                                if (isTestMode) {
                                                    System.out.println("getPandEcoresException: " + getPandEcoresException);
                                                }

                                            }
                                        }
                                    }

                                    break;
                                case "Win32_PhysicalMemoryArray":
                                    if (thisWindowsHardwareInfoLine.startsWith("MemoryDevices")) {
                                        try {
                                            ramSlotCount += Integer.parseInt(thisWindowsHardwareInfoProperty);
                                        } catch (NumberFormatException ramSlotsException) {
                                            if (isTestMode) {
                                                System.out.println("ramSlotsException: " + ramSlotsException);
                                            }
                                        }
                                    } else if (thisWindowsHardwareInfoLine.startsWith("MaxCapacity")) {
                                        try {
                                            ramMaxKilobytes += Long.parseLong(thisWindowsHardwareInfoProperty);
                                        } catch (NumberFormatException ramMaxSizeException) {
                                            if (isTestMode) {
                                                System.out.println("ramMaxSizeException: " + ramMaxSizeException);
                                            }
                                        }
                                    }

                                    break;
                                case "Win32_PhysicalMemory":
                                    if (thisWindowsHardwareInfoLine.startsWith("Tag")) {
                                        try {
                                            thisMemorySlotTag = Integer.parseInt(thisWindowsHardwareInfoProperty.replaceAll("[^0-9]", ""));
                                        } catch (NumberFormatException ramSlotTagException) {
                                            thisMemorySlotTag = -1;

                                            if (isTestMode) {
                                                System.out.println("ramSlotTagException: " + ramSlotTagException);
                                            }
                                        }
                                    } else if (thisWindowsHardwareInfoLine.startsWith("Capacity")) {
                                        String thisMemoryBankGB = "ERROR";

                                        try {
                                            long thisMemoryBankBytes = Long.parseLong(thisWindowsHardwareInfoProperty);
                                            ramTotalBytesFromBanks += thisMemoryBankBytes;

                                            thisMemoryBankGB = new DecimalFormat("#").format((thisMemoryBankBytes / 1024 / 1024 / 1024)) + " GB";
                                            if (thisMemoryBankGB.equals("0 GB")) {
                                                thisMemoryBankGB = "Empty";
                                            }
                                        } catch (NumberFormatException ramBankSizeException) {
                                            if (isTestMode) {
                                                System.out.println("ramBankSizeException: " + ramBankSizeException);
                                            }
                                        }

                                        if ((thisMemorySlotTag - lastMemorySlotTag) > 1) {
                                            for (int i = 0; i < (thisMemorySlotTag - lastMemorySlotTag - 1); i++) {
                                                if (ramSlots.isEmpty()) {
                                                    ramSlots = "Empty";
                                                } else {
                                                    ramSlots += " + Empty";
                                                }

                                                displayedRamSlotCount++;
                                            }
                                        }

                                        if (ramSlots.isEmpty()) {
                                            ramSlots = thisMemoryBankGB;
                                        } else {
                                            ramSlots += " + " + thisMemoryBankGB;
                                        }

                                        displayedRamSlotCount++;

                                        lastMemorySlotTag = thisMemorySlotTag;
                                    } else if (ramType.isEmpty() && (thisWindowsHardwareInfoLine.startsWith("MemoryType") || thisWindowsHardwareInfoLine.startsWith("SMBIOSMemoryType"))) {
                                        String memoryTypeCode = thisWindowsHardwareInfoProperty;

                                        // https://docs.microsoft.com/en-us/windows/win32/cimwin32prov/win32-physicalmemory
                                        // https://www.powershellgallery.com/packages/Systeminfo/1.1.4/Content/Scripts%5CMemory%5CMemoryModules.ps1
                                        switch (memoryTypeCode) {
                                            case "20":
                                                ramType = " DDR";
                                                break;
                                            case "21":
                                            case "22":
                                                ramType = " DDR2";
                                                break;
                                            case "24":
                                                ramType = " DDR3";
                                                break;
                                            case "26":
                                                ramType = " DDR4";
                                                break;
                                            case "27":
                                                ramType = " LP-DDR";
                                                break;
                                            case "28":
                                                ramType = " LP-DDR2";
                                                break;
                                            case "29":
                                                ramType = " LP-DDR3";
                                                break;
                                            case "30":
                                                ramType = " LP-DDR4";
                                                break;
                                            case "34": // This value is not listed in the documentation, but seen in practice on a system known to have DDR5 memory. (And also https://www.reddit.com/r/pcmasterrace/comments/16pohur/does_smbiosmemorytype_number_34_mean_ddr4_or_ddr5/)
                                                ramType = " DDR5";
                                                break;
                                            default:
                                                break;
                                        }
                                    } else if (ramSpeed.isEmpty() && thisWindowsHardwareInfoLine.startsWith("Speed") && !thisWindowsHardwareInfoProperty.equals("20307")) {
                                        // On Windows, Sony VIAO (SVD13215PBX) laptops showed "20307" as the RAM speed (and showed no speed on Linux), which is not correct so ignore it (like Linux). Not sure how to get the true RAM speed for those.
                                        ramSpeed = " @ " + thisWindowsHardwareInfoProperty.replaceAll("3[24]$", "33").replaceAll("67$", "66") + " MHz"; // Replace any MHz speeds that end with "32" or "34" to "33" and "67" to "66" to account for rounding differences (such as "1332 MHz" vs "1333 MHz" and "2667 MHz" vs "2666 MHz" etc) that would make the software show a slightly different number than what would be on the actual RAM label.
                                    } else if (thisWindowsHardwareInfoLine.startsWith("MaxVoltage")) {
                                        // TODO: Look into if this is ever gets filled.
                                    }

                                    break;
                                case "Get-PhysicalDisk":
                                    if (thisWindowsHardwareInfoLine.startsWith("HealthStatus")) {
                                        thisDriveHealthStatus = thisWindowsHardwareInfoProperty;
                                    } else if (thisWindowsHardwareInfoLine.startsWith("BusType")) {
                                        thisDriveBusType = thisWindowsHardwareInfoProperty;
                                    } else if (thisWindowsHardwareInfoLine.startsWith("MediaType")) {
                                        thisDriveMediaType = thisWindowsHardwareInfoProperty;

                                        if (thisDriveMediaType.equals("Unspecified")) {
                                            thisDriveMediaType = "";
                                        }
                                    } else if (thisWindowsHardwareInfoLine.startsWith("Model")) {
                                        thisDriveModel = thisWindowsHardwareInfoProperty.trim().replace("(", "").replace(")", "").replaceAll("\\s{2,}", " "); // NOTE: Remove all parens (which shouldn't normally exist anyways) to not break removing models when truncating storage line for PCsCRM.
                                    } else if (thisWindowsHardwareInfoLine.startsWith("SerialNumber")) {
                                        if (!thisWindowsHardwareInfoProperty.isEmpty() && !thisDriveBusType.equals("NVMe")) { // NVMe drive seem to not get the actual serial number filled in this field,
                                            storageSerialsArray.add(thisWindowsHardwareInfoProperty);
                                        }
                                    } else if (thisWindowsHardwareInfoLine.startsWith("FruId")) {
                                        if (!thisWindowsHardwareInfoProperty.isEmpty() && thisDriveBusType.equals("NVMe")) { // so use "FruId" for NVMe drive serial numbers instead (which matches the serial from "lsblk" in Linux): https://www.reddit.com/r/sysadmin/comments/1m1qv1j/powershell_wont_give_me_the_real_nvme_serial/
                                            storageSerialsArray.add(thisWindowsHardwareInfoProperty);
                                        }
                                    } else if (thisWindowsHardwareInfoLine.startsWith("Size")) {
                                        if (!thisDriveHealthStatus.equals("Healthy")) {
                                            driveHealthWarning = true;
                                        }

                                        try {
                                            double thisDriveGB = (Double.parseDouble(thisWindowsHardwareInfoProperty) / 1000 / 1000 / 1000);
                                            String thisDriveSize;
                                            if (thisDriveGB < 1000) {
                                                thisDriveSize = new DecimalFormat("#").format(thisDriveGB) + " GB";
                                            } else {
                                                thisDriveSize = new DecimalFormat("#").format((thisDriveGB / 1000)) + " TB";
                                            }

                                            String thisDisk = thisDriveSize + " " + (thisDriveBusType.equals("NVMe") ? "NVMe" : (thisDriveBusType.equals("SD") ? "eMMC" : (thisDriveBusType.equals("RAID") ? ("RAID" + (thisDriveMediaType.isEmpty() ? "" : " " + thisDriveMediaType)) : thisDriveMediaType)));

                                            if (!thisDriveModel.isEmpty()) {
                                                thisDisk += " (" + thisDriveModel + ")";
                                            }

                                            storageArray.add(thisDisk);
                                        } catch (NumberFormatException hardDriveSizeException) {
                                            if (isTestMode) {
                                                System.out.println("hardDriveSizeException: " + hardDriveSizeException);
                                            }
                                        }

                                        thisDriveHealthStatus = "";
                                        thisDriveBusType = "";
                                        thisDriveMediaType = "";
                                    }

                                    break;
                                case "Win32_VideoController":
                                    if (thisWindowsHardwareInfoLine.startsWith("InstalledDisplayDrivers")) {
                                        if (thisWindowsHardwareInfoProperty.isEmpty()) {
                                            String thisGPUerrorString = "DRIVER NOT INSTALLED";

                                            if (!gpuErrorArray.contains(thisGPUerrorString)) {
                                                gpuErrorArray.add(thisGPUerrorString);
                                            }
                                        }
                                    } else if (thisWindowsHardwareInfoLine.startsWith("AdapterCompatibility")) {
                                        thisGraphicsManufacturer = cleanDeviceBrand(thisWindowsHardwareInfoProperty);
                                    } else if (thisWindowsHardwareInfoLine.startsWith("Name")) {
                                        String thisGraphicsDevice = cleanDeviceModel(thisWindowsHardwareInfoProperty);

                                        if (!thisGraphicsManufacturer.isEmpty()) {
                                            if (thisGraphicsManufacturer.equals("(Standard display types)") || thisGraphicsDevice.equals("Microsoft Basic Display")) {
                                                String thisGPUerrorString = "DRIVER NOT INSTALLED";

                                                if (!gpuErrorArray.contains(thisGPUerrorString)) {
                                                    gpuErrorArray.add(thisGPUerrorString);
                                                }
                                            } else if (!thisGraphicsDevice.contains(thisGraphicsManufacturer)) {
                                                thisGraphicsDevice = thisGraphicsManufacturer + " " + thisGraphicsDevice;
                                            }

                                            thisGraphicsManufacturer = "";
                                        }

                                        if (!gpuArray.contains(thisGraphicsDevice)) { // TODO: Should this check against getFullGPU() instead of gpuArray?
                                            gpuArray.add(thisGraphicsDevice);
                                        }
                                    } else if (!isWindowsPE && thisWindowsHardwareInfoLine.startsWith("ConfigManagerErrorCode") && !thisWindowsHardwareInfoProperty.equals("0")) {
                                        String thisGPUerrorString = "DRIVER ERROR CODE " + thisWindowsHardwareInfoProperty;

                                        if (!gpuErrorArray.contains(thisGPUerrorString)) {
                                            gpuErrorArray.add(thisGPUerrorString);
                                        }
                                    } else if (!screenResolution.contains("x") && thisWindowsHardwareInfoLine.startsWith("CurrentHorizontalResolution")) {
                                        screenResolution = thisWindowsHardwareInfoProperty;
                                    } else if (!screenResolution.isEmpty() && !screenResolution.contains("x") && thisWindowsHardwareInfoLine.startsWith("CurrentVerticalResolution")) {
                                        screenResolution += "x" + thisWindowsHardwareInfoProperty;
                                    }

                                    break;
                                case "Win32_SoundDevice":
                                    if (thisWindowsHardwareInfoLine.startsWith("Manufacturer")) {
                                        thisAudioManufacturer = cleanDeviceBrand(thisWindowsHardwareInfoProperty);
                                    } else if (thisWindowsHardwareInfoLine.startsWith("ProductName")) {
                                        String thisAudioDevice = cleanDeviceModel(thisWindowsHardwareInfoProperty);

                                        if (!thisAudioManufacturer.isEmpty() && !thisAudioDevice.contains(thisAudioManufacturer)) {
                                            thisAudioDevice = thisAudioManufacturer + " " + thisAudioDevice;
                                        }

                                        if (!thisAudioDevice.equals("(Generic USB Audio) USB Audio")) {
                                            if (!audioArray.contains(thisAudioDevice)) { // TODO: Should this check against getFullAudio() instead of audioArray?
                                                audioArray.add(thisAudioDevice);
                                            }
                                        }

                                        thisAudioManufacturer = "";
                                    } else if (!isWindowsPE && thisWindowsHardwareInfoLine.startsWith("ConfigManagerErrorCode") && !thisWindowsHardwareInfoProperty.equals("0")) {
                                        String thisAudioErrorString = "DRIVER ERROR CODE " + thisWindowsHardwareInfoProperty;

                                        if (!audioErrorArray.contains(thisAudioErrorString)) {
                                            audioErrorArray.add(thisAudioErrorString);
                                        }
                                    }

                                    break;
                                case "Win32_PnPEntity":
                                case "Get-PnpDevice":
                                    if (thisWindowsHardwareInfoLine.startsWith("ConfigManagerErrorCode")) {
                                        pnpDeviceConfigManagerErrorCode = thisWindowsHardwareInfoProperty;
                                    } else if (thisWindowsHardwareInfoLine.startsWith("Name")) {
                                        pnpDeviceName = cleanDeviceModel(thisWindowsHardwareInfoProperty);
                                        if (pnpDeviceName.contains("touch screen") && !pnpDeviceConfigManagerErrorCode.equals("CM_PROB_PHANTOM")) {
                                            hasTouchscreen = true; // Ignore PHANTOM (Error Code 45) touch screens because it means an external touch screen was connected but isn't anymore.
                                        }
                                    } else if (thisWindowsHardwareInfoLine.startsWith("CompatibleIDwithClass")) {
                                        pnpDeviceCompatibleIDwithClass = thisWindowsHardwareInfoProperty;
                                    } else if (thisWindowsHardwareInfoLine.startsWith("DeviceID")) {
                                        if (!pnpDeviceCompatibleIDwithClass.isEmpty()) {
                                            // Ignore devices with no classes since I can't identify them
                                            // https://docs.microsoft.com/en-us/windows-hardware/drivers/install/identifiers-for-pci-devices
                                            // https://docs.microsoft.com/en-us/windows-hardware/drivers/install/standard-usb-identifiers

                                            String pnpDeviceClassCode = "XX";
                                            String pnpDeviceSubClassCode = "XX";
                                            String pnpDeviceProgProtCode = "XX";

                                            int ccIndex;
                                            int classIndex;
                                            if (((ccIndex = pnpDeviceCompatibleIDwithClass.indexOf("CC_")) > -1) && (pnpDeviceCompatibleIDwithClass.length() >= (ccIndex + 5))) {
                                                pnpDeviceClassCode = pnpDeviceCompatibleIDwithClass.substring(ccIndex + 3, ccIndex + 5).toLowerCase();

                                                if (pnpDeviceCompatibleIDwithClass.length() >= (ccIndex + 7)) {
                                                    pnpDeviceSubClassCode = pnpDeviceCompatibleIDwithClass.substring(ccIndex + 5, ccIndex + 7).toLowerCase();

                                                    if (pnpDeviceCompatibleIDwithClass.length() >= (ccIndex + 9)) {
                                                        pnpDeviceProgProtCode = pnpDeviceCompatibleIDwithClass.substring(ccIndex + 7, ccIndex + 9).toLowerCase();
                                                    }
                                                }
                                            } else if (((classIndex = pnpDeviceCompatibleIDwithClass.indexOf("Class_")) > -1) && (pnpDeviceCompatibleIDwithClass.length() >= (classIndex + 8))) {
                                                pnpDeviceClassCode = pnpDeviceCompatibleIDwithClass.substring(classIndex + 6, classIndex + 8).toLowerCase();

                                                int subClassIndex;
                                                if (((subClassIndex = pnpDeviceCompatibleIDwithClass.indexOf("SubClass_")) > -1) && (pnpDeviceCompatibleIDwithClass.length() >= (subClassIndex + 11))) {
                                                    pnpDeviceSubClassCode = pnpDeviceCompatibleIDwithClass.substring(subClassIndex + 9, subClassIndex + 11).toLowerCase();
                                                }

                                                int protIndex;
                                                if (((protIndex = pnpDeviceCompatibleIDwithClass.indexOf("Prot_")) > -1) && (pnpDeviceCompatibleIDwithClass.length() >= (protIndex + 7))) {
                                                    pnpDeviceProgProtCode = pnpDeviceCompatibleIDwithClass.substring(protIndex + 5, protIndex + 7).toLowerCase();
                                                }
                                            }

                                            //System.out.println("DeviceID: " + thisWindowsHardwareInfoProperty);
                                            //System.out.println("CompatibleIDwithClass: " + pnpDeviceCompatibleIDwithClass);
                                            //System.out.println("Class: " + pnpDeviceClassCode + " - SubClass: " + pnpDeviceSubClassCode + " - ProgProt: " + pnpDeviceProgProtCode);
                                            if (thisWindowsHardwareInfoProperty.contains("VEN_") && thisWindowsHardwareInfoProperty.contains("DEV_")) {
                                                // Check PCI Device Classes
                                                int venIndex;
                                                int devIndex;

                                                // PCI Classes: https://pci-ids.ucw.cz/read/PD/
                                                switch (pnpDeviceClassCode) {
                                                    case "03": // Display
                                                        if ((venIndex = thisWindowsHardwareInfoProperty.indexOf("VEN_")) > -1 && (devIndex = thisWindowsHardwareInfoProperty.indexOf("DEV_")) > -1) {
                                                            String thisGPUid = thisWindowsHardwareInfoProperty.substring(venIndex + 4, venIndex + 8).toLowerCase().replaceAll("[^a-z0-9]", "") + "-" + thisWindowsHardwareInfoProperty.substring(devIndex + 4, devIndex + 8).toLowerCase().replaceAll("[^a-z0-9]", "");

                                                            // gpuIDs are for used for the Drivers Cache Model Name
                                                            if (!thisGPUid.equals("1414-5353") && (!pnpDeviceConfigManagerErrorCode.equals("CM_PROB_PHANTOM") || thisGPUid.startsWith("8086-"))) {
                                                                // Ignore Microsoft Hyper-V virtual VGA https://pci-ids.ucw.cz/read/PC/1414/5353
                                                                // And ignore any PHANTOM (Error Code 45) devices EXCEPT for Intel devices (since they would likely be on the motherboard and not from a previously installed and them removed/replaced GPU).
                                                                // This is because all the onboard hardware will be listed in WinPE when we the Drivers Cache Model Name is used to install drivers from the Drivers Cache, but then some onboard devices may become PHANTOM after drivers are installed in the OS. But, we still want to maintain consistent Drivers Cache Model Name in both cases.

                                                                // Do just a few of the most common replacements for codes to brand names.
                                                                thisGPUid = thisGPUid.replace("8086-", "Intel-").replace("10de-", "NVIDIA-").replace("1002-", "AMD-").replace("1022-", "AMD-");

                                                                if (!gpuIDsArray.contains(thisGPUid)) {
                                                                    gpuIDsArray.add(thisGPUid);
                                                                }
                                                            }
                                                        }

                                                        if (!pnpDeviceConfigManagerErrorCode.equals("CM_PROB_PHANTOM")) {
                                                            // Want some PHANTOM (Error Code 45) devices for gpuIDs, but not listed in GPU field.
                                                            if (gpuArray.isEmpty() || gpuErrorArray.contains("DRIVER NOT INSTALLED") || getFullGPU().replaceAll("[^0-9]", "").isEmpty()) {
                                                                String thisGPUdeviceName = getCleanNameForWindowsDeviceID(thisWindowsHardwareInfoProperty, pnpDeviceName);

                                                                if (!pnpGPU.contains(thisGPUdeviceName)) {
                                                                    pnpGPU.add(thisGPUdeviceName);
                                                                }
                                                            }
                                                        }

                                                        break;
                                                    case "04": // Multimedia
                                                        if (pnpDeviceSubClassCode.equals("01") || pnpDeviceSubClassCode.equals("03")) {
                                                            // Only use "Multimedia audio controller" or "Audio device"
                                                            if ((venIndex = thisWindowsHardwareInfoProperty.indexOf("VEN_")) > -1 && (devIndex = thisWindowsHardwareInfoProperty.indexOf("DEV_")) > -1) {
                                                                String thisAudioID = thisWindowsHardwareInfoProperty.substring(venIndex + 4, venIndex + 8).toLowerCase().replaceAll("[^a-z0-9]", "") + "-" + thisWindowsHardwareInfoProperty.substring(devIndex + 4, devIndex + 8).toLowerCase().replaceAll("[^a-z0-9]", "");

                                                                // audioIDs are for used for the Drivers Cache Model Name
                                                                if (!pnpDeviceConfigManagerErrorCode.equals("CM_PROB_PHANTOM") || thisAudioID.startsWith("8086-")) {
                                                                    // Ignore any PHANTOM (Error Code 45) devices EXCEPT for Intel devices (since they would likely be on the motherboard and not from a previously installed and them removed/replaced GPU).
                                                                    // This is because all the onboard hardware will be listed in WinPE when we the Drivers Cache Model Name is used to install drivers from the Drivers Cache, but then some onboard devices may become PHANTOM after drivers are installed in the OS. But, we still want to maintain consistent Drivers Cache Model Name in both cases.

                                                                    // Do just a few of the most common replacements for codes to brand names.
                                                                    thisAudioID = thisAudioID.replace("8086-", "Intel-").replace("10de-", "NVIDIA-").replace("1002-", "AMD-").replace("1022-", "AMD-");

                                                                    if (!audioIDsArray.contains(thisAudioID)) {
                                                                        audioIDsArray.add(thisAudioID);
                                                                    }
                                                                }
                                                            }

                                                            if (!pnpDeviceConfigManagerErrorCode.equals("CM_PROB_PHANTOM")) {
                                                                // Want some PHANTOM (Error Code 45) devices for audioIDs, but not listed in Audio field.
                                                                String thisAudioDeviceName = getCleanNameForWindowsDeviceID(thisWindowsHardwareInfoProperty, pnpDeviceName);

                                                                if (!pnpAudio.contains(thisAudioDeviceName)) {
                                                                    pnpAudio.add(thisAudioDeviceName);
                                                                }
                                                            }
                                                        }

                                                        // TODO: Should I check SubClass 80 (Multimedia) for Camera? The FaceTime camera seems to use this
                                                        break;
                                                    case "02": // Network
                                                    case "0d": // Wireless
                                                        String currentWirlessString = getWireless();

                                                        if (!currentWirlessString.contains("Wi-Fi Detected") || !currentWirlessString.contains("Cellular Detected") || !currentWirlessString.contains("Bluetooth Detected")) {
                                                            String thisNetworkDeviceName = getCleanNameForWindowsDeviceID(thisWindowsHardwareInfoProperty, pnpDeviceName);
                                                            String lowercaseNetworkDeviceName = thisNetworkDeviceName.toLowerCase();

                                                            String thisWirelessDeviceName = "";

                                                            if ((!currentWirlessString.contains("Wi-Fi Detected") && !lowercaseNetworkDeviceName.contains("bluetooth") && (lowercaseNetworkDeviceName.contains("wireless") || lowercaseNetworkDeviceName.contains("wi-fi") || lowercaseNetworkDeviceName.contains("wifi") || lowercaseNetworkDeviceName.contains("802.11"))) || (!currentWirlessString.contains("Cellular Detected") && (lowercaseNetworkDeviceName.contains("mobile") || lowercaseNetworkDeviceName.contains("cellular") || lowercaseNetworkDeviceName.contains("wwan") || lowercaseNetworkDeviceName.replace("realtek", "").contains("lte"))) || (!currentWirlessString.contains("Bluetooth Detected") && lowercaseNetworkDeviceName.contains("bluetooth"))) {
                                                                thisWirelessDeviceName = thisNetworkDeviceName;
                                                            } else if (pnpDeviceClassCode.equals("02")) {
                                                                if (!currentWirlessString.contains("Wi-Fi Detected") && pnpDeviceSubClassCode.equals("80")) {
                                                                    // SubClass: Network (SubClass 00 is Ethernet, so is it safe to assume Network not Ethernet is Wi-Fi?)
                                                                    thisWirelessDeviceName = "Possible Unidentified Wi-Fi";
                                                                }
                                                            } else if (pnpDeviceClassCode.equals("0d")) {
                                                                // Seems like "40" may be the subclass for cellular cards under Wireless "0d", but don't have enough data to know for sure.

                                                                if (!currentWirlessString.contains("Bluetooth Detected") && pnpDeviceSubClassCode.equals("11")) {
                                                                    // SubClass: Bluetooth
                                                                    thisWirelessDeviceName = "Unidentified Bluetooth";
                                                                } else if (!currentWirlessString.contains("Wi-Fi Detected") && pnpDeviceSubClassCode.equals("80")) {
                                                                    // SubClass: Wireless
                                                                    thisWirelessDeviceName = "Unidentified Wi-Fi";
                                                                }
                                                            }

                                                            if (!thisWirelessDeviceName.isEmpty() && !pnpWireless.contains(thisWirelessDeviceName)) {
                                                                pnpWireless.add(thisWirelessDeviceName);
                                                            }
                                                        }

                                                        break;
                                                    default:
                                                        break;
                                                }
                                            } else if (thisWindowsHardwareInfoProperty.contains("VID_") && thisWindowsHardwareInfoProperty.contains("PID_")) {
                                                // Check USB Device Classes
                                                // USB Classes: https://usb-ids.gowdy.us/read/UC
                                                switch (pnpDeviceClassCode) {
                                                    case "0e": // Video
                                                        hasCamera = true;
                                                        hasScreen = true; // Assume any connected Camera means there's a built-in screen as well (because some All-in-Ones don't use the correct Chassis Type).

                                                        break;
                                                    case "02": // Communications
                                                    case "e0": // Wireless
                                                        String currentWirlessString = getWireless();

                                                        if (!currentWirlessString.contains("Wi-Fi Detected") || !currentWirlessString.contains("Cellular Detected") || !currentWirlessString.contains("Bluetooth Detected")) {
                                                            String thisPossibleWirelessDeviceName = getCleanNameForWindowsDeviceID(thisWindowsHardwareInfoProperty, pnpDeviceName);
                                                            String lowercasePossibleWirelessDeviceName = thisPossibleWirelessDeviceName.toLowerCase();

                                                            String thisWirelessDeviceName = "";

                                                            if ((!currentWirlessString.contains("Wi-Fi Detected") && !lowercasePossibleWirelessDeviceName.contains("bluetooth") && (lowercasePossibleWirelessDeviceName.contains("wireless") || lowercasePossibleWirelessDeviceName.contains("wi-fi") || lowercasePossibleWirelessDeviceName.contains("wifi") || lowercasePossibleWirelessDeviceName.contains("802.11"))) || (!currentWirlessString.contains("Cellular Detected") && (lowercasePossibleWirelessDeviceName.contains("mobile") || lowercasePossibleWirelessDeviceName.contains("cellular") || lowercasePossibleWirelessDeviceName.contains("wwan") || lowercasePossibleWirelessDeviceName.replace("realtek", "").contains("lte"))) || (!currentWirlessString.contains("Bluetooth Detected") && lowercasePossibleWirelessDeviceName.contains("bluetooth"))) {
                                                                thisWirelessDeviceName = thisPossibleWirelessDeviceName;
                                                            } else if (!currentWirlessString.contains("Bluetooth Detected") && pnpDeviceSubClassCode.equals("01") && pnpDeviceProgProtCode.equals("01")) {
                                                                // SubClass: RF - Programming Interface: Bluetooth
                                                                thisWirelessDeviceName = "Unidentified Bluetooth";
                                                            }

                                                            if (!thisWirelessDeviceName.isEmpty() && !pnpWireless.contains(thisWirelessDeviceName)) {
                                                                pnpWireless.add(thisWirelessDeviceName);
                                                            }
                                                        }

                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }
                                        }

                                        pnpDeviceConfigManagerErrorCode = "";
                                        pnpDeviceCompatibleIDwithClass = "";
                                    }

                                    break;
                                case "Win32_Battery":
                                    if (thisWindowsHardwareInfoLine.startsWith("DesignVoltage")) {
                                        windowsBatteryDesignVoltages.add(thisWindowsHardwareInfoProperty);
                                    } else if (thisWindowsHardwareInfoLine.startsWith("EstimatedChargeRemaining")) {
                                        windowsBatteryChargePercentages.add(thisWindowsHardwareInfoProperty);
                                    }

                                    break;
                                case "BatteryStatus":
                                    if (thisWindowsHardwareInfoLine.startsWith("ChargeRate")) {
                                        windowsBatteryChargeRates.add(thisWindowsHardwareInfoProperty);
                                    } else if (thisWindowsHardwareInfoLine.startsWith("Charging")) {
                                        windowsBatteryChargingStatuses.add(thisWindowsHardwareInfoProperty);
                                    } else if (thisWindowsHardwareInfoLine.startsWith("DischargeRate")) {
                                        windowsBatteryDischargeRates.add(thisWindowsHardwareInfoProperty);
                                    } else if (thisWindowsHardwareInfoLine.startsWith("Voltage")) {
                                        windowsBatteryCurrentVoltages.add(thisWindowsHardwareInfoProperty);
                                    }

                                    break;
                                case "BatteryStaticData":
                                    if (thisWindowsHardwareInfoLine.startsWith("DesignedCapacity")) {
                                        windowsBatteryDesignCapacities.add(thisWindowsHardwareInfoProperty);
                                    }

                                    break;
                                case "BatteryFullChargedCapacity":
                                    if (thisWindowsHardwareInfoLine.startsWith("FullChargedCapacity")) {
                                        windowsBatteryFullChargeCapacities.add(thisWindowsHardwareInfoProperty);
                                    }

                                    break;
                                case "BatteryCycleCount":
                                    if (thisWindowsHardwareInfoLine.startsWith("CycleCount")) {
                                        windowsBatteryCycleCounts.add(thisWindowsHardwareInfoProperty);
                                    }

                                    break;
                                case "WmiMonitorBasicDisplayParams":
                                    if (hasScreen && screenSize.equals("N/A")) {
                                        if (thisMaxHorizontalImageSizeString.isEmpty() && thisWindowsHardwareInfoLine.startsWith("MaxHorizontalImageSize")) {
                                            thisMaxHorizontalImageSizeString = thisWindowsHardwareInfoProperty;
                                        } else if (thisWindowsHardwareInfoLine.startsWith("MaxVerticalImageSize")) {
                                            try {
                                                double diagonalScreenSize = (Math.sqrt(Math.pow(Double.parseDouble(thisMaxHorizontalImageSizeString), 2) + Math.pow(Double.parseDouble(thisWindowsHardwareInfoProperty), 2)) / 2.54);

                                                screenSize = new DecimalFormat("#.#").format(diagonalScreenSize);

                                                if (screenSize.equals("0")) {
                                                    screenSize = "N/A";
                                                } else {
                                                    if (screenSize.endsWith(".9")) {
                                                        screenSize = new DecimalFormat("#").format(diagonalScreenSize);
                                                    }
                                                    screenSize += "-inch";
                                                }
                                            } catch (NumberFormatException screenSizeException) {
                                                if (isTestMode) {
                                                    System.out.println("screenSizeException: " + screenSizeException);
                                                }
                                            }

                                            thisMaxHorizontalImageSizeString = "";
                                        }
                                    }

                                    break;
                                case "Win32_CDROMDrive":
                                    if (thisDiscDriveName.isEmpty() && thisWindowsHardwareInfoLine.startsWith("Name")) {
                                        thisDiscDriveName = thisWindowsHardwareInfoProperty.trim().replaceAll("\\s{2,}", " ");
                                    } else if (thisDiscDriveLetter.isEmpty() && thisWindowsHardwareInfoLine.startsWith("Drive")) {
                                        thisDiscDriveLetter = thisWindowsHardwareInfoProperty;
                                    } else if (thisDiscDriveCapabilityDescriptions.isEmpty() && thisWindowsHardwareInfoLine.startsWith("CapabilityDescriptions")) {
                                        thisDiscDriveCapabilityDescriptions = thisWindowsHardwareInfoProperty;
                                    } else if (thisDiscDriveMediaType.isEmpty() && thisWindowsHardwareInfoLine.startsWith("MediaType")) {
                                        thisDiscDriveMediaType = thisWindowsHardwareInfoProperty;
                                    } else if (thisWindowsHardwareInfoLine.startsWith("MediaLoaded")) {
                                        String thisDiscDriveMediaLoaded = thisWindowsHardwareInfoProperty;

                                        if (!thisDiscDriveName.endsWith("USB Device")) {
                                            discDriveArray.add(thisDiscDriveName);

                                            discDriveLogicalNames.add(thisDiscDriveLetter);

                                            if (thisDiscDriveCapabilityDescriptions.contains("Supports writing")) {
                                                discDriveCanBurnCDs = true;
                                            }

                                            if (thisDiscDriveMediaType.contains("DVD Writer")) {
                                                discDriveCanBurnDVDs = true;
                                            }

                                            if (thisDiscDriveMediaLoaded.equals("True")) {
                                                hasDiscInDiscDrive = true;
                                            }
                                        }

                                        thisDiscDriveName = "";
                                        thisDiscDriveLetter = "";
                                        thisDiscDriveCapabilityDescriptions = "";
                                        thisDiscDriveMediaType = "";
                                    }

                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }

                // The "ver" command seems to be the only way that I could find to get the revision of the build which is not included in osBuildNumber and indicates the latest Cumulative Update that's been installed.
                String fullWindowsBuildWithRevision = new CommandReader("VER").getFirstOutputLineContaining("[Version");
                if (!fullWindowsBuildWithRevision.isEmpty()) {
                    String[] windowsVersionParts = fullWindowsBuildWithRevision.replaceAll("[^0-9.]", "").split("\\.");
                    if (windowsVersionParts.length > 0) {
                        String osBuildRevision = windowsVersionParts[windowsVersionParts.length - 1];
                        if (!osBuildRevision.equals(osBuildNumber) && !osBuildRevision.equals("0") && !osBuildRevision.equals("10")) {
                            osBuildNumber += "." + osBuildRevision;
                        }
                    }
                }

                if (!os.contains(" - Version ")) {
                    os += " - Build " + osBuildNumber;
                } else {
                    os += " (Build " + osBuildNumber + ")";
                }

                if (ramMaxKilobytes > 0L && ramMaxKilobytes >= (ramTotalBytesFromBanks / 1024)) {
                    maxRAM = " - " + new DecimalFormat("#").format((ramMaxKilobytes / 1024 / 1024)) + " GB Max";
                }

                if (displayedRamSlotCount < ramSlotCount) {
                    for (int i = 0; i < (ramSlotCount - displayedRamSlotCount); i++) {
                        if (ramSlots.isEmpty()) {
                            ramSlots = "Empty";
                        } else {
                            ramSlots += " + Empty";
                        }
                    }
                }

                Collections.sort(gpuIDsArray); // Make sure gpuIDs and audioIDs are always in the same order for Drivers Cache Model Name
                Collections.sort(audioIDsArray);

                if (!isWindowsPE && getFullAudio().equals("Microsoft HD Audio")) { // WinPE would never hit this, but *WinRE* does.
                    // Consider this an audio driver WARNING if "Microsoft HD Audio" is the ONLY device listed.
                    String thisAudioErrorString = "DRIVER MAY NOT BE INSTALLED";

                    if (!audioErrorArray.contains(thisAudioErrorString)) {
                        audioErrorArray.add(thisAudioErrorString);
                    }
                }

                if (!pnpGPU.isEmpty()) {
                    // pnpGPU will only get filled if GPU is N/A or DRIVER NOT INSTALLED or if GPU model doesn't contain any numbers.
                    if (gpuArray.isEmpty() || gpuErrorArray.contains("DRIVER NOT INSTALLED")) {
                        gpuArray.addAll(pnpGPU);

                        if (!isWindowsPE) {
                            String thisGPUerrorString = "DRIVER NOT INSTALLED";

                            if (!gpuErrorArray.contains(thisGPUerrorString)) {
                                gpuErrorArray.add(thisGPUerrorString);
                            }
                        }
                    } else {
                        String pnpGPUString = String.join(" + ", pnpGPU);
                        if (!pnpGPUString.contains("Device ") && !pnpGPUString.replaceAll("[^0-9]", "").isEmpty() && !getFullGPU().contains(pnpGPUString)) {
                            // If GPU doesn't contain any numbers and pnpGPU does (but not just Device ####), add on the pnpGPU.

                            int lastGPUIndex = (gpuArray.size() - 1);
                            String lastGPU = gpuArray.get(lastGPUIndex);
                            lastGPU += " (" + pnpGPUString + ")";
                            gpuArray.set(lastGPUIndex, lastGPU);
                        }
                    }
                }

                if (!pnpAudio.isEmpty()) {
                    if (audioArray.isEmpty()) {
                        audioArray.addAll(pnpAudio);

                        if (!isWindowsPE) {
                            String thisAudioErrorString = "DRIVER NOT INSTALLED";

                            if (!audioErrorArray.contains(thisAudioErrorString)) {
                                audioErrorArray.add(thisAudioErrorString);
                            }
                        }
                    } else {
                        String pnpAudioString = String.join(" + ", pnpAudio);

                        if (!getFullAudio().contains(pnpAudioString)) {
                            int lastAudioIndex = (audioArray.size() - 1);
                            String lastAudio = audioArray.get(lastAudioIndex);
                            lastAudio += " (" + pnpAudioString + ")";
                            audioArray.set(lastAudioIndex, lastAudio);
                        }
                    }
                }

                if (!pnpWireless.isEmpty()) {
                    for (String thisPnpWireless : pnpWireless) {
                        String thisLowercasePnpWireless = thisPnpWireless.toLowerCase();
                        // pnpWireless will get filled when Wi-Fi or Bluetooth was not detected probably because in WinPE or no drivers were installed in OS.

                        String currentWirlessString = getWireless();

                        if (!currentWirlessString.contains("Wi-Fi Detected") && !thisLowercasePnpWireless.contains("bluetooth") && (thisLowercasePnpWireless.contains("wireless") || thisLowercasePnpWireless.contains("wi-fi") || thisLowercasePnpWireless.contains("wifi") || thisLowercasePnpWireless.contains("802.11"))) {
                            wirelessArray.add(0, "Wi-Fi Detected" + (thisLowercasePnpWireless.contains("unidentified") ? "" : " (" + thisPnpWireless + ")"));
                        }

                        if (!currentWirlessString.contains("Cellular Detected") && (thisLowercasePnpWireless.contains("mobile") || thisLowercasePnpWireless.contains("cellular") || thisLowercasePnpWireless.contains("wwan") || thisLowercasePnpWireless.replace("realtek", "").contains("lte"))) {
                            wirelessArray.add("Cellular Detected" + (thisLowercasePnpWireless.contains("generic mobile broadband") ? "" : " (" + thisPnpWireless + ")"));
                        }

                        if (!currentWirlessString.contains("Bluetooth Detected") && thisLowercasePnpWireless.contains("bluetooth")) {
                            wirelessArray.add("Bluetooth Detected" + (thisLowercasePnpWireless.contains("unidentified") ? "" : " (" + thisPnpWireless + ")"));
                        }
                    }

                    if (!isWindowsPE) {
                        String thisWirelessErrorString = "DRIVER NOT INSTALLED";

                        if (!wirelessErrorArray.contains(thisWirelessErrorString)) {
                            wirelessErrorArray.add(thisWirelessErrorString);
                        }
                    }
                }

                if (new File("\\Install\\Diagnostic Tools\\btVersion_x64.exe").exists()) {
                    // Could never figure out how to get Bluetooth *VERSION* from CMD or PowerShell,
                    // but "Bluetooth Version Finder" (btVersion) v1.4 adds a CLI interface that can be used to get the BT version:
                    // https://www.sordum.org/10772/bluetooth-version-finder-v1-4/

                    String bluetoothVersion = new CommandReader(new String[]{"\\Install\\Diagnostic Tools\\btVersion_x64.exe", "/V"}).getFirstOutputLine(); // This will return nothing if NOT running as admin (such as in audit mode), or if is pre-1.4 btVersion (but the GUI app will launch in that case if running as admin).

                    if (bluetoothVersion.matches("^[0-9][.0-9]*$")) {
                        String bluetoothVersionDetectedString = "Bluetooth " + bluetoothVersion + " Detected";

                        int existingBluetoothDetectedIndex = wirelessArray.indexOf("Bluetooth Detected");
                        if (existingBluetoothDetectedIndex >= 0) {
                            wirelessArray.set(existingBluetoothDetectedIndex, bluetoothVersionDetectedString);
                        } else {
                            wirelessArray.add(bluetoothVersionDetectedString);
                        }
                    }
                }

                if (!windowsBatteryDesignCapacities.isEmpty()) {
                    if (windowsBatteryFullChargeCapacities.size() == windowsBatteryDesignCapacities.size()) {
                        for (int i = 0; i < windowsBatteryDesignCapacities.size(); i++) {
                            String thisBatteryCapacity = "";

                            try {
                                double thisBatteryDesignCapacity = Double.parseDouble(windowsBatteryDesignCapacities.get(i));
                                double thisBatteryFullChargeCapacity = Double.parseDouble(windowsBatteryFullChargeCapacities.get(i));

                                double thisBatteryCapacityDouble = 0;
                                if (thisBatteryDesignCapacity > 0 && thisBatteryFullChargeCapacity > 0) {
                                    thisBatteryCapacityDouble = ((thisBatteryFullChargeCapacity / thisBatteryDesignCapacity) * 100);
                                }

                                thisBatteryCapacity = new DecimalFormat("#.#").format(thisBatteryCapacityDouble) + "%";

                                if (thisBatteryCapacity.startsWith("0")) {
                                    thisBatteryCapacity = "NO POWER";
                                    batteryHealthErrorArray.add("No Power");
                                } else if (thisBatteryCapacityDouble < 40) {
                                    batteryHealthErrorArray.add("Capacity < 40%");
                                } else if (thisBatteryCapacityDouble < 60) {
                                    batteryHealthWarningArray.add("Capacity < 60%");
                                } else if (thisBatteryCapacity.equals("100%") && brand.equals("HP")) { // Some HP firmwares list their design capacity as whatever the full charge capacity is always resulting in exactly "100%" capacity. Sometimes updating firmware can correct this.
                                    thisBatteryCapacity = "UNKNOWN CAPACITY";
                                    batteryHealthWarningArray.add("HP Firmware Update Required for Accurate Battery Percentage");
                                }
                            } catch (NumberFormatException batteryCapacityException) {
                                if (isTestMode) {
                                    System.out.println("batteryCapacityException: " + batteryCapacityException);
                                }
                            }

                            String thisBatteryVoltage = "";
                            if (windowsBatteryDesignVoltages.size() == windowsBatteryDesignCapacities.size()) {
                                try {
                                    double batteryVoltageDouble = (Double.parseDouble(windowsBatteryCurrentVoltages.get(i)) / 1000);
                                    thisBatteryVoltage = new DecimalFormat("#.#").format(batteryVoltageDouble) + " V";

                                    double batteryDesignVoltageDouble = (Double.parseDouble(windowsBatteryDesignVoltages.get(i)) / 1000);
                                    if (batteryVoltageDouble < (batteryDesignVoltageDouble - 0.1)) {
                                        batteryHealthWarningArray.add("Low Voltage");
                                        thisBatteryVoltage += " of " + new DecimalFormat("#.#").format(batteryDesignVoltageDouble) + " V";
                                    }
                                } catch (NumberFormatException batteryVoltageException) {
                                    if (isTestMode) {
                                        System.out.println("batteryVoltageException: " + batteryVoltageException);
                                    }
                                }
                            }

                            String thisBatteryCycleCount = "";
                            if (windowsBatteryCycleCounts.size() == windowsBatteryDesignCapacities.size()) {
                                try {
                                    int batteryCycleCount = Integer.parseInt(windowsBatteryCycleCounts.get(i));
                                    if (batteryCycleCount > 0) {
                                        thisBatteryCycleCount = batteryCycleCount + " Cycle" + ((batteryCycleCount == 1) ? "" : "s");

                                        if (batteryCycleCount >= 1000) {
                                            batteryHealthErrorArray.add("Cycles >= 1000");
                                        } else if (batteryCycleCount >= 800) {
                                            batteryHealthWarningArray.add("Cycles >= 800");
                                        }
                                    }
                                } catch (NumberFormatException batteryCycleCountException) {
                                    if (isTestMode) {
                                        System.out.println("batteryCycleCountException: " + batteryCycleCountException);
                                    }
                                }
                            }

                            if ((windowsBatteryDischargeRates.size() == windowsBatteryDesignCapacities.size()) && (windowsBatteryChargeRates.size() == windowsBatteryDesignCapacities.size()) && (windowsBatteryChargePercentages.size() == windowsBatteryDesignCapacities.size())
                                    && windowsBatteryDischargeRates.get(i).startsWith("0") && windowsBatteryChargeRates.get(i).startsWith("0") && windowsBatteryChargePercentages.get(i).startsWith("0")) {
                                // Even if the capacity is not "0%", the battery may still be dead with no energy charging or discharging.

                                if (thisBatteryVoltage.startsWith("0")) { // If voltage is 0, there may be NO BATTERY installed as I've seen some systems list a battery with all zero values when no battery is installed.
                                    thisBatteryCapacity = "";
                                } else if (!thisBatteryCapacity.equals("NO POWER")) { // Otherwise, if there is no charge and no energy (charging or draining) then this is probably a DEAD BATTERY.
                                    thisBatteryCapacity = "NO POWER";
                                    batteryHealthErrorArray.add("No Power");
                                }
                            }

                            if (!thisBatteryCapacity.isEmpty()) {
                                try {
                                    if ((windowsBatteryChargingStatuses.size() == windowsBatteryDesignCapacities.size()) && (windowsBatteryDischargeRates.size() == windowsBatteryDesignCapacities.size()) && (windowsBatteryChargeRates.size() == windowsBatteryDesignCapacities.size()) && (windowsBatteryChargePercentages.size() == windowsBatteryDesignCapacities.size())
                                            && (windowsBatteryChargingStatuses.get(i).equals("True") || windowsBatteryDischargeRates.get(i).startsWith("0")) && windowsBatteryChargeRates.get(i).startsWith("0") && (Integer.parseInt(windowsBatteryChargePercentages.get(i)) < 94)) { // Seen HP laptops at 94-99% that are not currently charging because the system seems to consider it fully charge.

                                        batteryHealthWarningArray.add("Not Charging");
                                    }
                                } catch (NumberFormatException parseChargePercentageException) {
                                    if (isTestMode) {
                                        System.out.println("parseChargePercentageException: " + parseChargePercentageException);
                                    }
                                }

                                batteryHealthArray.add(thisBatteryCapacity + (thisBatteryVoltage.isEmpty() ? (thisBatteryCycleCount.isEmpty() ? "" : " (" + thisBatteryCycleCount + ")") : " (" + (thisBatteryCycleCount.isEmpty() ? "" : thisBatteryCycleCount + ", ") + thisBatteryVoltage + ")"));
                            }
                        }

                    }
                }
            } else if (isMacOS) {
                // Mac Info

                brand = "Apple";

                // CPU for Mac
                cpuBrand = cleanCPU(new CommandReader(new String[]{"/usr/sbin/sysctl", "-n", "machdep.cpu.vendor"}).getFirstOutputLine());
                cpu = cleanCPU(new CommandReader(new String[]{"/usr/sbin/sysctl", "-n", "machdep.cpu.brand_string"}).getFirstOutputLine());

                // A "Hyper-Threading Technology" key is available in system_profiler's SPHardwareDataType on macOS 10.14 Mojave and newer,
                // but not bothering to use is since the Linux and Windows code for checking for hyper threading uses the thread count vs core count.
                // No need to add a special hyper threading check just for Mac and only when on Mojave and newer when we can do the same thing across all operating systems and macOS version.
                cpuThreads = new CommandReader(new String[]{"/usr/sbin/sysctl", "-n", "hw.logicalcpu_max"}).getFirstOutputLine();

                boolean isAppleSiliconRosetta = (cpu.contains("Apple") && new CommandReader(new String[]{"/usr/sbin/sysctl", "-in", "sysctl.proc_translated"}).getFirstOutputLine().equals("1")); // https://developer.apple.com/documentation/apple_silicon/about_the_rosetta_translation_environment?language=objc
                // If we are running on Apple Silicon under Rosetta, "system_profiler" will not return true/full information about Processor/Chip and RAM.
                // This is because any child process launched from a Rosetta process will also run under Rosetta.
                // But, we can launch "system_profiler" using the "arch" command as "arch -arm64e system_profiler ..." which will force "system_profiler" to run natively on Apple Silicon instead of running under Rosetta.

                if (cpu.isEmpty()) {
                    cpu = "N/A";
                } else if (cpu.equals("Apple")) { // Just "Apple processor" ("processor" is removed in "cleanCPU()") would be returned from machdep.cpu.brand_string in macOS 11 prior to 11.1 when Apple Silicon was running natively. In 11.1 and later, the actual processor name (such as "Apple M1") is returned.
                    cpu = "N/A"; // We will get CPU from "Chip" in system_profiler's SPHardwareDataType instead.
                } else if (cpu.startsWith("VirtualApple")) { // "VirtualApple @ 2.50GHz processor" would be returned from machdep.cpu.brand_string in macOS 11 prior to 11.1 when Apple Silicon was running under Rosetta. In 11.1 and later, the actual processor name (such as "Apple M1") is returned.
                    cpu = "N/A"; // We will get CPU from "Chip" in system_profiler's SPHardwareDataType instead.

                    isAppleSiliconRosetta = true; // This should already be set to true by the sysctl.proc_translated check above, but set it here anyway.
                }

                ArrayList<String> systemProfilerCommand = new ArrayList<>();
                if (isAppleSiliconRosetta) {
                    systemProfilerCommand.addAll(Arrays.asList("/usr/bin/arch", "-arm64e"));
                }
                systemProfilerCommand.addAll(Arrays.asList("/usr/sbin/system_profiler", "SPHardwareDataType", "SPAudioDataType", "SPAirPortDataType", "SPBluetoothDataType", "SPDiscBurningDataType", "SPDisplaysDataType", "SPMemoryDataType", "SPNetworkDataType", "SPPowerDataType", "SPNVMeDataType", "SPSerialATADataType"));

                String[] macHardwareInfo = new CommandReader(systemProfilerCommand.toArray(String[]::new)).getOutputLines();

                String processorTraySerial = "N/A";
                String thisStorageDriveInfo = "";
                String thisDriveModel = "";
                String thisGPUmodel = "Unknown";
                String thisGPUbus = "";
                String thisBatteryCycleCount = "N/A";
                String thisBatteryHealth = "N/A";

                String thisWifiInfo = "";
                boolean bluetoothDetected = false;
                String bluetoothHCIversion = "";
                String bluetoothLMPversion = "";
                boolean bluetoothLE = false;
                boolean bluetoothHandoff = false;

                String thisMacHardwareInfoCategory = "Unknown";
                String thisStorageController = "Unknown";
                String thisNetworkController = "Unknown";
                String thisPowerSection = "Unknown";

                for (String thisMacHardwareInfoLine : macHardwareInfo) {
                    if (!thisMacHardwareInfoLine.startsWith(" ") && thisMacHardwareInfoLine.endsWith(":")) {
                        thisMacHardwareInfoCategory = thisMacHardwareInfoLine;
                        continue;
                    }

                    if (thisMacHardwareInfoLine.contains(":")) {
                        String thisMacHardwareInfoProperty = "";
                        if (thisMacHardwareInfoLine.contains(": ")) {
                            thisMacHardwareInfoProperty = thisMacHardwareInfoLine.substring(thisMacHardwareInfoLine.indexOf(": ") + 2);
                        }

                        switch (thisMacHardwareInfoCategory) {
                            case "Hardware:":
                                if (macShortModelName.equals("N/A") && thisMacHardwareInfoLine.startsWith("      Model Name:")) {
                                    macShortModelName = thisMacHardwareInfoProperty;
                                } else if (macModelIdentifierNumber.isEmpty() && thisMacHardwareInfoLine.startsWith("      Model Identifier:")) {
                                    macModelIdentifier = thisMacHardwareInfoProperty;
                                    macModelIdentifierNumber = macModelIdentifier.replaceAll("[^0-9,]", "");
                                } else if (cpu.equals("N/A") && thisMacHardwareInfoLine.startsWith("      Chip:")) {
                                    cpu = thisMacHardwareInfoProperty; // This will only be available when running on Apple Silicon.
                                } else if (cpuCurrentSpeedString.isEmpty() && thisMacHardwareInfoLine.startsWith("      Processor Speed:")) {
                                    cpuCurrentSpeedString = thisMacHardwareInfoProperty;
                                } else if (cpuCount.isEmpty() && thisMacHardwareInfoLine.startsWith("      Number of Processors:")) {
                                    cpuCount = thisMacHardwareInfoProperty;
                                } else if (cpuCores.isEmpty() && thisMacHardwareInfoLine.startsWith("      Total Number of Cores:")) {
                                    cpuCores = thisMacHardwareInfoProperty;

                                    if (cpuCores.contains(" (")) {
                                        // "Total Number of Cores" when running on Apple Silicon could be shown like: "8 (4 performance and 4 efficiency)"
                                        String[] cpuCoresParts = cpuCores.split(" \\(");
                                        cpuCores = cpuCoresParts[0];
                                        cpuPandEcoresString = cpuCoresParts[1].replace(" performance", "P").replace(" and ", " + ").replace(" efficiency", "E").replace(")", "");

                                        // The following Performance and Efficiency core MHz Max speeds are from the following blog post: https://eclecticlight.co/2025/10/30/updated-cpu-core-frequencies-for-all-current-apple-silicon-macs/
                                        // The information in this post is collected from running "sudo powermetrics -n 1 -s cpu_power" on each CPU by Howard Oakley and other contributors (https://eclecticlight.co/2025/10/28/updating-cpu-frequencies-for-apple-silicon-macs/).
                                        // Since the "powermetrics" command requires "sudo" and takes a few seconds to run, its simpler to hard code these values since they are static for each Apple Silicon model.
                                        // Apple Silicon Cores Max MHz Last Updated: 10/30/25
                                        double pCoresMaxMHz = 0;
                                        double eCoresMaxMHz = 0;
                                        switch (cpu) {
                                            case "Apple M1":
                                                pCoresMaxMHz = 3204;
                                                eCoresMaxMHz = 2064;
                                                break;
                                            case "Apple M1 Pro":
                                                pCoresMaxMHz = 3228;
                                                eCoresMaxMHz = 2064;
                                                break;
                                            case "Apple M1 Max":
                                                pCoresMaxMHz = 3228;
                                                eCoresMaxMHz = 2064;
                                                break;
                                            case "Apple M1 Ultra":
                                                pCoresMaxMHz = 3228;
                                                eCoresMaxMHz = 2064;
                                                break;
                                            case "Apple M2":
                                                pCoresMaxMHz = 3504;
                                                eCoresMaxMHz = 2424;
                                                break;
                                            case "Apple M2 Pro":
                                                pCoresMaxMHz = 3504;
                                                eCoresMaxMHz = 2424;
                                                break;
                                            case "Apple M2 Max":
                                                pCoresMaxMHz = 3696;
                                                eCoresMaxMHz = 2424;
                                                break;
                                            case "Apple M2 Ultra":
                                                pCoresMaxMHz = 3696;
                                                eCoresMaxMHz = 2424;
                                                break;
                                            case "Apple M3":
                                                pCoresMaxMHz = 4056;
                                                eCoresMaxMHz = 2748;
                                                break;
                                            case "Apple M3 Pro":
                                                pCoresMaxMHz = 4056;
                                                eCoresMaxMHz = 2748;
                                                break;
                                            case "Apple M3 Max":
                                                pCoresMaxMHz = 4056;
                                                eCoresMaxMHz = 2568;
                                                break;
                                            case "Apple M3 Ultra":
                                                pCoresMaxMHz = 4056;
                                                eCoresMaxMHz = 2568;
                                                break;
                                            case "Apple M4":
                                                pCoresMaxMHz = 4464;
                                                eCoresMaxMHz = 2892;
                                                break;
                                            case "Apple M4 Pro":
                                                pCoresMaxMHz = 4512;
                                                eCoresMaxMHz = 2592;
                                                break;
                                            case "Apple M4 Max":
                                                pCoresMaxMHz = 4512;
                                                eCoresMaxMHz = 2592;
                                                break;
                                            case "Apple M5":
                                                pCoresMaxMHz = 4608;
                                                eCoresMaxMHz = 3048;
                                                break;
                                            default:
                                                break;
                                        }

                                        if (pCoresMaxMHz > 0) {
                                            String pCoresMaxGHzString = new DecimalFormat("#.#").format(pCoresMaxMHz / 1000) + " GHz Max";
                                            cpuPandEcoresString = cpuPandEcoresString.replace("P", ("P @ " + pCoresMaxGHzString));
                                        }

                                        if (eCoresMaxMHz > 0) {
                                            String eCoresMaxGHzString = new DecimalFormat("#.#").format(eCoresMaxMHz / 1000) + " GHz Max";
                                            cpuPandEcoresString = cpuPandEcoresString.replace("E", ("E @ " + eCoresMaxGHzString));
                                        }
                                    }
                                } else if (ram.equals("N/A") && thisMacHardwareInfoLine.startsWith("      Memory:")) {
                                    ram = thisMacHardwareInfoProperty;
                                } else if (serial.equals("N/A") && thisMacHardwareInfoLine.startsWith("      Serial Number (system):")) {
                                    serial = thisMacHardwareInfoProperty;
                                    if (serial.equals("Not Available")) {
                                        serial = "N/A";
                                    }
                                } else if (processorTraySerial.equals("N/A") && thisMacHardwareInfoLine.startsWith("      Serial Number (processor tray):")) {
                                    processorTraySerial = thisMacHardwareInfoProperty.trim();
                                }

                                break;
                            case "Audio:":
                                if (thisMacHardwareInfoLine.startsWith("    ") && !thisMacHardwareInfoLine.startsWith("     ")) {
                                    if (thisMacHardwareInfoLine.indexOf(":") > 0) {
                                        String thisAudio = thisMacHardwareInfoLine.substring(0, thisMacHardwareInfoLine.indexOf(":")).trim(); // This can get something different than Devices on a Mac Pro at least
                                        if (thisAudio.equals("Devices")) {
                                            thisAudio = "Built-In";
                                        }

                                        audioArray.add(thisAudio);
                                    }
                                }

                                break;
                            case "Wi-Fi:":
                                if (thisMacHardwareInfoLine.startsWith("          Supported PHY Modes:")) {
                                    ArrayList<String> wifiVersionsAndProtocols = new ArrayList<>();
                                    String[] wifiProtocolsArray = thisMacHardwareInfoProperty.replace("802.11", "").split("/");
                                    for (String thisWiFiProtocol : wifiProtocolsArray) {
                                        thisWiFiProtocol = thisWiFiProtocol.trim();
                                        if (!thisWiFiProtocol.isEmpty()) {
                                            switch (thisWiFiProtocol) {
                                                case "b":
                                                    wifiVersionsAndProtocols.add("1/" + thisWiFiProtocol);
                                                    break;
                                                case "a":
                                                    wifiVersionsAndProtocols.add("2/" + thisWiFiProtocol);
                                                    break;
                                                case "g":
                                                    wifiVersionsAndProtocols.add("3/" + thisWiFiProtocol);
                                                    break;
                                                case "n":
                                                    wifiVersionsAndProtocols.add("4/" + thisWiFiProtocol);
                                                    break;
                                                case "ac":
                                                    wifiVersionsAndProtocols.add("5/" + thisWiFiProtocol);
                                                    break;
                                                case "ax":
                                                    wifiVersionsAndProtocols.add("6/" + thisWiFiProtocol);
                                                    break;
                                                case "be":
                                                    wifiVersionsAndProtocols.add("7/" + thisWiFiProtocol);
                                                    break;
                                                default:
                                                    wifiVersionsAndProtocols.add(thisWiFiProtocol);
                                                    break;
                                            }
                                        }
                                    }
                                    Collections.sort(wifiVersionsAndProtocols);
                                    thisWifiInfo = "Wi-Fi Detected (Supports " + String.join(", ", wifiVersionsAndProtocols) + ")";
                                } else if (thisMacHardwareInfoLine.startsWith("          Supported Channels:")) {
                                    if (thisMacHardwareInfoLine.contains("(6GHz)")) {
                                        thisWifiInfo = thisWifiInfo.replace("6/ax", "6E/ax");
                                    }
                                } else if (thisMacHardwareInfoLine.equals("          Status: Off")) {
                                    if (thisWifiInfo.isEmpty()) {
                                        thisWifiInfo = "Wi-Fi Detected (UNKNOWN Protocols - Wi-Fi DISABLED)";
                                    }
                                }

                                break;
                            case "Bluetooth:":
                                if (thisMacHardwareInfoLine.startsWith("          Chipset:")) {
                                    bluetoothDetected = true; // For some strange reason, detailed Bluetooth information no longer exists in Monterey, can only detect if it is present.
                                } else if (thisMacHardwareInfoLine.equals("          Bluetooth Low Energy Supported: Yes")) {
                                    bluetoothLE = true;
                                } else if (thisMacHardwareInfoLine.equals("          Handoff Supported: Yes")) {
                                    bluetoothHandoff = true;
                                } else if (thisMacHardwareInfoLine.startsWith("          HCI Version:") || thisMacHardwareInfoLine.startsWith("          LMP Version:")) {
                                    String thisBluetoothVersion = thisMacHardwareInfoProperty.replaceAll("[^A-Za-z0-9 .]", "");

                                    String[] thisBluetoothVersionParts = thisBluetoothVersion.split(" ");

                                    if (!thisBluetoothVersionParts[0].isEmpty()) {
                                        thisBluetoothVersion = thisBluetoothVersionParts[0];
                                    } else if (thisBluetoothVersionParts.length == 2) {
                                        // BT 5.0 on HS could be shown as " (0x9)"
                                        thisBluetoothVersion = thisBluetoothVersionParts[1];
                                    }

                                    if (thisBluetoothVersion.equals("0x9")) {
                                        thisBluetoothVersion = "5.0"; // BT 5.0 does not get detected correctly on High Sierra
                                    }
                                    if (thisMacHardwareInfoLine.startsWith("          HCI Version:")) {
                                        bluetoothHCIversion = thisBluetoothVersion;
                                    } else {
                                        bluetoothLMPversion = thisBluetoothVersion;
                                    }
                                }

                                break;
                            case "Disc Burning:":
                                if (thisMacHardwareInfoLine.startsWith("    ") && !thisMacHardwareInfoLine.startsWith("     ")) {
                                    discDriveArray.add(thisMacHardwareInfoLine.substring(0, thisMacHardwareInfoLine.length() - 1).trim().replaceAll("\\s{2,}", " "));
                                }

                                if (thisMacHardwareInfoLine.startsWith("      CD-Write:")) {
                                    discDriveCanBurnCDs = true;
                                } else if (thisMacHardwareInfoLine.startsWith("      DVD-Write:")) {
                                    discDriveCanBurnDVDs = true;
                                } else if (thisMacHardwareInfoLine.startsWith("      Media:")) {
                                    hasDiscInDiscDrive = !thisMacHardwareInfoLine.contains("insert a disc");
                                }

                                break;
                            case "Graphics/Displays:":
                                if (thisMacHardwareInfoLine.startsWith("      Chipset Model:")) {
                                    thisGPUmodel = cleanDeviceModel(cleanDeviceBrand(thisMacHardwareInfoProperty));
                                    thisGPUbus = "";
                                } else if (thisMacHardwareInfoLine.startsWith("      Bus:")) {
                                    thisGPUbus = thisMacHardwareInfoProperty + ": ";
                                } else if (thisMacHardwareInfoLine.startsWith("      VRAM") || thisMacHardwareInfoLine.startsWith("      Total Number of Cores:")) {
                                    // "VRAM" will exist on Intel Macs and "Total Number of Cores" will exist on Apple Silicon.
                                    String thisVRAMorCoresAmount = thisMacHardwareInfoProperty;
                                    if (thisVRAMorCoresAmount.contains(" MB")) {
                                        try {
                                            double thisVRAMamountDouble = Double.parseDouble(thisVRAMorCoresAmount.substring(0, thisVRAMorCoresAmount.indexOf(" MB")));
                                            if (thisVRAMamountDouble >= 1024) {
                                                thisVRAMorCoresAmount = new DecimalFormat("#.#").format(thisVRAMamountDouble / 1024) + " GB";
                                            }
                                        } catch (NumberFormatException vramAmountException) {
                                            if (isTestMode) {
                                                System.out.println("vramAmountException: " + vramAmountException);
                                            }
                                        }
                                    }

                                    String thisWholeGPU = thisGPUbus + thisGPUmodel + " (" + thisVRAMorCoresAmount + ((thisMacHardwareInfoLine.contains("Total Number of Cores:")) ? " Cores" : (thisMacHardwareInfoLine.contains("Dynamic") ? " - Shared" : "")) + ")";

                                    if (!gpuArray.contains(thisWholeGPU)) { // TODO: Should this check against getFullGPU() instead of gpuArray?
                                        if (thisGPUbus.equals("Built-In: ")) {
                                            gpuArray.add(0, thisWholeGPU);
                                        } else {
                                            gpuArray.add(thisWholeGPU);
                                        }
                                    }
                                } else if (thisMacHardwareInfoLine.startsWith("          Display Type: Built-In") || thisMacHardwareInfoLine.startsWith("          Display Type: Built-in")) { // Could be "Built-In" (for "Retina LCD") or "Built-in" (for "Liquid Retina XDR Display")
                                    hasScreen = true;
                                    macScreenFeaturesArray.add(thisMacHardwareInfoLine.substring(33).replace(" LCD", "").replace(" Display", "")); // This will extract "Retina" or "Liquid Retina XDR" etc and will just be empty if not a Retina screen.
                                } else if (screenResolution.isEmpty() && thisMacHardwareInfoLine.startsWith("          Resolution:") && thisMacHardwareInfoProperty.contains(" x ")) {
                                    // Need to be able to extract the proper screen resolution from all these types of values:
                                    // MacBook Pro 11,5 (15-inch, Mid 2015) resolution line is "2880 x 1800 Retina"
                                    // iMac 13,1 (21.5-inch, Late 2012) resolution line is "1920 x 1080 (1080p FHD - Full High Definition)"
                                    // iMac Pro 1,1 (2017) resolution line is "Retina 5K (5120 x 2880)"
                                    String[] resolutionParts = thisMacHardwareInfoProperty.replace("(", "").replace(")", "").split(" ");

                                    String lastResolutionPart = "";
                                    String resolutionWidthPart = "";
                                    String resolutionHeightPart = "";

                                    for (String thisResolutionPart : resolutionParts) {
                                        if (resolutionWidthPart.isEmpty() && thisResolutionPart.equals("x")) {
                                            resolutionWidthPart = lastResolutionPart;
                                        } else if (resolutionHeightPart.isEmpty() && lastResolutionPart.equals("x")) {
                                            resolutionHeightPart = thisResolutionPart;
                                        } else if (thisResolutionPart.endsWith("0p") || thisResolutionPart.endsWith("K")) { // Detect "1080p", "5K", etc and include in screen type.

                                            if (macScreenFeaturesArray.isEmpty()) {
                                                macScreenFeaturesArray.add(thisResolutionPart);
                                            } else {
                                                int lastMacScreenFeatureIndex = (macScreenFeaturesArray.size() - 1);
                                                String lastMacScreenFeature = macScreenFeaturesArray.get(lastMacScreenFeatureIndex);
                                                lastMacScreenFeature += " " + thisResolutionPart;
                                                macScreenFeaturesArray.set(lastMacScreenFeatureIndex, lastMacScreenFeature);
                                            }
                                        }

                                        lastResolutionPart = thisResolutionPart;
                                    }
                                    screenResolution = resolutionWidthPart + "x" + resolutionHeightPart;
                                }

                                break;
                            case "Memory:":
                                if (ramType.isEmpty() && thisMacHardwareInfoLine.startsWith("      Type:")) {
                                    // This top level "Type" (not within any Banks), will only exist when running on Apple Silicon.
                                    ramType = " " + thisMacHardwareInfoProperty;
                                } else if (thisMacHardwareInfoLine.startsWith("          Size:")) {
                                    if (ramSlots.isEmpty()) {
                                        ramSlots = thisMacHardwareInfoProperty;
                                    } else {
                                        ramSlots += " + " + thisMacHardwareInfoProperty;
                                    }
                                } else if (ramType.isEmpty() && thisMacHardwareInfoLine.startsWith("          Type:")) {
                                    ramType = " " + thisMacHardwareInfoProperty.replace(" SO-DIMM", ""); // Remove " SO-DIMM" suffix that exists on iMacs with DDR4 RAM.
                                    if (!ramType.contains("DDR")) {
                                        ramType = "";
                                    }
                                } else if (ramSpeed.isEmpty() && thisMacHardwareInfoLine.startsWith("          Speed:")) {
                                    ramSpeed = " @ " + thisMacHardwareInfoProperty.replace("32 ", "33 ").replace("34 ", "33 ").replace("67 ", "66 "); // Replace any MHz speeds that end with "32" or "34" to "33" and "67" to "66" to account for rounding differences (such as "1332 MHz" vs "1333 MHz" and "2667 MHz" vs "2666 MHz" etc) that would make the software show a slightly different number than what would be on the actual RAM label.
                                    if (!ramSpeed.contains(" MHz")) {
                                        ramSpeed = "";
                                    }
                                } else if (thisMacHardwareInfoLine.equals("      Upgradeable Memory: No") || cpu.startsWith("Apple")) { // Apple Silicon Macs DO NOT have this value set, but all their RAM is integrated into the Chip and not upgradeable. THIS CONDITION MUST BE LAST SINCE IT WILL ALWAYS BE TRUE ON APPLE SILICON AND WOULD PREVENT OTHER CONDITIONS FROM BEING HIT.
                                    maxRAM = " - NOT Upgradeable";
                                }

                                break;
                            case "Network:":
                                if (thisMacHardwareInfoLine.startsWith("    ") && !thisMacHardwareInfoLine.startsWith("     ")) {
                                    thisNetworkController = thisMacHardwareInfoLine;
                                    break;
                                }

                                if (thisNetworkController.contains("Ethernet")) {
                                    if (thisMacHardwareInfoLine.startsWith("          MAC Address:")) {
                                        ethernetMACarray.add(thisMacHardwareInfoProperty.toUpperCase());
                                    }
                                }

                                break;
                            case "Power:":
                                if (thisMacHardwareInfoLine.startsWith("    ") && !thisMacHardwareInfoLine.startsWith("     ")) {
                                    thisPowerSection = thisMacHardwareInfoLine;
                                    break;
                                }

                                if (thisPowerSection.equals("    Battery Information:")) {
                                    if (thisBatteryCycleCount.equals("N/A") && thisMacHardwareInfoLine.startsWith("          Cycle Count:")) {
                                        thisBatteryCycleCount = thisMacHardwareInfoProperty;
                                    } else if (thisBatteryHealth.equals("N/A") && thisMacHardwareInfoLine.startsWith("          Condition:")) {
                                        thisBatteryHealth = thisMacHardwareInfoProperty;
                                    }
                                }

                                break;
                            case "NVMExpress:":
                            case "SATA/SATA Express:":
                            case "SATA:": // SPSerialATADataType section title has been changed to just "SATA:" in macOS 11 Big Sur
                                if (thisMacHardwareInfoLine.startsWith("    ") && !thisMacHardwareInfoLine.startsWith("     ")) {
                                    thisStorageController = thisMacHardwareInfoLine;
                                    break;
                                }

                                if (thisMacHardwareInfoLine.startsWith("        ") && !thisMacHardwareInfoLine.startsWith("         ")) {
                                    // This indicates a new device section.
                                    thisStorageDriveInfo = "";
                                    thisDriveModel = "";
                                    break;
                                }

                                if (!thisStorageController.equals("    Thunderbolt AHCI Controller:") && !thisStorageController.equals("    Generic SSD Controller:")) { // "Generic SSD Controller" shows for external USB4 NVMe drives.
                                    if (thisMacHardwareInfoLine.startsWith("          Capacity:")) {
                                        thisStorageDriveInfo = thisMacHardwareInfoProperty;

                                        try {
                                            String[] thisStorageDriveInfoParts = thisStorageDriveInfo.split(" ");
                                            if ((thisStorageDriveInfoParts.length >= 2) && (thisStorageDriveInfoParts[1].length() == 2)) {
                                                thisStorageDriveInfo = (thisStorageDriveInfoParts[0] + " " + thisStorageDriveInfoParts[1]); // In case "Double.parseDouble" fails for some reason, at least get rid of the "bytes" part in parenthesis.
                                                thisStorageDriveInfo = new DecimalFormat("#").format(Double.parseDouble(thisStorageDriveInfoParts[0].replace(",", "."))) + " " + thisStorageDriveInfoParts[1]; // Use the existing GB or TB value, but round it to the nearest whole number.
                                            }
                                        } catch (NumberFormatException thisStorageDriveInfoException) {
                                            if (isTestMode) {
                                                System.out.println("thisStorageDriveInfoException: " + thisStorageDriveInfoException);
                                            }
                                        }

                                        if (thisStorageController.equals("    Apple SSD Controller:")) { // This is for NVMe SSDs (which don't have a Medium Type field).
                                            thisStorageDriveInfo += (thisMacHardwareInfoCategory.equals("NVMExpress:") ? " NVMe" : " SSD");
                                        }
                                    } else if (thisMacHardwareInfoLine.startsWith("          Model:")) {
                                        thisDriveModel = thisMacHardwareInfoProperty.trim().replace("(", "").replace(")", "").replaceAll("\\s{2,}", " "); // NOTE: Remove all parens (which shouldn't normally exist anyways) to not break removing models when truncating storage line for PCsCRM.

                                        if (!thisStorageDriveInfo.isEmpty() && thisStorageController.equals("    Apple SSD Controller:")) { // Will add model below if NOT in "Apple SSD Controller" section.
                                            if (!thisDriveModel.isEmpty()) {
                                                thisStorageDriveInfo += " (" + thisDriveModel + ")";
                                                thisDriveModel = "";
                                            }
                                        }
                                    } else if (thisMacHardwareInfoLine.startsWith("          Serial Number:")) {
                                        String thisDriveSerial = thisMacHardwareInfoProperty.replaceAll("^[. /]+", "").replaceAll("[. /]+$", ""); // Trim all leading and trailing spaces, periods, and slashes.
                                        if (!thisDriveSerial.isEmpty()) {
                                            storageSerialsArray.add(thisDriveSerial);
                                        }
                                    } else if (!thisStorageDriveInfo.isEmpty() && thisMacHardwareInfoLine.startsWith("          Removable Media:")) {
                                        if (thisMacHardwareInfoProperty.equals("Yes")) { // To ignore DVD drives.
                                            thisStorageDriveInfo = "";
                                        } else {
                                            storageArray.add(thisStorageDriveInfo);
                                        }
                                    } else if (!thisStorageDriveInfo.isEmpty() && !thisStorageController.equals("    Apple SSD Controller:") && thisMacHardwareInfoLine.startsWith("          Medium Type:")) {
                                        String ssdOrHdd = (thisMacHardwareInfoProperty.equals("Solid State") ? "SSD" : "HDD");

                                        int thisStorageDriveInfoIndex = storageArray.lastIndexOf(thisStorageDriveInfo);

                                        thisStorageDriveInfo += " " + ssdOrHdd;

                                        if (!thisDriveModel.isEmpty()) { // Already added model to "thisStorageDriveInfo" above if is in "Apple SSD Controller" section where "NVMe" or "SSD" would have already been added to "thisStorageDriveInfo".
                                            thisStorageDriveInfo += " (" + thisDriveModel + ")";
                                        }

                                        if (thisStorageDriveInfoIndex >= 0) {
                                            storageArray.set(thisStorageDriveInfoIndex, thisStorageDriveInfo);
                                        } else {
                                            storageArray.add(thisStorageDriveInfo);
                                        }
                                    } else if (!thisStorageDriveInfo.isEmpty() && thisMacHardwareInfoLine.startsWith("          TRIM Support:") && (thisStorageDriveInfo.endsWith(" SSD") || thisStorageDriveInfo.contains(" SSD (") || thisStorageDriveInfo.endsWith(" NVMe") || thisStorageDriveInfo.contains(" NVMe ("))) {
                                        if (!thisMacHardwareInfoProperty.equals("Yes")) {
                                            driveTrimWarning = true;
                                        }
                                    } else if (!thisStorageDriveInfo.isEmpty() && thisMacHardwareInfoLine.startsWith("          S.M.A.R.T. status:")) {
                                        if (!thisMacHardwareInfoProperty.equals("Verified")) {
                                            driveHealthWarning = true;
                                        }
                                    }
                                }

                                break;
                            default:
                                break;
                        }
                    }
                }

                // Chassis for Mac
                if (macShortModelName.contains("Book")) {
                    chassisType = "Laptop";
                    isLaptop = true;
                    hasScreen = true;
                    hasCamera = true;
                } else if (macShortModelName.startsWith("iMac")) {
                    chassisType = "All-in-One";
                    hasScreen = true;
                    hasCamera = true;
                } else {
                    chassisType = "Desktop";
                }

                if (!thisWifiInfo.isEmpty()) {
                    wirelessArray.add(thisWifiInfo);
                }

                if (bluetoothDetected) {
                    String thisBluetoothInfo = "Bluetooth";

                    if (!bluetoothHCIversion.isEmpty() || !bluetoothLMPversion.isEmpty()) {
                        thisBluetoothInfo += " " + (bluetoothHCIversion.contains(".") ? bluetoothHCIversion : bluetoothLMPversion);
                    } else {
                        // For some strange reason, detailed Bluetooth information no longer exists in Monterey, can only detect if it is present.
                        // BUT, I wrote a script (https://github.com/freegeek-pdx/macOS-Testing-and-Deployment-Scripts/blob/main/Other%20Scripts/get_bluetooth_from_all_mac_specs_pages.sh) to extract every Bluetooth version from every specs URL to be able to know what version this model has if Bluetooth is detected.

                        // Bluetooth Model IDs Last Updated: 10/27/25
                        if (Arrays.asList("Mac14,2", "Mac14,3", "Mac14,5", "Mac14,6", "Mac14,8", "Mac14,9", "Mac14,10", "Mac14,12", "Mac14,13", "Mac14,14", "Mac14,15", "Mac15,3", "Mac15,4", "Mac15,5", "Mac15,6", "Mac15,7", "Mac15,8", "Mac15,9", "Mac15,10", "Mac15,11", "Mac15,12", "Mac15,13", "Mac15,14", "Mac16,1", "Mac16,2", "Mac16,3", "Mac16,5", "Mac16,6", "Mac16,7", "Mac16,8", "Mac16,9", "Mac16,10", "Mac16,11", "Mac16,12", "Mac16,13", "Mac17,2").contains(macModelIdentifier)) {
                            thisBluetoothInfo += " 5.3";
                            bluetoothLE = true;
                            bluetoothHandoff = true;
                        } else if (Arrays.asList("Mac13,1", "Mac13,2", "Mac14,7", "MacBookAir9,1", "MacBookAir10,1", "MacBookPro15,1", "MacBookPro15,2", "MacBookPro15,3", "MacBookPro15,4", "MacBookPro16,1", "MacBookPro16,2", "MacBookPro16,3", "MacBookPro16,4", "MacBookPro17,1", "MacBookPro18,1", "MacBookPro18,2", "MacBookPro18,3", "MacBookPro18,4", "MacPro7,1", "Macmini8,1", "Macmini9,1", "iMac20,1", "iMac20,2", "iMac21,1", "iMac21,2", "iMacPro1,1").contains(macModelIdentifier)) {
                            thisBluetoothInfo += " 5.0";
                            bluetoothLE = true;
                            bluetoothHandoff = true;
                        } else if (Arrays.asList("MacBook10,1", "MacBookAir8,1", "MacBookAir8,2", "MacBookPro11,4", "MacBookPro11,5", "MacBookPro13,1", "MacBookPro13,2", "MacBookPro13,3", "MacBookPro14,1", "MacBookPro14,2", "MacBookPro14,3", "iMac18,1", "iMac18,2", "iMac18,3", "iMac19,1", "iMac19,2").contains(macModelIdentifier)) {
                            thisBluetoothInfo += " 4.2";
                            bluetoothLE = true;
                            bluetoothHandoff = true; // All Bluetooth 4.2 and newer models support Handoff
                        } else if (Arrays.asList("MacBook8,1", "MacBook9,1", "MacBookAir4,1", "MacBookAir4,2", "MacBookAir5,1", "MacBookAir5,2", "MacBookAir6,1", "MacBookAir6,2", "MacBookAir7,1", "MacBookAir7,2", "MacBookPro9,1", "MacBookPro9,2", "MacBookPro10,1", "MacBookPro10,2", "MacBookPro11,1", "MacBookPro11,2", "MacBookPro11,3", "MacBookPro12,1", "MacPro6,1", "Macmini5,1", "Macmini5,2", "Macmini5,3", "Macmini6,1", "Macmini6,2", "Macmini7,1", "iMac13,1", "iMac13,2", "iMac14,1", "iMac14,2", "iMac14,4", "iMac15,1", "iMac16,1", "iMac16,2", "iMac17,1").contains(macModelIdentifier)) {
                            // SOME of these models with Bluetooth 4.0 DON'T support Monterey, but it would be more effor to not include them.
                            thisBluetoothInfo += " 4.0";
                            bluetoothLE = true; // All Bluetooth 4.0 and above is BLE
                            if (!Arrays.asList("MacBookAir4,1", "MacBookAir4,2", "Macmini5,1", "Macmini5,2", "Macmini5,3").contains(macModelIdentifier)) {
                                bluetoothHandoff = true; // Most Bluetooth 4.0 models support Handoff, but some early models don't, so show support for all EXCEPT those models: https://support.apple.com/en-us/HT204689
                            }
                        } else if (Arrays.asList("MacBook5,2", "MacBook6,1", "MacBook7,1", "MacBookAir2,1", "MacBookAir3,1", "MacBookAir3,2", "MacBookPro4,1", "MacBookPro5,1", "MacBookPro5,2", "MacBookPro5,3", "MacBookPro5,5", "MacBookPro6,1", "MacBookPro6,2", "MacBookPro7,1", "MacBookPro8,1", "MacBookPro8,2", "MacBookPro8,3", "MacPro4,1", "MacPro5,1", "Macmini3,1", "Macmini4,1", "iMac9,1", "iMac10,1", "iMac11,2", "iMac11,3", "iMac12,1", "iMac12,2").contains(macModelIdentifier)) {
                            // NONE of these models with Bluetooth 2.1 (plus EDR) support Monterey, but it's no extra effort to include them anyways.
                            thisBluetoothInfo += " 2.1";
                        }
                    }

                    thisBluetoothInfo += " Detected";

                    if (bluetoothLE || bluetoothHandoff) {
                        thisBluetoothInfo += " (Supports ";

                        if (bluetoothLE) {
                            thisBluetoothInfo += "BLE";
                        }

                        if (bluetoothHandoff) {
                            thisBluetoothInfo += (bluetoothLE ? ", " : "") + "Handoff";
                        }

                        thisBluetoothInfo += ")";
                    }

                    wirelessArray.add(thisBluetoothInfo);
                }

                // Battery & Power Adapter for Mac
                if (isLaptop) {
                    String thisBatterySummary;

                    try {
                        String[] batteryCapacityLines = new CommandReader(new String[]{"/usr/sbin/ioreg", "-rc", "AppleSmartBattery"}).getOutputLines();
                        double maxCapacity = 0;
                        double designCapacity = 0;
                        for (String thisBatteryCapacityLine : batteryCapacityLines) {
                            if (maxCapacity == 0 && thisBatteryCapacityLine.startsWith("      \"MaxCapacity\" = ")) {
                                // "MaxCapacity" will always be "100" on Apple Silicon, use "AppleRawMaxCapacity" instead (but still checking both because I'm not sure when/if "AppleRawMaxCapacity" is unavailable).
                                maxCapacity = Double.parseDouble(thisBatteryCapacityLine.substring(thisBatteryCapacityLine.indexOf(" = ") + 3));
                            } else if ((maxCapacity == 0 || maxCapacity == 100) && thisBatteryCapacityLine.startsWith("      \"AppleRawMaxCapacity\" = ")) {
                                maxCapacity = Double.parseDouble(thisBatteryCapacityLine.substring(thisBatteryCapacityLine.indexOf(" = ") + 3));
                            } else if (designCapacity == 0 && thisBatteryCapacityLine.startsWith("      \"DesignCapacity\" = ")) {
                                designCapacity = Double.parseDouble(thisBatteryCapacityLine.substring(thisBatteryCapacityLine.indexOf(" = ") + 3));
                            }
                        }

                        if (maxCapacity > 0 && designCapacity > 0) {
                            double thisBatteryCapacityDouble = ((maxCapacity / designCapacity) * 100);
                            thisBatterySummary = new DecimalFormat("#.#").format(thisBatteryCapacityDouble) + "%";

                            if (thisBatterySummary.startsWith("0")) {
                                thisBatterySummary = "NO POWER";
                                batteryHealthErrorArray.add("No Power");
                            } else if (thisBatteryCapacityDouble < 40) {
                                batteryHealthErrorArray.add("Capacity < 40%");
                            } else if (thisBatteryCapacityDouble < 60) {
                                batteryHealthWarningArray.add("Capacity < 60%");
                            }
                        } else {
                            thisBatterySummary = "NO POWER";
                            batteryHealthErrorArray.add("No Power");
                        }
                    } catch (NumberFormatException batteryCapacityException) {
                        if (isTestMode) {
                            System.out.println("batteryCapacityException: " + batteryCapacityException);
                        }

                        thisBatterySummary = "UNKNOWN CAPACITY";
                        batteryHealthWarningArray.add("Unknown Capacity");
                    }

                    if (!thisBatteryCycleCount.equals("N/A")) {
                        thisBatterySummary += " (" + thisBatteryCycleCount + " Cycle" + (thisBatteryCycleCount.equals("1") ? "" : "s") + ")";

                        try {
                            int batteryCycleCountInteger = Integer.parseInt(thisBatteryCycleCount);

                            if (batteryCycleCountInteger >= 1000) {
                                batteryHealthErrorArray.add("Cycles >= 1000");
                            } else if (batteryCycleCountInteger >= 800) {
                                batteryHealthWarningArray.add("Cycles >= 800");
                            }
                        } catch (NumberFormatException batteryCycleCountException) {
                            if (isTestMode) {
                                System.out.println("batteryCycleCountException: " + batteryCycleCountException);
                            }
                        }
                    }

                    if (!thisBatterySummary.isEmpty()) {
                        batteryHealthArray.add(thisBatterySummary);
                    }

                    // Battery Health Check
                    if (!thisBatteryHealth.equals("Normal")) {
                        batteryHealthErrorArray.add("Condition " + thisBatteryHealth + " (Not Normal)");
                    }
                }

                int serialLength = serial.length();
                if (serialLength < 8) { // https://www.macrumors.com/2010/04/16/apple-tweaks-serial-number-format-with-new-macbook-pro/ & https://www.macrumors.com/2021/03/09/apple-randomized-serial-numbers-early-2021/
                    serial = processorTraySerial;

                    if (serial.equals("N/A") && !ethernetMACarray.isEmpty()) {
                        serialIsMAC = true;
                        serial = getEthernetMAC();
                    }
                } else if (serialLength == 11 || serialLength == 12) {
                    // The Configuration Code part of the Serial Number which indicates the model is the last 4 characters for 12 character serials and the last 3 characters for 11 character serials (which are very old and shouldn't actually be encountered: https://www.macrumors.com/2010/04/16/apple-tweaks-serial-number-format-with-new-macbook-pro/).
                    macSerialConfigCode = serial.substring(8);
                }

                // RECALLS
                // https://web.archive.org/web/20190105114612/https://www.apple.com/support/macbookpro-videoissues/ & https://www.macrumors.com/2017/05/20/apple-ends-2011-macbook-pro-repair-program/
                boolean macBookProPossibleBadGraphics = Arrays.asList("MacBookPro8,2", "MacBookPro8,3", "MacBookPro10,1").contains(macModelIdentifier);

                // https://www.macrumors.com/2013/08/16/apple-initiates-graphic-card-replacement-program-for-mid-2011-27-inch-imac/
                boolean iMacPossibleBadGraphics = (macShortModelName.equals("iMac") && Arrays.asList("DHJQ", "DHJW", "DL8Q", "DNGH", "DNJ9", "DMW8", "DPM1", "DPM2", "DPNV", "DNY0", "DRVP", "DY6F", "F610").contains(macSerialConfigCode));

                // https://www.macrumors.com/2016/02/06/late-2013-mac-pro-video-issues-repair-program/
                boolean macProPossibleBadGraphics = (macModelIdentifier.equals("MacPro6,1")
                        && Arrays.asList("P5", "P6", "P7", "P8", "P9", "PC", "PD", "PF", "PG", "PH").contains(serial.substring(3, 5))
                        && (getFullGPU().contains("AMD FirePro D500") || getFullGPU().contains("AMD FirePro D700")));

                if (macBookProPossibleBadGraphics || iMacPossibleBadGraphics || macProPossibleBadGraphics) {
                    gpuErrorArray.add("POSSIBLY RECALLED");
                }

                // https://web.archive.org/web/20220620162055/https://support.apple.com/en-us/HT212163
                boolean macBookPro2016and2017RecalledBatteryRecall = Arrays.asList("MacBookPro13,1", "MacBookPro13,2", "MacBookPro13,3", "MacBookPro14,1", "MacBookPro14,2", "MacBookPro14,3").contains(macModelIdentifier);

                // https://support.apple.com/15-inch-macbook-pro-battery-recall
                boolean macBookPro15inch2015PossibleBatteryRecall = Arrays.asList("MacBookPro11,4", "MacBookPro11,5").contains(macModelIdentifier);

                // https://web.archive.org/web/20221209124713/https://support.apple.com/13inch-macbookpro-battery-replacement
                boolean macBookPro13inch2016PossibleBatteryRecall = Arrays.asList("MacBookPro13,1", "MacBookPro14,1").contains(macModelIdentifier);

                if (macBookPro15inch2015PossibleBatteryRecall || macBookPro13inch2016PossibleBatteryRecall || macBookPro2016and2017RecalledBatteryRecall) {
                    batteryHealthWarningArray.add("Possibly Recalled");
                }

                // https://web.archive.org/web/20221208144358/https://support.apple.com/13-inch-macbook-pro-solid-state-drive-service
                boolean macBookPro13inch2017PossibleSSDRecall = (macModelIdentifier.equals("MacBookPro14,1") && (getFullStorage().contains("AP0128J") || getFullStorage().contains("AP0256J")));

                if (macBookPro13inch2017PossibleSSDRecall) {
                    driveRecalled = true;
                }

                // https://support.apple.com/13-inch-macbook-pro-display-backlight-service
                boolean macBookPro13inch2016PossibleBacklightRecall = Arrays.asList("MacBookPro13,1", "MacBookPro13,2").contains(macModelIdentifier);

                if (macBookPro13inch2016PossibleBacklightRecall) {
                    screenErrorArray.add("RECALLED IF BACKLIGHT IS BAD");
                }

                // Only the 2016 13-inch is recalled by Apple for the FLEXGATE issue, but the 15-inch and 2017 models may also have the same issue
                boolean macBookProOtherFlexgate = Arrays.asList("MacBookPro13,3", "MacBookPro14,1", "MacBookPro14,2", "MacBookPro14,3").contains(macModelIdentifier);

                if (macBookProOtherFlexgate) {
                    screenErrorArray.add("POSSIBLE BACKLIGHT ISSUE (BUT NOT RECALLED)");
                }

                // https://www.macrumors.com/2017/11/17/apple-extends-free-staingate-repairs/
                boolean macBookProScreenDelaminationRecall = Arrays.asList("MacBook8,1", "MacBook9,1", "MacBook10,1", "MacBookPro11,4", "MacBookPro11,5", "MacBookPro12,1", "MacBookPro13,1", "MacBookPro13,2", "MacBookPro13,3", "MacBookPro14,1", "MacBookPro14,2", "MacBookPro14,3").contains(macModelIdentifier);

                if (macBookProScreenDelaminationRecall) {
                    screenErrorArray.add("RECALLED IF HAS DELAMINATION");
                }

                // https://www.macrumors.com/2016/11/29/imac-broken-hinge-refunds-repair-program/
                boolean iMacHingeRecall = macModelIdentifier.equals("iMac14,2");

                if (iMacHingeRecall) {
                    screenErrorArray.add("RECALLED IF HINGE IS BAD");
                }

                // https://support.apple.com/keyboard-service-program-for-mac-notebooks
                boolean macBookProButterflyKeyboardRecall = Arrays.asList("MacBook8,1", "MacBook9,1", "MacBook10,1", "MacBookAir8,1", "MacBookAir8,2", "MacBookPro13,1", "MacBookPro13,2", "MacBookPro13,3", "MacBookPro14,1", "MacBookPro14,2", "MacBookPro14,3", "MacBookPro15,1", "MacBookPro15,2", "MacBookPro15,3", "MacBookPro15,4").contains(macModelIdentifier);

                if (macBookProButterflyKeyboardRecall) {
                    keyboardRecalled = true;
                }
            }

            boolean macHasT2chip = false;
            if (brand.equals("Apple")) {
                // Get Marketing Model Name (and extract screen size from Mac model) for Mac's running any OS

                if (isMacOS) {
                    macHasT2chip = !new CommandReader(new String[]{"/usr/sbin/ioreg", "-rn", "Apple T2 Controller", "-d", "1"}).getFirstOutputLineContaining("Apple T2 Controller").isEmpty();
                } else {
                    // On some Mac models, Family from dmidecode on Linux or SystemFamily from Win32_ComputerSystem on Windows is the correct shortModelName,
                    // but on other models it's also just "Mac" or "MacBook" (when it's not a MacBook) instead of the correct shortModelName of "MacBook Air" or "MacBook Pro".
                    // So instead of using Family/SystemFamily, we will get the correct shortModelName from the systemProductName which *seems* to always be the correct macModelIdentifier.

                    macShortModelName = systemProductName.replaceAll("[^A-Za-z]", "").replace("Pro", " Pro").replace("Air", " Air").replace("mini", " mini"); // The "Mac Studio" does not have a "Studio" suffix in the Model Identifier and Apple Silicon Macs should never be running Linux anyways.
                    macModelIdentifierNumber = systemProductName.replaceAll("[^0-9,]", "");

                    if ((macShortModelName.length() > 3) && macShortModelName.contains("Mac") && (macModelIdentifierNumber.length() > 2) && macModelIdentifierNumber.contains(",")) {
                        // Even though systemProductName seems to always be the correct macModelIdentifier, confirm it anyway and if correct,
                        // clear out systemProductName (macModelIdentifier) so it isn't unecessarily repeated at the end of the model line.

                        macModelIdentifier = systemProductName;
                        systemProductName = "N/A";
                    }

                    int serialLength = serial.length();
                    if (serialLength == 11 || serialLength == 12) {
                        // The Configuration Code part of the Serial Number which indicates the model is the last 4 characters for 12 character serials and the last 3 characters for 11 character serials (which are very old and shouldn't actually be encountered: https://www.macrumors.com/2010/04/16/apple-tweaks-serial-number-format-with-new-macbook-pro/).
                        macSerialConfigCode = serial.substring(8);
                    }

                    // Override chassis for macOS on PC ("Type" in "Chassis Information" from "dmidecode" is known to be wrong on at least some iMac models.)
                    if (macShortModelName.contains("Book")) {
                        chassisType = "Laptop";
                        isLaptop = true;
                        hasScreen = true;
                        hasCamera = true;
                    } else if (macShortModelName.startsWith("iMac")) {
                        chassisType = "All-in-One";
                        isLaptop = false;
                        hasScreen = true;
                        hasCamera = true;
                    } else {
                        chassisType = "Desktop";
                        isLaptop = false;
                        hasScreen = false;
                        hasCamera = false;
                    }
                }

                // Laptop Power Adapter Info for Mac - https://support.apple.com/HT201700
                // BUT, I wrote a script (https://github.com/freegeek-pdx/macOS-Testing-and-Deployment-Scripts/blob/main/Other%20Scripts/get_power_adapters_from_all_mac_specs_pages.sh) to extract every Power Adapter for each Model ID from every specs URL from the Model pages linked here: https://support.apple.com/HT213325
                // Power Adapter Model IDs Last Updated: 10/27/25
                if (Arrays.asList("MacBookPro1,1", "MacBookPro1,2", "MacBookPro2,1", "MacBookPro2,2", "MacBookPro3,1", "MacBookPro4,1", "MacBookPro5,1", "MacBookPro5,2", "MacBookPro5,3", "MacBookPro6,1", "MacBookPro6,2", "MacBookPro8,2", "MacBookPro8,3", "MacBookPro9,1").contains(macModelIdentifier)) {
                    powerAdapter = "85W MagSafe 1";
                } else if (Arrays.asList("MacBook1,1", "MacBook2,1", "MacBook3,1", "MacBook4,1", "MacBook5,1", "MacBook5,2", "MacBook6,1", "MacBook7,1", "MacBookPro5,4", "MacBookPro5,5", "MacBookPro7,1", "MacBookPro8,1", "MacBookPro9,2").contains(macModelIdentifier)) {
                    powerAdapter = "60W MagSafe 1";
                } else if (Arrays.asList("MacBookAir1,1", "MacBookAir2,1", "MacBookAir3,1", "MacBookAir3,2", "MacBookAir4,1", "MacBookAir4,2").contains(macModelIdentifier)) {
                    powerAdapter = "45W MagSafe 1";
                } else if (Arrays.asList("MacBookPro10,1", "MacBookPro11,2", "MacBookPro11,3", "MacBookPro11,4", "MacBookPro11,5").contains(macModelIdentifier)) {
                    powerAdapter = "85W MagSafe 2";
                } else if (Arrays.asList("MacBookPro10,2", "MacBookPro11,1", "MacBookPro12,1").contains(macModelIdentifier)) {
                    powerAdapter = "60W MagSafe 2";
                } else if (Arrays.asList("MacBookAir5,1", "MacBookAir5,2", "MacBookAir6,1", "MacBookAir6,2", "MacBookAir7,1", "MacBookAir7,2").contains(macModelIdentifier)) {
                    powerAdapter = "45W MagSafe 2";
                } else if (Arrays.asList("MacBookPro16,1", "MacBookPro16,4").contains(macModelIdentifier)) {
                    powerAdapter = "96W USB-C";
                } else if (Arrays.asList("MacBookPro13,3", "MacBookPro14,3", "MacBookPro15,1", "MacBookPro15,3").contains(macModelIdentifier)) {
                    powerAdapter = "87W USB-C";
                } else if (Arrays.asList("Mac14,7").contains(macModelIdentifier)) {
                    powerAdapter = "67W USB-C";
                } else if (Arrays.asList("MacBookPro13,1", "MacBookPro13,2", "MacBookPro14,1", "MacBookPro14,2", "MacBookPro15,2", "MacBookPro15,4", "MacBookPro16,2", "MacBookPro16,3", "MacBookPro17,1").contains(macModelIdentifier)) {
                    powerAdapter = "61W USB-C";
                } else if (Arrays.asList("MacBook10,1", "MacBookAir8,1", "MacBookAir8,2", "MacBookAir9,1", "MacBookAir10,1").contains(macModelIdentifier)) {
                    powerAdapter = "30W USB-C";
                } else if (Arrays.asList("MacBook8,1", "MacBook9,1").contains(macModelIdentifier)) {
                    powerAdapter = "29W USB-C";
                } else if (Arrays.asList("Mac14,6", "Mac14,10", "Mac15,7", "Mac15,9", "Mac15,11", "Mac16,5", "Mac16,7", "MacBookPro18,1", "MacBookPro18,2").contains(macModelIdentifier)) {
                    powerAdapter = "140W USB-C/MagSafe 3";
                } else if (Arrays.asList("Mac14,5", "Mac14,9", "MacBookPro18,3", "MacBookPro18,4").contains(macModelIdentifier)) {
                    powerAdapter = "67W or 96W USB-C/MagSafe 3";
                } else if (Arrays.asList("Mac14,2", "Mac15,12", "Mac16,12").contains(macModelIdentifier)) {
                    powerAdapter = "30W or 35W Dual Port or 70W USB-C/MagSafe 3";
                } else if (Arrays.asList("Mac14,15", "Mac15,13", "Mac16,13").contains(macModelIdentifier)) {
                    powerAdapter = "35W Dual Port or 70W USB-C/MagSafe 3";
                } else if (Arrays.asList("Mac15,3", "Mac15,6", "Mac15,8", "Mac15,10", "Mac16,1", "Mac16,6", "Mac16,8", "Mac17,2").contains(macModelIdentifier)) {
                    powerAdapter = "70W or 96W USB-C/MagSafe 3";
                }

                if (systemProductName.equals("N/A")) {
                    // Only overwrite any existing model on Linux/Windows if systemProductName was confirmed to be the correct macModelIdentifier.
                    // And systemProductName will always be N/A on macOS.
                    model = macShortModelName;

                    if (!macModelIdentifierNumber.isEmpty() && !model.contains(macModelIdentifierNumber)) {
                        if (macModelIdentifier.equals("Mac" + macModelIdentifierNumber)) {
                            model += " / " + macModelIdentifier;
                        } else {
                            model += " " + macModelIdentifierNumber;
                        }
                    }
                }

                boolean didGetLocalMacMarketingModelName = false;
                String possibleMarketingModelNames = "";
                if (isMacOS && cpu.startsWith("Apple")) {
                    // This local marketing model name only exists on Apple Silicon Macs.
                    String localMarketingModelName = new CommandReader(new String[]{"/bin/bash", "-c", "/usr/libexec/PlistBuddy -c 'Print :0:product-name' /dev/stdin <<< \"$(/usr/sbin/ioreg -arc IOPlatformDevice -k product-name)\" | /usr/bin/tr -d '[:cntrl:]'"}).getFirstOutputLine().trim().replaceAll("\\s{2,}", " "); // Remove control characters because this decoded value could end with a null char.

                    if (!localMarketingModelName.isEmpty()) {
                        model = localMarketingModelName;
                        didGetLocalMacMarketingModelName = true;
                    }
                } else {
                    // The following list of Marketing Model Names with grouped Model IDs and Serial Config Codes is generated from: https://github.com/freegeek-pdx/macOS-Testing-and-Deployment-Scripts/blob/main/Other%20Scripts/group_every_intel_mac_marketing_model_name_with_model_ids_and_serial_config_codes.sh
                    // And the full output from that script is here: https://github.com/freegeek-pdx/macOS-Testing-and-Deployment-Scripts/blob/main/Other%20Scripts/serial-config-codes-output/every_intel_mac_marketing_model_name_with_grouped_model_ids_and_serial_config_codes.txt
                    List<String> everyIntelMacMarketingModelNameWithGroupedModelIDsAndSerialConfigCodes = Arrays.asList(
                            "iMac (17-inch, Early 2006):iMac4,1:U2N:U2R:V4M:V4N:V4U:V66:VGB:VGZ:VH1:VHP:VV4:VV6:",
                            "iMac (17-inch, Late 2006 CD):iMac5,2:",
                            "iMac (17-inch, Late 2006):iMac5,1:AC1:VUX:VUY:WAR:WRR:WRW:WV8:WVR:X1A:X1W:X2W:X6Q:X9F:X9Y:XLF:Y3V:Y3W:Y3X:Y6K:Y94:Y97:YAG:YLJ:",
                            "iMac (17-inch, Mid 2006):iMac4,2:",
                            "iMac (20-inch, Early 2006):iMac4,1:U2P:U2S:V4P:V4Q:V4R:V67:VGC:VGM:VH0:VH2:VW4:VX0:WXN:X0U:",
                            "iMac (20-inch, Early 2008):iMac8,1:28B:2PN:2PR:3FF:3FG:3SZ:5A8:5J0:6F9:8R2:8R3:ZE2:ZE3:ZE5:ZE6:",
                            "iMac (20-inch, Early 2009):iMac9,1:0TF:0TH:6X0:8M5:8TS:8TT:9EX:9LN:",
                            "iMac (20-inch, Late 2006):iMac5,1:VUV:VUW:WRS:WRX:WSD:X0E:X29:X6S:X9E:X9G:XA4:XCR:XCY:Y3R:Y3U:Y9B:YAE:YDW:",
                            "iMac (20-inch, Mid 2007):iMac7,1:02X:09Q:0PQ:0PR:0PT:0U1:1NU:1NV:3PB:X85:X86:X87:X88:Z58:Z9G:ZEG:ZFD:",
                            "iMac (20-inch, Mid 2009):iMac9,1:6MH:6MJ:9TH:BAH:DMV:DWY:E86:FUN:FXN:GM9:H1S:HS6:HS7:HT6:HUE:",
                            "iMac (21.5-inch, 2017):iMac18,1:",
                            "iMac (21.5-inch, Early 2013):iMac13,3:",
                            "iMac (21.5-inch, Late 2009):iMac10,1:5PC:5PK:B9S:B9U:CY8:DMW:DMX:DWR:DWU:E8D:E8E:E8F:F0G:F0H:FQH:FU1:H9K:HDF:",
                            "iMac (21.5-inch, Late 2011):iMac12,1:DKL9:DKLH:DPNK:DPNW:",
                            "iMac (21.5-inch, Late 2012):iMac13,1:",
                            "iMac (21.5-inch, Late 2013):iMac14,1:iMac14,3:",
                            "iMac (21.5-inch, Late 2015):iMac16,1:iMac16,2:GF1J:GF1K:GF1L:GF1M:GG77:GG79:GG7D:GG7G:H0N6:H0P6:H1DX:H1DY:H1F1:H1F2:H1WR:H25M:H2KW:H8KX:HHMG:HQ9T:HQ9V:HQ9W:HYGQ:J0DG:J0DH:J0DJ:",
                            "iMac (21.5-inch, Mid 2010):iMac11,2:",
                            "iMac (21.5-inch, Mid 2011):iMac12,1:DHJF:DHJN:DHJR:DHJT:DL8M:DL8N:DMP0:DNWY:DPM0:DPNT:DWTP:DWTQ:F611:",
                            "iMac (21.5-inch, Mid 2014):iMac14,4:",
                            "iMac (24-inch, Early 2008):iMac8,1:0KM:0N4:1LW:28A:2E4:2NX:2PT:39S:3F9:3FH:3GS:3NX:5J1:5U6:6J3:6J6:6ZC:ZE4:ZE7:",
                            "iMac (24-inch, Early 2009):iMac9,1:0TG:0TJ:0TL:0TM:250:259:6X1:6X2:6X3:8M6:8XH:9ET:9F3:9LP:9LQ:9LR:9LS:E1B:",
                            "iMac (24-inch, Late 2006):iMac6,1:",
                            "iMac (24-inch, Mid 2007):iMac7,1:0PL:0PM:0PN:0PP:0PU:1NW:1SC:2CB:3PA:X89:X8A:Z59:Z9F:ZCR:ZCT:ZCV:ZCW:ZEF:ZGH:ZGP:",
                            "iMac (27-inch, Late 2009):iMac10,1:iMac11,1:5PE:5PJ:5PM:5RU:CYB:CYC:D4V:DMY:DMZ:DWZ:E1J:F0J:F0K:GRP:H9L:H9N:H9P:H9R:",
                            "iMac (27-inch, Late 2012):iMac13,2:",
                            "iMac (27-inch, Late 2013):iMac14,2:",
                            "iMac (27-inch, Mid 2010):iMac11,3:",
                            "iMac (27-inch, Mid 2011):iMac12,2:",
                            "iMac (Retina 4K, 21.5-inch, 2017):iMac18,2:",
                            "iMac (Retina 4K, 21.5-inch, 2019):iMac19,2:",
                            "iMac (Retina 4K, 21.5-inch, Late 2015):iMac16,2:GG78:GG7C:GG7F:GG7H:H0KF:H0P7:H15R:H1F3:H1F5:H1F7:H1F8:H1F9:H25N:H28H:H3RJ:H8KY:H8L0:H8L1:H8L2:H8L3:HLWV:",
                            "iMac (Retina 5K, 27-inch, 2017):iMac18,3:",
                            "iMac (Retina 5K, 27-inch, 2019):iMac19,1:",
                            "iMac (Retina 5K, 27-inch, 2020):iMac20,1:iMac20,2:",
                            "iMac (Retina 5K, 27-inch, Late 2014):iMac15,1:FY11:FY14:FY68:FY6F:GCTM:GDQY:GDR3:GDR4:GDR5:GDR6:GDR7:GDR8:GDR9:GDRC:GFFQ:GJDM:GJDN:GJDP:GJDQ:GPJN:GV7V:H5DN:H682:",
                            "iMac (Retina 5K, 27-inch, Late 2015):iMac17,1:",
                            "iMac (Retina 5K, 27-inch, Mid 2015):iMac15,1:FY10:FY13:FY67:FY6D:GL1Q:GL1R:GL1T:GL1V:GL1W:",
                            "iMac Pro (2017):iMacPro1,1:",
                            "Mac mini (2018):Macmini8,1:",
                            "Mac mini (Early 2006):Macmini1,1:U35:U36:U38:U39:VJN:VLK:VS5:VS7:VU2:VU4:WBZ:WCU:WEN:",
                            "Mac mini (Early 2009):Macmini3,1:19X:19Y:1BU:1BV:8NC:92G:9RR:9RS:AFR:BAV:",
                            "Mac mini (Late 2006):Macmini1,1:W0A:W0B:W0C:W0D:WKN:X1X:X1Y:X1Z:X20:XAS:Y9E:",
                            "Mac mini (Late 2009):Macmini3,1:306:307:9G5:9G6:9G7:9G8:AFK:B9X:CS6:DMG:DMH:F6J:",
                            "Mac mini (Late 2012):Macmini6,1:Macmini6,2:DWYL:DWYM:DY3G:DY3H:F9RK:F9RL:F9RM:F9VV:F9VW:F9W0:F9W1:F9W2:FD9G:FD9H:FD9J:FD9K:FDWK:FGML:FRFP:FW56:FW57:G430:",
                            "Mac mini (Late 2014):Macmini7,1:",
                            "Mac mini (Mid 2007):Macmini2,1:",
                            "Mac mini (Mid 2010):Macmini4,1:DD6H:DD6L:DDQ9:DDVN:DFDK:",
                            "Mac mini (Mid 2011):Macmini5,1:Macmini5,2:",
                            "Mac mini Server (Late 2012):Macmini6,2:DWYN:DY3J:F9VY:F9W3:FC08:FCCW:FP14:FP39:",
                            "Mac mini Server (Mid 2010):Macmini4,1:DD6K:DD6N:DDJF:",
                            "Mac mini Server (Mid 2011):Macmini5,3:",
                            "Mac Pro (2019):MacPro7,1:K7GD:K7GF:NYGV:P7QJ:P7QK:P7QL:P7QM:P7QN:P7QP:PLXV:PLXW:PLXX:PLXY:",
                            "Mac Pro (Early 2008):MacPro3,1:",
                            "Mac Pro (Early 2009):MacPro4,1:20G:20H:4PC:4PD:7BF:8MC:8PZ:8Q0:8TR:8TU:8XG:8XL:93H:9EU:9EV:9MC:9MD:9MG:9MJ:9MK:9ML:9QK:ANS:BXD:BXE:BXT:CZ2:CZ3:CZ4:E1C:E1D:E1E:EAA:EYX:EYY:F6H:GYH:",
                            "Mac Pro (Late 2013):MacPro6,1:",
                            "Mac Pro (Mid 2010):MacPro5,1:EUE:EUF:EUG:EUH:GWR:GY5:GZH:GZJ:GZK:GZL:GZM:H0X:H2N:H2P:H97:H99:HF7:HF8:HF9:HFA:HFC:HFD:HFF:HFG:HFJ:HFK:HFL:HFN:HG1:HG3:HP9:HPA:",
                            "Mac Pro (Mid 2012):MacPro5,1:F4MC:F4MD:F4MG:F4MH:F4YY:F500:F648:F649:F64C:F64D:F64F:F6T9:F6TC:F6TD:F6TF:F6TG:",
                            "Mac Pro (Rack, 2019):MacPro7,1:N5RH:N5RN:P7QQ:P7QR:P7QT:P7QV:PNTN:PNTP:PNTQ:PP3Y:",
                            "Mac Pro Server (Mid 2010):MacPro5,1:HPV:HPW:HPY:",
                            "Mac Pro Server (Mid 2012):MacPro5,1:F4MF:F4MJ:F501:",
                            "Mac Pro:MacPro1,1:MacPro2,1:",
                            "MacBook (13-inch, Aluminum, Late 2008):MacBook5,1:",
                            "MacBook (13-inch, Early 2008):MacBook4,1:0P0:0P1:0P2:0P4:0P5:0P6:1LX:1PX:1Q2:1Q7:1QA:1QB:1QE:1ZY:27H:27J:28C:28D:28E:385:3N9:3NA:3ND:3NE:3NF:3X6:47Z:4R7:4R8:",
                            "MacBook (13-inch, Early 2009):MacBook5,2:4R1:4R2:4R3:79D:79E:79F:7A2:85D:88J:8CP:8SJ:93K:",
                            "MacBook (13-inch, Late 2006):MacBook2,1:WGK:WGL:WGM:WGN:WGP:WGQ:WGS:WGT:WGU:WVN:X6G:X6H:X6J:X6K:X6L:X7X:X97:X98:XAR:XAT:XC5:XDN:XDR:XDS:XDT:XDU:XDV:XDW:XDX:XDY:XDZ:XE0:XE1:XE2:XE3:XHB:XHC:XKT:XMF:Y6L:Y6M:Y9A:YCU:",
                            "MacBook (13-inch, Late 2007):MacBook3,1:",
                            "MacBook (13-inch, Late 2008):MacBook4,1:3VY:5AQ:5HS:5HU:67C:6ES:6HY:6LL:6LM:6M1:6V9:6YP:7XD:",
                            "MacBook (13-inch, Late 2009):MacBook6,1:",
                            "MacBook (13-inch, Mid 2007):MacBook2,1:YA2:YA3:YA4:YA5:YA6:YA7:YA8:YA9:YJJ:YJK:YJL:YJM:YJN:YQ7:YQ8:YRG:YRH:YRJ:YRK:YSH:YSJ:YSK:YSL:YSM:YTK:YTL:YV8:YX1:YX2:YX4:YX5:YXZ:YY1:YYW:Z5V:Z5W:Z5X:Z5Y:Z5Z:Z60:Z88:ZA8:ZA9:ZAP:ZAQ:ZAS:ZAU:ZAV:ZAW:ZAX:ZAY:ZAZ:ZB0:ZB1:ZB2:ZB7:ZB8:ZB9:ZBA:ZBB:ZBE:ZBF:ZBG:ZBH:ZBJ:ZBK:ZCN:",
                            "MacBook (13-inch, Mid 2009):MacBook5,2:9GU:9GV:A1W:A1X:A1Y:A9P:A9Q:A9Y:ABW:ASC:",
                            "MacBook (13-inch, Mid 2010):MacBook7,1:",
                            "MacBook (13-inch):MacBook1,1:",
                            "MacBook (Retina, 12-inch, 2017):MacBook10,1:",
                            "MacBook (Retina, 12-inch, Early 2015):MacBook8,1:",
                            "MacBook (Retina, 12-inch, Early 2016):MacBook9,1:",
                            "MacBook Air (11-inch, Early 2014):MacBookAir6,1:FM72:G083:G084:G2CF:G2GH:G2GJ:G2PY:G2Q0:G4FY:G4H0:G4H4:G4HK:G4HM:G58J:G5RK:G5RL:G5RM:G6D3:GLK9:GP4N:GP4P:",
                            "MacBook Air (11-inch, Early 2015):MacBookAir7,1:",
                            "MacBook Air (11-inch, Late 2010):MacBookAir3,1:",
                            "MacBook Air (11-inch, Mid 2011):MacBookAir4,1:",
                            "MacBook Air (11-inch, Mid 2012):MacBookAir5,1:",
                            "MacBook Air (11-inch, Mid 2013):MacBookAir6,1:F5N7:F5N8:F5YV:F5YW:FH51:FH52:FKYN:FKYP:FLCF:FMR5:FMR6:FMR9:FMRC:FMRD:FMRF:FMRG:FMRM:FMRN:FN5M:FN7F:FP2N:FP3C:FQLG:FT30:",
                            "MacBook Air (13-inch, 2017):MacBookAir7,2:J1WK:J1WL:J1WM:J1WT:J1WV:J8N7:J8XG:J8XH:J9HX:J9TN:J9TP:J9TQ:JC9H:JCD6:JFLY:JKHD:JKHF:LQ07:LQF1:MFWJ:",
                            "MacBook Air (13-inch, Early 2014):MacBookAir6,2:G085:G086:G2CC:G2CD:G2GK:G2GL:G2GM:G2GN:G356:G4H1:G4H2:G4H3:G4HN:G4HP:G58K:G5RN:G5RP:G5RQ:G6D4:G6D5:G829:G8J1:GLK7:GLK8:GP4L:GP4M:",
                            "MacBook Air (13-inch, Early 2015):MacBookAir7,2:G940:G941:G942:G943:G944:GKJT:GKJV:GL20:GL21:GL22:GL23:GL24:GL25:GLCN:GLCP:GM14:GM15:GM38:GM6M:GM9G:GMC3:GMD3:GN8C:GNJJ:GNKM:H3QD:H3QF:H3QJ:H3QK:H569:H8VT:H8VV:H8VW:H8VX:HD7X:HD80:HD98:HDV4:HDV5:HDV6:HF4F:HF4H:HF9N:J6VL:",
                            "MacBook Air (13-inch, Late 2010):MacBookAir3,2:",
                            "MacBook Air (13-inch, Mid 2011):MacBookAir4,2:",
                            "MacBook Air (13-inch, Mid 2012):MacBookAir5,2:",
                            "MacBook Air (13-inch, Mid 2013):MacBookAir6,2:F5V7:F5V8:F6T5:F6T6:FH53:FKYQ:FKYR:FLCG:FM23:FM3Y:FM74:FMR7:FMR8:FMRH:FMRJ:FMRK:FMRL:FMRV:FMRW:FMRY:FN3Y:FN40:FN7G:FP2P:FQL9:FQLC:FQLD:FQLF:G6PM:",
                            "MacBook Air (Late 2008):MacBookAir2,1:22D:22E:5L9:5LA:5TX:5U1:5U7:60R:62W:63V:63W:6JN:",
                            "MacBook Air (Mid 2009):MacBookAir2,1:9A5:9A6:9A7:9A8:",
                            "MacBook Air (Original):MacBookAir1,1:",
                            "MacBook Air (Retina, 13-inch, 2018):MacBookAir8,1:",
                            "MacBook Air (Retina, 13-inch, 2019):MacBookAir8,2:",
                            "MacBook Air (Retina, 13-inch, 2020):MacBookAir9,1:",
                            "MacBook Pro (13-inch, 2016, Four Thunderbolt 3 Ports):MacBookPro13,2:",
                            "MacBook Pro (13-inch, 2016, Two Thunderbolt 3 ports):MacBookPro13,1:",
                            "MacBook Pro (13-inch, 2017, Four Thunderbolt 3 Ports):MacBookPro14,2:",
                            "MacBook Pro (13-inch, 2017, Two Thunderbolt 3 ports):MacBookPro14,1:",
                            "MacBook Pro (13-inch, 2018, Four Thunderbolt 3 Ports):MacBookPro15,2:JHC8:JHC9:JHCC:JHCD:JHCF:JHD2:JHD3:JHD4:JHD5:KK98:KK99:KK9C:KQ1X:KQ1Y:KQ20:KQ21:KQ22:KQ23:KQ24:KQ25:KQ26:KQ27:L42X:L4FC:L4FD:L4FF:L4FG:L4FJ:L4JT:L7GD:LK8C:",
                            "MacBook Pro (13-inch, 2019, Four Thunderbolt 3 ports):MacBookPro15,2:LVDC:LVDD:LVDF:LVDG:LVDH:LVDL:LVDM:LVDN:LVDP:MV9K:MV9R:N5T5:NCLV:NCLW:NCLX:NCLY:NCM0:NCM1:NCM2:NQM8:P4G1:P4G2:",
                            "MacBook Pro (13-inch, 2019, Two Thunderbolt 3 ports):MacBookPro15,4:",
                            "MacBook Pro (13-inch, 2020, Four Thunderbolt 3 ports):MacBookPro16,2:",
                            "MacBook Pro (13-inch, 2020, Two Thunderbolt 3 ports):MacBookPro16,3:",
                            "MacBook Pro (13-inch, Early 2011):MacBookPro8,1:DH2G:DH2H:DH2L:DH2M:DLN5:DLN6:DM75:DMLF:DMLH:DMLJ:DNCM:DNGD:DNKP:DNKQ:DNTK:DNVY:DR7W:DRJ7:DRJ9:DRJJ:DRJK:DRW1:DRW2:DRW7:DT4G:DT4H:DT60:DT61:DT62:DT63:DT64:DT65:DT66:DT67:ST61:",
                            "MacBook Pro (13-inch, Late 2011):MacBookPro8,1:DV13:DV14:DV16:DV17:DVHJ:DVHK:DVHP:DVHQ:DW13:DY1J:DY1K:DY5T:DY5V:DY6C:DY77:DYL0:DYL1:DYL2:F298:F299:",
                            "MacBook Pro (13-inch, Mid 2009):MacBookPro5,5:",
                            "MacBook Pro (13-inch, Mid 2010):MacBookPro7,1:",
                            "MacBook Pro (13-inch, Mid 2012):MacBookPro9,2:",
                            "MacBook Pro (15-inch, 2.4/2.2GHz):MacBookPro3,1:02V:0LQ:0LZ:0M0:0PA:0S3:0S6:1CY:1CZ:2QU:2QV:X91:X92:XAG:XAH:Y9S:Y9T:YAL:YAM:YKX:YKY:YKZ:YL0:YQ3:YW5:YW9:YWA:YWD:YYV:YYX:YZ0:Z05:Z09:Z0G:",
                            "MacBook Pro (15-inch, 2.53GHz, Mid 2009):MacBookPro5,4:",
                            "MacBook Pro (15-inch, 2016):MacBookPro13,3:",
                            "MacBook Pro (15-inch, 2017):MacBookPro14,3:",
                            "MacBook Pro (15-inch, 2018):MacBookPro15,1:MacBookPro15,3:JG5H:JG5J:JG5K:JG5L:JG5M:JGH5:JGH6:JGH7:JGH8:KGYF:KGYG:KGYH:KQ9Q:KQ9R:KQ9T:KQ9V:KQ9W:KQ9X:KQ9Y:KQC0:KQC1:KQC2:KQC3:KQC4:KQC5:KQC6:KQC7:KQC8:KQC9:KQCC:KQCD:KQCF:KQCG:KQCH:KQCJ:KQCK:KQCL:KQCM:KQCN:KQCP:KQCQ:KQCR:KQCT:KQCV:KQCW:KQCX:KWJ2:L4HW:L4HX:L539:L53D:L7GC:LC8J:LC8K:LC8L:LCM6:MJLR:MJLT:",
                            "MacBook Pro (15-inch, 2019):MacBookPro15,1:MacBookPro15,3:LVCF:LVCG:LVCH:LVCJ:LVCK:LVCL:LVDQ:LVDR:LVDT:LVDV:MV9T:MVC0:N5T6:N6KF:N6RJ:NCM3:NCM4:NCM5:NCM6:NQM9:NQMC:NQMD:NQMF:",
                            "MacBook Pro (15-inch, Core 2 Duo):MacBookPro2,2:",
                            "MacBook Pro (15-inch, Early 2008):MacBookPro4,1:1AJ:1EK:1EM:1JZ:1K0:1SH:1XR:1XW:27N:2AZ:2B0:2CE:2DT:2DX:2MF:2PK:33B:3LY:3LZ:48T:4R5:4R6:YJX:YJY:YJZ:YK0:ZLU:",
                            "MacBook Pro (15-inch, Early 2011):MacBookPro8,2:DF8V:DF8X:DF8Y:DF91:DLN7:DLN8:DMC8:DMC9:DMDG:DMDH:DMDJ:DMGG:DMMF:DMMH:DMMJ:DMPG:DMPK:DMPL:DMPM:DMPN:DMPP:DMPQ:DMPR:DMQP:DNC3:DNCN:DNGF:DNH5:DNHY:DNKM:DNKY:DNM4:DNMW:DNRD:DNVK:DRJC:DRJD:DRJF:DRJL:DRJM:DRW3:DRW4:DRWD:DT4J:DT54:DT55:DT56:DT57:DT58:DT59:DT5C:DT5D:DT5F:DT5G:DT5H:DT5J:DT5L:DT68:DT69:DT6C:DT6D:DT6F:DT6G:DT6H:DT6J:DT6K:DT6L:DT6M:DT6R:",
                            "MacBook Pro (15-inch, Glossy):MacBookPro1,1:VWW:VWX:VWY:VWZ:W3N:W92:W93:W94:W9F:W9Q:WAG:WAW:WB8:WBE:WBF:WBH:WBJ:WD7:WD8:WD9:WDA:WDB:WDC:WDD:WTS:WW0:WW1:WW2:WW3:",
                            "MacBook Pro (15-inch, Late 2008):MacBookPro5,1:",
                            "MacBook Pro (15-inch, Late 2011):MacBookPro8,2:DV7L:DV7M:DV7N:DV7P:DVHL:DVHM:DVHR:DW3G:DW3H:DW3J:DW47:DY1L:DY1M:DY1N:DY1P:DY1Q:DY1R:DY1T:DY1V:DY1W:DY1Y:DY20:DY21:DY5K:DY5P:DY5Q:DY5R:DY5Y:DY60:DY7G:DYG6:DYG7:DYK9:DYKC:DYR1:F0K6:F0V2:",
                            "MacBook Pro (15-inch, Mid 2009):MacBookPro5,3:",
                            "MacBook Pro (15-inch, Mid 2010):MacBookPro6,2:",
                            "MacBook Pro (15-inch, Mid 2012):MacBookPro9,1:",
                            "MacBook Pro (16-inch, 2019):MacBookPro16,1:MacBookPro16,4:",
                            "MacBook Pro (17-inch, 2.4GHz):MacBookPro3,1:027:028:02D:09R:09S:0LR:0ND:0NM:0PD:1CW:1CX:1MF:1MG:2QW:X94:XA9:YAA:YAN:YAP:YNQ:YNS:YNW:YQ4:YQ5:YR2:YRD:YRE:YRF:YWB:YWC:YZ1:YZ2:Z5M:",
                            "MacBook Pro (17-inch, Core 2 Duo):MacBookPro2,1:",
                            "MacBook Pro (17-inch, Early 2008):MacBookPro4,1:1BY:1ED:1EN:1ER:1K2:1K8:1K9:1KA:1Q3:1SG:2CF:2DY:2DZ:2ED:3DC:3DD:3DE:3DF:3M0:3M4:3M5:YP3:YP4:ZLV:",
                            "MacBook Pro (17-inch, Early 2009):MacBookPro5,2:2QP:2QT:776:77A:7AP:7AS:7XQ:7XR:7XS:87K:87L:87M:87N:8FK:8FL:8FM:8FY:8FZ:8G0:",
                            "MacBook Pro (17-inch, Early 2011):MacBookPro8,3:DF92:DF93:DLN9:DLNC:DMGH:DMQT:DMQW:DMR2:DMR4:DMR5:DMR7:DMR8:DMR9:DMRC:DNGG:DNKN:DRJG:DRJH:DRJN:DRW5:DRW6:DT5M:DT5N:DT5P:DT5Q:DT5R:DT5T:DT5V:DT5W:DT5Y:DT6N:DT6P:",
                            "MacBook Pro (17-inch, Late 2008):MacBookPro4,1:3R8:3R9:4RT:4RW:57J:5U0:634:65A:663:664:666:668:6CT:6JK:",
                            "MacBook Pro (17-inch, Late 2011):MacBookPro8,3:AY5W:DV10:DV11:DVHN:DVHV:DVHW:DW48:DY22:DY23:DY24:DY25:DY26:DY5W:DYG8:F13Y:F140:",
                            "MacBook Pro (17-inch, Mid 2009):MacBookPro5,2:8YA:8YB:91T:A3M:A3N:A5R:A5W:AF3:AKV:AKW:AMV:AMW:AN1:ANC:AND:ANE:ANF:ANJ:AUU:E6L:",
                            "MacBook Pro (17-inch, Mid 2010):MacBookPro6,1:",
                            "MacBook Pro (17-inch):MacBookPro1,2:",
                            "MacBook Pro (Original):MacBookPro1,1:THV:VGW:VGX:VGY:VJ0:VJ1:VJ2:VJ3:VJ5:VJ6:VJ7:VJM:VMU:VSD:VTZ:VU0:VWA:VWB:VXW:VXX:W2Q:",
                            "MacBook Pro (Retina, 13-inch, Early 2013):MacBookPro10,2:FFRP:FFRR:FG1F:FG28:FGM8:FGN5:FGN6:FGPJ:FHCH:FHN0:",
                            "MacBook Pro (Retina, 13-inch, Early 2015):MacBookPro12,1:",
                            "MacBook Pro (Retina, 13-inch, Late 2012):MacBookPro10,2:DR53:DR54:DR55:DR56:F775:F776:F7YF:F897:F8V6:F8V7:F8V8:F9JT:F9V1:F9VQ:FG7Q:FG7R:FL85:FMLJ:",
                            "MacBook Pro (Retina, 13-inch, Late 2013):MacBookPro11,1:FGYY:FH00:FH01:FH02:FH03:FH04:FH05:FRF6:FRF7:FRQF:FT4Q:FT4R:FT4T:FT4V:FTC9:FTCD:FTCH:FTCK:FTCL:FTPH:FTPJ:FTPK:FTT4:FVVW:FVWQ:FWKF:G4N6:G4N7:",
                            "MacBook Pro (Retina, 13-inch, Mid 2014):MacBookPro11,1:G3QH:G3QJ:G3QK:G3QL:G3QQ:G3QR:G3QT:G7RD:G7RF:G7YQ:G7YR:G8L0:G96R:G96T:G96V:G96W:G96Y:G970:G971:G972:G9FL:G9FM:G9FN:G9FP:G9FQ:G9FR:GDJM:",
                            "MacBook Pro (Retina, 15-inch, Early 2013):MacBookPro10,1:FFT0:FFT1:FFT2:FFT3:FFT4:FG1H:FG1J:FGFH:FGFJ:FGFK:FGFL:FGN7:FGWF:FGWG:FGWH:FHCQ:FHCR:FJ47:FJVJ:FL94:FMLK:FR8D:",
                            "MacBook Pro (Retina, 15-inch, Late 2013):MacBookPro11,2:MacBookPro11,3:FD56:FD57:FD58:FD59:FR1M:FRDM:FRG2:FRG3:FRQH:FRQJ:FRQK:FRQL:FT4P:FTK0:FTK1:FTPL:FTPM:FTPN:FTPP:FTPQ:FTPR:FTPT:FTPV:FTPW:FTPY:FTTJ:FVN4:FVYN:FWFY:FWHW:FWKK:FWKL:G4JQ:G5HL:",
                            "MacBook Pro (Retina, 15-inch, Mid 2014):MacBookPro11,2:MacBookPro11,3:G3QC:G3QD:G3QG:G3QN:G3QP:G85Y:G86P:G86Q:G86R:G8F4:G8J7:G8L1:G96K:G96L:G96M:G96N:G96P:G96Q:G973:G974:G9FT:G9JN:G9L6:G9L7:G9L8:G9L9:GDPP:ZORD:",
                            "MacBook Pro (Retina, 15-inch, Mid 2015):MacBookPro11,4:MacBookPro11,5:",
                            "MacBook Pro (Retina, Mid 2012):MacBookPro10,1:DKQ1:DKQ2:DKQ4:DKQ5:F51R:F5Y2:F69W:F69Y:F6DN:F6F3:F6L9:F8JY:F96W:F9F1:F9F2:FCQ3:",
                            "Xserve (Early 2008):Xserve2,1:",
                            "Xserve (Early 2009):Xserve3,1:",
                            "Xserve (Late 2006):Xserve1,1:"
                    );

                    ArrayList<String> possibleMarketingModelNamesList = new ArrayList<>();
                    for (String thisIntelMacMarketingModelNameWithGroupedModelIDsAndSerialConfigCodes : everyIntelMacMarketingModelNameWithGroupedModelIDsAndSerialConfigCodes) {
                        if (!macSerialConfigCode.isEmpty() && thisIntelMacMarketingModelNameWithGroupedModelIDsAndSerialConfigCodes.contains(":" + macSerialConfigCode + ":")) {
                            possibleMarketingModelNamesList.clear();
                            possibleMarketingModelNamesList.add(thisIntelMacMarketingModelNameWithGroupedModelIDsAndSerialConfigCodes.substring(0, thisIntelMacMarketingModelNameWithGroupedModelIDsAndSerialConfigCodes.indexOf(":")));
                            break;
                        } else if (thisIntelMacMarketingModelNameWithGroupedModelIDsAndSerialConfigCodes.contains(":" + macModelIdentifier + ":")) {
                            possibleMarketingModelNamesList.add(thisIntelMacMarketingModelNameWithGroupedModelIDsAndSerialConfigCodes.substring(0, thisIntelMacMarketingModelNameWithGroupedModelIDsAndSerialConfigCodes.indexOf(":")));
                        }
                    }

                    int possibleMarketingModelNamesListCount = possibleMarketingModelNamesList.size();
                    if (possibleMarketingModelNamesListCount == 1) {
                        model = possibleMarketingModelNamesList.get(0);
                        didGetLocalMacMarketingModelName = true;
                    } else if (possibleMarketingModelNamesListCount != 0) {
                        possibleMarketingModelNames = String.join(" or ", possibleMarketingModelNamesList) + " - " + possibleMarketingModelNamesListCount + " POSSIBLE MODELS";
                    }
                }

                boolean marketingModelNameWasCachedOnMacOS = false;
                boolean didDownloadMarketingModelName = false;
                if (!didGetLocalMacMarketingModelName && !serial.equals("N/A") && !serialIsMAC) {
                    if (isMacOS && !macSerialConfigCode.isEmpty()) {
                        // If About This Mac has been opened, the Marketing Model Name will be cached in this user preference.
                        // Since "defaults read" has no option to traverse into keys of dictionary values, use the whole "defaults export" output and parse it with "PlistBuddy" to get at the specific key of the "CPU Names" dictionary value that we want.
                        // Using "defaults export" instead of accessing the plist file directly with "PlistBuddy" is important since preferences are not guaranteed to be written to disk if they were just set.
                        String cachedMarketingModelName = new CommandReader(new String[]{"/bin/bash", "-c", "/usr/libexec/PlistBuddy -c \"Print :'CPU Names':" + macSerialConfigCode + "-en-US_US\" /dev/stdin <<< \"$(/usr/bin/defaults export com.apple.SystemProfiler -)\""}).getFirstOutputLine().trim().replaceAll("\\s{2,}", " ");
                        if (!cachedMarketingModelName.isEmpty() && cachedMarketingModelName.startsWith(macShortModelName)) { // Make sure the value starts with the short model name, since technically anything could be set to this value manually.
                            model = cachedMarketingModelName;
                            marketingModelNameWasCachedOnMacOS = true;
                        }
                    }

                    if (!marketingModelNameWasCachedOnMacOS) {
                        int maxLoadModelAttempts = 3;
                        for (int loadModelAttempt = 1; loadModelAttempt <= maxLoadModelAttempts; loadModelAttempt++) {
                            try {
                                String downloadedMarketingModelName = "";

                                if (!macSerialConfigCode.isEmpty()) {
                                    // The following URL API is what "About This Mac" uses to load the Marketing Model Name.

                                    // NOTE: Loading apple.com with WebReader seems to fail on Linux and Windows (with Java 21) with SSLHandshakeException handshake_failure, but not on macOS.
                                    // But, loading the URL via "curl" or "Invoke-RestMethod" works, so do that instead on Linux and Windows.
                                    if (isLinux) {
                                        downloadedMarketingModelName = new CommandReader(new String[]{"/usr/bin/curl", "-m", "5", "-sfL", "https://support-sp.apple.com/sp/product?cc=" + macSerialConfigCode}).getFirstOutputLineContaining("<configCode>");
                                    } else if (isWindows) {
                                        downloadedMarketingModelName = new CommandReader(new String[]{"\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12, [Net.SecurityProtocolType]::Ssl3; (Invoke-RestMethod -Uri 'https://support-sp.apple.com/sp/product?cc=" + macSerialConfigCode + "' -UseBasicParsing -TimeoutSec 5).root.configCode"}).getFirstOutputLine();
                                    } else {
                                        downloadedMarketingModelName = new WebReader("https://support-sp.apple.com/sp/product?cc=" + macSerialConfigCode).getFirstOutputLineContaining("<configCode>");
                                    }

                                    if (downloadedMarketingModelName.contains("<configCode>")) {
                                        downloadedMarketingModelName = downloadedMarketingModelName.substring(downloadedMarketingModelName.indexOf("<configCode>") + 12, downloadedMarketingModelName.indexOf("</configCode>"));
                                    } else if (!isWindows) {
                                        downloadedMarketingModelName = "";
                                    }

                                    downloadedMarketingModelName = downloadedMarketingModelName.trim().replaceAll("\\s{2,}", " ");
                                }

                                if (downloadedMarketingModelName.isEmpty() || !downloadedMarketingModelName.startsWith(macShortModelName) || downloadedMarketingModelName.equals(macShortModelName)) {
                                    // If the "About This Mac" URL API (used above) failed or only returned the Short Model Name (such as how "MacBook Air" will only be returned for *SOME* 2013 "MacBookAir6,1" or "MacBookAir6,2" serials),
                                    // fallback on using the "Specs Search" URL API (used below) to retrieve the Marketing Model Name (since it will return "MacBook Air (11-inch, Mid 2013)" for the 2013 "MacBookAir6,1" and "MacBook Air (13-inch, Mid 2013)" for the 2013 "MacBookAir6,2").
                                    // For more information about this "Specs Search" URL API, see: https://github.com/freegeek-pdx/macOS-Testing-and-Deployment-Scripts/blob/main/Other%20Scripts/get_specs_url_from_serial.sh
                                    // IMPORTANT: On May 15th, 2025, "https://km.support.apple.com/kb/index?page=categorydata" started returning 403 Forbidden! But other active "page" values that are still used on other parts of their site still work, so I think this was intentionally taken down.

                                    // NOTE: Loading apple.com with WebReader seems to fail on Linux and Windows (with Java 21) with SSLHandshakeException handshake_failure, but not on macOS.
                                    // But, loading the URL via "curl" or "Invoke-RestMethod" works, so do that instead on Linux and Windows.
                                    if (isLinux) {
                                        downloadedMarketingModelName = new CommandReader(new String[]{"/usr/bin/curl", "-m", "10", "-sfL", "https://km.support.apple.com/kb/index?page=categorydata&serialnumber=" + serial}).getFirstOutputLineContaining("\"name\": \""); // I have seen this URL API timeout after 5 seconds when called multiple times rapidly (likely because of rate limiting), so give it a 10 second timeout which seems to always work.
                                    } else if (isWindows) {
                                        downloadedMarketingModelName = new CommandReader(new String[]{"\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12, [Net.SecurityProtocolType]::Ssl3; (Invoke-RestMethod -Uri 'https://km.support.apple.com/kb/index?page=categorydata&serialnumber=" + serial + "' -UseBasicParsing -TimeoutSec 10).name"}).getFirstOutputLine();
                                    } else {
                                        downloadedMarketingModelName = new WebReader("https://km.support.apple.com/kb/index?page=categorydata&serialnumber=" + serial).getOutputLinesAsString();
                                    }

                                    if (downloadedMarketingModelName.contains("\"name\": \"")) {
                                        downloadedMarketingModelName = new JSONObject(downloadedMarketingModelName).getString("name");
                                    } else if (!isWindows) {
                                        downloadedMarketingModelName = "";
                                    }

                                    downloadedMarketingModelName = downloadedMarketingModelName.trim().replaceAll("\\s{2,}", " ");
                                }

                                if (!downloadedMarketingModelName.isEmpty()) {
                                    didDownloadMarketingModelName = true;

                                    if (!downloadedMarketingModelName.startsWith(macShortModelName)) {
                                        if (possibleMarketingModelNames.isEmpty()) {
                                            model = macShortModelName + " - UNKNOWN Marketing Model Name";
                                        } else {
                                            model = possibleMarketingModelNames;
                                        }
                                    } else {
                                        model = downloadedMarketingModelName;

                                        if (isMacOS && !macSerialConfigCode.isEmpty()) {
                                            try {
                                                // If the model contains parentheses, "defaults write" has trouble with it and the value needs to be specially quoted.
                                                // https://apple.stackexchange.com/questions/300845/how-do-i-handle-e-g-correctly-escape-parens-in-a-defaults-write-key-val#answer-300853
                                                Runtime.getRuntime().exec(new String[]{"/usr/bin/defaults", "write", "com.apple.SystemProfiler", "CPU Names", "-dict-add", macSerialConfigCode + "-en-US_US", ((model.contains("(") || model.contains(")")) ? "'" + model + "'" : model)}).waitFor();
                                            } catch (IOException | InterruptedException cacheMarketingModelNameException) {
                                                if (isTestMode) {
                                                    System.out.println("cacheMarketingModelNameException: " + cacheMarketingModelNameException);
                                                }
                                            }
                                        }
                                    }

                                    break;
                                } else if (loadModelAttempt < maxLoadModelAttempts) {
                                    try {
                                        TimeUnit.SECONDS.sleep(loadModelAttempt);
                                    } catch (InterruptedException sleepException) {
                                        // Ignore sleepException
                                    }
                                }
                            } catch (JSONException macModelException) {
                                if (isTestMode) {
                                    System.out.println("macModelException: " + macModelException);
                                }
                            }
                        }
                    }
                }

                if (didGetLocalMacMarketingModelName || marketingModelNameWasCachedOnMacOS || didDownloadMarketingModelName) {
                    if (model.equals(macShortModelName)) {
                        if (possibleMarketingModelNames.isEmpty()) {
                            model += " (No Marketing Model Name Specified)";
                        } else {
                            model = possibleMarketingModelNames;
                        }
                    }
                } else if ((serial.length() < 8) || serialIsMAC) {
                    if (possibleMarketingModelNames.isEmpty()) {
                        model += " - UNKNOWN Marketing Model Name - NO SERIAL";
                    } else {
                        model = possibleMarketingModelNames + " - NO SERIAL";
                    }
                } else if (possibleMarketingModelNames.isEmpty()) {
                    model += " - UNKNOWN Marketing Model Name - INTERNET REQUIRED";
                } else {
                    model = possibleMarketingModelNames + " - INTERNET REQUIRED";
                }

                if (!macModelIdentifierNumber.isEmpty() && !model.contains(macModelIdentifierNumber)) {
                    if (model.contains(macShortModelName) && !macModelIdentifier.equals("Mac" + macModelIdentifierNumber)) {
                        model = model.replace(macShortModelName, macShortModelName + " " + macModelIdentifierNumber);
                    } else {
                        model += " / " + macModelIdentifier;
                    }
                }

                if (hasScreen) {
                    if (isMacOS) {
                        // Get Mac Screen Size (of built-in screen) using native Objective-C methods via JXA (JavaScript for Automation). Based On: https://stackoverflow.com/a/12589799
                        screenSize = new CommandReader(new String[]{"/usr/bin/osascript", "-l", "JavaScript", "-e", "ObjC.import('AppKit'); $.NSApplication.sharedApplication; for (const thisScreen of $.NSScreen.screens.js) { const thisScreenNumber = thisScreen.deviceDescription.js.NSScreenNumber.js; if ($.CGDisplayIsBuiltin(thisScreenNumber)) { const physicalScreenSize = $.CGDisplayScreenSize(thisScreenNumber); (Math.round((Math.sqrt(Math.pow(physicalScreenSize.width, 2) + Math.pow(physicalScreenSize.height, 2)) / 25.4) * 10) / 10); break } }"}).getFirstOutputLine();

                        if (screenSize.isEmpty() || screenSize.equals("0")) { // I've never seen this happen, but doesn't hurt to check.
                            screenSize = "N/A";
                        } else {
                            if (screenSize.endsWith(".9")) {
                                screenSize = new DecimalFormat("#").format(Double.parseDouble(screenSize));
                            }
                            screenSize += "-inch";
                        }
                    }

                    // If not on macOS or somehow that direct screen size check failed (or macScreenType is empty), see if we can get the screen size (or screen type) from the Marketing Model Name
                    if (screenSize.equals("N/A") || macScreenFeaturesArray.isEmpty()) {
                        String[] modelWords = model.split("\\W+");

                        for (int i = 0; i < modelWords.length; i++) {
                            if (modelWords[i].equals("Retina") && (macScreenFeaturesArray.isEmpty() || !macScreenFeaturesArray.contains("Retina"))) {
                                macScreenFeaturesArray.add("Retina");
                            } else if (screenSize.equals("N/A") && modelWords[i].equals("inch")) {
                                try {
                                    String screenInches = modelWords[i - 1];
                                    if (Integer.parseInt(screenInches) < 10) {
                                        screenInches = modelWords[i - 2] + "." + screenInches;
                                    }
                                    screenSize = screenInches + "-inch";
                                } catch (NumberFormatException screenSizeException) {
                                    if (isTestMode) {
                                        System.out.println("screenSizeException: " + screenSizeException);
                                    }
                                }
                            }
                        }
                    }

                    if (isMacOS) {
                        if (new CommandReader(new String[]{"/usr/bin/osascript", "-l", "JavaScript", "-e", "ObjC.import('AppKit'); $.NSApplication.sharedApplication; for (const thisScreen of $.NSScreen.screens.js) { if ($.CGDisplayIsBuiltin(thisScreen.deviceDescription.js.NSScreenNumber.js)) { thisScreen.canRepresentDisplayGamut($.NSDisplayGamutP3); break } }"}).getFirstOutputLine().equals("true")) {
                            macScreenFeaturesArray.add("P3");
                        }

                        if (new CommandReader(new String[]{"/usr/bin/osascript", "-l", "JavaScript", "-e", "ObjC.import('AppKit'); $.NSApplication.sharedApplication; for (const thisScreen of $.NSScreen.screens.js) { if ($.CGDisplayIsBuiltin(thisScreen.deviceDescription.js.NSScreenNumber.js)) { thisScreen.maximumFramesPerSecond; break } }"}).getFirstOutputLine().equals("120")) {
                            // This method is only available on macOS 12 and newer (which the only Macs that support ProMotion shipped with), but it's fine if it errors and returns nothing on older versions of macOS.
                            // https://developer.apple.com/documentation/appkit/nsscreen/3824745-maximumframespersecond?language=objc
                            macScreenFeaturesArray.add("ProMotion");
                        }
                    }

                    if (!macScreenFeaturesArray.isEmpty()) {
                        if (screenSize.equals("N/A")) {
                            screenSize = String.join(" + ", macScreenFeaturesArray);
                        } else {
                            screenSize += " (" + String.join(" + ", macScreenFeaturesArray) + ")";
                        }
                    }
                }

                if (isMacOS && (macHasT2chip || cpu.startsWith("Apple"))) {
                    String macModelPartNumber = new CommandReader("/usr/libexec/remotectl dumpstate | /usr/bin/awk '($1 == \"RegionInfo\") { if ($NF == \"=>\") { region_info = \"LL/A\" } else { region_info = $NF } } ($1 == \"ModelNumber\") { if ($NF ~ /\\//) { print $NF } else { print $NF region_info } exit }'").getFirstOutputLine();
                    if (!macModelPartNumber.isEmpty()) { // This "M####LL/A" style Model Part Number is only be accessible in software on T2 and Apple Silicon Macs.
                        model += " / " + macModelPartNumber;
                    }
                }
            }

            if (isLinux || isWindows) {
                if (brand.equals("N/A")) {
                    brand = motherboardBrand;
                }

                String systemProductNameAndSKU = "";

                boolean modelIsActualSKU = false;
                if (brand.equals("HP") && model.equals("N/A")) {
                    // HP stores the Type/System ID in the Motherboard Product value and often (but not always) the System Version is unused leaving the Model variable N/A.
                    // So, in that case, we might as well fill it with relevant info, and the following code will move it to the end and put the proper Model Name first.
                    model = motherboardModel;
                    modelIsActualSKU = true;
                }

                if (!systemProductName.equals("N/A") && !model.toLowerCase().equals(systemProductName.toLowerCase())) {
                    systemProductNameAndSKU = systemProductName;
                }

                if (!systemProductSKU.equals("N/A") && !systemProductNameAndSKU.toLowerCase().contains(systemProductSKU.toLowerCase()) && !model.toLowerCase().equals(systemProductSKU.toLowerCase())) {
                    if (systemProductNameAndSKU.isEmpty()) {
                        systemProductNameAndSKU = systemProductSKU;
                    } else {
                        systemProductNameAndSKU += " (" + systemProductSKU + ")";
                    }
                }

                if (!systemProductNameAndSKU.isEmpty() && !model.toLowerCase().equals(systemProductNameAndSKU.toLowerCase())) {
                    if (model.equals("N/A")) {
                        model = systemProductNameAndSKU;
                    } else if ((systemProductNameAndSKU.startsWith(brand) && !model.startsWith(brand)) || (systemProductNameAndSKU.replace(" (", "").contains(" ") && !model.contains(" ")) || modelIsActualSKU) {
                        model = systemProductNameAndSKU + " / " + model;
                    } else {
                        model += " / " + systemProductNameAndSKU;
                    }
                }

                if (!motherboardVersion.equals("N/A") && !motherboardModel.toLowerCase().equals(motherboardVersion.toLowerCase())) {
                    if (motherboardVersion.startsWith(brand) && !motherboardModel.startsWith(brand)) {
                        motherboardModel = motherboardVersion + " / " + motherboardModel;
                    } else {
                        motherboardModel += " / " + motherboardVersion;
                    }
                }

                // Use Motherboard info if Model is N/A or if Motherboard contains all that Model does and more.
                if (model.equals("N/A") || motherboardModel.toLowerCase().contains(model.toLowerCase())) {
                    possibleCustomDesktopOrBareMotherboard = (!hasScreen && model.equals("N/A"));
                    model = motherboardModel;
                }

                if (serial.equals("N/A")) {
                    if (!possibleCustomDesktopOrBareMotherboard) {
                        possibleCustomDesktopOrBareMotherboard = (!hasScreen && !motherboardSerial.equals("N/A"));
                    }

                    serial = motherboardSerial;

                    if (serial.equals("N/A") && !ethernetMACarray.isEmpty()) {
                        serialIsMAC = true;
                        serial = getEthernetMAC();
                    }
                }
            }

            if (motherboardBrand.equals("N/A")) {
                motherboardBrand = brand;
            }

            if (motherboardModel.equals("N/A")) {
                motherboardModel = model;
            }

            if (motherboardSerial.equals("N/A")) {
                motherboardSerial = serial;
            }

            if (model.startsWith(brand + " ")) {
                if (model.startsWith("Dell System ")) {
                    // Some Dell models start with "Dell System"
                    model = model.substring(11).trim();
                } else {
                    model = model.substring(brand.length()).trim();
                }

                if (model.startsWith("/")) {
                    // If the Version was just the Brand and systemProductNameAndSKU was added, we may end up with a model that starts with "/ "
                    model = model.substring(1).trim();
                }
            }

            if (motherboardModel.startsWith(motherboardBrand + " ")) {
                if (motherboardModel.startsWith("Dell System ")) {
                    // Some Dell models start with "Dell System"
                    motherboardModel = motherboardModel.substring(11).trim();
                } else {
                    motherboardModel = motherboardModel.substring(motherboardBrand.length()).trim();
                }

                if (motherboardModel.startsWith("/")) {
                    motherboardModel = motherboardModel.substring(1).trim();
                }
            }

            String[] cpuModelParts = cpu.split("@");

            double cpuModelSpeedGHz = 0.0;
            if (cpuModelParts.length == 2) {
                cpu = cpuModelParts[0].trim();

                String cpuModelSpeedPart = cpuModelParts[1].trim();

                if (!cpuModelSpeedPart.replaceAll("[^0-9]", "").isEmpty()) {
                    try {
                        if (cpuModelSpeedPart.contains("MHz")) {
                            cpuModelSpeedGHz = (Double.parseDouble(cpuModelSpeedPart.substring(0, cpuModelSpeedPart.indexOf("MHz")).trim()) / 1000);
                        } else if (cpuModelSpeedPart.contains("GHz")) {
                            cpuModelSpeedGHz = Double.parseDouble(cpuModelSpeedPart.substring(0, cpuModelSpeedPart.indexOf("GHz")).trim());
                        }
                    } catch (NumberFormatException cpuModelSpeedException) {
                        if (isTestMode) {
                            System.out.println("cpuModelSpeedException: " + cpuModelSpeedException);
                        }
                    }
                }
            }

            double cpuCurrentSpeedGHz = cpuModelSpeedGHz;
            if (!cpuCurrentSpeedString.replaceAll("[^0-9]", "").isEmpty()) {
                try {
                    if (cpuCurrentSpeedString.contains("MHz")) {
                        cpuCurrentSpeedGHz = (Double.parseDouble(cpuCurrentSpeedString.substring(0, cpuCurrentSpeedString.indexOf("MHz")).trim()) / 1000);
                    } else if (cpuCurrentSpeedString.contains("GHz")) {
                        cpuCurrentSpeedGHz = Double.parseDouble(cpuCurrentSpeedString.substring(0, cpuCurrentSpeedString.indexOf("GHz")).trim());
                    }
                } catch (NumberFormatException cpuCurrentSpeedToGHzException) {
                    if (isTestMode) {
                        System.out.println("cpuCurrentSpeedToGHzException: " + cpuCurrentSpeedToGHzException);
                    }
                }
            }

            double cpuMaxSpeedGHz = cpuCurrentSpeedGHz;
            if (!cpuMaxSpeedString.replaceAll("[^0-9]", "").isEmpty()) {
                try {
                    if (cpuMaxSpeedString.contains("MHz")) {
                        cpuMaxSpeedGHz = (Double.parseDouble(cpuMaxSpeedString.substring(0, cpuMaxSpeedString.indexOf("MHz")).trim()) / 1000);
                    } else if (cpuMaxSpeedString.contains("GHz")) {
                        cpuMaxSpeedGHz = Double.parseDouble(cpuMaxSpeedString.substring(0, cpuMaxSpeedString.indexOf("GHz")).trim());
                    }
                } catch (NumberFormatException cpuMaxSpeedToGHzException) {
                    if (isTestMode) {
                        System.out.println("cpuMaxSpeedToGHzException: " + cpuMaxSpeedToGHzException);
                    }
                }
            }

            if ((cpuModelSpeedGHz == 0.0) && (cpuCurrentSpeedGHz != 0.0)) {
                cpuModelSpeedGHz = cpuCurrentSpeedGHz;
            }

            if (cpuPandEcoresString.contains("@")) { // If "cpuPandEcoresString" contains Max speeds for P+E cores, DO NOT show an overall Max speed for the CPU.
                cpuMaxSpeedGHz = cpuModelSpeedGHz;
            }

            boolean cpuModelSpeedIsMaxSpeed = false;
            if ((cpuModelSpeedGHz == 0.0) && (cpuMaxSpeedGHz != 0.0)) {
                cpuModelSpeedGHz = cpuMaxSpeedGHz;
                cpuModelSpeedIsMaxSpeed = true;
            }

            if (cpuCurrentSpeedGHz == 0.0) {
                cpuCurrentSpeedGHz = cpuMaxSpeedGHz;
            }

            String cpuSpeedsString = "";

            if (cpuModelSpeedGHz > 0.0) {
                cpuSpeedsString = new DecimalFormat("#.#").format(cpuModelSpeedGHz) + " GHz";

                double cpuModelVsCurrentSpeedGHzDifference = Math.abs(cpuModelSpeedGHz - cpuCurrentSpeedGHz);
                double cpuCurrentVsMaxSpeedGHzDifference = Math.abs(cpuCurrentSpeedGHz - cpuMaxSpeedGHz);
                double cpuModelVsMaxSpeedGHzDifference = Math.abs(cpuModelSpeedGHz - cpuMaxSpeedGHz);
                double cpuSpeedDifferenceThreshold = 0.35; // Only show difference CPU speeds if they are at least this far from eachother.

                if (cpuModelVsMaxSpeedGHzDifference >= cpuSpeedDifferenceThreshold) {
                    if ((cpuModelVsCurrentSpeedGHzDifference >= cpuSpeedDifferenceThreshold) && (cpuCurrentVsMaxSpeedGHzDifference >= cpuSpeedDifferenceThreshold)) {
                        cpuSpeedsString += " / " + new DecimalFormat("#.#").format(cpuCurrentSpeedGHz) + " GHz / " + new DecimalFormat("#.#").format(cpuMaxSpeedGHz) + " GHz Max";
                    } else {
                        cpuSpeedsString += " / " + new DecimalFormat("#.#").format(cpuMaxSpeedGHz) + " GHz Max";
                    }
                } else if (cpuModelVsCurrentSpeedGHzDifference >= cpuSpeedDifferenceThreshold) {
                    cpuSpeedsString += " / " + new DecimalFormat("#.#").format(cpuCurrentSpeedGHz) + " GHz";
                } else if (cpuModelSpeedIsMaxSpeed) { // Intel 12th Gen and newer DO NOT list a model (base) speed, so we can only show the Max speed and want to label it as such.
                    cpuSpeedsString += " Max";
                }
            }

            if (!cpuSpeedsString.isEmpty() && (!cpuPandEcoresString.contains("@") || !cpuSpeedsString.startsWith("0."))) { // On Linux, the "cpuSpeedsString" may be incorrectly something like "0.8 GHz" for modern Intel processors with P and E cores and "cpuPandEcoresString" may contain the true Max speeds. If so, don't show the incorrect base speed.
                cpu += " @ " + cpuSpeedsString;
            }

            if (!cpuCount.isEmpty()) {
                try {
                    int cpuCountInteger = Integer.parseInt(cpuCount);
                    if (cpuCountInteger > 1) {
                        cpu = cpuCountInteger + "x " + cpu;
                    }
                } catch (NumberFormatException cpuCountException) {
                    if (isTestMode) {
                        System.out.println("cpuCountException: " + cpuCountException);
                    }
                }
            }

            if (!cpuThreads.isEmpty()) {
                try {
                    cpuThreadCount = Integer.parseInt(cpuThreads);
                } catch (NumberFormatException cpuThreadsException) {
                    if (isTestMode) {
                        System.out.println("cpuThreadsException: " + cpuThreadsException);
                    }
                }
            }

            if (!cpuCores.isEmpty()) {
                cpu += " (" + cpuCores + " Core" + (cpuCores.equals("1") ? "" : "s");

                if (!cpuPandEcoresString.isEmpty()) {
                    cpu += " - " + cpuPandEcoresString;
                } else if (cpuThreadCount > 0) {
                    try {
                        cpu += ((cpuThreadCount > Integer.parseInt(cpuCores)) ? " + HT" : "");
                    } catch (NumberFormatException cpuCoresException) {
                        if (isTestMode) {
                            System.out.println("cpuCoresException: " + cpuCoresException);
                        }
                    }
                }

                cpu += ")";
            }

            if (!cpuBrand.isEmpty() && !cpu.contains(cpuBrand)) {
                cpu = cpuBrand + " " + cpu;
            }

            if (macHasT2chip) {
                cpu += " + T2 Security Chip";
            }

            if (ramTotalBytesFromBanks > 0L) {
                String possibleRAM = new DecimalFormat("#").format((ramTotalBytesFromBanks / 1024 / 1024 / 1024)) + " GB";
                if (!ram.equals(possibleRAM)) {
                    ram = possibleRAM;
                }
            }

            boolean ramIsLowVoltage = ((ramType.equals(" DDR3") && ramVoltage.equals("1.35 V")) || (ramType.equals(" DDR4") && ramVoltage.equals("1.05 V")));
            String fullRAMtype = ramType + (ramIsLowVoltage ? "L" : "") + ramSpeed;
            ram += fullRAMtype;

            String[] ramSlotsParts = ramSlots.split(" \\+ ");

            if (!ramSlots.isEmpty() && ramSlots.contains("2 GB + 2 GB + 2 GB") && !ramSlots.contains("Empty")) {
                // If a PC has 3x or more 2 GB RAM chips, and ONLY 2 GB RAM chips, with and NO empty slots
                // it's pretty much a guarantee that the RAM is integrated (soldered on),
                // so display soldered PC RAM in a more clear and consolidated way.

                int onlyTwoGBramChipsCount = 0;

                for (String thisRamSlot : ramSlotsParts) {
                    if (thisRamSlot.equals("2 GB")) {
                        onlyTwoGBramChipsCount++;
                    } else {
                        onlyTwoGBramChipsCount = 0;
                        break;
                    }
                }

                if (onlyTwoGBramChipsCount > 0) {
                    ramSlots = onlyTwoGBramChipsCount + "x 2 GB Integrated";
                    maxRAM = " - NOT Upgradeable";
                }
            }

            int ramSlotCount = ramSlotsParts.length;
            motherboardRAM = (ramSlots.isEmpty() ? "" : ramSlotCount + " Slot" + ((ramSlotCount == 1) ? " " : "s ")) + fullRAMtype.trim() + maxRAM;

            String detailedRAM = ram + (ramSlots.isEmpty() ? "" : " (" + ramSlots + ")") + maxRAM;

            if (hasTouchscreen) {
                if (screenSize.equals("N/A")) {
                    screenSize = "Touchscreen";
                } else {
                    screenSize += " (Touchscreen)";
                }
            }

            if (!screenResolution.isEmpty()) {
                if (screenSize.equals("N/A")) {
                    screenSize = screenResolution;
                } else {
                    screenSize += ": " + screenResolution;
                }
            }

            screenSize = screenSize.replace(") (", " + ");

            if (false) { // DEBUG: Set to "true" to test proper display for all array values and errors.
                storageArray.add("DEBUG STORAGE 1");
                storageArray.add("DEBUG STORAGE 2");
                storageSerialsArray.add("DEBUG STORAGE SERIAL 1");
                storageSerialsArray.add("DEBUG STORAGE SERIAL 2");
                driveHealth = "DEBUG STORAGE HEALTH";
                driveHealthWarning = true;
                driveTrimWarning = true;
                driveRecalled = true;

                batteryHealthArray.add("DEBUG BATTERY 1");
                batteryHealthArray.add("DEBUG BATTERY 2");
                batteryHealthWarningArray.add("DEBUG BATTERY WARNING 1");
                batteryHealthWarningArray.add("DEBUG BATTERY WARNING 2");
                batteryHealthWarningArray.add("Possibly Recalled");
                batteryHealthErrorArray.add("DEBUG BATTERY ERROR 1");
                batteryHealthErrorArray.add("DEBUG BATTERY ERROR 2");
                batteryHealthErrorArray.add("Condition Debug");

                gpuArray.add("DEBUG GPU 1");
                gpuArray.add("DEBUG GPU 2");
                gpuErrorArray.add("DEBUG GPU ERROR 1");
                gpuErrorArray.add("DEBUG GPU ERROR 1");

                audioArray.add("DEBUG AUDIO 1");
                audioArray.add("DEBUG AUDIO 2");
                audioErrorArray.add("DEBUG AUDIO ERROR 1");
                audioErrorArray.add("DEBUG AUDIO ERROR 1");

                wirelessArray.add("DEBUG WIRELESS 1");
                wirelessArray.add("DEBUG WIRELESS 2");
                wirelessErrorArray.add("DEBUG WIRELESS ERROR 1");
                wirelessErrorArray.add("DEBUG WIRELESS ERROR 1");

                screenErrorArray.add("DEBUG SCREEN ERROR 1");
                screenErrorArray.add("DEBUG SCREEN ERROR 1");

                discDriveArray.add("DEBUG DISC DRIVE 1");
                discDriveArray.add("DEBUG DISC DRIVE 2");
                discDriveCanBurnCDs = false;
                discDriveCanBurnDVDs = false;
                hasDiscInDiscDrive = true;

                keyboardRecalled = true;
            } else if (false) { // DEBUG: Set to "true" to test all "N/A" values.
                os = "N/A";
                fullOS = "N/A";

                model = "N/A";
                macShortModelName = "N/A";
                macModelIdentifier = "";
                fullModel = "N/A";
                motherboardModel = "N/A";
                fullMotherboardModel = "N/A";
                chassisType = "N/A";
                isLaptop = false;
                hasScreen = false;
                macScreenFeaturesArray.clear();
                hasTouchscreen = false;
                hasCamera = false;
                possibleCustomDesktopOrBareMotherboard = false;
                brand = "N/A";
                fullBrand = "N/A";
                motherboardBrand = "N/A";
                fullMotherboardBrand = "N/A";
                serial = "N/A";
                fullSerial = "N/A";
                motherboardSerial = "N/A";
                fullMotherboardSerial = "N/A";
                cpu = "N/A";
                fullCPU = "N/A";
                cpuThreadCount = 0;
                ram = "N/A";
                detailedRAM = "N/A";
                fullRAM = "N/A";
                motherboardRAM = "N/A";
                fullMotherboardRAM = "N/A";
                storage = "N/A";
                storageArray.clear();
                storageSerial = "";
                storageSerialsArray.clear();
                disksFormattedWithoutPartitionTable.clear();
                driveHealth = "N/A";
                driveHealthWarning = false;
                driveTrimWarning = false;
                driveRecalled = false;
                batteryHealthArray.clear();
                powerAdapter = "";
                batteryHealthWarningArray.clear();
                batteryHealthErrorArray.clear();
                gpu = "N/A";
                gpuArray.clear();
                gpuIDsArray.clear();
                gpuErrorArray.clear();
                audio = "N/A";
                audioArray.clear();
                audioIDsArray.clear();
                audioErrorArray.clear();
                wirelessArray.clear();
                wirelessErrorArray.clear();
                screenSize = "N/A";
                fullScreenSize = "N/A";
                screenResolution = "";
                screenErrorArray.clear();
                discDrive = "N/A";
                discDriveArray.clear();
                discDriveLogicalNames.clear();
                discDriveCanBurnCDs = false;
                discDriveCanBurnDVDs = false;
                hasDiscInDiscDrive = false;
                keyboardRecalled = false;
                serialIsMAC = false;

                ethernetMACarray.clear();
            }

            fullOS = os;
            fullBrand = brand;
            fullMotherboardBrand = motherboardBrand;
            fullModel = model;
            fullMotherboardModel = motherboardModel;
            fullSerial = serial;
            fullMotherboardSerial = motherboardSerial;
            fullCPU = cpu;
            fullRAM = detailedRAM;
            fullMotherboardRAM = motherboardRAM;
            storage = getFullStorage();
            storageSerial = getFullStorageSerial();
            gpu = getFullGPU();
            audio = getFullAudio();
            fullScreenSize = screenSize;
            discDrive = getFullDiscDrive();
            Collections.sort(discDriveLogicalNames);

            // Trim fields to PCsCRM limits (which are all 150 characters)
            // Prior to May 2024, max field lengths varied and some had much shorter limits, which is why some truncations are now needlessly meticulous, but leaving them in place doesn't hurt.
            int maxFieldLength = 150;

            if (os.length() > maxFieldLength) {
                os = os.substring(0, (maxFieldLength - 1)) + "…";
            }

            if (brand.length() > maxFieldLength) {
                brand = brand.substring(0, (maxFieldLength - 1)) + "…";
            }

            if (motherboardBrand.length() > maxFieldLength) {
                motherboardBrand = motherboardBrand.substring(0, (maxFieldLength - 1)) + "…";
            }

            if (model.length() > maxFieldLength) {
                if (brand.equals("Apple")) {
                    model = model.replace("Thunderbolt 3", "TB3");

                    if (model.length() > maxFieldLength) {
                        model = model.replace("-inch", "\"");

                        if (model.length() > maxFieldLength) {
                            model = model.replace(" 20", " '");

                            if (model.length() > maxFieldLength) {
                                model = model.substring(0, (maxFieldLength - 1)) + "…";
                            }
                        }
                    }
                } else {
                    model = model.substring(0, (maxFieldLength - 1)) + "…";
                }
            }

            if (motherboardModel.length() > maxFieldLength) {
                motherboardModel = motherboardModel.substring(0, (maxFieldLength - 1)) + "…";
            }

            if (serial.length() > maxFieldLength) {
                serial = serial.replaceAll("[^A-Za-z0-9]", "");

                if (serial.length() > maxFieldLength) {
                    serial = serial.substring(0, maxFieldLength); // Don't do an ellipsis to get the most serial info
                }
            }

            if (motherboardSerial.length() > maxFieldLength) {
                motherboardSerial = motherboardSerial.replaceAll("[^A-Za-z0-9]", "");

                if (motherboardSerial.length() > maxFieldLength) {
                    motherboardSerial = motherboardSerial.substring(0, maxFieldLength); // Don't do an ellipsis to get the most serial info
                }
            }

            if (cpu.length() > maxFieldLength) {
                cpu = cpu.replace("T2 Security Chip", "T2 Chip");

                if (cpu.length() > maxFieldLength) {
                    cpu = cpu.replace("T2 Chip", "T2");

                    if (cpu.length() > maxFieldLength) {
                        cpu = cpu.replace("GHz Max", "Max");

                        if (cpu.length() > maxFieldLength) {
                            cpu = cpu.replace(" GHz", "");

                            if (cpu.length() > maxFieldLength) {
                                cpu = cpu.replace(" Max", "");

                                if (cpu.length() > maxFieldLength) {
                                    cpu = cpu.substring(0, (maxFieldLength - 1)) + "…";
                                }
                            }
                        }
                    }
                }
            }

            if (detailedRAM.length() <= maxFieldLength) {
                ram = detailedRAM;
            } else {
                detailedRAM = ram + (ramSlots.isEmpty() ? "" : " (" + ramSlots + ")");

                if (detailedRAM.length() <= maxFieldLength) {
                    ram = detailedRAM;
                } else {
                    detailedRAM = detailedRAM.replace(" GB", "GB");

                    if (detailedRAM.length() <= maxFieldLength) {
                        ram = detailedRAM;
                    } else {
                        detailedRAM = detailedRAM.replace(" + ", "+");

                        if (detailedRAM.length() <= maxFieldLength) {
                            ram = detailedRAM;
                        }
                    }
                }
            }

            if (motherboardRAM.length() > maxFieldLength) {
                if (motherboardRAM.contains(" - ")) {
                    motherboardRAM = motherboardRAM.substring(0, motherboardRAM.lastIndexOf(" - "));
                }

                if (motherboardRAM.length() > maxFieldLength) {
                    motherboardRAM = motherboardRAM.substring(0, (maxFieldLength - 1)) + "…";
                }
            }

            if (storage.length() > maxFieldLength) {
                storage = storage.replaceAll("\\(.*?\\)", "").trim().replaceAll("\\s{2,}", " "); // Remove all model names which will always be in parens and will never contain parens within them since they were manually removed and there are never parens anywhere else in the line.

                if (storage.length() > maxFieldLength) {
                    storage = storage.replace(" GB", "GB").replace(" TB", "TB").replace(" RAID", "").replace(" + ", "+");

                    if (storage.length() > maxFieldLength) {
                        storage = storage.replace(" HDD", "").replace(" SSD", "").replace(" NVMe", "").replace("GB", " GB").replace("TB", " TB").replace("+", " + ");

                        if (storage.length() > maxFieldLength) {
                            storage = storage.replace(" GB", "GB").replace(" TB", "TB").replace(" + ", "+");

                            if (storage.length() > maxFieldLength) {
                                storage = storage.substring(0, (maxFieldLength - 1)) + "…";
                            }
                        }
                    }
                }
            }

            if (storageSerial.length() > 75) { // This field has a 75 character limit, unlike the other fields.
                storageSerial = storageSerial.replaceAll("[^A-Za-z0-9]", "");

                if (storageSerial.length() > 75) {
                    storageSerial = storageSerial.substring(0, 75); // Don't do an ellipsis to get the most storage serial info
                }
            }

            if (gpu.length() > maxFieldLength) {
                gpu = gpu.substring(0, (maxFieldLength - 1)) + "…";
            }

            if (audio.length() > maxFieldLength) {
                audio = audio.substring(0, (maxFieldLength - 1)) + "…";
            }

            if (screenSize.length() > maxFieldLength) {
                screenSize = screenSize.replace("-inch", "\"");

                if (screenSize.length() > maxFieldLength) {
                    screenSize = screenSize.replace("Touchscreen", "TS"); // Hopefully it'll be pretty obvious to folks that "(TS)" means "Touchscreen"

                    if (screenSize.length() > maxFieldLength) {
                        screenSize = screenSize.replace("ProMotion", "PM");

                        if (screenSize.length() > maxFieldLength) {
                            screenSize = screenSize.replace("Liquid Retina", "LRD");

                            if (screenSize.length() > maxFieldLength) {
                                screenSize = screenSize.replace("Retina", "RD"); // Use "RD" as the abbreviation for "Retina Display" since it's less ambiguous that just "R"

                                if (screenSize.length() > maxFieldLength) {
                                    screenSize = screenSize.replace(" XDR", "-XDR"); // Don't want to end up with a combined "LRDXDR" so make it "LRD-XRD" instead when spaces get removed.
                                    screenSize = screenSize.replace(" ", "");

                                    if (screenSize.length() > maxFieldLength) {
                                        screenSize = screenSize.substring(0, (maxFieldLength - 1)) + "…";
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (discDrive.length() > maxFieldLength) {
                discDrive = discDrive.substring(0, (maxFieldLength - 1)) + "…";
            }

        } catch (Exception loadSpecsException) { // We want this to catch any an all kinds of exceptions
            System.out.println("loadSpecsException: " + loadSpecsException);
            loadSpecsException.printStackTrace();

            loadSpecsExceptionString = loadSpecsException.toString();
        }

        if (isTestMode) {
            printSpecs();
        }
    }

    public String getCleanNameForWindowsDeviceID(String deviceID) {
        return getCleanNameForWindowsDeviceID(deviceID, "");
    }

    public String getCleanNameForWindowsDeviceID(String deviceID, String fallbackName) {
        String searchDB = null;
        String vendorCode = null;
        String deviceCode = null;

        // https://docs.microsoft.com/en-us/windows-hardware/drivers/install/identifiers-for-pci-devices
        // https://docs.microsoft.com/en-us/windows-hardware/drivers/install/standard-usb-identifiers
        int venIndex;
        int devIndex;
        int vidIndex;
        int pidIndex;

        if ((venIndex = deviceID.indexOf("VEN_")) > -1 && (devIndex = deviceID.indexOf("DEV_")) > -1) {
            // Assume PCI Device (Even though could be HDAUDIO or ACPI or something, but we might get a vendor at least)
            searchDB = "pci.ids";
            vendorCode = deviceID.substring(venIndex + 4, venIndex + 8).toLowerCase();
            deviceCode = deviceID.substring(devIndex + 4, devIndex + 8).toLowerCase();
        } else if ((vidIndex = deviceID.indexOf("VID_")) > -1 && (pidIndex = deviceID.indexOf("PID_")) > -1) {
            // Assume USB Device
            searchDB = "usb.ids";
            vendorCode = deviceID.substring(vidIndex + 4, vidIndex + 8).toLowerCase();
            deviceCode = deviceID.substring(pidIndex + 4, pidIndex + 8).toLowerCase();
        }

        if (searchDB != null && vendorCode != null && deviceCode != null) {
            String foundVendor = null;
            String foundDevice = null;
            String foundSubDevice = null;

            String subVendorCode = null;
            String subDeviceCode = null;

            try (BufferedReader databaseBufferedReader = new BufferedReader(new InputStreamReader(this.getClass().getResource("/Resources/" + searchDB).openStream()))) {
                String thisDatabaseLine;
                while ((thisDatabaseLine = databaseBufferedReader.readLine()) != null) {
                    if (!thisDatabaseLine.isEmpty() && !thisDatabaseLine.startsWith("#")) {
                        if (!thisDatabaseLine.startsWith("\t")) {
                            if (foundVendor != null) {
                                break;
                            } else if (thisDatabaseLine.startsWith(vendorCode + "  ")) {
                                foundVendor = thisDatabaseLine.substring(vendorCode.length() + 2);
                            }
                        } else if (foundVendor != null && thisDatabaseLine.startsWith("\t")) {
                            if (foundDevice == null) {
                                if (thisDatabaseLine.startsWith("\t" + deviceCode + "  ")) {
                                    foundDevice = thisDatabaseLine.substring(deviceCode.length() + 3);

                                    int subsysIndex;
                                    if (searchDB.equals("pci.ids") && (subsysIndex = deviceID.indexOf("SUBSYS_")) > -1) {
                                        // Only keep searching for subdevices in PCI IDs since USB IDs have no subdevice
                                        subDeviceCode = deviceID.substring(subsysIndex + 7, subsysIndex + 11).toLowerCase();
                                        subVendorCode = deviceID.substring(subsysIndex + 11, subsysIndex + 15).toLowerCase();
                                    } else {
                                        break;
                                    }
                                }
                            } else if (subVendorCode != null && subDeviceCode != null) {
                                if (thisDatabaseLine.startsWith("\t\t")) {
                                    if (thisDatabaseLine.startsWith("\t\t" + subVendorCode + " " + subDeviceCode + "  ")) {
                                        foundSubDevice = thisDatabaseLine.substring(subVendorCode.length() + subDeviceCode.length() + 5);
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception getCleanNameForWindowsDeviceIDException) {
                System.out.println("getCleanNameForWindowsDeviceIDException: " + getCleanNameForWindowsDeviceIDException);
            }

            if (foundVendor != null) {
                String cleanVendor = cleanDeviceBrand(foundVendor);

                if (foundDevice != null) {
                    String cleanDevice = cleanDeviceModel(foundDevice);

                    if (foundSubDevice != null) {
                        cleanDevice += " [" + cleanDeviceModel(foundSubDevice) + "]";
                    }

                    if (cleanDevice.contains(cleanVendor)) {
                        cleanVendor = "";
                    } else {
                        cleanVendor += " ";
                    }

                    return cleanVendor + cleanDevice;
                }

                String nameWithDeviceCode = cleanVendor + " Device " + deviceCode.toUpperCase();
                return (fallbackName.isEmpty() ? nameWithDeviceCode : (fallbackName + " [" + nameWithDeviceCode + "]"));
            }

            String nameWithVendorAndDeviceCode = "Vendor " + vendorCode.toUpperCase() + " Device " + deviceCode.toUpperCase();
            return (fallbackName.isEmpty() ? nameWithVendorAndDeviceCode : (fallbackName + " [" + nameWithVendorAndDeviceCode + "]"));
        }

        return (fallbackName.isEmpty() ? "N/A" : fallbackName);
    }

    //GETTERS
    public String getWindowsLicenseInfo(boolean isTestMode, boolean isLoggedIn) {
        return getWindowsLicenseInfo(isTestMode, isLoggedIn, true);
    }

    public String getWindowsLicenseInfo(boolean isTestMode, boolean isLoggedIn, boolean obscureDPKs) {
        // This code started out as a simple Windows license info output for display, but then slowly evolved into being part of a much more
        // complex process to also license Windows and really should be broken out into it's own class with separate properties so that
        // this code and the code that calls it (which parses the returned string) could be greatly cleaned up and simplified.

        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            boolean isWindows11 = osName.startsWith("Windows 11");
            boolean isWindowsHomeEdition = (os.contains(" Home"));

            String[] slmgrDlvInfo = new CommandReader(new String[]{"\\Windows\\System32\\cscript.exe", "/nologo", "\\Windows\\System32\\slmgr.vbs", "/dlv"}).getOutputLines();

            String productKeyChannel = "<i>UNKNOWN</i>";
            String partialProductKey = "";
            String licenseStatus = "<i>UNKNOWN</i>";

            for (String thisSlmgrDlvInfoLine : slmgrDlvInfo) {
                if (thisSlmgrDlvInfoLine.startsWith("Product Key Channel: ")) {
                    productKeyChannel = thisSlmgrDlvInfoLine.substring(thisSlmgrDlvInfoLine.indexOf(": ") + 2);
                } else if (thisSlmgrDlvInfoLine.startsWith("Partial Product Key: ")) {
                    partialProductKey = thisSlmgrDlvInfoLine.substring(thisSlmgrDlvInfoLine.indexOf(": ") + 2);
                } else if (thisSlmgrDlvInfoLine.startsWith("License Status: ")) {
                    licenseStatus = thisSlmgrDlvInfoLine.substring(thisSlmgrDlvInfoLine.indexOf(": ") + 2);
                }
            }

            String actualProductKey = (partialProductKey.isEmpty() ? ":</b> <i>UNKNOWN</i>" : (" in Registry (Partial):</b> " + partialProductKey)) + "<br/><i>ERROR RETRIEVING FULL PRODUCT KEY</i>"; // This should never be used.

            boolean isLicensed = (licenseStatus.equals("Licensed"));
            boolean possibleNonRefurbProductKey = true; // NOTE: NO LONGER CONSIDERING ANY PHYSICAL COAs FROM THE "OEM:NONSLP" CHANNEL TO BE VALID REFURB KEYS SINCE WE ARE NOW USING 100% DPKs. OLD CODE: (!productKeyChannel.equals("OEM:NONSLP")); // If Product Key didn't come from Registry as "OEM:NONSLP", it CAN'T be a Refurb Product Key from a physical COA.
            // Refurb DPKs will have the productKeyChannel of "OEM:DM" which is checked below by confirming the DPK in the Registry is the DPK in the "oa3tool-assemble.xml" file. 
            boolean isCitizenshipDPK = false;

            if (!isLicensed && productKeyChannel.equals("OEM:DM") && new File("\\Install\\DPK\\Logs\\oa3tool-assemble.xml").exists()) {
                // NOTE: When applying a Refurb DPK, we only run "slmgr /ipk" to install the DPK and DO NOT run "slmgr /ato" to Activate Windows as doing so would be a CBR compliance violation.
                // So, if "License Status" is NOT "Licensed" (such as "Notification"), but it is from a Refurb DPK that we have installed and not activated, consider is licensed.
                // The Refurb DPK will still be verified below and "possibleNonRefurbProductKey" will be set accordingly.
                licenseStatus = "Licensed <i>(Refurb DPK Applied)</i>";
                isLicensed = true;
            }

            String[] productKeyInfoFromBIOS = new CommandReader(new String[]{"\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", "Get-CimInstance SoftwareLicensingService -Property OA3xOriginalProductKeyDescription,OA3xOriginalProductKey | Format-List OA3xOriginalProductKeyDescription,OA3xOriginalProductKey"}).getOutputLines();

            String originalProductKeyFromBIOS = "";

            for (String thisProductKeyInfoFromBIOSline : productKeyInfoFromBIOS) {
                if (thisProductKeyInfoFromBIOSline.startsWith("OA3xOriginalProductKeyDescription")) {
                    if (productKeyChannel.contains("UNKNOWN")) {
                        String[] thisProductKeyFromBIOSdescriptionParts = thisProductKeyInfoFromBIOSline.substring(thisProductKeyInfoFromBIOSline.indexOf(": ") + 2).split(" ");
                        String thisProductKeyFromBIOSchannel = thisProductKeyFromBIOSdescriptionParts[thisProductKeyFromBIOSdescriptionParts.length - 1];
                        if (!thisProductKeyFromBIOSchannel.isEmpty()) {
                            productKeyChannel = thisProductKeyFromBIOSchannel;
                        }
                    }
                } else if (thisProductKeyInfoFromBIOSline.startsWith("OA3xOriginalProductKey")) {
                    originalProductKeyFromBIOS = thisProductKeyInfoFromBIOSline.substring(thisProductKeyInfoFromBIOSline.indexOf(": ") + 2);

                    if (!originalProductKeyFromBIOS.isEmpty()) {
                        if (!isLoggedIn || obscureDPKs) {
                            originalProductKeyFromBIOS = "XXXXX-XXXXX-XXXXX-XXXXX-" + originalProductKeyFromBIOS.substring(originalProductKeyFromBIOS.length() - 5); // Do not display whole Product Keys so that they are not easy to incorrectly re-use or be unnecessarily seen by volunteers.
                        }

                        if (!isLicensed) {
                            licenseStatus = "Licensed <i>(Digital Product Key in BIOS)</i>";
                            isLicensed = true;
                        }
                    }
                }
            }

            String currentProductNameFromRegistry = "";
            String currentProductIdFromRegistry = "";
            String coaIDfromProductID = "";
            String productKeyID = ""; // This is for the name and formatting of this ID for Refurb DPKs.

            if (isLicensed) {
                if (productKeyFromRegistryVbsTempPath == null) {
                    try (InputStream productKeyFromRegistryVbsInputStream = this.getClass().getClassLoader().getResourceAsStream("Resources/product-key-from-registry.vbs")) {
                        File productKeyFromRegistryVbsTempFile = File.createTempFile("qa_helper-product_key_from_registry", ".vbs");
                        productKeyFromRegistryVbsTempFile.deleteOnExit();

                        try (FileOutputStream productKeyFromRegistryOutputStream = new FileOutputStream(productKeyFromRegistryVbsTempFile)) {
                            byte[] buf = new byte[1024];
                            int bytesRead;

                            while ((bytesRead = productKeyFromRegistryVbsInputStream.read(buf)) > 0) {
                                productKeyFromRegistryOutputStream.write(buf, 0, bytesRead);
                            }

                            productKeyFromRegistryVbsTempPath = productKeyFromRegistryVbsTempFile.getPath();
                        } catch (Exception writeProductKeyFromRegistryException) {
                            if (isTestMode) {
                                System.out.println("writeProductKeyFromRegistryException: " + writeProductKeyFromRegistryException);
                            }
                        }
                    } catch (Exception readProductKeyFromRegistryVbsException) {
                        if (isTestMode) {
                            System.out.println("readProductKeyFromRegistryVbsException: " + readProductKeyFromRegistryVbsException);
                        }
                    }
                }

                String currentProductKeyFromRegistry = "";

                if (productKeyFromRegistryVbsTempPath != null) {
                    String[] productKeyFromRegistryVbsOutputLines = new CommandReader(new String[]{"\\Windows\\System32\\cscript.exe", "/nologo", productKeyFromRegistryVbsTempPath}).getOutputLines();

                    for (String thisProductKeyFromRegistryVbsOutputLine : productKeyFromRegistryVbsOutputLines) {
                        if (thisProductKeyFromRegistryVbsOutputLine.startsWith("Product Name: ")) {
                            currentProductNameFromRegistry = thisProductKeyFromRegistryVbsOutputLine.substring(thisProductKeyFromRegistryVbsOutputLine.indexOf(": ") + 2);
                            if (isWindows11 && currentProductNameFromRegistry.contains("Windows 10")) {
                                currentProductNameFromRegistry = currentProductNameFromRegistry.replace("Windows 10", "Windows 11"); // Product Key info from Registry will still say Windows 10 even when using a DPK for Windows 11, so manually change the display so the technicians don't get confused and thing the wrong DPK was used.
                            }
                        } else if (thisProductKeyFromRegistryVbsOutputLine.startsWith("Product ID: ")) {
                            currentProductIdFromRegistry = thisProductKeyFromRegistryVbsOutputLine.substring(thisProductKeyFromRegistryVbsOutputLine.indexOf(": ") + 2);
                        } else if (thisProductKeyFromRegistryVbsOutputLine.startsWith("Product Key: ")) {
                            currentProductKeyFromRegistry = thisProductKeyFromRegistryVbsOutputLine.substring(thisProductKeyFromRegistryVbsOutputLine.indexOf(": ") + 2);
                        }
                    }
                }

                List<String> windowsGenericProductKeys = Arrays.asList( // Generic Product Keys used with Win 10 upgrades and retail/oem activations (digitally licensed computers): https://www.tenforums.com/tutorials/95922-generic-product-keys-install-windows-10-editions.html
                        "YTMG3-N6DKC-DKB77-7M9GH-8HVX7", "37GNV-YCQVD-38XP9-T848R-FC2HD", "46J3N-RY6B3-BJFDY-VBFT9-V22HG", // Windows 10 Home
                        "VK7JG-NPHTM-C97JM-9MPGT-3V66T", "NF6HC-QH89W-F8WYV-WWXV4-WFG6P", "RHGJR-N7FVY-Q3B8F-KBQ6V-46YP4" // Windows 10 Pro
                );

                ArrayList<String> windowsGenericPartialProductKeys = new ArrayList<>();
                windowsGenericProductKeys.forEach(thisGenericProductKey -> {
                    windowsGenericPartialProductKeys.add(thisGenericProductKey.substring(thisGenericProductKey.lastIndexOf("-") + 1));
                });

                if (windowsGenericProductKeys.contains(currentProductKeyFromRegistry)) {
                    if (windowsGenericPartialProductKeys.contains(partialProductKey) || (!originalProductKeyFromBIOS.isEmpty() && originalProductKeyFromBIOS.endsWith(partialProductKey))) {
                        // I think this will always be true (and the OR statment is probably not necessary), but want to be sure to show a non-generic or non-BIOS partial Product Key if it exists.
                        String genericProductKeyNote = "<i>GENERIC PRODUCT KEY IN REGISTRY (Digital License)</i>";

                        if (!originalProductKeyFromBIOS.isEmpty()) {
                            // When activated from a digial license the actual Product Key SHOULD be retrievable from BIOS (but may not be if it originally shipped with an older version of Windows? Not sure of all the different ways Windows can get a digital license.)
                            actualProductKey = " in BIOS:</b> " + originalProductKeyFromBIOS + "<br/>" + genericProductKeyNote;
                        } else {
                            actualProductKey = ":</b> " + genericProductKeyNote;
                        }
                    } else {
                        actualProductKey = (partialProductKey.isEmpty() ? ":</b> <i>UNKNOWN - </i>" : (" in Registry (Partial):</b> " + partialProductKey + "<br/>")) + "<i>GENERIC PRODUCT KEY IN REGISTRY</i>";
                    }

                    currentProductIdFromRegistry = "";
                } else if (currentProductKeyFromRegistry.isEmpty() || currentProductKeyFromRegistry.equals("BBBBB-BBBBB-BBBBB-BBBBB-BBBBB")) {
                    String noRegistryProductKeyNote = "NO FULL PRODUCT KEY IN REGISTRY";
                    if (currentProductKeyFromRegistry.equals("BBBBB-BBBBB-BBBBB-BBBBB-BBBBB")) {
                        // This is what is in the Registry if the Product Key has been cleared with "slmgr /cpky". The Partial Product Key from "slmgr /dlv" won't get changed through.
                        noRegistryProductKeyNote = "PRODUCT KEY WAS CLEARED FROM REGISTRY";
                    }

                    if (windowsGenericPartialProductKeys.contains(partialProductKey)) {
                        String genericPartialProductKeyNote = "GENERIC PRODUCT KEY MAY BE IN REGISTRY (Digital License)";

                        if (!originalProductKeyFromBIOS.isEmpty()) {
                            actualProductKey = " in BIOS:<br/> " + originalProductKeyFromBIOS + "<br/><i>" + noRegistryProductKeyNote + " - " + genericPartialProductKeyNote + "</i>";
                        } else {
                            actualProductKey = ":</b> <i>" + noRegistryProductKeyNote + "<br/>" + genericPartialProductKeyNote + "</i>";
                        }

                        currentProductIdFromRegistry = "";
                    } else if (!originalProductKeyFromBIOS.isEmpty() && originalProductKeyFromBIOS.endsWith(partialProductKey)) {
                        actualProductKey = " in BIOS:</b> " + originalProductKeyFromBIOS + "<br/><i>" + noRegistryProductKeyNote + "</i>";
                    } else {
                        actualProductKey = (partialProductKey.isEmpty() ? ":</b> <i>UNKNOWN - </i>" : (" in Registry (Partial):</b> " + partialProductKey + "<br/>")) + "<i>" + noRegistryProductKeyNote + "</i>";
                    }
                } else {
                    // Obscure all Product Keys when not logged in and only display whole Product Keys for physical COAs when logged in since they would be visible anyways (which could have been entered to license manually from an OEM COA sticker before being able to use a DPK).
                    // This way DPKs are never easy to incorrectly re-use or are unnecessarily seen by volunteers.
                    actualProductKey = " in Registry:</b> " + ((!isLoggedIn || (!productKeyChannel.equals("OEM:NONSLP") && obscureDPKs)) ? "XXXXX-XXXXX-XXXXX-XXXXX-" + currentProductKeyFromRegistry.substring(currentProductKeyFromRegistry.length() - 5) : currentProductKeyFromRegistry);
                }

                if (!currentProductIdFromRegistry.isEmpty() && currentProductIdFromRegistry.endsWith("AAOEM")) {
                    coaIDfromProductID = currentProductIdFromRegistry.replaceAll("[^0-9]", "");
                    if (coaIDfromProductID.startsWith("0")) {
                        coaIDfromProductID = coaIDfromProductID.substring(1);
                    }

                    if (coaIDfromProductID.length() == 14) {
                        coaIDfromProductID = coaIDfromProductID.substring(0, 5) + "-" + coaIDfromProductID.substring(5);
                        coaIDfromProductID = coaIDfromProductID.substring(0, 9) + "-" + coaIDfromProductID.substring(9);
                        coaIDfromProductID = coaIDfromProductID.substring(0, 13) + "-" + coaIDfromProductID.substring(13);
                    } else {
                        coaIDfromProductID = "";
                    }
                }

                if (!coaIDfromProductID.isEmpty()) {
                    try {
                        productKeyID = String.valueOf(Long.parseLong(coaIDfromProductID.replaceAll("[^0-9]", ""))); // Strip dashes out and convert to long to remove leading zeros.
                    } catch (NumberFormatException getProductKeyIDexception) {
                        if (isTestMode) {
                            System.out.println("getProductKeyIDexception: " + getProductKeyIDexception);
                        }
                    }
                }

                if (productKeyChannel.equals("OEM:DM") && new File("\\Install\\DPK\\Logs\\oa3tool-assemble.xml").exists()) {
                    // "OEM:DM" could mean that the computer has an Embedded Digital Product Key OR that we have issued it Refurbished Digital Product Key using oa3tool.
                    // Therefore, only allow "OEM:DM" Product Keys if the "oa3tool-assemble.xml" file exists (which contains the Refurb DPK applied by us) AND the currentProductKeyFromRegistry and productKeyID matches the info in there AND it contains a valid Licensable Part Number for the correct version and edition.

                    try {
                        String oa3toolAssembleContents = String.join("", Files.readAllLines(Paths.get("\\Install\\DPK\\Logs\\oa3tool-assemble.xml"))).replace(" ", ""); // Remove all spaces because "ProductKeyID" value could have trailing spaces.

                        ArrayList<String> validRefurbDPKLPNs = new ArrayList<>();
                        if (isWindows11) {
                            if (isWindowsHomeEdition) {
                                validRefurbDPKLPNs.add("WV2-00048");
                            } else {
                                validRefurbDPKLPNs.add("QLF-00626"); // Citizenship
                                validRefurbDPKLPNs.add("QLF-00624"); // Commercial
                            }
                        } else {
                            if (isWindowsHomeEdition) {
                                validRefurbDPKLPNs.add("WV2-00047");
                            } else {
                                validRefurbDPKLPNs.add("QLF-00623"); // Citizenship
                                validRefurbDPKLPNs.add("QLF-00621"); // Commercial
                            }
                        }

                        String licensablePartNumber = "";
                        int productKeyPartNumberOpenTagIndex = oa3toolAssembleContents.indexOf("<ProductKeyPartNumber>");
                        int productKeyPartNumberCloseTagIndex = oa3toolAssembleContents.indexOf("</ProductKeyPartNumber>");
                        if ((productKeyPartNumberOpenTagIndex > 0) && (productKeyPartNumberCloseTagIndex > 0) && ((productKeyPartNumberCloseTagIndex - productKeyPartNumberOpenTagIndex) == 31)) {
                            licensablePartNumber = oa3toolAssembleContents.substring(productKeyPartNumberOpenTagIndex + 22, productKeyPartNumberCloseTagIndex);
                        }

                        if (oa3toolAssembleContents.contains("<ProductKey>" + currentProductKeyFromRegistry + "</ProductKey>") && oa3toolAssembleContents.contains("<ProductKeyID>" + productKeyID + "</ProductKeyID>") && validRefurbDPKLPNs.contains(licensablePartNumber)) {
                            isCitizenshipDPK = (!isWindowsHomeEdition && licensablePartNumber.equals(validRefurbDPKLPNs.get(0)));
                            possibleNonRefurbProductKey = false;
                        }
                    } catch (IOException readOa3toolAssembleexception) {
                        if (isTestMode) {
                            System.out.println("readOa3toolAssembleexception: " + readOa3toolAssembleexception);
                        }
                    }
                }
            }

            return ((isLicensed && possibleNonRefurbProductKey)
                    ? "<b style='color: REPLACE_FOR_ERROR_TITLE'>WINDOWS APPEARS TO BE LICENSED WITH A NON-REFURBISHED PC PRODUCT KEY</b>"
                    : "<b style='color: " + (isLicensed ? "REPLACE_FOR_SUCESS_TITLE" : "REPLACE_FOR_WARNING_TITLE") + "'>" + (isLicensed ? "Properly Licensed With a Windows " + (isWindows11 ? "11" : "10") + " " + (isWindowsHomeEdition ? "Home" : "Pro") + " " + (isCitizenshipDPK ? "<i>Citizenship</i>" : "<u>Commercial</u>") + " Digital Product Key for Refurbished PC" : "Windows IS NOT Currently Licensed") + "</b>")
                    + (isLicensed
                            ? "<br/><br/><br/>"
                            + "<b>License Status:</b> " + licenseStatus
                            + "<br/><br/>"
                            + ((!currentProductNameFromRegistry.isEmpty())
                            ? "<b>Product Name:</b> " + currentProductNameFromRegistry
                            + "<br/><br/>"
                            : "")
                            + "<b>Product Key Channel:</b> " + productKeyChannel
                            + "<br/><br/>"
                            + ((!currentProductIdFromRegistry.isEmpty())
                            ? "<b>Product ID:</b> " + currentProductIdFromRegistry
                            + "<br/><br/>"
                            : "")
                            + ((!coaIDfromProductID.isEmpty())
                            ? "<b>COA ID / COA Serial Number:</b> " + coaIDfromProductID
                            + "<br/><br/>"
                            : "")
                            + ((!productKeyID.isEmpty())
                            ? "<b>Product Key ID:</b> " + productKeyID
                            + "<br/><br/>"
                            : "")
                            + "<b>Product Key" + actualProductKey
                            : "");
        }

        return "";
    }

    public String getLoadSpecsException() {
        return loadSpecsExceptionString;
    }

    public String getOS() {
        return os;
    }

    public String getFullOS() {
        return fullOS;
    }

    public String getChassisType() {
        return chassisType;
    }

    public boolean isLaptop() {
        return isLaptop;
    }

    public boolean hasScreen() {
        return hasScreen;
    }

    public boolean hasTouchscreen() {
        return hasTouchscreen;
    }

    public boolean hasCamera() {
        return hasCamera;
    }

    public boolean getPossibleCustomDesktopOrBareMotherboard() {
        return possibleCustomDesktopOrBareMotherboard;
    }

    public String getBrand() {
        return brand;
    }

    public String getFullBrand() {
        return fullBrand;
    }

    public String getMotherboardBrand() {
        return motherboardBrand;
    }

    public String getFullMotherboardBrand() {
        return fullMotherboardBrand;
    }

    public String getModel() {
        return model;
    }

    public String getFullModel() {
        return fullModel;
    }

    public String getMotherboardModel() {
        return motherboardModel;
    }

    public String getFullMotherboardModel() {
        return fullMotherboardModel;
    }

    public String getMacShortModelName() {
        return macShortModelName;
    }

    public String getMacModelIdentifier() {
        return macModelIdentifier;
    }

    public String getSerial() {
        return serial;
    }

    public String getFullSerial() {
        return fullSerial;
    }

    public String getMotherboardSerial() {
        return motherboardSerial;
    }

    public String getFullMotherboardSerial() {
        return fullMotherboardSerial;
    }

    public String getBiosUUID() {
        return biosUUID;
    }

    public boolean getSerialIsMAC() {
        return serialIsMAC;
    }

    public String getEthernetMAC() {
        return (ethernetMACarray.isEmpty() ? "N/A" : String.join(" + ", getEthernetMACarray()));
    }

    public ArrayList<String> getEthernetMACarray() {
        Collections.sort(ethernetMACarray); // Sort to make sure always returned in same order when used as serial.
        return ethernetMACarray;
    }

    public String getCPU() {
        return cpu;
    }

    public String getFullCPU() {
        return fullCPU;
    }

    public int getThreadCount() {
        return cpuThreadCount;
    }

    public String getRAM() {
        return ram;
    }

    public String getFullRAM() {
        return fullRAM;
    }

    public String getMotherboardRAM() {
        return motherboardRAM;
    }

    public String getFullMotherboardRAM() {
        return fullMotherboardRAM;
    }

    public String getGPU() {
        return gpu;
    }

    public String getFullGPU() {
        return (gpuArray.isEmpty() ? "N/A" : String.join(" + ", gpuArray));
    }

    public ArrayList<String> getGPUarray() {
        return gpuArray;
    }

    public String getGPUids() {
        return (gpuIDsArray.isEmpty() ? "N-A" : String.join("+", gpuIDsArray));
    }

    public ArrayList<String> getGPUidsArray() {
        return gpuIDsArray;
    }

    public String getGPUerrorString() {
        return String.join(" + ", gpuErrorArray);
    }

    public ArrayList<String> getGPUerrorArray() {
        return gpuErrorArray;
    }

    public String getAudio() {
        return audio;
    }

    public String getFullAudio() {
        return (audioArray.isEmpty() ? "N/A" : String.join(" + ", audioArray));
    }

    public ArrayList<String> getAudioArray() {
        return audioArray;
    }

    public String getAudioIDs() {
        return (audioIDsArray.isEmpty() ? "N-A" : String.join("+", audioIDsArray));
    }

    public ArrayList<String> getAudioIDsArray() {
        return audioIDsArray;
    }

    public String getAudioErrorString() {
        return String.join(" + ", audioErrorArray);
    }

    public ArrayList<String> getAudioErrorArray() {
        return audioErrorArray;
    }

    public String getWireless() {
        return (wirelessArray.isEmpty() ? "N/A" : String.join(" + ", wirelessArray));
    }

    public ArrayList<String> getWirelessArray() {
        return wirelessArray;
    }

    public String getWirelessErrorString() {
        return String.join(" + ", wirelessErrorArray);
    }

    public ArrayList<String> getWirelessErrorArray() {
        return wirelessErrorArray;
    }

    public String getBatteryHealthString() {
        return (batteryHealthArray.isEmpty() ? "N/A" : String.join(" + ", batteryHealthArray));
    }

    public ArrayList<String> getBatteryHealthArray() {
        return batteryHealthArray;
    }

    public String getBatteryHealthWarningString() {
        return String.join(" + ", batteryHealthWarningArray);
    }

    public ArrayList<String> getBatteryHealthWarningArray() {
        return batteryHealthWarningArray;
    }

    public String getBatteryHealthErrorString() {
        return String.join(" + ", batteryHealthErrorArray);
    }

    public ArrayList<String> getBatteryHealthErrorArray() {
        return batteryHealthErrorArray;
    }

    public String getPowerAdapter() {
        return powerAdapter;
    }

    public String getScreenSize() {
        return screenSize;
    }

    public String getFullScreenSize() {
        return fullScreenSize;
    }

    public String getScreenErrorString() {
        return String.join(" + ", screenErrorArray);
    }

    public ArrayList<String> getScreenErrorArray() {
        return screenErrorArray;
    }

    public String getDiscDrive() {
        return discDrive;
    }

    public String getFullDiscDrive() {
        return (discDriveArray.isEmpty() ? "N/A" : String.join(" + ", discDriveArray));
    }

    public ArrayList<String> getDiscDriveArray() {
        return discDriveArray;
    }

    public ArrayList<String> getDiscDriveLogicalNames() {
        return discDriveLogicalNames;
    }

    public boolean discDriveCanBurnCDs() {
        return discDriveCanBurnCDs;
    }

    public boolean discDriveCanBurnDVDs() {
        return discDriveCanBurnDVDs;
    }

    public boolean hasDiscInDiscDrive() {
        return hasDiscInDiscDrive;
    }

    public String getStorage() {
        return storage;
    }

    public String getFullStorage() {
        return (storageArray.isEmpty() ? "N/A" : String.join(" + ", storageArray));
    }

    public ArrayList<String> getStorageArray() {
        return storageArray;
    }

    public String getStorageSerial() {
        return storageSerial;
    }

    public String getFullStorageSerial() {
        return (storageSerialsArray.isEmpty() ? "" : String.join(" + ", storageSerialsArray));
    }

    public ArrayList<String> getStorageSerialsArray() {
        return storageSerialsArray;
    }

    public boolean getDriveHealthWarning() {
        return driveHealthWarning;
    }

    public boolean getDriveTrimWarning() {
        return driveTrimWarning;
    }

    public boolean getDriveRecalled() {
        return driveRecalled;
    }

    public String getDisksFormattedWithoutPartitionTable() {
        return String.join(", ", disksFormattedWithoutPartitionTable);
    }

    public String getDriveHealth() {
        return driveHealth;
    }

    public boolean getKeyboardRecalled() {
        return keyboardRecalled;
    }

    public void printSpecs() {
        System.out.println("OS: " + fullOS);
        System.out.println("Chassis Type: " + chassisType);
        System.out.println("Is Laptop: " + (isLaptop ? "Yes" : "No"));
        System.out.println("Has Screen: " + (hasScreen ? "Yes" : "No"));
        System.out.println("Has Touchscreen: " + (hasTouchscreen ? "Yes" : "No"));
        System.out.println("Has Camera: " + (hasCamera ? "Yes" : "No"));
        System.out.println("Possible Custom Desktop or Bare Motherboard: " + (possibleCustomDesktopOrBareMotherboard ? "Yes" : "No"));

        System.out.println("");
        System.out.println("Brand: " + fullBrand);
        System.out.println("Motherboard Brand: " + fullMotherboardBrand);
        System.out.println("Model: " + fullModel);
        System.out.println("Motherboard Model: " + fullMotherboardModel);
        System.out.println("Serial: " + fullSerial);
        System.out.println("Motherboard Serial: " + fullMotherboardSerial);
        System.out.println("Serial Is MAC: " + (serialIsMAC ? "Yes" : "No"));
        System.out.println("MAC: " + getEthernetMAC());
        System.out.println("BIOS UUID: " + biosUUID);

        System.out.println("");
        System.out.println("CPU: " + fullCPU);
        System.out.println("RAM: " + fullRAM);
        System.out.println("Motherboard RAM: " + fullMotherboardRAM);
        System.out.println("Wireless: " + getWireless() + (wirelessErrorArray.isEmpty() ? "" : " - " + getWirelessErrorString()));
        System.out.println("Screen: " + fullScreenSize + (screenErrorArray.isEmpty() ? "" : " - " + getScreenErrorString()));
        System.out.println("GPU: " + getFullGPU() + (gpuErrorArray.isEmpty() ? "" : " - " + getGPUerrorString()));
        if (!gpuIDsArray.isEmpty()) {
            System.out.println("GPU IDs: " + getGPUids());
        }
        System.out.println("Audio: " + getFullAudio() + (audioErrorArray.isEmpty() ? "" : " - " + getAudioErrorString()));
        if (!audioIDsArray.isEmpty()) {
            System.out.println("Audio IDs: " + getAudioIDs());
        }

        System.out.println("");
        System.out.println("Battery Health: " + getBatteryHealthString());
        String batteryHealthWarningString = getBatteryHealthWarningString();
        System.out.println("Battery Warning: " + (batteryHealthWarningString.isEmpty() ? "None" : batteryHealthWarningString));
        String batteryHealthErrorString = getBatteryHealthErrorString();
        System.out.println("Battery Error: " + (batteryHealthErrorString.isEmpty() ? "None" : batteryHealthErrorString));
        if (!powerAdapter.isEmpty()) {
            System.out.println("Power Adapter: " + powerAdapter);
        }

        System.out.println("");
        System.out.println("Disc Drive: " + getFullDiscDrive());
        System.out.println("Disc Drive Logical Names: " + String.join(", ", discDriveLogicalNames));
        System.out.println("Disc Drive Can Burn CDs: " + (discDriveCanBurnCDs ? "Yes" : "No"));
        System.out.println("Disc Drive Can Burn DVDs: " + (discDriveCanBurnDVDs ? "Yes" : "No"));
        System.out.println("Disc in Disc Drive: " + (hasDiscInDiscDrive ? "Yes" : "No"));

        System.out.println("");
        System.out.println("Storage: " + getFullStorage());
        System.out.println("Storage Serial: " + getFullStorageSerial());
        System.out.println("Storage Health Warning: " + (driveHealthWarning ? "Yes" : "No"));
        System.out.println("Drive TRIM Warning: " + (driveTrimWarning ? "Yes" : "No"));
        System.out.println("Drive Recalled: " + (driveRecalled ? "Yes" : "No"));
        String disksFormattedWithoutPartitionTableString = getDisksFormattedWithoutPartitionTable();
        System.out.println("Disks Formatted Without Partition Table: " + (disksFormattedWithoutPartitionTableString.isEmpty() ? "None" : disksFormattedWithoutPartitionTableString));
        System.out.println("Storage Health:");
        System.out.println(driveHealth);

        System.out.println("");
    }
}
