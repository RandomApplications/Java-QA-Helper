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
package GUI;

import SOAP.SOAPParse;
import Utilities.*;
import com.formdev.flatlaf.util.UIScale;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * This class gathers and displays the PC's history from the CRM.
 *
 * @author Stefani Monson (of PCs for People) & Pico Mitchell (of Free Geek)
 */
public class PIDHistoryWindow extends javax.swing.JFrame {

    private final ArrayList<HashMap<String, String>> historyData;

    /**
     * Creates new form PIDHistoryWindow
     *
     * @param statusHistoryDataXML
     * @param loggedSpecs
     * @param testMode
     */
    public PIDHistoryWindow(String statusHistoryDataXML, HashMap<String, String> loggedSpecs, boolean testMode) {
        historyData = new ArrayList<>();

        try {
            ArrayList<HashMap<String, String>> statusHistoryData = new SOAPParse().parseDataset(statusHistoryDataXML);

            // Must sort because may come from the API out of order.
            String rawDateHeaderName = new PrivateStrings().getPCsCRMStatusHistoryRawHeaderNames()[0];
            Collections.sort(statusHistoryData, (HashMap<String, String> thisRow, HashMap<String, String> thatRow) -> {
                return thisRow.get(rawDateHeaderName).compareTo(thatRow.get(rawDateHeaderName)); // Sort as strings because converting to Date() loses sub-second precision.
            });

            historyData.addAll(statusHistoryData);
        } catch (Exception parseStatusHistoryException) {
            System.out.println("parseStatusHistoryException: " + parseStatusHistoryException);
        }

        initComponents();

        setMinimumSize(UIScale.scale(getMinimumSize())); // Scale window minimum size by userScalingFactor for correct minimum size with HiDPI on Linux.

        pidHistoryTabbedPane.setIconAt(0, new TwemojiImage("MagnifyingGlassTiltedLeft", this).toImageIcon(16));
        pidHistoryTabbedPane.setIconAt(1, new TwemojiImage("Memo", this).toImageIcon(16));

        statusHistoryPane.setBorder(null); // Not sure why this needs to be set again to take effect when it's already set in initComponents(), but it is needed.

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tblHistory.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblHistory.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tblHistory.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        if (System.getProperty("os.name").startsWith("Windows") && (new File("\\Windows\\System32\\startnet.cmd").exists() || new File("\\Windows\\System32\\winpeshl.ini").exists()) && !new CommandReader(new String[]{"\\Windows\\System32\\reg.exe", "query", "HKLM\\SYSTEM\\Setup", "/v", "FactoryPreInstallInProgress"}).getFirstOutputLineContaining("0x1").isEmpty()) {
            setAlwaysOnTop(true); // Want all windows to be always on top in WinPE so they don't get lost behind full screen PowerShell window.
        }

        if (loggedSpecs != null) {
            String specsDisplayHTML = "<html>";
            for (HashMap.Entry<String, String> thisSpec : loggedSpecs.entrySet()) {
                String thisSpecKey = thisSpec.getKey();
                String thisSpecValue = thisSpec.getValue();

                switch (thisSpecKey) {
                    case "PID":
                        setTitle("QA Helper  —  PID History for " + thisSpecValue.toUpperCase());
                        break;
                    case "Brand":
                        specsDisplayHTML += "<div style='padding: 10px;'><b>" + thisSpecKey + ":</b><br/>" + thisSpecValue + "</div>";
                        break;
                    default:
                        specsDisplayHTML += "<div style='padding: 10px; border-top: 1px solid #CCCCCC;'><b>" + thisSpecKey + ":</b><br/>" + thisSpecValue + "</div>";

                        if (testMode && thisSpecKey.equals("Disc Drive")) {
                            specsDisplayHTML += "<div style='padding: 10px; border-top: 1px solid #CCCCCC; text-align: center'><b><i>BELOW THIS LINE ARE ALL AVAILABLE FIELDS FOR TEST MODE</i></b></div>";
                        }

                        break;
                }
            }
            specsDisplayHTML += "</html>";

            loggedSpecsEditorPane.setText(specsDisplayHTML);
        }
    }

