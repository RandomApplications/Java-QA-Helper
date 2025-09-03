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
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;

/**
 * @author Pico Mitchell (of Free Geek)
 */
public final class QAMouseTestWindow extends javax.swing.JDialog {

    /**
     * Creates new form QAMouseTestWindow
     *
     * @param parentFrame
     * @param isModal
     */
    public QAMouseTestWindow(java.awt.Frame parentFrame, boolean isModal) {
        super(parentFrame, isModal);

        initComponents();

        if (!System.getProperty("os.name").startsWith("Windows")) {
            lblMouseProperties.setVisible(false);
            btnOpenMouseProperties.setEnabled(false);
            btnOpenMouseProperties.setVisible(false);
            pack(); // Need to re-pack after hiding Mouse Properties so windows and scroll area isn't too tall.
        }
        
        setMinimumSize(UIScale.scale(getMinimumSize())); // Scale window minimum size by userScaleFactor for correct minimum size with HiDPI on Linux.
        
        lblMouseIcon.setPreferredSize(null); // Undo preferred size so that the icon is displayed properly with HiDPI on Linux. (But keep preferred size in GUI builder so the design looks right).

        double userScaleFactor = UIScale.getUserScaleFactor();
        if (userScaleFactor != 1.0) {
            // Scale scroll-test-image.png for HiDPI on Linux
            try {
                BufferedImage scrollTestImage = ImageIO.read(getClass().getResource("/Resources/scroll-test-image.png"));

                BufferedImage scaledScrollTestImage = new BufferedImage(UIScale.scale(scrollTestImage.getWidth()), UIScale.scale(scrollTestImage.getHeight()), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics2D = (Graphics2D) scaledScrollTestImage.getGraphics();
                graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                UIScale.scaleGraphics(graphics2D);
                graphics2D.drawImage(scrollTestImage, 0, 0, null);
                graphics2D.dispose();

                scrollTestLabel.setIcon(new javax.swing.ImageIcon(scaledScrollTestImage));
            } catch (IOException ex) {

            }
        }

        scrollTestPane.getVerticalScrollBar().setEnabled(false);
        scrollTestPane.getVerticalScrollBar().getModel().addChangeListener((ChangeEvent event) -> {
            BoundedRangeModel model = (BoundedRangeModel) event.getSource();
            int value = model.getValue();
            int extent = model.getExtent();
            int maximum = model.getMaximum();

            if (lblScrollDown.isEnabled() && (value + extent) >= (maximum - 20)) {
                lblScrollDown.setText("Scrolled Down!");
                lblScrollDown.setEnabled(false);
            } else if (!lblScrollDown.isEnabled() && lblScrollUp.isEnabled() && value <= 20) {
                lblScrollUp.setText("Scrolled Up!");
                lblScrollUp.setEnabled(false);
            }
        });

        scrollTestPane.getHorizontalScrollBar().setEnabled(false);
        scrollTestPane.getHorizontalScrollBar().getModel().addChangeListener((ChangeEvent event) -> {
            BoundedRangeModel model = (BoundedRangeModel) event.getSource();
            int value = model.getValue();
            int extent = model.getExtent();
            int maximum = model.getMaximum();

            if (lblScrollRight.isEnabled() && (value + extent) >= (maximum - 20)) {
                lblScrollRight.setText("Scrolled Right!");
                lblScrollRight.setEnabled(false);
            } else if (!lblScrollRight.isEnabled() && lblScrollLeft.isEnabled() && value <= 20) {
                lblScrollLeft.setText("Scrolled Left!");
                lblScrollLeft.setEnabled(false);
            }
        });

        btnDragDown.setTransferHandler(new ValueExportTransferHandler("Dropped Down!"));
        fieldDropDown.setTransferHandler(new ValueImportTransferHandler());

        btnDragOver.setTransferHandler(new ValueExportTransferHandler("Dropped Over!"));
        fieldDropOver.setTransferHandler(new ValueImportTransferHandler());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnResetTests = new javax.swing.JButton();
        lblMouseIcon = new javax.swing.JLabel();
        btnMouseTestDone = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        lblClickTest1 = new javax.swing.JLabel();
        lblClickTest2 = new javax.swing.JLabel();
        btnLeftClick = new javax.swing.JButton();
        btnMiddleClick = new javax.swing.JButton();
        btnRightClick = new javax.swing.JButton();
        lblMouseProperties = new javax.swing.JLabel();
        btnOpenMouseProperties = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        lblScrollTest1 = new javax.swing.JLabel();
        lblScrollTest2 = new javax.swing.JLabel();
        lblScrollTest3 = new javax.swing.JLabel();
        lblScrollTest4 = new javax.swing.JLabel();
        scrollTestPane = new javax.swing.JScrollPane();
        scrollTestLabel = new javax.swing.JLabel();
        lblScrollDown = new javax.swing.JLabel();
        lblScrollRight = new javax.swing.JLabel();
        lblScrollUp = new javax.swing.JLabel();
        lblScrollLeft = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        lblDragTest1 = new javax.swing.JLabel();
        lblDragTest2 = new javax.swing.JLabel();
        btnDragDown = new javax.swing.JButton();
        btnDragOver = new javax.swing.JButton();
        fieldDropOver = new javax.swing.JTextField();
        fieldDropDown = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("QA Helper  —  Mouse Test");
        setIconImages(new TwemojiImage("AppIcon", this).toImageIconsForFrame());
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(465, 615));
        setName("mouseTestDialog"); // NOI18N
        setResizable(false);

        btnResetTests.setMnemonic(KeyEvent.VK_R);
        btnResetTests.setText("Reset Mouse Tests");
        btnResetTests.setMargin(new java.awt.Insets(0, 15, 0, 15));
        btnResetTests.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetTestsActionPerformed(evt);
            }
        });

        lblMouseIcon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMouseIcon.setIcon(new TwemojiImage("ComputerMouse", this).toImageIcon(32));
        lblMouseIcon.setPreferredSize(new java.awt.Dimension(0, 32));

        btnMouseTestDone.setMnemonic(KeyEvent.VK_D);
        btnMouseTestDone.setText("Done Testing Mouse");
        btnMouseTestDone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMouseTestDoneActionPerformed(evt);
            }
        });

        lblClickTest1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblClickTest1.setText("<html>If there are <u>two sets of mouse buttons</u>, make sure to <b>test both sets</b></html>");

        lblClickTest2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblClickTest2.setText("<html><b>of mouse buttons</b> by using the \"Reset Mouse Tests\" button above.</html>");

        btnLeftClick.setText("Left Click Here");
        btnLeftClick.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnLeftClickMouseClicked(evt);
            }
        });

        btnMiddleClick.setText("Middle Click Here");
        btnMiddleClick.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnMiddleClickMouseClicked(evt);
            }
        });

        btnRightClick.setText("Right Click Here");
        btnRightClick.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnRightClickMouseClicked(evt);
            }
        });

        lblMouseProperties.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMouseProperties.setText("<html><i>If you are having <u>issues detecting any clicks</u>, try adjusting <b>Mouse Properties</b>.</i></html>");

        btnOpenMouseProperties.setText("Open Mouse Properties");
        btnOpenMouseProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenMousePropertiesActionPerformed(evt);
            }
        });

        lblScrollTest1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblScrollTest1.setText("<html>First, scroll down to the <b>BOTTOM LEFT CORNER</b>. If you can scroll</html>");

        lblScrollTest2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblScrollTest2.setText("<html>horizontally, then scroll over to the <b>BOTTOM RIGHT CORNER</b> and</html>");

        lblScrollTest3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblScrollTest3.setText("<html>then scroll up to the <b>TOP RIGHT CORNER</b>. Now, whether or not you</html>");

        lblScrollTest4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblScrollTest4.setText("<html>could scroll horizontally, scroll back to the <b>TOP LEFT CORNER</b>.</html>");

        scrollTestLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Resources/scroll-test-image.png"))); // NOI18N
        scrollTestPane.setViewportView(scrollTestLabel);

        lblScrollDown.setText("Scroll Down");

        lblScrollRight.setText("Scroll Right");

        lblScrollUp.setText("Scroll Up");

        lblScrollLeft.setText("Scroll Left");

        lblDragTest1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDragTest1.setText("<html>If there is a <u>trackpad as well as a pointing stick</u>, make sure to <b>test both</b></html>");

        lblDragTest2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDragTest2.setText("<html><b>pointing devices</b> by using the \"Reset Mouse Tests\" button above.</html>");

        btnDragDown.setText("Click & Drag Down");
        btnDragDown.setToolTipText("");
        btnDragDown.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                btnDragDownMouseDragged(evt);
            }
        });

        btnDragOver.setText("Click & Drag Over");
        btnDragOver.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                btnDragOverMouseDragged(evt);
            }
        });

        fieldDropOver.setEditable(false);
        fieldDropOver.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldDropOver.setText("Drop Over to Here");
        fieldDropOver.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                fieldDropOverPropertyChange(evt);
            }
        });

        fieldDropDown.setEditable(false);
        fieldDropDown.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fieldDropDown.setText("Drop Down to Here");
        fieldDropDown.setToolTipText("");
        fieldDropDown.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                fieldDropDownPropertyChange(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1)
            .addComponent(jSeparator3)
            .addComponent(jSeparator2)
            .addGroup(layout.createSequentialGroup()
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnResetTests)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblMouseIcon, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnMouseTestDone))
                    .addComponent(lblClickTest1)
                    .addComponent(lblClickTest2)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnLeftClick)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnMiddleClick)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnRightClick))
                    .addComponent(lblMouseProperties)
                    .addComponent(btnOpenMouseProperties, javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(lblScrollTest1)
                    .addComponent(lblScrollTest2)
                    .addComponent(lblScrollTest3)
                    .addComponent(lblScrollTest4)
                    .addComponent(scrollTestPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblScrollUp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblScrollLeft))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblScrollDown)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblScrollRight))
                    .addComponent(lblDragTest1)
                    .addComponent(lblDragTest2)
                    .addComponent(btnDragDown, javax.swing.GroupLayout.Alignment.CENTER)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnDragOver)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(fieldDropOver, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(fieldDropDown, javax.swing.GroupLayout.Alignment.CENTER, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18)))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btnDragDown, btnDragOver, btnLeftClick, btnMiddleClick, btnMouseTestDone, btnResetTests, btnRightClick, fieldDropDown, fieldDropOver});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnResetTests)
                    .addComponent(btnMouseTestDone)
                    .addComponent(lblMouseIcon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblClickTest1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblClickTest2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnLeftClick)
                    .addComponent(btnMiddleClick)
                    .addComponent(btnRightClick))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMouseProperties, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnOpenMouseProperties)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblScrollTest1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblScrollTest2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblScrollTest3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblScrollTest4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scrollTestPane, javax.swing.GroupLayout.DEFAULT_SIZE, 132, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblScrollDown)
                    .addComponent(lblScrollRight))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblScrollUp)
                    .addComponent(lblScrollLeft))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblDragTest1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblDragTest2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnDragDown)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnDragOver)
                    .addComponent(fieldDropOver, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(fieldDropDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(UIScale.scale(18), UIScale.scale(18), UIScale.scale(18)))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {btnDragDown, btnDragOver, btnLeftClick, btnMiddleClick, btnMouseTestDone, btnResetTests, btnRightClick, fieldDropDown, fieldDropOver});

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnMiddleClickMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnMiddleClickMouseClicked
        if (evt.getButton() == java.awt.event.MouseEvent.BUTTON2) {
            btnMiddleClick.setText("Middle Clicked!");
            btnMiddleClick.setEnabled(false);
        }
    }//GEN-LAST:event_btnMiddleClickMouseClicked

    private void btnLeftClickMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnLeftClickMouseClicked
        if (evt.getButton() == java.awt.event.MouseEvent.BUTTON1) {
            btnLeftClick.setText("Left Clicked!");
            btnLeftClick.setEnabled(false);
        }
    }//GEN-LAST:event_btnLeftClickMouseClicked

    private void btnRightClickMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnRightClickMouseClicked
        if (evt.getButton() == java.awt.event.MouseEvent.BUTTON3) {
            btnRightClick.setText("Right Clicked!");
            btnRightClick.setEnabled(false);
        }
    }//GEN-LAST:event_btnRightClickMouseClicked

    private void btnDragDownMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDragDownMouseDragged
        if (btnDragDown.isEnabled())
            btnDragDown.getTransferHandler().exportAsDrag(btnDragDown, evt, TransferHandler.COPY);
    }//GEN-LAST:event_btnDragDownMouseDragged

    private void btnDragOverMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDragOverMouseDragged
        if (btnDragOver.isEnabled())
            btnDragOver.getTransferHandler().exportAsDrag(btnDragOver, evt, TransferHandler.COPY);
    }//GEN-LAST:event_btnDragOverMouseDragged

    private void fieldDropDownPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_fieldDropDownPropertyChange
        if (!fieldDropDown.isEnabled() && btnDragDown.isEnabled()) {
            btnDragDown.setEnabled(false);
            btnDragDown.setText("Dragged Down!");
        }
    }//GEN-LAST:event_fieldDropDownPropertyChange

    private void fieldDropOverPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_fieldDropOverPropertyChange
        if (!fieldDropOver.isEnabled() && btnDragOver.isEnabled()) {
            btnDragOver.setEnabled(false);
            btnDragOver.setText("Dragged Over!");
        }
    }//GEN-LAST:event_fieldDropOverPropertyChange

    private void btnResetTestsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetTestsActionPerformed
        btnLeftClick.setText("Left Click Here");
        btnLeftClick.setEnabled(true);

        btnMiddleClick.setText("Middle Click Here");
        btnMiddleClick.setEnabled(true);

        btnRightClick.setText("Right Click Here");
        btnRightClick.setEnabled(true);

        scrollTestPane.getVerticalScrollBar().setValue(0);
        scrollTestPane.getHorizontalScrollBar().setValue(0);

        lblScrollDown.setText("Scroll Down");
        lblScrollDown.setEnabled(true);

        lblScrollUp.setText("Scroll Up");
        lblScrollUp.setEnabled(true);

        lblScrollRight.setText("Scroll Right");
        lblScrollRight.setEnabled(true);

        lblScrollLeft.setText("Scroll Left");
        lblScrollLeft.setEnabled(true);

        btnDragDown.setText("Click & Drag Down");
        btnDragDown.setEnabled(true);

        fieldDropDown.setText("Drop Down to Here");
        fieldDropDown.setEnabled(true);

        btnDragOver.setText("Click & Drag Over");
        btnDragOver.setEnabled(true);

        fieldDropOver.setText("Drop Over to Here");
        fieldDropOver.setEnabled(true);
    }//GEN-LAST:event_btnResetTestsActionPerformed

    private void btnMouseTestDoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMouseTestDoneActionPerformed
        dispose();
    }//GEN-LAST:event_btnMouseTestDoneActionPerformed

    private void btnOpenMousePropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenMousePropertiesActionPerformed
        if (System.getProperty("os.name").startsWith("Windows")) {
            try {
                Runtime.getRuntime().exec(new String[]{"\\Windows\\System32\\control.exe", "main.cpl"}).waitFor();
            } catch (IOException | InterruptedException openMousePropertiesException) {
                System.out.println("openMousePropertiesException: " + openMousePropertiesException);
            }
        }
    }//GEN-LAST:event_btnOpenMousePropertiesActionPerformed

    // FROM: https://stackoverflow.com/questions/28844574/drag-and-drop-from-jbutton-to-jcomponent-in-java
    public static class ValueExportTransferHandler extends TransferHandler {

        public static final DataFlavor SUPPORTED_DATE_FLAVOR = DataFlavor.stringFlavor;
        private final String value;

        public ValueExportTransferHandler(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return DnDConstants.ACTION_COPY_OR_MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            Transferable t = new StringSelection(getValue());
            return t;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            super.exportDone(source, data, action);
            // Decide what to do after the drop has been accepted
        }

    }

    public static class ValueImportTransferHandler extends TransferHandler {

        public static final DataFlavor SUPPORTED_DATE_FLAVOR = DataFlavor.stringFlavor;

        public ValueImportTransferHandler() {
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            return support.isDataFlavorSupported(SUPPORTED_DATE_FLAVOR);
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            boolean accept = false;
            if (canImport(support)) {
                try {
                    Transferable t = support.getTransferable();
                    Object value = t.getTransferData(SUPPORTED_DATE_FLAVOR);

                    if (value instanceof String) {
                        Component component = support.getComponent();
                        if (component instanceof JTextField) {
                            JTextField thisTextField = ((JTextField) component);
                            if ((thisTextField.getText().equals("Drop Down to Here") && value.toString().equals("Dropped Down!"))
                                    || (thisTextField.getText().equals("Drop Over to Here") && value.toString().equals("Dropped Over!"))) {
                                thisTextField.setText(value.toString());
                                thisTextField.setEnabled(false);
                                accept = true;
                            }
                        }
                    }
                } catch (UnsupportedFlavorException | IOException acceptDropException) {
                    // Ignore Error
                }
            }
            return accept;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDragDown;
    private javax.swing.JButton btnDragOver;
    private javax.swing.JButton btnLeftClick;
    private javax.swing.JButton btnMiddleClick;
    private javax.swing.JButton btnMouseTestDone;
    private javax.swing.JButton btnOpenMouseProperties;
    private javax.swing.JButton btnResetTests;
    private javax.swing.JButton btnRightClick;
    private javax.swing.JTextField fieldDropDown;
    private javax.swing.JTextField fieldDropOver;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JLabel lblClickTest1;
    private javax.swing.JLabel lblClickTest2;
    private javax.swing.JLabel lblDragTest1;
    private javax.swing.JLabel lblDragTest2;
    private javax.swing.JLabel lblMouseIcon;
    private javax.swing.JLabel lblMouseProperties;
    private javax.swing.JLabel lblScrollDown;
    private javax.swing.JLabel lblScrollLeft;
    private javax.swing.JLabel lblScrollRight;
    private javax.swing.JLabel lblScrollTest1;
    private javax.swing.JLabel lblScrollTest2;
    private javax.swing.JLabel lblScrollTest3;
    private javax.swing.JLabel lblScrollTest4;
    private javax.swing.JLabel lblScrollUp;
    private javax.swing.JLabel scrollTestLabel;
    private javax.swing.JScrollPane scrollTestPane;
    // End of variables declaration//GEN-END:variables
}
