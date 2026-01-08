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

import Utilities.*;
import com.formdev.flatlaf.util.UIScale;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class gathers and displays the PC's history from the CRM.
 *
 * @author Stefani Monson (of PCs for People) & Pico Mitchell (of Free Geek)
 */
public class StatusHistoryWindow extends javax.swing.JFrame {

    private final ArrayList<HashMap<String, String>> historyData;
    private final PrivateStrings privateStrings = new PrivateStrings();
    private final String[] displayColumnNames = privateStrings.getPCsCRMStatusHistoryDisplayColumnNames();
    private PCsCRMManager PCsCRMManager;
    private boolean isTestMode = false;

    /**
     * Creates new form StatusHistoryWindow
     *
     * @param statusHistoryDataContent
     * @param loggedSpecs
     * @param passedPCsCRMManager
     * @param testMode
     */
    public StatusHistoryWindow(String statusHistoryDataContent, HashMap<String, String> loggedSpecs, PCsCRMManager passedPCsCRMManager, boolean testMode) {
        PCsCRMManager = passedPCsCRMManager;
        isTestMode = testMode;

        historyData = new ArrayList<>();

        if (statusHistoryDataContent != null) {
            try {
                String[] rawColumnNames = privateStrings.getPCsCRMStatusHistoryRawColumnNames();

                JSONArray statusHistoryJsonArray = new JSONArray(new JSONObject(statusHistoryDataContent).getString("responseData"));
                for (int thisStatusHistoryIndex = 0; thisStatusHistoryIndex < statusHistoryJsonArray.length(); thisStatusHistoryIndex++) {
                    JSONObject thisStatusHistoryDictionary = statusHistoryJsonArray.getJSONObject(thisStatusHistoryIndex);

                    if (thisStatusHistoryDictionary.has(rawColumnNames[0]) && thisStatusHistoryDictionary.has(rawColumnNames[1]) && thisStatusHistoryDictionary.has(rawColumnNames[2])) {
                        HashMap<String, String> thisStatusHistoryEvent = new HashMap<>();
                        thisStatusHistoryEvent.put(displayColumnNames[0], thisStatusHistoryDictionary.getString(rawColumnNames[0]));
                        thisStatusHistoryEvent.put(displayColumnNames[1], thisStatusHistoryDictionary.getString(rawColumnNames[1]));
                        thisStatusHistoryEvent.put(displayColumnNames[2], thisStatusHistoryDictionary.getString(rawColumnNames[2]));
                        historyData.add(thisStatusHistoryEvent);
                    }
                }
            } catch (JSONException parseStatusHistoryException) {
                System.out.println("parseStatusHistoryException: " + parseStatusHistoryException);
            }
        }

        if (historyData.isEmpty()) {
            HashMap<String, String> unknownStatusHistoryEvent = new HashMap<>();
            unknownStatusHistoryEvent.put(displayColumnNames[0], "UNKNOWN DATE");
            unknownStatusHistoryEvent.put(displayColumnNames[1], "UNKNOWN STATUS");
            unknownStatusHistoryEvent.put(displayColumnNames[2], "UNKNOWN TECH");
            historyData.add(unknownStatusHistoryEvent);
        } else {
            // Must sort because may come from the API out of order.
            String sortColumnName = displayColumnNames[0];
            Collections.sort(historyData, (HashMap<String, String> thisRow, HashMap<String, String> thatRow) -> {
                return thatRow.get(sortColumnName).compareTo(thisRow.get(sortColumnName)); // Sort as strings because converting to Date() loses sub-second precision.
            });
        }

        initComponents();

        setMinimumSize(UIScale.scale(getMinimumSize())); // Scale window minimum size by userScaleFactor for correct minimum size with HiDPI on Linux.

        statusHistoryTabbedPane.setIconAt(0, new TwemojiImage("MagnifyingGlassTiltedLeft", this).toImageIcon(16));
        statusHistoryTabbedPane.setIconAt(1, new TwemojiImage("Memo", this).toImageIcon(16));

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
                    case "ID":
                        setTitle("QA Helper  —  Status History for " + thisSpecValue.toUpperCase());
                        break;
                    case "Status":
                        specsDisplayHTML += "<div style='padding: 10px;'><b>" + thisSpecKey + ":</b><br/>" + thisSpecValue + "</div>";
                        break;
                    default:
                        specsDisplayHTML += "<div style='padding: 10px; border-top: 1px solid #CCCCCC;'><b>" + thisSpecKey + ":</b>" + (thisSpecValue.startsWith("<br/>") ? "" : "<br/>") + ((thisSpecValue.startsWith("http://") || thisSpecValue.startsWith("https://")) ? ("<a href=\"" + thisSpecValue + "\">" + thisSpecValue + "</a>") : thisSpecValue) + "</div>"; // Notes field could start with "<br/>".

                        if (testMode && thisSpecKey.equals("Notes")) {
                            specsDisplayHTML += "<div style='padding: 10px; border-top: 1px solid #CCCCCC; text-align: center'><b><i>BELOW THIS LINE ARE ALL AVAILABLE FIELDS FOR TEST MODE</i></b></div>";
                        }

                        break;
                }
            }
            specsDisplayHTML += "</html>";

            loggedSpecsEditorPane.setText(specsDisplayHTML);

            loggedSpecsEditorPane.addHyperlinkListener((HyperlinkEvent hyperlinkEvent) -> {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hyperlinkEvent.getEventType())) {
                    try {
                        Desktop.getDesktop().browse(hyperlinkEvent.getURL().toURI());
                    } catch (IOException | URISyntaxException openLoggedSpecsLinkException) {
                        System.out.println("openLoggedSpecsLinkException: " + openLoggedSpecsLinkException);
                    }
                }
            });
        }
    }

    private DefaultTableModel getData() {
        DefaultTableModel dtm = new DefaultTableModel(displayColumnNames.length, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        dtm.setColumnIdentifiers(displayColumnNames);
        dtm.setRowCount(0);

        for (HashMap<String, String> row : historyData) {
            String[] rowArray = new String[displayColumnNames.length];
            int i = 0;
            for (String key : row.keySet()) {
                if (key.equals(displayColumnNames[0])) {
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
                } else if (key.equals(displayColumnNames[1])) {
                    rowArray[1] = row.get(key);
                } else if (key.equals(displayColumnNames[2])) {
                    String rawUsername = row.get(displayColumnNames[2]);
                    if (rawUsername.equals("UNKNOWN TECH")) {
                        rowArray[2] = rawUsername;
                    } else {
                        rawUsername = rawUsername.toLowerCase();

                        String displayName = rawUsername;

                        try {
                            HashMap<String, String> thisUserInfo = PCsCRMManager.getUserInfo(rawUsername, isTestMode);

                            if (thisUserInfo.containsKey("displayName")) {
                                displayName = thisUserInfo.get("displayName");
                            }
                        } catch (Exception getUserInfoException) {
                            if (isTestMode) {
                                System.out.println("getUserInfoException: " + getUserInfoException);
                            }
                        }

                        rowArray[2] = displayName;
                    }
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

    public String getLatestTech() {
        if (historyData.isEmpty()) {
            return "UNKNOWN TECH";
        }

        HashMap<String, String> latestHistoryItem = historyData.get(0);
        String rawUsername = latestHistoryItem.get(displayColumnNames[2]);
        if (rawUsername.equals("UNKNOWN TECH")) {
            return rawUsername;
        }
        rawUsername = rawUsername.toLowerCase();

        String displayName = rawUsername;

        try {
            HashMap<String, String> thisUserInfo = PCsCRMManager.getUserInfo(rawUsername, isTestMode);

            if (thisUserInfo.containsKey("displayName")) {
                displayName = thisUserInfo.get("displayName");
            }
        } catch (Exception getUserInfoException) {
            if (isTestMode) {
                System.out.println("getUserInfoException: " + getUserInfoException);
            }
        }

        return displayName;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        statusHistoryTabbedPane = new javax.swing.JTabbedPane();
        statusHistoryPane = new javax.swing.JScrollPane();
        tblHistory = new javax.swing.JTable();
        loggedSpecsPane = new javax.swing.JScrollPane();
        loggedSpecsEditorPane = new javax.swing.JEditorPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("QA Helper  —  Status History");
        setIconImages(new TwemojiImage("AppIcon", this).toImageIconsForFrame());
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(800, 300));
        setName("statusHistoryFrame"); // NOI18N

        statusHistoryPane.setBorder(null);

        tblHistory.setModel(getData());
        tblHistory.setRowHeight(UIScale.scale(30));
        tblHistory.setShowGrid(true);
        tblHistory.setShowVerticalLines(false);
        tblHistory.getTableHeader().setReorderingAllowed(false);
        statusHistoryPane.setViewportView(tblHistory);

        statusHistoryTabbedPane.addTab("Status History", statusHistoryPane);

        loggedSpecsPane.setBorder(null);

        loggedSpecsEditorPane.setEditable(false);
        loggedSpecsEditorPane.setContentType("text/html"); // NOI18N
        loggedSpecsPane.setViewportView(loggedSpecsEditorPane);

        statusHistoryTabbedPane.addTab("Logged Specs", loggedSpecsPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusHistoryTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 732, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusHistoryTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane loggedSpecsEditorPane;
    private javax.swing.JScrollPane loggedSpecsPane;
    private javax.swing.JScrollPane statusHistoryPane;
    private javax.swing.JTabbedPane statusHistoryTabbedPane;
    private javax.swing.JTable tblHistory;
    // End of variables declaration//GEN-END:variables
}
