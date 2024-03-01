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
import java.awt.Desktop;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.Year;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.HyperlinkEvent;

/**
 *
 * @author Pico Mitchell (of Free Geek)
 */
public class QACreditsWindow extends javax.swing.JFrame {

    /**
     * Creates new form QACreditsWindow
     */
    public QACreditsWindow() {
        initComponents();

        setMinimumSize(UIScale.scale(getMinimumSize())); // Scale window minimum size by userScalingFactor for correct minimum size with HiDPI on Linux.

        if (System.getProperty("os.name").startsWith("Windows") && (new File("\\Windows\\System32\\startnet.cmd").exists() || new File("\\Windows\\System32\\winpeshl.ini").exists()) && !new CommandReader(new String[]{"\\Windows\\System32\\reg.exe", "query", "HKLM\\SYSTEM\\Setup", "/v", "FactoryPreInstallInProgress"}).getFirstOutputLineContaining("0x1").isEmpty()) {
            setAlwaysOnTop(true); // Want all windows to be always on top in WinPE so they don't get lost behind full screen PowerShell window.
        }

        appIconLabel.setPreferredSize(null); // Undo preferred size so that the icon is displayed properly with HiDPI on Linux. (But keep preferred size in GUI builder so the design looks right).

        String currentYear = Year.now().toString();

        copyrightLabel.setText(copyrightLabel.getText().replace("[YEAR]", currentYear));

        try {
            versionLabel.setText("<html><b>Version:</b> " + new BufferedReader(new InputStreamReader(this.getClass().getResource("/Resources/qa-helper-version.txt").openStream())).readLine() + " (<b>Java:</b> " + System.getProperty("java.version") + ")</html>");
        } catch (NullPointerException | IOException loadVersionException) {
            versionLabel.setText("");
        }

        try {
            creditsEditorPane.setPage(this.getClass().getResource("/Resources/qa-helper-credits.html"));
        } catch (NullPointerException | IOException loadCreditsHTMLException) {
            creditsEditorPane.setText("ERROR LOADING CREDITS");
        }

        creditsEditorPane.addHyperlinkListener((HyperlinkEvent hyperlinkEvent) -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(hyperlinkEvent.getEventType())) {
                String urlString = hyperlinkEvent.getURL().toString();

                if (urlString.contains("#License=")) {
                    String licenseFilename = urlString.split("#License=")[1];

                    String licenseHeader = "<b><i>UNKNOWN License</i></b>";

                    switch (licenseFilename) {
                        case "Twemoji-CCBY4":
                            licenseHeader = "<b>App &amp; UI Icons:</b><br/><i>Twemoji</i> licensed under <u>CC-BY 4.0</u><br/>Copyright &copy; " + currentYear + " Twitter, Inc and other contributors";
                            break;
                        case "FlatLaf-Apache2":
                            licenseHeader = "<b>UI Theme:</b><br/><i>FlatLaf</i> licensed under the <u>Apache 2.0 License</u><br/>Copyright &copy; " + currentYear + " FormDev Software GmbH. All rights reserved.";
                            break;
                        case "OpenJDK-GPL2wCE":
                            licenseHeader = "<b>Includes</b> <i>OpenJDK</i><br/>An open-source implementation of the <i>Java SE Platform</i> under the<br/><u>GNU General Public License, version 2, with the Classpath Exception</u><br/>Copyright &copy; " + currentYear + " Oracle Corporation and/or its affiliates.";
                            break;
                        case "SOAPLibs-EDL1":
                            licenseHeader = "<b>Includes</b> <i>Jakarta Activation</i>, and parts of the <i>Jakarta XML Web Services</i>,<br/><i>Eclipse Metro</i>, and <i>JAXB</i> projects all licensed under <u>EDL 1.0</u><br/>Copyright &copy; " + currentYear + " Oracle and/or its affiliates. All rights reserved.";
                            break;
                        case "PCIIDRepo-3CBSD":
                            licenseHeader = "<b>Includes</b> <i>PCI ID Repository</i> licensed under <u>3-Clause BSD</u><br/>Copyright &copy; " + currentYear + " Martin Mares and Albert Pool";
                            break;
                        case "USBIDRepo-3CBSD":
                            licenseHeader = "<b>Includes</b> <i>USB ID Repository</i> licensed under <u>3-Clause BSD</u><br/>Copyright &copy; " + currentYear + " Stephen J. Gowdy";
                            break;
                        default:
                            break;
                    }

                    String licenseContents = "License NOT FOUND";

                    try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("Resources/Licenses/" + licenseFilename + ".txt")) {
                        if (is != null) {
                            try (InputStreamReader isr = new InputStreamReader(is); BufferedReader reader = new BufferedReader(isr)) {
                                licenseContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                            } catch (IOException ex) {

                            }
                        }
                    } catch (IOException ex) {

                    }

                    JTextArea licenseTextArea = new JTextArea(30, 80);
                    licenseTextArea.setText(licenseContents);
                    licenseTextArea.setCaretPosition(0);
                    licenseTextArea.setEditable(false);
                    licenseTextArea.setLineWrap(true);
                    licenseTextArea.setWrapStyleWord(true);
                    licenseTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, UIScale.scale(11)));
                    JScrollPane licenseScrollPane = new JScrollPane(licenseTextArea);

