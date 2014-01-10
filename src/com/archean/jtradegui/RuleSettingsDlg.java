/*
 * Created by JFormDesigner on Fri Jan 10 09:19:10 MSK 2014
 */

package com.archean.jtradegui;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import com.archean.jautotrading.RuleWorker;
import com.archean.jtradeapi.BaseTradeApi;
import com.archean.jtradeapi.Calculator;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import javafx.scene.control.ComboBox;

/**
 * @author Yarr harr
 */
public class RuleSettingsDlg extends JDialog {
    static class TradingRule {
        RuleWorker.BaseCondition condition;
        // RuleWorker.BaseAction action; // todo
    }
    public TradingRule result = null;

    public RuleSettingsDlg(Frame owner) {
        super(owner);
        initComponents();
    }

    public RuleSettingsDlg(Dialog owner) {
        super(owner);
        initComponents();
    }

    private void comboBoxTradePriceTypeActionPerformed(ActionEvent e) {
        spinnerTradePriceCustom.setVisible(comboBoxTradePriceType.getSelectedIndex() == 0);
    }

    private void comboBoxAmountActionPerformed(ActionEvent e) {
        SpinnerNumberModel model = (SpinnerNumberModel)spinnerAmount.getModel();
        switch(comboBoxAmount.getSelectedIndex()) {
            case 0: // Constant
                model.setMaximum(0);
                model.setStepSize(0.01);
                sliderAmount.setVisible(false);
                break;
            case 1: // % of balance
                model.setMaximum(100);
                model.setStepSize(1);
                sliderAmount.setVisible(true);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void sliderAmountStateChanged(ChangeEvent e) {
        spinnerAmount.setValue(sliderAmount.getValue());
    }

    private void cancelButtonActionPerformed(ActionEvent e) {
        this.result = null;
        dispose();
    }

    private BaseTradeApi.PriceType getPriceType() {
        switch(comboBoxPriceType.getSelectedIndex()) {
            case 0:
                return BaseTradeApi.PriceType.LAST;
            case 1:
                return BaseTradeApi.PriceType.ASK;
            case 2:
                return BaseTradeApi.PriceType.BID;
            case 3:
                return BaseTradeApi.PriceType.HIGH;
            case 4:
                return BaseTradeApi.PriceType.LOW;
            case 5:
                return BaseTradeApi.PriceType.AVG;
            default:
                throw new IllegalArgumentException();
        }
    }
    private Calculator.ArithmeticCompareCondition getCompareType(int i) {
        switch(i) {
            case 0:
                return Calculator.ArithmeticCompareCondition.EQUAL;
            case 1:
                return Calculator.ArithmeticCompareCondition.GREATER;
            case 2:
                return Calculator.ArithmeticCompareCondition.LESS;
            case 3:
                return Calculator.ArithmeticCompareCondition.GREATER_OR_EQUAL;
            case 4:
                return Calculator.ArithmeticCompareCondition.LESS_OR_EQUAL;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void okButtonActionPerformed(ActionEvent e) {
        result = new TradingRule();
        switch(tabbedPaneCondition.getSelectedIndex()) {
            case 0:
                result.condition = new RuleWorker.PriceCondition(getPriceType(), getCompareType(comboBoxPriceCompareType.getSelectedIndex()), new BigDecimal((Double)spinnerPrice.getValue(), MathContext.DECIMAL64));
                break;
        }
        switch(tabbedPaneAction.getSelectedIndex()) {
            // not implemented todo
        }
        dispose();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.archean.jtradegui.locale");
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        tabbedPaneCondition = new JTabbedPane();
        panelPriceRule = new JPanel();
        labelPriceType = new JLabel();
        comboBoxPriceType = new JComboBox<>();
        comboBoxPriceCompareType = new JComboBox<>();
        spinnerPrice = new JSpinner();
        tabbedPaneAction = new JTabbedPane();
        panelActionTrade = new JPanel();
        labelTradeType = new JLabel();
        comboBoxTradeType = new JComboBox<>();
        labelTradePriceType = new JLabel();
        comboBoxTradePriceType = new JComboBox<>();
        labelAmount = new JLabel();
        comboBoxAmount = new JComboBox<>();
        spinnerTradePriceCustom = new JSpinner();
        spinnerAmount = new JSpinner();
        sliderAmount = new JSlider();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:85dlu, fill:11dlu:grow"));

                //======== tabbedPaneCondition ========
                {
                    tabbedPaneCondition.setTabPlacement(SwingConstants.BOTTOM);
                    tabbedPaneCondition.setBorder(new TitledBorder(bundle.getString("RuleSettingsDlg.tabbedPaneCondition.border")));

                    //======== panelPriceRule ========
                    {
                        panelPriceRule.setLayout(new FormLayout(
                            "5dlu:grow, $lcgap, default, $lcgap, 81dlu, $lcgap, default:grow",
                            "3dlu, 2*($lgap, default)"));

                        //---- labelPriceType ----
                        labelPriceType.setText(bundle.getString("RuleSettingsDlg.labelPriceType.text"));
                        labelPriceType.setLabelFor(comboBoxPriceType);
                        panelPriceRule.add(labelPriceType, CC.xy(3, 3, CC.RIGHT, CC.DEFAULT));

                        //---- comboBoxPriceType ----
                        comboBoxPriceType.setModel(new DefaultComboBoxModel<>(new String[] {
                            "Last",
                            "Ask",
                            "Bid",
                            "High",
                            "Low",
                            "Avg"
                        }));
                        panelPriceRule.add(comboBoxPriceType, CC.xy(5, 3));

                        //---- comboBoxPriceCompareType ----
                        comboBoxPriceCompareType.setModel(new DefaultComboBoxModel<>(new String[] {
                            "Equal",
                            "Greater",
                            "Less",
                            "Greater or equal",
                            "Less or equal"
                        }));
                        panelPriceRule.add(comboBoxPriceCompareType, CC.xy(3, 5));

                        //---- spinnerPrice ----
                        spinnerPrice.setModel(new SpinnerNumberModel(1.0E-8, 0.0, null, 1.0));
                        panelPriceRule.add(spinnerPrice, CC.xy(5, 5));
                    }
                    tabbedPaneCondition.addTab(bundle.getString("RuleSettingsDlg.panelPriceRule.tab.title"), panelPriceRule);
                }
                contentPanel.add(tabbedPaneCondition, CC.xy(1, 1));

                //======== tabbedPaneAction ========
                {
                    tabbedPaneAction.setBorder(new TitledBorder(bundle.getString("RuleSettingsDlg.tabbedPaneAction.border")));
                    tabbedPaneAction.setTabPlacement(SwingConstants.BOTTOM);

                    //======== panelActionTrade ========
                    {
                        panelActionTrade.setLayout(new FormLayout(
                            "5dlu:grow, $lcgap, default, $lcgap, 77dlu, $lcgap, 5dlu, $lcgap, default, $lcgap, 52dlu, $lcgap, default:grow",
                            "3dlu, 5*($lgap, default)"));

                        //---- labelTradeType ----
                        labelTradeType.setText(bundle.getString("RuleSettingsDlg.labelTradeType.text"));
                        labelTradeType.setLabelFor(comboBoxTradeType);
                        panelActionTrade.add(labelTradeType, CC.xy(3, 3));

                        //---- comboBoxTradeType ----
                        comboBoxTradeType.setModel(new DefaultComboBoxModel<>(new String[] {
                            "Buy",
                            "Sell"
                        }));
                        panelActionTrade.add(comboBoxTradeType, CC.xy(5, 3));

                        //---- labelTradePriceType ----
                        labelTradePriceType.setText(bundle.getString("RuleSettingsDlg.labelTradePriceType.text"));
                        panelActionTrade.add(labelTradePriceType, CC.xy(9, 3));

                        //---- comboBoxTradePriceType ----
                        comboBoxTradePriceType.setModel(new DefaultComboBoxModel<>(new String[] {
                            "Custom",
                            "Last",
                            "Ask",
                            "Bid",
                            "High",
                            "Low",
                            "Avg"
                        }));
                        comboBoxTradePriceType.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                comboBoxTradePriceTypeActionPerformed(e);
                            }
                        });
                        panelActionTrade.add(comboBoxTradePriceType, CC.xy(11, 3));

                        //---- labelAmount ----
                        labelAmount.setText(bundle.getString("RuleSettingsDlg.labelAmount.text"));
                        labelAmount.setLabelFor(comboBoxAmount);
                        panelActionTrade.add(labelAmount, CC.xy(3, 5));

                        //---- comboBoxAmount ----
                        comboBoxAmount.setModel(new DefaultComboBoxModel<>(new String[] {
                            "Constant",
                            "% of balance"
                        }));
                        comboBoxAmount.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                comboBoxAmountActionPerformed(e);
                            }
                        });
                        panelActionTrade.add(comboBoxAmount, CC.xy(5, 5));
                        panelActionTrade.add(spinnerTradePriceCustom, CC.xywh(9, 5, 3, 1));

                        //---- spinnerAmount ----
                        spinnerAmount.setModel(new SpinnerNumberModel(0.01, 0.0, null, 0.01));
                        panelActionTrade.add(spinnerAmount, CC.xywh(3, 7, 3, 1));

                        //---- sliderAmount ----
                        sliderAmount.addChangeListener(new ChangeListener() {
                            @Override
                            public void stateChanged(ChangeEvent e) {
                                sliderAmountStateChanged(e);
                            }
                        });
                        panelActionTrade.add(sliderAmount, CC.xywh(3, 9, 3, 1));
                    }
                    tabbedPaneAction.addTab(bundle.getString("RuleSettingsDlg.panelActionTrade.tab.title"), panelActionTrade);
                }
                contentPanel.add(tabbedPaneAction, CC.xy(1, 2));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.createEmptyBorder("5dlu, 0dlu, 0dlu, 0dlu"));
                buttonBar.setLayout(new FormLayout(
                    "$glue, $button, $rgap, $button",
                    "pref"));

                //---- okButton ----
                okButton.setText(bundle.getString("RuleSettingsDlg.okButton.text"));
                okButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed(e);
                    }
                });
                buttonBar.add(okButton, CC.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText(bundle.getString("RuleSettingsDlg.cancelButton.text"));
                cancelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        cancelButtonActionPerformed(e);
                    }
                });
                buttonBar.add(cancelButton, CC.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JTabbedPane tabbedPaneCondition;
    private JPanel panelPriceRule;
    private JLabel labelPriceType;
    private JComboBox<String> comboBoxPriceType;
    private JComboBox<String> comboBoxPriceCompareType;
    private JSpinner spinnerPrice;
    private JTabbedPane tabbedPaneAction;
    private JPanel panelActionTrade;
    private JLabel labelTradeType;
    private JComboBox<String> comboBoxTradeType;
    private JLabel labelTradePriceType;
    private JComboBox<String> comboBoxTradePriceType;
    private JLabel labelAmount;
    private JComboBox<String> comboBoxAmount;
    private JSpinner spinnerTradePriceCustom;
    private JSpinner spinnerAmount;
    private JSlider sliderAmount;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}