/*
 * Created by JFormDesigner on Wed Jan 01 13:03:04 MSK 2014
 */

package com.archean.jtradegui;

import javax.swing.border.*;
import com.archean.jtradeapi.AccountManager;
import com.archean.jtradeapi.ApiWorker;
import com.archean.jtradeapi.BaseTradeApi;
import com.archean.jtradeapi.Utils;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * @author Yarr harr
 */
public class TraderMainForm extends JPanel {
    private double feePercent = 0.2;

    static private class ApiLag {
        long priceUpdated;
        long depthUpdated;
        long ordersUpdated;
        long marketHistoryUpdated;
        long accountHistoryUpdated;
        long balancesUpdated;
    }

    private ApiLag lastUpdated = new ApiLag();

    public ApiWorker worker = new ApiWorker();
    public Map<String, BaseTradeApi.StandartObjects.CurrencyPair> pairList = new TreeMap<>();
    static ResourceBundle locale = ResourceBundle.getBundle("com.archean.jtradegui.locale", new UTF8Control());
    private volatile boolean initialized = false;
    private volatile boolean needStepUpdate = true;
    private volatile boolean needPriceChangeUpdate = true;
    private double priceChangeLastPrice = 0;
    private Thread apiLagThread = null;
    private Thread priceChangeTimerThread = null;

    public void stopWorker() {
        initialized = false;
        worker.stopAllThreads();
    }

    public void startWorker() {
        initialized = true;
        setUpdateOptions();
    }

    public void killThreads() {
        initialized = false;
        if(apiLagThread != null)
            apiLagThread.interrupt();
        if(priceChangeTimerThread != null)
            priceChangeTimerThread.interrupt();
        stopWorker();
    }

    public void setSettings(double feePercent, int timeInterval) {
        this.feePercent = feePercent;
        worker.setTimeInterval(timeInterval);
        spinnerUpdateInterval.setValue(timeInterval);
        spinnerFeePercent.setValue(feePercent);
    }

    public void setPair(final String pairName) {
        comboBoxPair.setSelectedItem(pairName);
        worker.setPair(pairList.get(pairName).pairId);
        clearMarketData();
    }