    private DefaultTableModel getData() {
        String[] displayHeaders = new PrivateStrings().getPCsCRMStatusHistoryDisplayHeaderNames();

        DefaultTableModel dtm = new DefaultTableModel(displayHeaders.length, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        dtm.setColumnIdentifiers(displayHeaders);
        dtm.setRowCount(0);

        String[] rawHeaders = new PrivateStrings().getPCsCRMStatusHistoryRawHeaderNames();

        for (HashMap<String, String> row : historyData) {
            String[] rowArray = new String[displayHeaders.length];
            int i = 0;
            for (String key : row.keySet()) {
                if (key.equals(rawHeaders[0])) {
                    String thisDateString = row.get(key);

                    try {
                        rowArray[0] = new SimpleDateFormat("EEE, MMM d yyyy h:mm:ss a").format(
                                (thisDateString.contains(".") // For some reason some values don't have sub-second precision.
                                        ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                                        : new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).parse(thisDateString)
                        );
                    } catch (ParseException parseDateException) {
                        rowArray[0] = thisDateString;
                    }
                } else if (key.equals(rawHeaders[1])) {
                    rowArray[1] = row.get(key);
                } else if (key.equals(rawHeaders[2])) {
                    String rawUsername = row.get(rawHeaders[2]).toLowerCase();
                    String[] usernameParts = rawUsername.split("[^a-z0-9]");

                    String displayUsername = rawUsername;

                    if (usernameParts.length > 1) {
                        for (String thisUsernamePart : usernameParts) {
                            String capitalizedUsernamePart = thisUsernamePart.substring(0, 1).toUpperCase() + thisUsernamePart.substring(1);

                            if (displayUsername.equals(rawUsername)) {
                                displayUsername = capitalizedUsernamePart;
                            } else {
                                displayUsername += " " + capitalizedUsernamePart;
                            }
                        }
                    }

                    rowArray[2] = displayUsername;
                }

                i++;

            }

            if (rowArray.length > 0) {
                Object[] rowObject = rowArray;

                dtm.addRow(rowObject);
            }
        }

        return dtm;
    }

    public String[] getLatestStatusAndTech() {
        HashMap<String, String> latestHistoryItem = historyData.get(historyData.size() - 1);
        String[] rawHeaders = new PrivateStrings().getPCsCRMStatusHistoryRawHeaderNames();

        String rawUsername = latestHistoryItem.get(rawHeaders[2]).toLowerCase();
        String[] usernameParts = rawUsername.split("[^a-z0-9]");

        String displayUsername = rawUsername;

        if (usernameParts.length > 1) {
            for (String thisUsernamePart : usernameParts) {
                String capitalizedUsernamePart = thisUsernamePart.substring(0, 1).toUpperCase() + thisUsernamePart.substring(1);

                if (displayUsername.equals(rawUsername)) {
                    displayUsername = capitalizedUsernamePart;
                } else {
                    displayUsername += " " + capitalizedUsernamePart;
                }
            }
        }

        return new String[]{latestHistoryItem.get(rawHeaders[1]), displayUsername};
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pidHistoryTabbedPane = new javax.swing.JTabbedPane();
        statusHistoryPane = new javax.swing.JScrollPane();
        tblHistory = new javax.swing.JTable();
        loggedSpecsPane = new javax.swing.JScrollPane();
        loggedSpecsEditorPane = new javax.swing.JEditorPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("QA Helper  —  PID History");
        setBounds(new java.awt.Rectangle(0, 23, 800, 300));
        setIconImages(new TwemojiImage("AppIcon", this).toImageIconsForFrame());
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(800, 300));
        setName("pidHistoryFrame"); // NOI18N

        statusHistoryPane.setBorder(null);

        tblHistory.setModel(getData());
        tblHistory.setRowHeight(UIScale.scale(30));
        tblHistory.setShowGrid(true);
        tblHistory.setShowVerticalLines(false);
        tblHistory.getTableHeader().setReorderingAllowed(false);
        statusHistoryPane.setViewportView(tblHistory);

        pidHistoryTabbedPane.addTab("Status History", statusHistoryPane);

        loggedSpecsPane.setBorder(null);

        loggedSpecsEditorPane.setEditable(false);
        loggedSpecsEditorPane.setContentType("text/html"); // NOI18N
        loggedSpecsPane.setViewportView(loggedSpecsEditorPane);

        pidHistoryTabbedPane.addTab("Logged Specs", loggedSpecsPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pidHistoryTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 732, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pidHistoryTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane loggedSpecsEditorPane;
    private javax.swing.JScrollPane loggedSpecsPane;
    private javax.swing.JTabbedPane pidHistoryTabbedPane;
    private javax.swing.JScrollPane statusHistoryPane;
    private javax.swing.JTable tblHistory;
    // End of variables declaration//GEN-END:variables
}
