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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

        setMinimumSize(UIScale.scale(getMinimumSize())); // Scale window minimum size by userScalingFactor for correct minimum size with HiDPI on Linux.

        lblIcon.setPreferredSize(null); // Undo preferred size so that the icon is displayed properly with HiDPI on Linux. (But keep preferred size in GUI builder so the design looks right).

        loadingProgressTextAreaScrollPane.setPreferredSize(UIScale.scale(loadingProgressTextAreaScrollPane.getPreferredSize())); // Scale preferred loadingProgressTextAreaScrollPane size for Linux HiDPI

        loadingProgressTextAreaScrollPane.setVisible(false);
        loadingProgressBar.setIndeterminate(true);

        boolean allowClosingLoadingWindowToQuit = true;

        if (System.getProperty("os.name").startsWith("Linux")) {
            if (new File("/proc/cmdline").exists()) {
                try {
                    List<String> linuxBootArguments = Arrays.asList(String.join(" ", Files.readAllLines(Paths.get("/proc/cmdline"), StandardCharsets.UTF_8)).split(" "));
                    boolean isLinuxUbiquityMode = (linuxBootArguments.contains("automatic-ubiquity") || linuxBootArguments.contains("only-ubiquity"));

                    if (isLinuxUbiquityMode && !new File("/tmp/detailed_hostname.txt").exists()) {
                        // Do not allow QA Helper to be closed while loading in Linux Ubiquity Mode if the detailed_hostname file hasn't already been created, because we need it!
                        allowClosingLoadingWindowToQuit = false;
                    }
                } catch (IOException getLinuxBootArgsException) {
                    System.out.println("getLinuxBootArgsException: " + getLinuxBootArgsException);
                }
            }
        } else if (System.getProperty("os.name").startsWith("Windows")) {
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
        setLoadingTextAndDisplay(loadingText, titleText, null);
    }

    public void setLoadingTextAndDisplay(String loadingText, String titleText, String iconName) {
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

        pack();

        if (!isVisible()) {
            setLoadingProgressBarToMax(0);
            setLoadingProgressText("");

            setLocationRelativeTo(null);
        }

        setVisible(true);
        setState(Frame.NORMAL);
        toFront();
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
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
                .addComponent(loadingProgressTextAreaScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblIcon;
    private javax.swing.JLabel lblLoadingText;
    private javax.swing.JProgressBar loadingProgressBar;
    private javax.swing.JTextArea loadingProgressTextArea;
    private javax.swing.JScrollPane loadingProgressTextAreaScrollPane;
    // End of variables declaration//GEN-END:variables
}
