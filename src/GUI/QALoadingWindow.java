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
package GUI;

import Utilities.*;
import com.formdev.flatlaf.util.UIScale;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingWorker;

/**
 * @author Pico Mitchell (of Free Geek)
 */
public final class QALoadingWindow extends javax.swing.JFrame {

    boolean isWindowsPE = false;

    /**
     * Creates new form QALoadingWindow
     */
    public QALoadingWindow() {
        initComponents();

        setMinimumSize(UIScale.scale(getMinimumSize())); // Scale window minimum size by userScaleFactor for correct minimum size with HiDPI on Linux.

        lblIcon.setPreferredSize(null); // Undo preferred size so that the icon is displayed properly with HiDPI on Linux. (But keep preferred size in GUI builder so the design looks right).

        loadingProgressVolumePanel.setVisible(false);

        loadingProgressTextAreaScrollPane.setPreferredSize(UIScale.scale(loadingProgressTextAreaScrollPane.getPreferredSize())); // Scale preferred loadingProgressTextAreaScrollPane size for Linux HiDPI

        loadingProgressTextAreaScrollPane.setVisible(false);
        loadingProgressBar.setIndeterminate(true);

        // Undo preferred sizes so that the volume buttons are displayed properly with HiDPI on Linux. (But keep preferred sizes in GUI builder so the design looks right).
        btnSetVolumeTo10Percent.setPreferredSize(null);
        btnSetVolumeTo25Percent.setPreferredSize(null);
        btnSetVolumeTo50Percent.setPreferredSize(null);
        btnSetVolumeTo75Percent.setPreferredSize(null);
        btnSetVolumeTo100Percent.setPreferredSize(null);

        boolean allowClosingLoadingWindowToQuit = true;

        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            if (new File("/proc/cmdline").exists()) {
                try {
                    List<String> linuxBootArguments = Arrays.asList(String.join(" ", Files.readAllLines(Paths.get("/proc/cmdline"))).split(" "));
                    boolean isLinuxUbiquityMode = (linuxBootArguments.contains("automatic-ubiquity") || linuxBootArguments.contains("only-ubiquity"));

                    if (isLinuxUbiquityMode && !new File("/tmp/detailed_hostname.txt").exists()) {
                        // Do not allow QA Helper to be closed while loading in Linux Ubiquity Mode if the detailed_hostname file hasn't already been created, because we need it!
                        allowClosingLoadingWindowToQuit = false;
                    }
                } catch (IOException getLinuxBootArgsException) {
                    System.out.println("getLinuxBootArgsException: " + getLinuxBootArgsException);
                }
            }
        } else if (osName.startsWith("Windows")) {
            if (!new File("\\Install\\Drivers Cache Model Name.txt").exists()) {
                // Do not allow QA Helper to be closed while loading in Windows if the Drivers Cache Model Name file hasn't already been created, because we need it!
                allowClosingLoadingWindowToQuit = false;
            }

            if ((new File("\\Windows\\System32\\startnet.cmd").exists() || new File("\\Windows\\System32\\winpeshl.ini").exists()) && !new CommandReader(new String[]{"\\Windows\\System32\\reg.exe", "query", "HKLM\\SYSTEM\\Setup", "/v", "FactoryPreInstallInProgress"}).getFirstOutputLineContaining("0x1").isEmpty()) {
                isWindowsPE = true;
                setAlwaysOnTop(true); // Want all windows to be always on top in WinPE so they don't get lost behind full screen PowerShell window.
            }
        }

        if (allowClosingLoadingWindowToQuit) {
            setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        }