                    JOptionPane.showMessageDialog(this, new Object[]{
                        "<html>" + licenseHeader + "<br/><br/></html>",
                        licenseScrollPane}, "QA Helper  —  License", JOptionPane.PLAIN_MESSAGE);
                } else {
                    try {
                        Desktop.getDesktop().browse(hyperlinkEvent.getURL().toURI());
                    } catch (IOException | URISyntaxException openCreditsLinkException) {
                        System.out.println("openCreditsLinkException: " + openCreditsLinkException);
                    }
                }
            }
        });
    }

    public void showCreditsWindow() {
        if (!isVisible()) {
            creditsScrollPane.setBorder(null); // Make sure border is set to null on each open because it can get reset it light/dark mode what changed.
            creditsScrollPane.getVerticalScrollBar().setValue(0);
            creditsScrollPane.getHorizontalScrollBar().setValue(0);
            setLocationRelativeTo(null);
        }

        setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        appIconLabel = new javax.swing.JLabel();
        appNameLabel = new javax.swing.JLabel();
        copyrightLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();
        creditsSeparator1 = new javax.swing.JSeparator();
        creditsScrollPane = new javax.swing.JScrollPane();
        creditsEditorPane = new javax.swing.JEditorPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("QA Helper  —  Credits");
        setIconImages(new TwemojiImage("AppIcon", this).toImageIconsForFrame());
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(460, 460));
        setName("creditsFrame"); // NOI18N
        setResizable(false);

        appIconLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        appIconLabel.setIcon(new TwemojiImage("AppIcon", this).toImageIcon(false));
        appIconLabel.setPreferredSize(new java.awt.Dimension(64, 64));

        appNameLabel.setText("<html><b style='font-size: larger'>QA Helper</b></html>");

        copyrightLabel.setText("Copyright © [YEAR] PCs for People & Free Geek - MIT License");

        versionLabel.setText("<html><b>Version:</b> YYYY.MM.DD-R</html>");

        creditsScrollPane.setBorder(null);

        creditsEditorPane.setEditable(false);
        creditsEditorPane.setContentType("text/html"); // NOI18N
        creditsScrollPane.setViewportView(creditsEditorPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(UIScale.scale(12), UIScale.scale(12), UIScale.scale(12))
                .addComponent(appIconLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(appNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 169, Short.MAX_VALUE)
                        .addComponent(versionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(copyrightLabel))
                .addGap(UIScale.scale(12), UIScale.scale(12), UIScale.scale(12)))
            .addComponent(creditsSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(creditsScrollPane, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(UIScale.scale(12), UIScale.scale(12), UIScale.scale(12))
                        .addComponent(appIconLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(UIScale.scale(24), UIScale.scale(24), UIScale.scale(24))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(appNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(versionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(copyrightLabel)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(creditsSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(creditsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 369, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel appIconLabel;
    private javax.swing.JLabel appNameLabel;
    private javax.swing.JLabel copyrightLabel;
    private javax.swing.JEditorPane creditsEditorPane;
    private javax.swing.JScrollPane creditsScrollPane;
    private javax.swing.JSeparator creditsSeparator1;
    private javax.swing.JLabel versionLabel;
    // End of variables declaration//GEN-END:variables
}