    public Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("timeInterval", spinnerUpdateInterval.getValue());
        settings.put("feePercent", feePercent);
        settings.put("currentPair", comboBoxPair.getSelectedItem());
        // settings.put("keyPair", worker.tradeApi.apiKeyPair);
        return settings;
    }

    protected void processException(final Exception e) {
        e.printStackTrace();
        TrayIconController.showMessage(locale.getString("notification.error.title"), e.getLocalizedMessage(), TrayIcon.MessageType.ERROR);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StyledDocument document = textPaneLog.getStyledDocument();
                SimpleAttributeSet errorStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(errorStyle, Color.RED);
                StyleConstants.setBold(errorStyle, true);
                try {
                    document.insertString(document.getLength(), e.getLocalizedMessage() + "\n", errorStyle);
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    protected void processNotification(final String notification) {
        TrayIconController.showMessage(locale.getString("notification.info.title"), notification, TrayIcon.MessageType.INFO);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StyledDocument document = textPaneLog.getStyledDocument();
                SimpleAttributeSet notificationStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(notificationStyle, Color.BLUE);
                StyleConstants.setItalic(notificationStyle, true);
                try {
                    document.insertString(document.getLength(), notification + "\n", notificationStyle);
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    protected void lockPanel(boolean lock) {
        this.setEnabled(!lock);
        Component[] com = this.getComponents();
        for (Component c : com) {
            c.setEnabled(!lock);
        }
    }

    protected String getCurrentPrimaryCurrency() {
        return pairList.get(comboBoxPair.getSelectedItem()).firstCurrency;
    }

    protected String getCurrentSecondaryCurrency() {
        return pairList.get(comboBoxPair.getSelectedItem()).secondCurrency;
    }

    protected double getCurrentPrimaryBalance() {
        return worker.accountInfo.balance.getBalance(getCurrentPrimaryCurrency());
    }

    protected double getCurrentSecondaryBalance() {
        return worker.accountInfo.balance.getBalance(getCurrentSecondaryCurrency());
    }

    protected void updateLabelTotal() {
        if(tabbedPaneTrade.getSelectedIndex() == 0)
        { // Buy
            double balance = getCurrentSecondaryBalance();
            double enteredTotal = (Double) spinnerBuyOrderAmount.getValue() * (Double) spinnerBuyOrderPrice.getValue() * ((100.0 + feePercent) / 100.0);
            labelBuyOrderTotalValue.setText(Utils.Strings.formatNumber(enteredTotal) + " / " + Utils.Strings.formatNumber(balance) + " " + getCurrentSecondaryCurrency());
            if (enteredTotal > balance) {
                labelBuyOrderTotalValue.setForeground(Color.RED);
            } else {
                labelBuyOrderTotalValue.setForeground(Color.BLACK);
            }
        } else
        { // Sell
            double balance = getCurrentPrimaryBalance(),
                    amount = (Double) spinnerSellOrderAmount.getValue(),
                    enteredTotal = (Double) spinnerSellOrderAmount.getValue() * (Double) spinnerSellOrderPrice.getValue() / ((100.0 + feePercent) / 100.0);

            labelSellOrderTotalValue.setText(Utils.Strings.formatNumber(amount) + " / " + Utils.Strings.formatNumber(balance) + " " + getCurrentPrimaryCurrency() + " (" + Utils.Strings.formatNumber(enteredTotal) + " " + getCurrentSecondaryCurrency() + ")");
            if (amount > balance) {
                labelSellOrderTotalValue.setForeground(Color.RED);
            } else {
                labelSellOrderTotalValue.setForeground(Color.BLACK);
            }
        }
    }

    private void sliderBuyOrderAmountStateChanged(ChangeEvent e) {
        double balance = getCurrentSecondaryBalance(), price = (Double) spinnerBuyOrderPrice.getValue(), percent = sliderBuyOrderAmount.getValue();
        if (price > 0 && percent > 0 && balance > 0) {
            double amount = (balance / price) * (percent * 1.0 / 100.0) / ((100.0 + feePercent) / 100.0);
            spinnerBuyOrderAmount.setValue(amount);
        }
    }

    private void sliderSellOrderAmountStateChanged(ChangeEvent e) {
        double balance = getCurrentPrimaryBalance(), percent = sliderSellOrderAmount.getValue();
        double amount = (balance * percent * 1.0 / 100.0);
        spinnerSellOrderAmount.setValue(amount);
    }

    protected void clearMarketData() {
        needPriceChangeUpdate = true;
        needStepUpdate = true;
        ((DefaultTableModel) tableAccountHistory.getModel()).setRowCount(0);
        ((DefaultTableModel) tableMarketHistory.getModel()).setRowCount(0);
        ((DefaultTableModel) tableBuyOrders.getModel()).setRowCount(0);
        ((DefaultTableModel) tableSellOrders.getModel()).setRowCount(0);
        ((DefaultTableModel) tableOpenOrders.getModel()).setRowCount(0);
        spinnerBuyOrderPrice.setValue(0.0);
        spinnerSellOrderPrice.setValue(0.0);
        textFieldAvgPrice.setText("");
        textFieldBuyPrice.setText("");
        textFieldSellPrice.setText("");
        textFieldHighPrice.setText("");
        textFieldLowPrice.setText("");
        textFieldVolume.setText("");
    }

    public void initMarket(AccountManager.Account account) {
        initialized = false;
        worker.stopAllThreads();
        worker.initTradeApiInstance(account);
        try {
            pairList = worker.tradeApi.getCurrencyPairs().makeNameInfoMap();
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (Map.Entry<String, BaseTradeApi.StandartObjects.CurrencyPair> entry : pairList.entrySet()) {
                model.addElement(entry.getKey());
            }
            comboBoxPair.setModel(model);
            comboBoxPair.setSelectedIndex(0);
            worker.setPair(((TreeMap<String, BaseTradeApi.StandartObjects.CurrencyPair>) pairList).firstEntry().getValue().pairId);
            clearMarketData();
            worker.setTimeInterval((Integer)spinnerUpdateInterval.getValue());
            feePercent = worker.tradeApi.getFeePercent(worker.getPair());
            needStepUpdate = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // GUI MVC functions
    private void setPrice(final JTextField field, double newValue) {
        String oldValue = field.getText();
        if(oldValue.equals("")) {
            field.setText(Utils.Strings.formatNumber(newValue));
            field.setBackground(new Color(240, 240, 240)); // Default color
        } else {
            double oldPrice = Double.parseDouble(oldValue);
            if(oldPrice != newValue) {
                field.setText(Utils.Strings.formatNumber(newValue));
                field.setBackground(newValue > oldPrice ? Color.GREEN : Color.RED);
            }
        }
    }
    private void updatePrices(final BaseTradeApi.StandartObjects.Prices price) {
        lastUpdated.priceUpdated = System.currentTimeMillis();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setPrice(textFieldLastPrice, price.last);
                setPrice(textFieldLowPrice, price.low);
                setPrice(textFieldHighPrice, price.high);
                setPrice(textFieldAvgPrice, price.average);
                setPrice(textFieldBuyPrice, price.buy);
                setPrice(textFieldSellPrice, price.sell);
                setPrice(textFieldVolume, price.volume);

                if(needStepUpdate) {
                    if (spinnerBuyOrderPrice.getValue().equals(0.0)) {
                        spinnerBuyOrderPrice.setValue(price.buy);
                    }
                    if (spinnerSellOrderPrice.getValue().equals(0.0)) {
                        spinnerSellOrderPrice.setValue(price.sell);
                    }
                    double stepSize = price.buy / 100.0;
                    if (stepSize < 0.00000001) stepSize = 0.00000001;
                    ((SpinnerNumberModel) spinnerBuyOrderPrice.getModel()).setStepSize(stepSize);
                    ((SpinnerNumberModel) spinnerBuyOrderAmount.getModel()).setStepSize(getCurrentSecondaryBalance() * 0.01 / price.buy);

                    stepSize = price.sell / 100.0;
                    if (stepSize < 0.00000001) stepSize = 0.00000001;
                    ((SpinnerNumberModel) spinnerSellOrderPrice.getModel()).setStepSize(stepSize);
                    ((SpinnerNumberModel) spinnerSellOrderAmount.getModel()).setStepSize(getCurrentPrimaryBalance() * 0.01);
                    updateLabelTotal();
                    needStepUpdate = false;
                }

                if(needPriceChangeUpdate) {
                    priceChangeLastPrice = price.last;
                    labelPriceChangePercent.setText("0%");
                    needPriceChangeUpdate = false;
                } else {
                    double percent = (price.last - priceChangeLastPrice) / priceChangeLastPrice * 100.0;
                    labelPriceChangePercent.setText(Utils.Strings.formatNumber(percent, "####.##") + "%");
                    if(percent > 0)
                        labelPriceChangePercent.setForeground(Color.GREEN);
                    else if(percent < 0)
                        labelPriceChangePercent.setForeground(Color.RED);
                    else
                        labelPriceChangePercent.setForeground(Color.BLACK);
                }
            }
        });
    }

    private void updateDepth(final BaseTradeApi.StandartObjects.Depth depth) {
        lastUpdated.depthUpdated = System.currentTimeMillis();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DefaultTableModel model = (DefaultTableModel) tableBuyOrders.getModel();
                model.setRowCount(depth.buyOrders.size());
                int i = 0;
                for (BaseTradeApi.StandartObjects.Order order : depth.buyOrders) {
                    model.setValueAt(Utils.Strings.formatNumber(order.price), i, 0);
                    model.setValueAt(Utils.Strings.formatNumber(order.amount), i, 1);
                    i++;
                }
                model = (DefaultTableModel) tableSellOrders.getModel();
                model.setRowCount(depth.sellOrders.size());
                i = 0;
                for (BaseTradeApi.StandartObjects.Order order : depth.sellOrders) {
                    model.setValueAt(Utils.Strings.formatNumber(order.price), i, 0);
                    model.setValueAt(Utils.Strings.formatNumber(order.amount), i, 1);
                    i++;
                }
            }
        });
    }

    private void updateMarketHistory(final List<BaseTradeApi.StandartObjects.Order> history) {
        lastUpdated.marketHistoryUpdated = System.currentTimeMillis();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                DefaultTableModel model = (DefaultTableModel) tableMarketHistory.getModel();
                model.setRowCount(history.size());
                for (BaseTradeApi.StandartObjects.Order order : history) {
                    model.setValueAt(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(order.time), i, 0);
                    model.setValueAt(locale.getString(order.type == BaseTradeApi.Constants.ORDER_SELL ? "sell.text" : "buy.text"), i, 1);
                    model.setValueAt(Utils.Strings.formatNumber(order.price), i, 2);
                    model.setValueAt(Utils.Strings.formatNumber(order.amount), i, 3);
                    model.setValueAt(Utils.Strings.formatNumber(order.amount * order.price / ((100.0 + feePercent) / 100.0)) + " " + getCurrentSecondaryCurrency(), i, 4);
                    i++;
                }
            }
        });
    }

    private void updateAccountHistory(final List<BaseTradeApi.StandartObjects.Order> history) {
        lastUpdated.accountHistoryUpdated = System.currentTimeMillis();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                DefaultTableModel model = (DefaultTableModel) tableAccountHistory.getModel();
                model.setRowCount(history.size());
                for (BaseTradeApi.StandartObjects.Order order : history) {
                    model.setValueAt(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(order.time), i, 0);
                    model.setValueAt(locale.getString(order.type == BaseTradeApi.Constants.ORDER_SELL ? "sell.text" : "buy.text"), i, 1);
                    model.setValueAt(Utils.Strings.formatNumber(order.price), i, 2);
                    model.setValueAt(Utils.Strings.formatNumber(order.amount), i, 3);
                    model.setValueAt(Utils.Strings.formatNumber(order.amount * order.price / ((100.0 + feePercent) / 100.0)) + " " + getCurrentSecondaryCurrency(), i, 4);
                    i++;
                }
            }
        });
    }

    private void updateOpenOrders(final List<BaseTradeApi.StandartObjects.Order> orders) {
        lastUpdated.ordersUpdated = System.currentTimeMillis();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                DefaultTableModel model = (DefaultTableModel) tableOpenOrders.getModel();
                model.setRowCount(orders.size());
                for (BaseTradeApi.StandartObjects.Order order : orders) {
                    model.setValueAt(order.id, i, 0); // ID
                    model.setValueAt(locale.getString(order.type == BaseTradeApi.Constants.ORDER_SELL ? "sell.text" : "buy.text"), i, 1); // Type
                    model.setValueAt(Utils.Strings.formatNumber(order.price), i, 2); // Price
                    model.setValueAt(Utils.Strings.formatNumber(order.amount), i, 3); // Amount
                    model.setValueAt(Utils.Strings.formatNumber(order.amount * order.price / ((100.0 + feePercent) / 100.0)) + " " + getCurrentSecondaryCurrency(), i, 4); // Total
                    i++;
                }
            }
        });
    }

    private void updateAccountBalances(final Map<String, Double> balances) {
        lastUpdated.balancesUpdated = System.currentTimeMillis();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                DefaultTableModel model = (DefaultTableModel) tableBalances.getModel();
                model.setRowCount(balances.size());
                for (Map.Entry<String, Double> balance : balances.entrySet()) {
                    model.setValueAt(balance.getKey(), i, 0); // Currency name
                    model.setValueAt(Utils.Strings.formatNumber(balance.getValue()), i, 1); // Amount
                    i++;
                }
            }
        });
    }


    public void fillGui(ApiWorker.ApiDataType dataType, Object data) {
        switch (dataType) {
            case MARKET_PRICES:
                updatePrices((BaseTradeApi.StandartObjects.Prices) data);
                break;
            case MARKET_DEPTH:
                updateDepth((BaseTradeApi.StandartObjects.Depth) data);
                break;
            case MARKET_HISTORY:
                updateMarketHistory((List<BaseTradeApi.StandartObjects.Order>) data);
                break;
            case ACCOUNT_BALANCES:
                updateAccountBalances((Map<String, Double>) data);
                break;
            case ACCOUNT_ORDERS:
                updateOpenOrders((List<BaseTradeApi.StandartObjects.Order>) data);
                break;
            case ACCOUNT_HISTORY:
                updateAccountHistory((List<BaseTradeApi.StandartObjects.Order>) data);
                break;
            default:
                throw new IllegalArgumentException();
        }
        revalidate();
    }

    public TraderMainForm(AccountManager.Account account) {
        initComponents();

        spinnerBuyOrderPrice.setEditor(new JSpinner.NumberEditor(spinnerBuyOrderPrice, "##############.########"));
        spinnerBuyOrderAmount.setEditor(new JSpinner.NumberEditor(spinnerBuyOrderAmount, "##############.########"));
        spinnerSellOrderPrice.setEditor(new JSpinner.NumberEditor(spinnerSellOrderPrice, "##############.########"));
        spinnerSellOrderAmount.setEditor(new JSpinner.NumberEditor(spinnerSellOrderAmount, "##############.########"));

        worker.setCallBack(new ApiWorker.Callback() {
            @Override
            public void onUpdate(final ApiWorker.ApiDataType dataType, final Object data) {
                fillGui(dataType, data);
            }

            @Override
            public void onError(Exception e) {
                processException(e);
            }
        });
        initMarket(account);

        apiLagThread = new Thread(new Runnable() {
            @Override public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            double apiLag = (System.currentTimeMillis() - lastUpdated.priceUpdated) / 1000.0;
                            textFieldApiLag.setText(Utils.Strings.formatNumber(apiLag) + " sec");
                        }
                    });
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        priceChangeTimerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    needPriceChangeUpdate = true;
                    try {
                        Thread.sleep(1000 * 15 * 60);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });

        apiLagThread.start();
        priceChangeTimerThread.start();
    }

    protected void setUpdateOptions() {
        if(!initialized) return;
        worker.setActiveThreads(new ApiWorker.ApiDataType[] { ApiWorker.ApiDataType.MARKET_PRICES, ApiWorker.ApiDataType.MARKET_DEPTH, ApiWorker.ApiDataType.MARKET_HISTORY, ApiWorker.ApiDataType.ACCOUNT_BALANCES, ApiWorker.ApiDataType.ACCOUNT_ORDERS, ApiWorker.ApiDataType.ACCOUNT_HISTORY }); // Update all
        /* switch (tabbedPaneInfo.getSelectedIndex()) {
            case 0: // Orders
                worker.setActiveThreads(new ApiWorker.ApiDataType[]{ApiWorker.ApiDataType.MARKET_PRICES, ApiWorker.ApiDataType.ACCOUNT_ORDERS});
                break;
            case 1: // Balances
                worker.setActiveThreads(new ApiWorker.ApiDataType[]{ApiWorker.ApiDataType.MARKET_PRICES, ApiWorker.ApiDataType.ACCOUNT_BALANCES});
                break;
            case 2: // Depth
                worker.setActiveThreads(new ApiWorker.ApiDataType[]{ApiWorker.ApiDataType.MARKET_PRICES, ApiWorker.ApiDataType.MARKET_DEPTH});
                break;
            case 3: // History
                if (panelHistory.getSelectedIndex() == 0)
                    worker.setActiveThreads(new ApiWorker.ApiDataType[]{ApiWorker.ApiDataType.MARKET_PRICES, ApiWorker.ApiDataType.ACCOUNT_HISTORY});
                else
                    worker.setActiveThreads(new ApiWorker.ApiDataType[]{ApiWorker.ApiDataType.MARKET_PRICES, ApiWorker.ApiDataType.MARKET_HISTORY});
                break;
            default: // Other tabs
                worker.setActiveThreads(new ApiWorker.ApiDataType[]{ApiWorker.ApiDataType.MARKET_PRICES});
        } */
    }

    private void tabbedPaneInfoStateChanged(ChangeEvent e) {
        setUpdateOptions();
    }

    private void panelHistoryStateChanged(ChangeEvent e) {
        setUpdateOptions();
    }

    private void comboBoxPairActionPerformed(ActionEvent e) {
        setPair((String) comboBoxPair.getSelectedItem());
    }

    private void buttonApplySettingsActionPerformed(ActionEvent e) {
        setSettings((Double) spinnerFeePercent.getValue(), (Integer) spinnerUpdateInterval.getValue());
        buttonApplySettings.setEnabled(false);
    }

    private void settingsChanged(ChangeEvent e) {
        buttonApplySettings.setEnabled(true);
    }

    private void spinnerPriceOrAmountStateChanged(ChangeEvent e) {
        updateLabelTotal();
    }

    private void buttonCommitBuyOrderActionPerformed(ActionEvent e) {
        final String orderRepresentation = String.format(locale.getString("order_representation.template"), locale.getString("buy.text"), Utils.Strings.formatNumber(spinnerBuyOrderAmount.getValue()), getCurrentPrimaryCurrency(), Utils.Strings.formatNumber(spinnerBuyOrderPrice.getValue()), getCurrentSecondaryCurrency());

        if (JOptionPane.showConfirmDialog(this, String.format(locale.getString("order_confirmation.template"), orderRepresentation), locale.getString("order_confirmation.caption"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonCommitBuyOrder.setEnabled(false);
                    try {
                        long orderId = worker.tradeApi.createOrder(pairList.get(comboBoxPair.getSelectedItem()).pairId, BaseTradeApi.Constants.ORDER_BUY, (Double) spinnerBuyOrderAmount.getValue(), (Double) spinnerBuyOrderPrice.getValue());
                        if (orderId != 0) {
                            processNotification(String.format(locale.getString("order_created.template"), orderRepresentation, orderId));
                        }
                    } catch (Exception e1) {
                        processException(e1);
                    } finally {
                        buttonCommitBuyOrder.setEnabled(true);
                    }
                }
            });
        }
    }

    private void buttonCommitSellOrderActionPerformed(ActionEvent e) {
        final String orderRepresentation = String.format(locale.getString("order_representation.template"), locale.getString("sell.text"), Utils.Strings.formatNumber(spinnerSellOrderAmount.getValue()), getCurrentPrimaryCurrency(), Utils.Strings.formatNumber(spinnerSellOrderPrice.getValue()), getCurrentSecondaryCurrency());


        if (JOptionPane.showConfirmDialog(this, String.format(locale.getString("order_confirmation.template"), orderRepresentation), locale.getString("order_confirmation.caption"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        buttonCommitSellOrder.setEnabled(false);
                        long orderId = worker.tradeApi.createOrder(pairList.get(comboBoxPair.getSelectedItem()).pairId, BaseTradeApi.Constants.ORDER_SELL, (Double) spinnerSellOrderAmount.getValue(), (Double) spinnerSellOrderPrice.getValue());
                        if (orderId != 0) {
                            processNotification(String.format(locale.getString("order_created.template"), orderRepresentation, orderId));
                        }
                    } catch (Exception e1) {
                        processException(e1);
                    } finally {
                        buttonCommitSellOrder.setEnabled(true);
                    }
                }
            });
        }
    }

    private void tableOpenOrdersPopupMenuHandler(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popupMenuOrders.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void menuItemCancelOrderActionPerformed(ActionEvent e) {
        if (JOptionPane.showConfirmDialog(this, String.format(locale.getString("order_cancel_confirmation.template"), tableOpenOrders.getValueAt(tableOpenOrders.getSelectedRow(), 0)), locale.getString("order_cancel_confirmation.caption"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    tableOpenOrders.setEnabled(false);
                    try {
                        worker.tradeApi.cancelOrder((Long) tableOpenOrders.getValueAt(tableOpenOrders.getSelectedRow(), 0));
                        processNotification(String.format(locale.getString("order_cancelled.template"), tableOpenOrders.getValueAt(tableOpenOrders.getSelectedRow(), 0)));
                        ((DefaultTableModel) tableOpenOrders.getModel()).removeRow(tableOpenOrders.getSelectedRow());
                    } catch (Exception e1) {
                        processException(e1);
                    } finally {
                        tableOpenOrders.setEnabled(true);
                    }
                }
            });

        }
    }

    private void buttonBuyOrderPriceSetCurrentActionPerformed(ActionEvent e) {
        spinnerBuyOrderPrice.setValue(worker.marketInfo.price.buy);
    }

    private void buttonSellOrderPriceSetCurrentActionPerformed(ActionEvent e) {
        spinnerSellOrderPrice.setValue(worker.marketInfo.price.sell);
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.archean.jtradegui.locale", new UTF8Control());
        DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
        comboBoxPair = new JComboBox();
        separator3 = new JSeparator();
        panelPrice = new JPanel();
        labelBuyPrice = new JLabel();
        labelLastPrice = new JLabel();
        labelSellPrice = new JLabel();
        textFieldBuyPrice = new JTextField();
        textFieldLastPrice = new JTextField();
        textFieldSellPrice = new JTextField();
        labelLowPrice = new JLabel();
        labelPriceChange = new JLabel();
        labelHighPrice = new JLabel();
        textFieldLowPrice = new JTextField();
        labelPriceChangePercent = new JLabel();
        textFieldHighPrice = new JTextField();
        labelAvgPrice = new JLabel();
        labelApiLag = new JLabel();
        labelVolume = new JLabel();
        textFieldAvgPrice = new JTextField();
        textFieldApiLag = new JTextField();
        textFieldVolume = new JTextField();
        separator1 = new JSeparator();
        tabbedPaneTrade = new JTabbedPane();
        panelBuy = new JPanel();
        labelBuyOrderPrice = new JLabel();
        spinnerBuyOrderPrice = new JSpinner();
        buttonBuyOrderPriceSetCurrent = new JButton();
        buttonCommitBuyOrder = new JButton();
        labelBuyOrderAmount = new JLabel();
        spinnerBuyOrderAmount = new JSpinner();
        sliderBuyOrderAmount = new JSlider();
        labelBuyOrderTotal = new JLabel();
        labelBuyOrderTotalValue = new JLabel();
        panelSell = new JPanel();
        labelSellOrderPrice = new JLabel();
        spinnerSellOrderPrice = new JSpinner();
        buttonSellOrderPriceSetCurrent = new JButton();
        buttonCommitSellOrder = new JButton();
        labelSellOrderAmount = new JLabel();
        spinnerSellOrderAmount = new JSpinner();
        sliderSellOrderAmount = new JSlider();
        labelSellOrderTotal = new JLabel();
        labelSellOrderTotalValue = new JLabel();
        separator2 = new JSeparator();
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
        panelHistory = new JTabbedPane();
        panelMyTrades = new JPanel();
        scrollPane6 = new JScrollPane();
        tableAccountHistory = new JTable();
        panelMarketTrades = new JPanel();
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
        popupMenuOrders = new JPopupMenu();
        menuItemCancelOrder = new JMenuItem();

        //======== this ========
        setLayout(new FormLayout(
            "[90dlu,pref]:grow, $lcgap, [92dlu,pref]:grow",
            "default, 10dlu, top:[95dlu,default], 10dlu, default, 0dlu, 10dlu, 15dlu, $lgap, fill:[150dlu,default]:grow"));

        //---- comboBoxPair ----
        comboBoxPair.setMaximumRowCount(20);
        comboBoxPair.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                comboBoxPairActionPerformed(e);
            }
        });
        add(comboBoxPair, CC.xywh(1, 1, 3, 1));
        add(separator3, CC.xywh(1, 2, 3, 1));

        //======== panelPrice ========
        {
            panelPrice.setLayout(new FormLayout(
                "default:grow, $lcgap, 70dlu, $lcgap, default:grow",
                "3*(default, $lgap), [18dlu,default], 2*($lgap, default)"));

            //---- labelBuyPrice ----
            labelBuyPrice.setText(bundle.getString("TraderMainForm.labelBuyPrice.text"));
            labelBuyPrice.setFont(labelBuyPrice.getFont().deriveFont(labelBuyPrice.getFont().getStyle() | Font.BOLD));
            labelBuyPrice.setLabelFor(textFieldBuyPrice);
            panelPrice.add(labelBuyPrice, CC.xy(1, 1, CC.CENTER, CC.DEFAULT));

            //---- labelLastPrice ----
            labelLastPrice.setText(bundle.getString("TraderMainForm.labelLastPrice.text"));
            labelLastPrice.setLabelFor(textFieldLastPrice);
            labelLastPrice.setFont(labelLastPrice.getFont().deriveFont(labelLastPrice.getFont().getStyle() | Font.BOLD));
            panelPrice.add(labelLastPrice, CC.xy(3, 1, CC.CENTER, CC.DEFAULT));

            //---- labelSellPrice ----
            labelSellPrice.setText(bundle.getString("TraderMainForm.labelSellPrice.text"));
            labelSellPrice.setLabelFor(textFieldSellPrice);
            labelSellPrice.setFont(labelSellPrice.getFont().deriveFont(Font.BOLD));
            panelPrice.add(labelSellPrice, CC.xy(5, 1, CC.CENTER, CC.DEFAULT));

            //---- textFieldBuyPrice ----
            textFieldBuyPrice.setEditable(false);
            panelPrice.add(textFieldBuyPrice, CC.xy(1, 3, CC.CENTER, CC.DEFAULT));

            //---- textFieldLastPrice ----
            textFieldLastPrice.setEditable(false);
            panelPrice.add(textFieldLastPrice, CC.xy(3, 3, CC.CENTER, CC.DEFAULT));

            //---- textFieldSellPrice ----
            textFieldSellPrice.setEditable(false);
            panelPrice.add(textFieldSellPrice, CC.xy(5, 3, CC.CENTER, CC.DEFAULT));

            //---- labelLowPrice ----
            labelLowPrice.setText(bundle.getString("TraderMainForm.labelLowPrice.text"));
            labelLowPrice.setFont(labelLowPrice.getFont().deriveFont(labelLowPrice.getFont().getStyle() | Font.BOLD));
            labelLowPrice.setLabelFor(textFieldLowPrice);
            panelPrice.add(labelLowPrice, CC.xy(1, 5, CC.CENTER, CC.DEFAULT));

            //---- labelPriceChange ----
            labelPriceChange.setText(bundle.getString("TraderMainForm.labelPriceChange.text"));
            labelPriceChange.setFont(labelPriceChange.getFont().deriveFont(Font.BOLD|Font.ITALIC));
            panelPrice.add(labelPriceChange, CC.xy(3, 5, CC.CENTER, CC.DEFAULT));

            //---- labelHighPrice ----
            labelHighPrice.setText(bundle.getString("TraderMainForm.labelHighPrice.text"));
            labelHighPrice.setFont(labelHighPrice.getFont().deriveFont(labelHighPrice.getFont().getStyle() | Font.BOLD));
            labelHighPrice.setLabelFor(textFieldHighPrice);
            panelPrice.add(labelHighPrice, CC.xy(5, 5, CC.CENTER, CC.DEFAULT));

            //---- textFieldLowPrice ----
            textFieldLowPrice.setEditable(false);
            panelPrice.add(textFieldLowPrice, CC.xy(1, 7, CC.CENTER, CC.DEFAULT));

            //---- labelPriceChangePercent ----
            labelPriceChangePercent.setText("0%");
            panelPrice.add(labelPriceChangePercent, CC.xy(3, 7, CC.CENTER, CC.DEFAULT));

            //---- textFieldHighPrice ----
            textFieldHighPrice.setEditable(false);
            panelPrice.add(textFieldHighPrice, CC.xy(5, 7, CC.CENTER, CC.DEFAULT));

            //---- labelAvgPrice ----
            labelAvgPrice.setText(bundle.getString("TraderMainForm.labelAvgPrice.text"));
            labelAvgPrice.setFont(labelAvgPrice.getFont().deriveFont(labelAvgPrice.getFont().getStyle() | Font.BOLD));
            labelAvgPrice.setLabelFor(textFieldAvgPrice);
            panelPrice.add(labelAvgPrice, CC.xy(1, 9, CC.CENTER, CC.DEFAULT));

            //---- labelApiLag ----
            labelApiLag.setText(bundle.getString("TraderMainForm.labelApiLag.text"));
            labelApiLag.setLabelFor(textFieldApiLag);
            labelApiLag.setFont(labelApiLag.getFont().deriveFont(Font.ITALIC));
            panelPrice.add(labelApiLag, CC.xy(3, 9, CC.CENTER, CC.DEFAULT));

            //---- labelVolume ----
            labelVolume.setText(bundle.getString("TraderMainForm.labelVolume.text"));
            labelVolume.setFont(labelVolume.getFont().deriveFont(labelVolume.getFont().getStyle() | Font.BOLD));
            labelVolume.setLabelFor(textFieldVolume);
            panelPrice.add(labelVolume, CC.xy(5, 9, CC.CENTER, CC.DEFAULT));

            //---- textFieldAvgPrice ----
            textFieldAvgPrice.setEditable(false);
            panelPrice.add(textFieldAvgPrice, CC.xy(1, 11, CC.CENTER, CC.DEFAULT));

            //---- textFieldApiLag ----
            textFieldApiLag.setEditable(false);
            textFieldApiLag.setHorizontalAlignment(SwingConstants.TRAILING);
            panelPrice.add(textFieldApiLag, CC.xy(3, 11, CC.FILL, CC.DEFAULT));

            //---- textFieldVolume ----
            textFieldVolume.setEditable(false);
            panelPrice.add(textFieldVolume, CC.xy(5, 11, CC.CENTER, CC.DEFAULT));
        }
        add(panelPrice, CC.xywh(1, 3, 3, 1));
        add(separator1, CC.xywh(1, 4, 3, 1));

        //======== tabbedPaneTrade ========
        {

            //======== panelBuy ========
            {
                panelBuy.setLayout(new FormLayout(
                    "5dlu, $lcgap, 30dlu, $lcgap, 108dlu, $lcgap, 45dlu:grow, $lcgap, [70dlu,default]:grow",
                    "5dlu, 2*($lgap, default), $lgap, 14dlu"));

                //---- labelBuyOrderPrice ----
                labelBuyOrderPrice.setText(bundle.getString("TraderMainForm.labelBuyOrderPrice.text"));
                labelBuyOrderPrice.setLabelFor(spinnerBuyOrderPrice);
                panelBuy.add(labelBuyOrderPrice, CC.xy(3, 3, CC.LEFT, CC.DEFAULT));

                //---- spinnerBuyOrderPrice ----
                spinnerBuyOrderPrice.setModel(new SpinnerNumberModel(0.0, 0.0, null, 1.0));
                spinnerBuyOrderPrice.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        spinnerPriceOrAmountStateChanged(e);
                    }
                });
                panelBuy.add(spinnerBuyOrderPrice, CC.xy(5, 3));

                //---- buttonBuyOrderPriceSetCurrent ----
                buttonBuyOrderPriceSetCurrent.setText(bundle.getString("TraderMainForm.buttonBuyOrderPriceSetCurrent.text"));
                buttonBuyOrderPriceSetCurrent.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonBuyOrderPriceSetCurrentActionPerformed(e);
                    }
                });
                panelBuy.add(buttonBuyOrderPriceSetCurrent, CC.xy(7, 3, CC.FILL, CC.DEFAULT));

                //---- buttonCommitBuyOrder ----
                buttonCommitBuyOrder.setText(bundle.getString("buy.text"));
                buttonCommitBuyOrder.setFont(buttonCommitBuyOrder.getFont().deriveFont(buttonCommitBuyOrder.getFont().getStyle() | Font.BOLD));
                buttonCommitBuyOrder.setBackground(new Color(45, 195, 22));
                buttonCommitBuyOrder.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonCommitBuyOrderActionPerformed(e);
                    }
                });
                panelBuy.add(buttonCommitBuyOrder, CC.xy(9, 3, CC.FILL, CC.DEFAULT));

                //---- labelBuyOrderAmount ----
                labelBuyOrderAmount.setText(bundle.getString("TraderMainForm.labelBuyOrderAmount.text"));
                labelBuyOrderAmount.setLabelFor(spinnerBuyOrderAmount);
                panelBuy.add(labelBuyOrderAmount, CC.xy(3, 5, CC.LEFT, CC.DEFAULT));

                //---- spinnerBuyOrderAmount ----
                spinnerBuyOrderAmount.setModel(new SpinnerNumberModel(0.0, 0.0, null, 0.01));
                spinnerBuyOrderAmount.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        spinnerPriceOrAmountStateChanged(e);
                    }
                });
                panelBuy.add(spinnerBuyOrderAmount, CC.xy(5, 5));

                //---- sliderBuyOrderAmount ----
                sliderBuyOrderAmount.setValue(0);
                sliderBuyOrderAmount.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        sliderBuyOrderAmountStateChanged(e);
                    }
                });
                panelBuy.add(sliderBuyOrderAmount, CC.xywh(7, 5, 3, 1, CC.FILL, CC.DEFAULT));

                //---- labelBuyOrderTotal ----
                labelBuyOrderTotal.setText(bundle.getString("TraderMainForm.labelBuyOrderTotal.text"));
                panelBuy.add(labelBuyOrderTotal, CC.xy(3, 7, CC.LEFT, CC.DEFAULT));

                //---- labelBuyOrderTotalValue ----
                labelBuyOrderTotalValue.setText("0 / 0");
                panelBuy.add(labelBuyOrderTotalValue, CC.xywh(5, 7, 5, 1, CC.LEFT, CC.DEFAULT));
            }
            tabbedPaneTrade.addTab(bundle.getString("buy.text"), panelBuy);

            //======== panelSell ========
            {
                panelSell.setLayout(new FormLayout(
                    "5dlu, $lcgap, 30dlu, $lcgap, 108dlu, $lcgap, 45dlu:grow, $lcgap, [70dlu,default]:grow",
                    "5dlu, 2*($lgap, default), $lgap, 14dlu"));

                //---- labelSellOrderPrice ----
                labelSellOrderPrice.setText(bundle.getString("TraderMainForm.labelSellOrderPrice.text"));
                labelSellOrderPrice.setLabelFor(spinnerSellOrderPrice);
                panelSell.add(labelSellOrderPrice, CC.xy(3, 3, CC.LEFT, CC.DEFAULT));

                //---- spinnerSellOrderPrice ----
                spinnerSellOrderPrice.setModel(new SpinnerNumberModel(0.0, 0.0, null, 1.0));
                spinnerSellOrderPrice.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        spinnerPriceOrAmountStateChanged(e);
                    }
                });
                panelSell.add(spinnerSellOrderPrice, CC.xy(5, 3));

                //---- buttonSellOrderPriceSetCurrent ----
                buttonSellOrderPriceSetCurrent.setText(bundle.getString("TraderMainForm.buttonSellOrderPriceSetCurrent.text"));
                buttonSellOrderPriceSetCurrent.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonSellOrderPriceSetCurrentActionPerformed(e);
                    }
                });
                panelSell.add(buttonSellOrderPriceSetCurrent, CC.xy(7, 3, CC.FILL, CC.DEFAULT));

                //---- buttonCommitSellOrder ----
                buttonCommitSellOrder.setText(bundle.getString("sell.text"));
                buttonCommitSellOrder.setBackground(new Color(255, 81, 81));
                buttonCommitSellOrder.setFont(buttonCommitSellOrder.getFont().deriveFont(buttonCommitSellOrder.getFont().getStyle() | Font.BOLD));
                buttonCommitSellOrder.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonCommitSellOrderActionPerformed(e);
                    }
                });
                panelSell.add(buttonCommitSellOrder, CC.xy(9, 3, CC.FILL, CC.DEFAULT));

                //---- labelSellOrderAmount ----
                labelSellOrderAmount.setText(bundle.getString("TraderMainForm.labelSellOrderAmount.text"));
                labelSellOrderAmount.setLabelFor(spinnerSellOrderAmount);
                panelSell.add(labelSellOrderAmount, CC.xy(3, 5, CC.LEFT, CC.DEFAULT));

                //---- spinnerSellOrderAmount ----
                spinnerSellOrderAmount.setModel(new SpinnerNumberModel(0.0, 0.0, null, 0.01));
                spinnerSellOrderAmount.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        spinnerPriceOrAmountStateChanged(e);
                    }
                });
                panelSell.add(spinnerSellOrderAmount, CC.xy(5, 5));

                //---- sliderSellOrderAmount ----
                sliderSellOrderAmount.setValue(0);
                sliderSellOrderAmount.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        sliderSellOrderAmountStateChanged(e);
                    }
                });
                panelSell.add(sliderSellOrderAmount, CC.xywh(7, 5, 3, 1, CC.FILL, CC.DEFAULT));

                //---- labelSellOrderTotal ----
                labelSellOrderTotal.setText(bundle.getString("TraderMainForm.labelSellOrderTotal.text"));
                panelSell.add(labelSellOrderTotal, CC.xy(3, 7, CC.LEFT, CC.DEFAULT));

                //---- labelSellOrderTotalValue ----
                labelSellOrderTotalValue.setText("0 / 0");
                panelSell.add(labelSellOrderTotalValue, CC.xywh(5, 7, 5, 1, CC.LEFT, CC.DEFAULT));
            }
            tabbedPaneTrade.addTab(bundle.getString("sell.text"), panelSell);
        }
        add(tabbedPaneTrade, CC.xywh(1, 5, 3, 1));
        add(separator2, CC.xywh(1, 7, 3, 1));

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
                        },
                        new String[] {
                            "#", "Type", "Price", "Amount", "Total"
                        }
                    ) {
                        Class<?>[] columnTypes = new Class<?>[] {
                            Object.class, String.class, Object.class, Object.class, Object.class
                        };
                        boolean[] columnEditable = new boolean[] {
                            true, false, false, false, false
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
                    tableOpenOrders.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            tableOpenOrdersPopupMenuHandler(e);
                        }
                        @Override
                        public void mouseReleased(MouseEvent e) {
                            tableOpenOrdersPopupMenuHandler(e);
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
                        },
                        new String[] {
                            "Currency", "Balance"
                        }
                    ) {
                        Class<?>[] columnTypes = new Class<?>[] {
                            String.class, Object.class
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
                    tableBalances.setAutoCreateRowSorter(true);
                    tableBalances.setCellSelectionEnabled(true);
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
                        },
                        new String[] {
                            "Price", "Amount"
                        }
                    ) {
                        boolean[] columnEditable = new boolean[] {
                            false, false
                        };
                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                            return columnEditable[columnIndex];
                        }
                    });
                    tableBuyOrders.setAutoCreateRowSorter(true);
                    tableBuyOrders.setCellSelectionEnabled(true);
                    scrollPaneBuyOrders.setViewportView(tableBuyOrders);
                }
                panelDepth.add(scrollPaneBuyOrders, CC.xy(1, 3));

                //======== scrollPaneSellOrders ========
                {

                    //---- tableSellOrders ----
                    tableSellOrders.setModel(new DefaultTableModel(
                        new Object[][] {
                        },
                        new String[] {
                            "Price", "Amount"
                        }
                    ) {
                        boolean[] columnEditable = new boolean[] {
                            false, false
                        };
                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                            return columnEditable[columnIndex];
                        }
                    });
                    tableSellOrders.setAutoCreateRowSorter(true);
                    scrollPaneSellOrders.setViewportView(tableSellOrders);
                }
                panelDepth.add(scrollPaneSellOrders, CC.xy(3, 3));
            }
            tabbedPaneInfo.addTab(bundle.getString("TraderMainForm.panelDepth.tab.title"), panelDepth);

            //======== panelHistory ========
            {
                panelHistory.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        panelHistoryStateChanged(e);
                    }
                });

                //======== panelMyTrades ========
                {
                    panelMyTrades.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                    //======== scrollPane6 ========
                    {

                        //---- tableAccountHistory ----
                        tableAccountHistory.setModel(new DefaultTableModel(
                            new Object[][] {
                            },
                            new String[] {
                                "Time", "Type", "Price", "Amount", "Total"
                            }
                        ) {
                            Class<?>[] columnTypes = new Class<?>[] {
                                Object.class, String.class, Object.class, Object.class, Object.class
                            };
                            boolean[] columnEditable = new boolean[] {
                                false, false, false, false, false
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
                        tableAccountHistory.setAutoCreateRowSorter(true);
                        scrollPane6.setViewportView(tableAccountHistory);
                    }
                    panelMyTrades.add(scrollPane6, CC.xy(1, 1));
                }
                panelHistory.addTab(bundle.getString("TraderMainForm.panelMyTrades.tab.title"), panelMyTrades);

                //======== panelMarketTrades ========
                {
                    panelMarketTrades.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                    //======== scrollPane7 ========
                    {

                        //---- tableMarketHistory ----
                        tableMarketHistory.setModel(new DefaultTableModel(
                            new Object[][] {
                            },
                            new String[] {
                                "Time", "Type", "Price", "Amount", "Total"
                            }
                        ) {
                            Class<?>[] columnTypes = new Class<?>[] {
                                Object.class, String.class, Object.class, Object.class, Object.class
                            };
                            boolean[] columnEditable = new boolean[] {
                                false, false, false, false, false
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
                        tableMarketHistory.setAutoCreateRowSorter(true);
                        scrollPane7.setViewportView(tableMarketHistory);
                    }
                    panelMarketTrades.add(scrollPane7, CC.xy(1, 1));
                }
                panelHistory.addTab(bundle.getString("TraderMainForm.panelMarketTrades.tab.title"), panelMarketTrades);
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
                spinnerFeePercent.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        settingsChanged(e);
                    }
                });
                panelSettings.add(spinnerFeePercent, CC.xy(5, 1));

                //---- label1 ----
                label1.setText(bundle.getString("TraderMainForm.label1.text"));
                label1.setLabelFor(spinnerUpdateInterval);
                panelSettings.add(label1, CC.xy(3, 3));

                //---- spinnerUpdateInterval ----
                spinnerUpdateInterval.setModel(new SpinnerNumberModel(100, 0, 30000, 50));
                spinnerUpdateInterval.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        settingsChanged(e);
                    }
                });
                panelSettings.add(spinnerUpdateInterval, CC.xy(5, 3));

                //---- buttonApplySettings ----
                buttonApplySettings.setText(bundle.getString("TraderMainForm.buttonApplySettings.text"));
                buttonApplySettings.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonApplySettingsActionPerformed(e);
                    }
                });
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
        add(tabbedPaneInfo, CC.xywh(1, 8, 3, 3));

        //======== popupMenuOrders ========
        {

            //---- menuItemCancelOrder ----
            menuItemCancelOrder.setText(bundle.getString("TraderMainForm.menuItemCancelOrder.text"));
            menuItemCancelOrder.setForeground(Color.red);
            menuItemCancelOrder.setIcon(new ImageIcon(getClass().getResource("/icons/delete.png")));
            menuItemCancelOrder.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    menuItemCancelOrderActionPerformed(e);
                }
            });
            popupMenuOrders.add(menuItemCancelOrder);
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox comboBoxPair;
    private JSeparator separator3;
    private JPanel panelPrice;
    private JLabel labelBuyPrice;
    private JLabel labelLastPrice;
    private JLabel labelSellPrice;
    private JTextField textFieldBuyPrice;
    private JTextField textFieldLastPrice;
    private JTextField textFieldSellPrice;
    private JLabel labelLowPrice;
    private JLabel labelPriceChange;
    private JLabel labelHighPrice;
    private JTextField textFieldLowPrice;
    private JLabel labelPriceChangePercent;
    private JTextField textFieldHighPrice;
    private JLabel labelAvgPrice;
    private JLabel labelApiLag;
    private JLabel labelVolume;
    private JTextField textFieldAvgPrice;
    private JTextField textFieldApiLag;
    private JTextField textFieldVolume;
    private JSeparator separator1;
    private JTabbedPane tabbedPaneTrade;
    private JPanel panelBuy;
    private JLabel labelBuyOrderPrice;
    private JSpinner spinnerBuyOrderPrice;
    private JButton buttonBuyOrderPriceSetCurrent;
    private JButton buttonCommitBuyOrder;
    private JLabel labelBuyOrderAmount;
    private JSpinner spinnerBuyOrderAmount;
    private JSlider sliderBuyOrderAmount;
    private JLabel labelBuyOrderTotal;
    private JLabel labelBuyOrderTotalValue;
    private JPanel panelSell;
    private JLabel labelSellOrderPrice;
    private JSpinner spinnerSellOrderPrice;
    private JButton buttonSellOrderPriceSetCurrent;
    private JButton buttonCommitSellOrder;
    private JLabel labelSellOrderAmount;
    private JSpinner spinnerSellOrderAmount;
    private JSlider sliderSellOrderAmount;
    private JLabel labelSellOrderTotal;
    private JLabel labelSellOrderTotalValue;
    private JSeparator separator2;
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
    private JTabbedPane panelHistory;
    private JPanel panelMyTrades;
    private JScrollPane scrollPane6;
    private JTable tableAccountHistory;
    private JPanel panelMarketTrades;
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
    private JPopupMenu popupMenuOrders;
    private JMenuItem menuItemCancelOrder;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