        pack();
    }

    public void setLoadingTextAndDisplay(String loadingText, String titleText) {
        setLoadingTextAndDisplay(loadingText, titleText, false, null);
    }

    public void setLoadingTextAndDisplay(String loadingText, String titleText, boolean showVolumeButtons) {
        setLoadingTextAndDisplay(loadingText, titleText, showVolumeButtons, null);
    }

    public void setLoadingTextAndDisplay(String loadingText, String titleText, String iconName) {
        setLoadingTextAndDisplay(loadingText, titleText, false, iconName);
    }

    public void setLoadingTextAndDisplay(String loadingText, String titleText, boolean showVolumeButtons, String iconName) {
        if (titleText == null || titleText.isEmpty()) {
            titleText = "Loading";
        }

        String newTitle = "QA Helper  —  " + titleText;
        if (!getTitle().equals(newTitle)) {
            setTitle(newTitle);
        }

        lblIcon.setIcon(new TwemojiImage(((iconName == null) ? "AppIcon" : iconName), this).toImageIcon(false));

        if (loadingText != null && !loadingText.isEmpty()) {
            lblLoadingText.setText("<html><b style='font-size: larger'>" + loadingText.replace("QA Helper", "<i>QA Helper</i>" + (isWindowsPE ? "&nbsp;" : "")) + "</b></html>");
        }

        if (!System.getProperty("os.name").startsWith("Windows")) {
            if (loadingProgressVolumePanel.isVisible() != showVolumeButtons) {
                loadingProgressVolumePanel.setVisible(showVolumeButtons);
            }
        }

        pack();

        if (!isVisible()) {
            setLoadingProgressBarToMax(0);

            setLoadingProgressText("");

            setLocationRelativeTo(null);
        }

        setVisible(true);
        setState(Frame.NORMAL);
        toFront();

        if (showVolumeButtons) {
            btnSetVolumeTo50Percent.requestFocusInWindow();
        }
    }

    public void closeWindow() {
        // Set frame to normal before disposing to avoid issue in Windows with HiDPI
        // where the size an position could be wrong next time it's opened if it was minimized when it was disposed.

        setState(Frame.NORMAL);
        dispose();
    }

    public String getLoadingText() {
        return lblLoadingText.getText().replaceAll("\\<[^>]*>", "");
    }

    public boolean isIndeterminate() {
        return loadingProgressBar.isIndeterminate();
    }

    public void setLoadingProgressBarToMax(int progressMaximum) {
        if (progressMaximum > 0) {
            loadingProgressBar.setValue(0);
            loadingProgressBar.setMinimum(0);
            loadingProgressBar.setMaximum(progressMaximum);

            if (loadingProgressBar.isIndeterminate()) {
                loadingProgressBar.setIndeterminate(false);
            }
        } else if (!loadingProgressBar.isIndeterminate()) {
            loadingProgressBar.setIndeterminate(true);
        }
    }

    public void incrementLoadingProgressBar() {
        if (!loadingProgressBar.isIndeterminate()) {
            loadingProgressBar.setValue(loadingProgressBar.getValue() + 1);
        }
    }

    public void setLoadingProgressText(String loadingProgressText) {
        loadingProgressTextArea.setText(loadingProgressText);

        boolean shouldShowOrHideProgressText = false;

        if (loadingProgressTextArea.getText().isEmpty()) {
            if (loadingProgressTextAreaScrollPane.isVisible()) {
                shouldShowOrHideProgressText = true;
            }
        } else if (!loadingProgressTextAreaScrollPane.isVisible()) {
            shouldShowOrHideProgressText = true;
        }

        if (shouldShowOrHideProgressText) {
            loadingProgressTextAreaScrollPane.setVisible(!loadingProgressTextAreaScrollPane.isVisible());

            final Dimension prePackSize = getSize();

            pack();

            if (isVisible()) {
                // Wait up to 1/2 second in the background before re-centering because pack() may not happen immediately.
                (new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        for (int waitForPack = 0; waitForPack < 50; waitForPack++) {
                            if (!prePackSize.equals(getSize())) {
                                break;
                            }

                            TimeUnit.MILLISECONDS.sleep(10);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        setLocationRelativeTo(null);
                        setVisible(true);
                        setState(Frame.NORMAL);
                    }
                }).execute();
            }
        }
    }

    public void addLoadingProgressText(String loadingProgressText) {
        setLoadingProgressText(loadingProgressTextArea.getText() + (loadingProgressTextArea.getText().isEmpty() ? "" : "\n") + loadingProgressText);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblIcon = new javax.swing.JLabel();
        lblLoadingText = new javax.swing.JLabel();
        loadingProgressBar = new javax.swing.JProgressBar();
        loadingProgressTextAreaScrollPane = new javax.swing.JScrollPane();
        loadingProgressTextArea = new javax.swing.JTextArea();
        loadingProgressVolumePanel = new javax.swing.JPanel();
        lblVolume = new javax.swing.JLabel();
        btnSetVolumeTo10Percent = new javax.swing.JButton();
        btnSetVolumeTo25Percent = new javax.swing.JButton();
        btnSetVolumeTo50Percent = new javax.swing.JButton();
        btnSetVolumeTo75Percent = new javax.swing.JButton();
        btnSetVolumeTo100Percent = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("QA Helper  —  Loading");
        setIconImages(new TwemojiImage("AppIcon", this).toImageIconsForFrame());
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(416, 0));
        setName("loadingFrame"); // NOI18N
        setResizable(false);

        lblIcon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblIcon.setIcon(new TwemojiImage("AppIcon", this).toImageIcon(false));
        lblIcon.setPreferredSize(new java.awt.Dimension(64, 64));

        lblLoadingText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblLoadingText.setText("<html><b style='font-size: larger'><i>QA Helper</i> is Loading Computer Specs</b></html>");

        loadingProgressBar.setIndeterminate(true);

        loadingProgressTextAreaScrollPane.setMinimumSize(new java.awt.Dimension(380, 420));
        loadingProgressTextAreaScrollPane.setPreferredSize(new java.awt.Dimension(380, 420));

        loadingProgressTextArea.setEditable(false);
        loadingProgressTextArea.setLineWrap(true);
        loadingProgressTextArea.setWrapStyleWord(true);
        loadingProgressTextAreaScrollPane.setViewportView(loadingProgressTextArea);

        lblVolume.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblVolume.setText("<html><b>Volume:</b></html>");

        btnSetVolumeTo10Percent.setText("<html>10<span style='font-size: smaller'>%</span></html>");
        btnSetVolumeTo10Percent.setMinimumSize(new java.awt.Dimension(60, 24));
        btnSetVolumeTo10Percent.setPreferredSize(new java.awt.Dimension(60, 24));
        btnSetVolumeTo10Percent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetVolumeTo10PercentActionPerformed(evt);
            }
        });

        btnSetVolumeTo25Percent.setText("<html>25<span style='font-size: smaller'>%</span></html>");
        btnSetVolumeTo25Percent.setMinimumSize(new java.awt.Dimension(60, 24));
        btnSetVolumeTo25Percent.setPreferredSize(new java.awt.Dimension(60, 24));
        btnSetVolumeTo25Percent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetVolumeTo25PercentActionPerformed(evt);
            }
        });

        btnSetVolumeTo50Percent.setText("<html>50<span style='font-size: smaller'>%</span></html>");
        btnSetVolumeTo50Percent.setMinimumSize(new java.awt.Dimension(60, 24));
        btnSetVolumeTo50Percent.setPreferredSize(new java.awt.Dimension(60, 24));
        btnSetVolumeTo50Percent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetVolumeTo50PercentActionPerformed(evt);
            }
        });

        btnSetVolumeTo75Percent.setText("<html>75<span style='font-size: smaller'>%</span></html>");
        btnSetVolumeTo75Percent.setMinimumSize(new java.awt.Dimension(60, 24));
        btnSetVolumeTo75Percent.setPreferredSize(new java.awt.Dimension(60, 24));
        btnSetVolumeTo75Percent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetVolumeTo75PercentActionPerformed(evt);
            }
        });

        btnSetVolumeTo100Percent.setText("<html>100<span style='font-size: smaller'>%</span></html>");
        btnSetVolumeTo100Percent.setMargin(new java.awt.Insets(2, 10, 2, 10));
        btnSetVolumeTo100Percent.setMinimumSize(new java.awt.Dimension(60, 24));
        btnSetVolumeTo100Percent.setPreferredSize(new java.awt.Dimension(60, 24));
        btnSetVolumeTo100Percent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetVolumeTo100PercentActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout loadingProgressVolumePanelLayout = new javax.swing.GroupLayout(loadingProgressVolumePanel);
        loadingProgressVolumePanel.setLayout(loadingProgressVolumePanelLayout);
        loadingProgressVolumePanelLayout.setHorizontalGroup(
            loadingProgressVolumePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loadingProgressVolumePanelLayout.createSequentialGroup()
                .addComponent(lblVolume)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSetVolumeTo10Percent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSetVolumeTo25Percent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSetVolumeTo50Percent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSetVolumeTo75Percent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSetVolumeTo100Percent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        loadingProgressVolumePanelLayout.setVerticalGroup(
            loadingProgressVolumePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loadingProgressVolumePanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(loadingProgressVolumePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSetVolumeTo10Percent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSetVolumeTo25Percent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSetVolumeTo50Percent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSetVolumeTo75Percent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSetVolumeTo100Percent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblVolume, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(loadingProgressVolumePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblIcon, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblLoadingText, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(loadingProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(loadingProgressTextAreaScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                .addComponent(lblIcon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                .addComponent(lblLoadingText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                .addComponent(loadingProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(loadingProgressVolumePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(loadingProgressTextAreaScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSetVolumeTo10PercentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetVolumeTo10PercentActionPerformed
        setVolume(10);
    }//GEN-LAST:event_btnSetVolumeTo10PercentActionPerformed

    private void btnSetVolumeTo25PercentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetVolumeTo25PercentActionPerformed
        setVolume(25);
    }//GEN-LAST:event_btnSetVolumeTo25PercentActionPerformed

    private void btnSetVolumeTo50PercentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetVolumeTo50PercentActionPerformed
        setVolume(50);
    }//GEN-LAST:event_btnSetVolumeTo50PercentActionPerformed

    private void btnSetVolumeTo75PercentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetVolumeTo75PercentActionPerformed
        setVolume(75);
    }//GEN-LAST:event_btnSetVolumeTo75PercentActionPerformed

    private void btnSetVolumeTo100PercentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetVolumeTo100PercentActionPerformed
        setVolume(100);
    }//GEN-LAST:event_btnSetVolumeTo100PercentActionPerformed

    private void setVolume(int desiredVolumePercentage) {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux") && new File("/tmp/qa_helper-audio_output_card.txt").exists()) {
            String selectedOutputCardString = "";
            String outputCard = "0";

            try {
                selectedOutputCardString = Files.readString(Paths.get("/tmp/qa_helper-audio_output_card.txt")).trim();
            } catch (IOException readAudioOutputCardException) {
                System.out.println("readAudioOutputCardException: " + readAudioOutputCardException);
            }

            boolean headphonesAreConnected = false;

            if (selectedOutputCardString.startsWith("Output ")) {
                headphonesAreConnected = selectedOutputCardString.contains(" USB ");
                String[] selectedOutputCardStringParts = selectedOutputCardString.split(":");
                outputCard = selectedOutputCardStringParts[0].replaceAll("[^0-9]", "");
            }

            /* NOTE: No longer picking default output card as of version 2025.2.20-3 to be able to more easily test USB speakers, but keeping old code commented out for reference.
            String lastOutputCard = "UNKNOWN CARD";
            String[] outputCardsAndDevicesInfo = new CommandReader(new String[]{"/usr/bin/aplay", "-l"}).getOutputLinesContaining("card");

            for (String thisOutputCardAndDeviceInfo : outputCardsAndDevicesInfo) {
                if (!thisOutputCardAndDeviceInfo.contains("HDMI")) {
                    String[] thisOutputCardAndDeviceInfoParts = thisOutputCardAndDeviceInfo.split(":");
                    if (thisOutputCardAndDeviceInfoParts[0].startsWith("card") && !thisOutputCardAndDeviceInfoParts[0].equals(lastOutputCard)) {
                        lastOutputCard = thisOutputCardAndDeviceInfoParts[0];
                        String thisOutputCard = lastOutputCard.replaceAll("[^0-9]", "");

                        // Actual desired volume will get set below.
                        String setVolumeForOutputCardOutput = new CommandReader(new String[]{"/usr/bin/amixer", "-c", thisOutputCard, "-M", "sset", "Master", "playback", "100%", "unmute"}).getFirstOutputLine();

                        if (!setVolumeForOutputCardOutput.isEmpty()) {
                            outputCard = thisOutputCard;
                        }
                    }
                }
            }
             */
            boolean acpiListenerGotHeadphoneState = false;

            if (!headphonesAreConnected && new File("/tmp/qa_helper-acpi_listener.txt").exists()) {
                try {
                    List<String> acpiListenerLines = Files.readAllLines(Paths.get("/tmp/qa_helper-acpi_listener.txt"));
                    for (String acpiListenerLine : acpiListenerLines) {
                        // headphonesAreConnected will be accurate for the last event detected. Can't just check last line since other events could get logged.
                        if (acpiListenerLine.endsWith("HEADPHONE unplug")) {
                            headphonesAreConnected = false;
                            acpiListenerGotHeadphoneState = true;
                        } else if (acpiListenerLine.endsWith("HEADPHONE plug")) {
                            headphonesAreConnected = true;
                            acpiListenerGotHeadphoneState = true;
                        }
                    }
                } catch (IOException acpiListenerException) {
                    System.out.println("acpiListenerException: " + acpiListenerException);
                }
            }

            if (!headphonesAreConnected && !acpiListenerGotHeadphoneState && new File("/proc/asound/card" + outputCard + "/codec#0").exists()) {
                // This method of headphone detection is not reliable and does not work on all computer.
                // But, still use it as a fallback if nothing has been logged yet by acpi_listen which started when QA Helper launched.

                try {
                    List<String> outputCardCodecInfoLines = Files.readAllLines(Paths.get("/proc/asound/card" + outputCard + "/codec#0"));
                    boolean nextPinCtlsIsHeadphoneJack = false;
                    for (String thisOutputCardCodecInfoLine : outputCardCodecInfoLines) {
                        if (thisOutputCardCodecInfoLine.contains("Conn = Analog, Color = Unknown") || thisOutputCardCodecInfoLine.contains("Conn = 1/8, Color = Green")) {
                            nextPinCtlsIsHeadphoneJack = true;
                        } else if (nextPinCtlsIsHeadphoneJack && thisOutputCardCodecInfoLine.contains("Pin-ctls:")) {
                            if (thisOutputCardCodecInfoLine.contains("0x00:")) {
                                headphonesAreConnected = true;
                                break;
                            }

                            nextPinCtlsIsHeadphoneJack = false;
                        }
                    }
                } catch (IOException outputCardCodecInfoException) {
                    System.out.println("outputCardCodecInfoException: " + outputCardCodecInfoException);
                }
            }

            String[][] outputCardArguments = new String[][]{
                {"-c", outputCard},
                {}, // Still try to set default volume for when we're not in pre-install environment.
                {"-D", "pulse"}, // Still try to set pulse volume for when we're not in pre-install environment.
                {"-D", "pipewire"} // And try pipewire which replaces pulse on Mint 22 and newer.
            };

            for (String[] thisOutputCardArguments : outputCardArguments) {
                String thisCardMixerControlName = "Master";
                ArrayList<String> amixerScontentsCommand = new ArrayList<>();
                amixerScontentsCommand.add("/usr/bin/amixer");
                amixerScontentsCommand.addAll(Arrays.asList(thisOutputCardArguments));
                amixerScontentsCommand.add("scontents");
                for (String thisMixerContentsLine : new CommandReader(amixerScontentsCommand.toArray(String[]::new)).getOutputLines()) {
                    if (thisMixerContentsLine.startsWith("Simple mixer control '")) {
                        thisCardMixerControlName = thisMixerContentsLine.substring(21);
                    } else if (thisMixerContentsLine.startsWith("  Playback channels")) {
                        try {
                            boolean shouldMute = ((headphonesAreConnected && thisCardMixerControlName.startsWith("'Speaker")) || (!headphonesAreConnected && thisCardMixerControlName.startsWith("'Headphone")));
                            if (thisCardMixerControlName.startsWith("'Sidetone")) { // Saw that turning up Sidetone on USB headphones causes constant loud buzz, so always MUTE!
                                shouldMute = true;
                            } else if (selectedOutputCardString.contains(" USB ")) { // Otherwise, never must USB output since it could be speakers OR headphones.
                                shouldMute = false;
                            }

                            ArrayList<String> amixerSsetCommand = new ArrayList<>();
                            amixerSsetCommand.add("/usr/bin/amixer");
                            amixerSsetCommand.addAll(Arrays.asList(thisOutputCardArguments));
                            amixerSsetCommand.addAll(Arrays.asList("-M", "sset", thisCardMixerControlName, "playback", (shouldMute ? "0" : (thisCardMixerControlName.contains(" Boost'") ? "100" : desiredVolumePercentage)) + "%", (shouldMute ? "mute" : "unmute")));
                            Runtime.getRuntime().exec(amixerSsetCommand.toArray(String[]::new)).waitFor();
                        } catch (IOException | InterruptedException setCardOutputVolumeException) {
                            System.out.println("setCardOutputVolumeException (" + String.join(" ", thisOutputCardArguments) + " + " + thisCardMixerControlName + "): " + setCardOutputVolumeException);
                        }
                    }
                }
            }
        } else if (osName.startsWith("Mac OS X") || osName.startsWith("macOS")) {
            try {
                Runtime.getRuntime().exec(new String[]{"/usr/bin/osascript", "-e", "set volume output volume " + desiredVolumePercentage + " without output muted", "-e", "set volume alert volume 100"}).waitFor();
            } catch (IOException | InterruptedException setMacOutputVolumeException) {
                System.out.println("setMacOutputVolumeException: " + setMacOutputVolumeException);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSetVolumeTo100Percent;
    private javax.swing.JButton btnSetVolumeTo10Percent;
    private javax.swing.JButton btnSetVolumeTo25Percent;
    private javax.swing.JButton btnSetVolumeTo50Percent;
    private javax.swing.JButton btnSetVolumeTo75Percent;
    private javax.swing.JLabel lblIcon;
    private javax.swing.JLabel lblLoadingText;
    private javax.swing.JLabel lblVolume;
    private javax.swing.JProgressBar loadingProgressBar;
    private javax.swing.JTextArea loadingProgressTextArea;
    private javax.swing.JScrollPane loadingProgressTextAreaScrollPane;
    private javax.swing.JPanel loadingProgressVolumePanel;
    // End of variables declaration//GEN-END:variables
}
