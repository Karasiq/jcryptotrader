/*
 * Created by JFormDesigner on Wed Jan 01 13:03:04 MSK 2014
 */

package com.archean.jtradegui;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.StyleSheet;

import com.archean.jtradeapi.AccountManager;
import com.archean.jtradeapi.ApiWorker;
import com.archean.jtradeapi.BaseTradeApi;
import com.archean.jtradeapi.Utils;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author Yarr harr
 */
public class TraderMainForm extends JPanel {
    Thread workerThread = null;
    ApiWorker worker = new ApiWorker();
    Map<String, BaseTradeApi.StandartObjects.CurrencyPair> pairList = new TreeMap<>();
    protected void processException(Exception e) {
        StyledDocument document = textPaneLog.getStyledDocument();
        SimpleAttributeSet errorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errorStyle, Color.RED);
        StyleConstants.setBold(errorStyle, true);
        try {
            document.insertString(document.getLength(), e.getLocalizedMessage(), errorStyle);
        } catch (BadLocationException e1) {
            e1.printStackTrace();
        }
    }
    protected void processNotification(String notification) {
        StyledDocument document = textPaneLog.getStyledDocument();
        SimpleAttributeSet notificationStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(notificationStyle, Color.BLUE);
        StyleConstants.setItalic(notificationStyle, true);
        try {
            document.insertString(document.getLength(), notification, notificationStyle);
        } catch (BadLocationException e1) {
            e1.printStackTrace();
        }
    }
    public void initMarket(AccountManager.Account account) {
        if(!workerThread.isInterrupted()) {
            workerThread.interrupt(); // Stop thread
        }
        worker.initTradeApiInstance(account);
        try {
            pairList = worker.tradeApi.getCurrencyPairs().makeNameInfoMap();
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for(Map.Entry<String, BaseTradeApi.StandartObjects.CurrencyPair> entry : pairList.entrySet()) {
                model.addElement(entry.getKey());
            }
            comboBoxPair.setModel(model);
            comboBoxPair.setSelectedIndex(0);
            worker.setPair(((TreeMap<String, BaseTradeApi.StandartObjects.CurrencyPair>)pairList).firstEntry().getValue().pairId);
            workerThread = new Thread(worker); // Create new thread
        } catch (Exception e) {
            this.processException(e);
        }
    }
    public void fillGui(BaseTradeApi.StandartObjects.AccountInfo accountInfo, BaseTradeApi.StandartObjects.MarketInfo marketInfo) {
        // Current market data:
        textFieldLowPrice.setText(Double.toString(marketInfo.price.low));
        textFieldHighPrice.setText(Double.toString(marketInfo.price.high));
        textFieldAvgPrice.setText(Double.toString(marketInfo.price.average));
        textFieldBuyPrice.setText(Double.toString(marketInfo.price.buy));
        textFieldSellPrice.setText(Double.toString(marketInfo.price.sell));
        textFieldVolume.setText(Double.toString(marketInfo.volume));

        // Depth:
        if(tabbedPaneInfo.getSelectedIndex() == 2 /* Depth tab */) {
            DefaultTableModel model = (DefaultTableModel)tableBuyOrders.getModel();
            model.setRowCount(marketInfo.depth.buyOrders.size());
            int i = 0;
            for(BaseTradeApi.StandartObjects.Order order : marketInfo.depth.buyOrders) {
                model.setValueAt(order.price, i, 0);
                model.setValueAt(order.amount, i, 0);
                i++;
            }
            model = (DefaultTableModel)tableSellOrders.getModel();
            model.setRowCount(marketInfo.depth.sellOrders.size());
            i = 0;
            for(BaseTradeApi.StandartObjects.Order order : marketInfo.depth.sellOrders) {
                model.setValueAt(order.price, i, 0);
                model.setValueAt(order.amount, i, 0);
                i++;
            }
        }

        // History:
        if(tabbedPaneInfo.getSelectedIndex() == 3 /* History tab */) {
            // Market history:
            int i = 0;
            DefaultTableModel model = (DefaultTableModel)tableMarketHistory.getModel();
            model.setRowCount(marketInfo.history.size());
            for(BaseTradeApi.StandartObjects.Order order : marketInfo.history) {
                model.setValueAt(order.type == BaseTradeApi.Constants.ORDER_SELL ? buttonCommitSellOrder.getText() : buttonCommitBuyOrder.getText(), i, 0);
                model.setValueAt(order.price, i, 1);
                model.setValueAt(order.amount, i, 2);
                model.setValueAt(order.amount * order.price, i, 3);
                i++;
            }
            // Account history:
            i = 0;
            model = (DefaultTableModel)tableAccountHistory.getModel();
            model.setRowCount(accountInfo.history.size());
            for(BaseTradeApi.StandartObjects.Order order : accountInfo.history) {
                model.setValueAt(order.type == BaseTradeApi.Constants.ORDER_SELL ? "Sell" : "Buy", i, 0);
                model.setValueAt(order.price, i, 1);
                model.setValueAt(order.amount, i, 2);
                model.setValueAt(order.amount * order.price, i, 3);
                i++;
            }
        }

        // Open orders:
        if(tabbedPaneInfo.getSelectedIndex() == 0 /* Orders tab */) {
            int i = 0;
            DefaultTableModel model = (DefaultTableModel)tableOpenOrders.getModel();
            model.setRowCount(accountInfo.orders.size());
            for(BaseTradeApi.StandartObjects.Order order : accountInfo.orders) {
                model.setValueAt(order.type == BaseTradeApi.Constants.ORDER_SELL ? "Sell" : "Buy", i, 0); // Type
                model.setValueAt(order.price, i, 1); // Price
                model.setValueAt(order.amount, i, 2); // Amount
                model.setValueAt(order.amount * order.price, i, 3); // Total
                i++;
            }
        }

        // Balance:
        if(tabbedPaneInfo.getSelectedIndex() == 1 /* Balances tab */) {
            int i = 0;
            DefaultTableModel model = (DefaultTableModel)tableBalances.getModel();
            model.setRowCount(accountInfo.balance.size());
            for(Map.Entry<String, Double> balance : accountInfo.balance.entrySet()) {
                if(model.getValueAt(i, 0).equals(balance.getKey()) && !model.getValueAt(i, 1).equals(balance.getValue())) {
                    double changedBalance = balance.getValue() - (Double)model.getValueAt(i, 1);
                    processNotification("Available balance changed: " + (changedBalance > 0 ? "+" : "") + Utils.Strings.formatNumber(changedBalance) + " " + balance.getKey());
                }
                model.setValueAt(balance.getKey(), i, 0); // Currency name
                model.setValueAt(balance.getValue(), i, 1); // Amount
                i++;
            }
        }
    }
    protected Properties locale = new Properties();
    public TraderMainForm(AccountManager.Account account) {
        try {
            locale.load(this.getClass().getResourceAsStream("locale.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        initMarket(account);
        worker.setCallBack(new ApiWorker.Callback() {
            @Override
            public void onUpdate(ApiWorker worker) {
                fillGui(worker.accountInfo, worker.marketInfo.get(0));
            }
            @Override
            public void onError(Exception e) {
                processException(e);
            }
        });
        initComponents();

        SwingWorker apiPauser = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                while(!Thread.currentThread().isInterrupted()) {
                    worker.paused = !isVisible();
                    Thread.sleep(400);
                }
                return null;
            }
        };
        apiPauser.execute();
    }

    private void tabbedPaneInfoStateChanged(ChangeEvent e) {
        JTabbedPane pane = (JTabbedPane)e.getSource();
        switch(pane.getSelectedIndex()) {
            case 0: // Orders
                worker.setAccountInfoUpdate(true, true, false).setMarketInfoUpdate(true, false, false);
                break;
            case 1: // Balances
                worker.setAccountInfoUpdate(true, false, false).setMarketInfoUpdate(true, false, false);
                break;
            case 2: // Depth
                worker.setAccountInfoUpdate(false, false, false).setMarketInfoUpdate(true, true, false);
                break;
            case 3: // History
                worker.setAccountInfoUpdate(false, false, false).setMarketInfoUpdate(true, false, false);
                break;
            default: // Other tabs
                worker.setAccountInfoUpdate(false, false, false).setMarketInfoUpdate(true, false, false);
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.archean.jtradegui.locale");
        DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
        panelTrade = new JPanel();
        comboBoxPair = new JComboBox();
        panelPrice = new JPanel();
        labelBuyPrice = new JLabel();
        labelSellPrice = new JLabel();
        textFieldBuyPrice = new JTextField();
        textFieldSellPrice = new JTextField();
        labelLowPrice = new JLabel();
        labelHighPrice = new JLabel();
        textFieldLowPrice = new JTextField();
        textFieldHighPrice = new JTextField();
        textFieldAvgPrice = new JTextField();
        labelAvgPrice = new JLabel();
        labelVolume = new JLabel();
        textFieldVolume = new JTextField();
        separator1 = new JSeparator();
        tabbedPaneTrade = new JTabbedPane();
        panelBuy = new JPanel();
        labelBuyOrderPrice = new JLabel();
        spinnerBuyOrderPrice = new JSpinner();
        buttonBuyOrderPriceSetCurrent = new JButton();
        labelBuyOrderAmount = new JLabel();
        spinnerBuyOrderAmount = new JSpinner();
        sliderBuyOrderAmount = new JSlider();
        labelBuyOrderTotal = new JLabel();
        labelBuyOrderTotalValue = new JLabel();
        buttonCommitBuyOrder = new JButton();
        panelSell = new JPanel();
        labelSellOrderPrice = new JLabel();
        spinnerSellOrderPrice = new JSpinner();
        buttonSellOrderPriceSetCurrent = new JButton();
        labelSellOrderAmount = new JLabel();
        spinnerSellOrderAmount = new JSpinner();
        sliderSellOrderAmount = new JSlider();
        labelSellOrderTotal = new JLabel();
        labelSellOrderTotalValue = new JLabel();
        buttonCommitSellOrder = new JButton();
        tabbedPaneInfo = new JTabbedPane();
        panelOrders = new JPanel();
        scrollPane4 = new JScrollPane();
        tableOpenOrders = new JTable();
        panelBalance = new JPanel();
        scrollPane3 = new JScrollPane();
        tableBalances = new JTable();
        panelDepth = new JPanel();
        labelBuyOrders = compFactory.createLabel(bundle.getString("TraderMainForm.labelBuyOrders.textWithMnemonic"));
        labelSellOrders = compFactory.createLabel(bundle.getString("TraderMainForm.labelSellOrders.textWithMnemonic"));
        scrollPaneBuyOrders = new JScrollPane();
        tableBuyOrders = new JTable();
        scrollPaneSellOrders = new JScrollPane();
        tableSellOrders = new JTable();
        panelHistory = new JPanel();
        labelAccountHistory = new JLabel();
        labelMarketHistory = new JLabel();
        scrollPane6 = new JScrollPane();
        tableAccountHistory = new JTable();
        scrollPane7 = new JScrollPane();
        tableMarketHistory = new JTable();
        panelSettings = new JPanel();
        labelFeePercent = new JLabel();
        spinnerFeePercent = new JSpinner();
        label1 = new JLabel();
        spinnerUpdateInterval = new JSpinner();
        buttonApplySettings = new JButton();
        panelLog = new JPanel();
        scrollPane5 = new JScrollPane();
        textPaneLog = new JTextPane();

        //======== panelTrade ========
        {
            panelTrade.setLayout(new FormLayout(
                "[90dlu,pref]:grow, $lcgap, [92dlu,pref]:grow",
                "2*(default, $lgap), 73dlu, $lgap, 3dlu, $lgap, default, 0dlu, 15dlu, $lgap, fill:[140dlu,default]:grow"));
            panelTrade.add(comboBoxPair, CC.xywh(1, 1, 3, 1));

            //======== panelPrice ========
            {
                panelPrice.setLayout(new FormLayout(
                    "2*(default:grow)",
                    "[10dlu,default], 7*($lgap, default)"));

                //---- labelBuyPrice ----
                labelBuyPrice.setText(bundle.getString("TraderMainForm.labelBuyPrice.text"));
                labelBuyPrice.setFont(labelBuyPrice.getFont().deriveFont(labelBuyPrice.getFont().getStyle() | Font.BOLD));
                labelBuyPrice.setLabelFor(textFieldBuyPrice);
                panelPrice.add(labelBuyPrice, CC.xy(1, 1, CC.CENTER, CC.DEFAULT));

                //---- labelSellPrice ----
                labelSellPrice.setText(bundle.getString("TraderMainForm.labelSellPrice.text"));
                labelSellPrice.setLabelFor(textFieldSellPrice);
                labelSellPrice.setFont(labelSellPrice.getFont().deriveFont(Font.BOLD));
                panelPrice.add(labelSellPrice, CC.xy(2, 1, CC.CENTER, CC.DEFAULT));

                //---- textFieldBuyPrice ----
                textFieldBuyPrice.setToolTipText(bundle.getString("TraderMainForm.textFieldBuyPrice.toolTipText"));
                textFieldBuyPrice.setEditable(false);
                panelPrice.add(textFieldBuyPrice, CC.xy(1, 3));

                //---- textFieldSellPrice ----
                textFieldSellPrice.setToolTipText(bundle.getString("TraderMainForm.textFieldSellPrice.toolTipText"));
                textFieldSellPrice.setEditable(false);
                panelPrice.add(textFieldSellPrice, CC.xy(2, 3));

                //---- labelLowPrice ----
                labelLowPrice.setText(bundle.getString("TraderMainForm.labelLowPrice.text"));
                labelLowPrice.setFont(labelLowPrice.getFont().deriveFont(labelLowPrice.getFont().getStyle() | Font.BOLD));
                labelLowPrice.setLabelFor(textFieldLowPrice);
                panelPrice.add(labelLowPrice, CC.xy(1, 5, CC.CENTER, CC.DEFAULT));

                //---- labelHighPrice ----
                labelHighPrice.setText(bundle.getString("TraderMainForm.labelHighPrice.text"));
                labelHighPrice.setFont(labelHighPrice.getFont().deriveFont(labelHighPrice.getFont().getStyle() | Font.BOLD));
                labelHighPrice.setLabelFor(textFieldHighPrice);
                panelPrice.add(labelHighPrice, CC.xy(2, 5, CC.CENTER, CC.DEFAULT));

                //---- textFieldLowPrice ----
                textFieldLowPrice.setToolTipText(bundle.getString("TraderMainForm.textFieldLowPrice.toolTipText"));
                textFieldLowPrice.setEditable(false);
                panelPrice.add(textFieldLowPrice, CC.xy(1, 7));

                //---- textFieldHighPrice ----
                textFieldHighPrice.setToolTipText(bundle.getString("TraderMainForm.textFieldHighPrice.toolTipText"));
                textFieldHighPrice.setEditable(false);
                panelPrice.add(textFieldHighPrice, CC.xy(2, 7));

                //---- textFieldAvgPrice ----
                textFieldAvgPrice.setToolTipText(bundle.getString("TraderMainForm.textFieldAvgPrice.toolTipText"));
                textFieldAvgPrice.setEditable(false);
                panelPrice.add(textFieldAvgPrice, CC.xy(1, 11));

                //---- labelAvgPrice ----
                labelAvgPrice.setText(bundle.getString("TraderMainForm.labelAvgPrice.text"));
                labelAvgPrice.setFont(labelAvgPrice.getFont().deriveFont(labelAvgPrice.getFont().getStyle() | Font.BOLD));
                labelAvgPrice.setLabelFor(textFieldAvgPrice);
                panelPrice.add(labelAvgPrice, CC.xy(1, 9, CC.CENTER, CC.DEFAULT));

                //---- labelVolume ----
                labelVolume.setText(bundle.getString("TraderMainForm.labelVolume.text"));
                labelVolume.setFont(labelVolume.getFont().deriveFont(labelVolume.getFont().getStyle() | Font.BOLD));
                labelVolume.setLabelFor(textFieldVolume);
                panelPrice.add(labelVolume, CC.xy(2, 9, CC.CENTER, CC.DEFAULT));

                //---- textFieldVolume ----
                textFieldVolume.setToolTipText(bundle.getString("TraderMainForm.textFieldVolume.toolTipText"));
                textFieldVolume.setEditable(false);
                panelPrice.add(textFieldVolume, CC.xy(2, 11));
            }
            panelTrade.add(panelPrice, CC.xywh(1, 3, 3, 3));
            panelTrade.add(separator1, CC.xywh(1, 7, 3, 1));

            //======== tabbedPaneTrade ========
            {

                //======== panelBuy ========
                {
                    panelBuy.setLayout(new FormLayout(
                        "5dlu, $lcgap, 30dlu, $lcgap, 108dlu, $lcgap, [70dlu,default]:grow",
                        "2*(default, $lgap), 14dlu"));

                    //---- labelBuyOrderPrice ----
                    labelBuyOrderPrice.setText(bundle.getString("TraderMainForm.labelBuyOrderPrice.text"));
                    labelBuyOrderPrice.setLabelFor(spinnerBuyOrderPrice);
                    panelBuy.add(labelBuyOrderPrice, CC.xy(3, 1, CC.LEFT, CC.DEFAULT));

                    //---- spinnerBuyOrderPrice ----
                    spinnerBuyOrderPrice.setModel(new SpinnerNumberModel(0.0, 0.0, null, 1.0));
                    panelBuy.add(spinnerBuyOrderPrice, CC.xy(5, 1));

                    //---- buttonBuyOrderPriceSetCurrent ----
                    buttonBuyOrderPriceSetCurrent.setText(bundle.getString("TraderMainForm.buttonBuyOrderPriceSetCurrent.text"));
                    panelBuy.add(buttonBuyOrderPriceSetCurrent, CC.xy(7, 1, CC.LEFT, CC.DEFAULT));

                    //---- labelBuyOrderAmount ----
                    labelBuyOrderAmount.setText(bundle.getString("TraderMainForm.labelBuyOrderAmount.text"));
                    labelBuyOrderAmount.setLabelFor(spinnerBuyOrderAmount);
                    panelBuy.add(labelBuyOrderAmount, CC.xy(3, 3, CC.LEFT, CC.DEFAULT));

                    //---- spinnerBuyOrderAmount ----
                    spinnerBuyOrderAmount.setModel(new SpinnerNumberModel(0.0, 0.0, null, 0.01));
                    panelBuy.add(spinnerBuyOrderAmount, CC.xy(5, 3));

                    //---- sliderBuyOrderAmount ----
                    sliderBuyOrderAmount.setValue(0);
                    panelBuy.add(sliderBuyOrderAmount, CC.xy(7, 3, CC.LEFT, CC.DEFAULT));

                    //---- labelBuyOrderTotal ----
                    labelBuyOrderTotal.setText(bundle.getString("TraderMainForm.labelBuyOrderTotal.text"));
                    panelBuy.add(labelBuyOrderTotal, CC.xy(3, 5, CC.LEFT, CC.DEFAULT));

                    //---- labelBuyOrderTotalValue ----
                    labelBuyOrderTotalValue.setText("0 / 0");
                    panelBuy.add(labelBuyOrderTotalValue, CC.xy(5, 5, CC.RIGHT, CC.DEFAULT));

                    //---- buttonCommitBuyOrder ----
                    buttonCommitBuyOrder.setText(bundle.getString("TraderMainForm.buttonCommitBuyOrder.text"));
                    buttonCommitBuyOrder.setFont(buttonCommitBuyOrder.getFont().deriveFont(buttonCommitBuyOrder.getFont().getStyle() | Font.BOLD));
                    buttonCommitBuyOrder.setBackground(new Color(46, 195, 23, 122));
                    panelBuy.add(buttonCommitBuyOrder, CC.xy(7, 5, CC.LEFT, CC.DEFAULT));
                }
                tabbedPaneTrade.addTab(bundle.getString("TraderMainForm.panelBuy.tab.title"), panelBuy);

                //======== panelSell ========
                {
                    panelSell.setLayout(new FormLayout(
                        "5dlu, $lcgap, 30dlu, $lcgap, 108dlu, $lcgap, [100dlu,default]:grow",
                        "2*(default, $lgap), 14dlu"));

                    //---- labelSellOrderPrice ----
                    labelSellOrderPrice.setText(bundle.getString("TraderMainForm.labelSellOrderPrice.text"));
                    labelSellOrderPrice.setLabelFor(spinnerSellOrderPrice);
                    panelSell.add(labelSellOrderPrice, CC.xy(3, 1, CC.LEFT, CC.DEFAULT));

                    //---- spinnerSellOrderPrice ----
                    spinnerSellOrderPrice.setModel(new SpinnerNumberModel(0.0, 0.0, null, 1.0));
                    panelSell.add(spinnerSellOrderPrice, CC.xy(5, 1));

                    //---- buttonSellOrderPriceSetCurrent ----
                    buttonSellOrderPriceSetCurrent.setText(bundle.getString("TraderMainForm.buttonSellOrderPriceSetCurrent.text"));
                    panelSell.add(buttonSellOrderPriceSetCurrent, CC.xy(7, 1, CC.LEFT, CC.DEFAULT));

                    //---- labelSellOrderAmount ----
                    labelSellOrderAmount.setText(bundle.getString("TraderMainForm.labelSellOrderAmount.text"));
                    labelSellOrderAmount.setLabelFor(spinnerSellOrderAmount);
                    panelSell.add(labelSellOrderAmount, CC.xy(3, 3, CC.LEFT, CC.DEFAULT));

                    //---- spinnerSellOrderAmount ----
                    spinnerSellOrderAmount.setModel(new SpinnerNumberModel(0.0, 0.0, null, 0.01));
                    panelSell.add(spinnerSellOrderAmount, CC.xy(5, 3));

                    //---- sliderSellOrderAmount ----
                    sliderSellOrderAmount.setValue(0);
                    panelSell.add(sliderSellOrderAmount, CC.xy(7, 3, CC.LEFT, CC.DEFAULT));

                    //---- labelSellOrderTotal ----
                    labelSellOrderTotal.setText(bundle.getString("TraderMainForm.labelSellOrderTotal.text"));
                    panelSell.add(labelSellOrderTotal, CC.xy(3, 5, CC.LEFT, CC.DEFAULT));

                    //---- labelSellOrderTotalValue ----
                    labelSellOrderTotalValue.setText("0 / 0");
                    panelSell.add(labelSellOrderTotalValue, CC.xy(5, 5, CC.RIGHT, CC.DEFAULT));

                    //---- buttonCommitSellOrder ----
                    buttonCommitSellOrder.setText(bundle.getString("TraderMainForm.buttonCommitSellOrder.text"));
                    buttonCommitSellOrder.setBackground(new Color(255, 82, 82, 226));
                    buttonCommitSellOrder.setFont(buttonCommitSellOrder.getFont().deriveFont(buttonCommitSellOrder.getFont().getStyle() | Font.BOLD));
                    panelSell.add(buttonCommitSellOrder, CC.xy(7, 5, CC.LEFT, CC.DEFAULT));
                }
                tabbedPaneTrade.addTab(bundle.getString("TraderMainForm.panelSell.title"), panelSell);
            }
            panelTrade.add(tabbedPaneTrade, CC.xywh(1, 9, 3, 1));

            //======== tabbedPaneInfo ========
            {
                tabbedPaneInfo.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        tabbedPaneInfoStateChanged(e);
                    }
                });

                //======== panelOrders ========
                {
                    panelOrders.setLayout(new FormLayout(
                        "default:grow",
                        "default"));

                    //======== scrollPane4 ========
                    {

                        //---- tableOpenOrders ----
                        tableOpenOrders.setModel(new DefaultTableModel(
                            new Object[][] {
                                {null, null, null, null},
                                {null, null, null, null},
                            },
                            new String[] {
                                "Type", "Price", "Amount", "Total"
                            }
                        ) {
                            Class<?>[] columnTypes = new Class<?>[] {
                                String.class, Double.class, Double.class, Double.class
                            };
                            boolean[] columnEditable = new boolean[] {
                                false, false, false, false
                            };
                            @Override
                            public Class<?> getColumnClass(int columnIndex) {
                                return columnTypes[columnIndex];
                            }
                            @Override
                            public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return columnEditable[columnIndex];
                            }
                        });
                        scrollPane4.setViewportView(tableOpenOrders);
                    }
                    panelOrders.add(scrollPane4, CC.xy(1, 1));
                }
                tabbedPaneInfo.addTab(bundle.getString("TraderMainForm.panelOrders.tab.title"), panelOrders);

                //======== panelBalance ========
                {
                    panelBalance.setLayout(new FormLayout(
                        "112dlu:grow",
                        "default"));

                    //======== scrollPane3 ========
                    {

                        //---- tableBalances ----
                        tableBalances.setModel(new DefaultTableModel(
                            new Object[][] {
                                {null, null},
                                {null, null},
                            },
                            new String[] {
                                "Currency", "Balance"
                            }
                        ) {
                            Class<?>[] columnTypes = new Class<?>[] {
                                String.class, Double.class
                            };
                            boolean[] columnEditable = new boolean[] {
                                false, false
                            };
                            @Override
                            public Class<?> getColumnClass(int columnIndex) {
                                return columnTypes[columnIndex];
                            }
                            @Override
                            public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return columnEditable[columnIndex];
                            }
                        });
                        scrollPane3.setViewportView(tableBalances);
                    }
                    panelBalance.add(scrollPane3, CC.xy(1, 1));
                }
                tabbedPaneInfo.addTab(bundle.getString("TraderMainForm.panelBalance.tab.title"), panelBalance);

                //======== panelDepth ========
                {
                    panelDepth.setLayout(new FormLayout(
                        "[90dlu,default], $lcgap, [90dlu,default]",
                        "default, $lgap, fill:137dlu:grow"));

                    //---- labelBuyOrders ----
                    labelBuyOrders.setText("Buy orders");
                    labelBuyOrders.setHorizontalAlignment(SwingConstants.CENTER);
                    labelBuyOrders.setFont(labelBuyOrders.getFont().deriveFont(labelBuyOrders.getFont().getStyle() | Font.BOLD));
                    labelBuyOrders.setLabelFor(tableBuyOrders);
                    panelDepth.add(labelBuyOrders, CC.xy(1, 1));

                    //---- labelSellOrders ----
                    labelSellOrders.setText("Sell orders");
                    labelSellOrders.setLabelFor(tableSellOrders);
                    labelSellOrders.setHorizontalAlignment(SwingConstants.CENTER);
                    labelSellOrders.setFont(labelSellOrders.getFont().deriveFont(labelSellOrders.getFont().getStyle() | Font.BOLD));
                    panelDepth.add(labelSellOrders, CC.xy(3, 1));

                    //======== scrollPaneBuyOrders ========
                    {

                        //---- tableBuyOrders ----
                        tableBuyOrders.setModel(new DefaultTableModel(
                            new Object[][] {
                                {null, null},
                                {null, null},
                            },
                            new String[] {
                                "Price", "Amount"
                            }
                        ) {
                            Class<?>[] columnTypes = new Class<?>[] {
                                Double.class, Double.class
                            };
                            boolean[] columnEditable = new boolean[] {
                                false, false
                            };
                            @Override
                            public Class<?> getColumnClass(int columnIndex) {
                                return columnTypes[columnIndex];
                            }
                            @Override
                            public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return columnEditable[columnIndex];
                            }
                        });
                        scrollPaneBuyOrders.setViewportView(tableBuyOrders);
                    }
                    panelDepth.add(scrollPaneBuyOrders, CC.xy(1, 3));

                    //======== scrollPaneSellOrders ========
                    {

                        //---- tableSellOrders ----
                        tableSellOrders.setModel(new DefaultTableModel(
                            new Object[][] {
                                {null, null},
                                {null, null},
                            },
                            new String[] {
                                "Price", "Amount"
                            }
                        ) {
                            Class<?>[] columnTypes = new Class<?>[] {
                                Double.class, Double.class
                            };
                            boolean[] columnEditable = new boolean[] {
                                false, false
                            };
                            @Override
                            public Class<?> getColumnClass(int columnIndex) {
                                return columnTypes[columnIndex];
                            }
                            @Override
                            public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return columnEditable[columnIndex];
                            }
                        });
                        scrollPaneSellOrders.setViewportView(tableSellOrders);
                    }
                    panelDepth.add(scrollPaneSellOrders, CC.xy(3, 3));
                }
                tabbedPaneInfo.addTab(bundle.getString("TraderMainForm.panelDepth.tab.title"), panelDepth);

                //======== panelHistory ========
                {
                    panelHistory.setLayout(new FormLayout(
                        "[150dlu,default]:grow, $lcgap, [150dlu,default]:grow",
                        "top:13dlu, $lgap, [140dlu,default]"));

                    //---- labelAccountHistory ----
                    labelAccountHistory.setText(bundle.getString("TraderMainForm.labelAccountHistory.text"));
                    labelAccountHistory.setLabelFor(tableAccountHistory);
                    labelAccountHistory.setFont(labelAccountHistory.getFont().deriveFont(labelAccountHistory.getFont().getStyle() | Font.BOLD));
                    panelHistory.add(labelAccountHistory, CC.xy(1, 1, CC.CENTER, CC.DEFAULT));

                    //---- labelMarketHistory ----
                    labelMarketHistory.setText(bundle.getString("TraderMainForm.labelMarketHistory.text"));
                    labelMarketHistory.setFont(labelMarketHistory.getFont().deriveFont(labelMarketHistory.getFont().getStyle() | Font.BOLD));
                    panelHistory.add(labelMarketHistory, CC.xy(3, 1, CC.CENTER, CC.DEFAULT));

                    //======== scrollPane6 ========
                    {

                        //---- tableAccountHistory ----
                        tableAccountHistory.setModel(new DefaultTableModel(
                            new Object[][] {
                                {null, null, null, null},
                                {null, null, null, null},
                            },
                            new String[] {
                                "Type", "Price", "Amount", "Total"
                            }
                        ) {
                            Class<?>[] columnTypes = new Class<?>[] {
                                String.class, Double.class, Double.class, Double.class
                            };
                            boolean[] columnEditable = new boolean[] {
                                false, false, false, false
                            };
                            @Override
                            public Class<?> getColumnClass(int columnIndex) {
                                return columnTypes[columnIndex];
                            }
                            @Override
                            public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return columnEditable[columnIndex];
                            }
                        });
                        scrollPane6.setViewportView(tableAccountHistory);
                    }
                    panelHistory.add(scrollPane6, CC.xy(1, 3, CC.FILL, CC.FILL));

                    //======== scrollPane7 ========
                    {

                        //---- tableMarketHistory ----
                        tableMarketHistory.setModel(new DefaultTableModel(
                            new Object[][] {
                                {null, null, null, null},
                                {null, null, null, null},
                            },
                            new String[] {
                                "Type", "Price", "Amount", "Total"
                            }
                        ) {
                            Class<?>[] columnTypes = new Class<?>[] {
                                String.class, Double.class, Double.class, Double.class
                            };
                            boolean[] columnEditable = new boolean[] {
                                false, false, false, false
                            };
                            @Override
                            public Class<?> getColumnClass(int columnIndex) {
                                return columnTypes[columnIndex];
                            }
                            @Override
                            public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return columnEditable[columnIndex];
                            }
                        });
                        scrollPane7.setViewportView(tableMarketHistory);
                    }
                    panelHistory.add(scrollPane7, CC.xy(3, 3, CC.FILL, CC.FILL));
                }
                tabbedPaneInfo.addTab(bundle.getString("TraderMainForm.panelHistory.tab.title"), panelHistory);

                //======== panelSettings ========
                {
                    panelSettings.setLayout(new FormLayout(
                        "5dlu, $lcgap, 67dlu, $lcgap, 57dlu, 2*($lcgap, default)",
                        "2*(default, $lgap), default"));

                    //---- labelFeePercent ----
                    labelFeePercent.setText(bundle.getString("TraderMainForm.labelFeePercent.text"));
                    labelFeePercent.setLabelFor(spinnerFeePercent);
                    panelSettings.add(labelFeePercent, CC.xy(3, 1));

                    //---- spinnerFeePercent ----
                    spinnerFeePercent.setModel(new SpinnerNumberModel(0.2, 0.0, 100.0, 0.1));
                    panelSettings.add(spinnerFeePercent, CC.xy(5, 1));

                    //---- label1 ----
                    label1.setText(bundle.getString("TraderMainForm.label1.text"));
                    label1.setLabelFor(spinnerUpdateInterval);
                    panelSettings.add(label1, CC.xy(3, 3));

                    //---- spinnerUpdateInterval ----
                    spinnerUpdateInterval.setModel(new SpinnerNumberModel(250, 0, 30000, 50));
                    panelSettings.add(spinnerUpdateInterval, CC.xy(5, 3));

                    //---- buttonApplySettings ----
                    buttonApplySettings.setText(bundle.getString("TraderMainForm.buttonApplySettings.text"));
                    panelSettings.add(buttonApplySettings, CC.xywh(3, 5, 3, 1));
                }
                tabbedPaneInfo.addTab(bundle.getString("TraderMainForm.panelSettings.tab.title"), panelSettings);

                //======== panelLog ========
                {
                    panelLog.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                    //======== scrollPane5 ========
                    {
                        scrollPane5.setViewportView(textPaneLog);
                    }
                    panelLog.add(scrollPane5, CC.xy(1, 1));
                }
                tabbedPaneInfo.addTab(bundle.getString("TraderMainForm.panelLog.tab.title"), panelLog);
            }
            panelTrade.add(tabbedPaneInfo, CC.xywh(1, 11, 3, 3));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panelTrade;
    private JComboBox comboBoxPair;
    private JPanel panelPrice;
    private JLabel labelBuyPrice;
    private JLabel labelSellPrice;
    private JTextField textFieldBuyPrice;
    private JTextField textFieldSellPrice;
    private JLabel labelLowPrice;
    private JLabel labelHighPrice;
    private JTextField textFieldLowPrice;
    private JTextField textFieldHighPrice;
    private JTextField textFieldAvgPrice;
    private JLabel labelAvgPrice;
    private JLabel labelVolume;
    private JTextField textFieldVolume;
    private JSeparator separator1;
    private JTabbedPane tabbedPaneTrade;
    private JPanel panelBuy;
    private JLabel labelBuyOrderPrice;
    private JSpinner spinnerBuyOrderPrice;
    private JButton buttonBuyOrderPriceSetCurrent;
    private JLabel labelBuyOrderAmount;
    private JSpinner spinnerBuyOrderAmount;
    private JSlider sliderBuyOrderAmount;
    private JLabel labelBuyOrderTotal;
    private JLabel labelBuyOrderTotalValue;
    private JButton buttonCommitBuyOrder;
    private JPanel panelSell;
    private JLabel labelSellOrderPrice;
    private JSpinner spinnerSellOrderPrice;
    private JButton buttonSellOrderPriceSetCurrent;
    private JLabel labelSellOrderAmount;
    private JSpinner spinnerSellOrderAmount;
    private JSlider sliderSellOrderAmount;
    private JLabel labelSellOrderTotal;
    private JLabel labelSellOrderTotalValue;
    private JButton buttonCommitSellOrder;
    private JTabbedPane tabbedPaneInfo;
    private JPanel panelOrders;
    private JScrollPane scrollPane4;
    private JTable tableOpenOrders;
    private JPanel panelBalance;
    private JScrollPane scrollPane3;
    private JTable tableBalances;
    private JPanel panelDepth;
    private JLabel labelBuyOrders;
    private JLabel labelSellOrders;
    private JScrollPane scrollPaneBuyOrders;
    private JTable tableBuyOrders;
    private JScrollPane scrollPaneSellOrders;
    private JTable tableSellOrders;
    private JPanel panelHistory;
    private JLabel labelAccountHistory;
    private JLabel labelMarketHistory;
    private JScrollPane scrollPane6;
    private JTable tableAccountHistory;
    private JScrollPane scrollPane7;
    private JTable tableMarketHistory;
    private JPanel panelSettings;
    private JLabel labelFeePercent;
    private JSpinner spinnerFeePercent;
    private JLabel label1;
    private JSpinner spinnerUpdateInterval;
    private JButton buttonApplySettings;
    private JPanel panelLog;
    private JScrollPane scrollPane5;
    private JTextPane textPaneLog;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
