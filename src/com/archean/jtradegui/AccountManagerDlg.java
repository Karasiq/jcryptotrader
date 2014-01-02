/*
 * Created by JFormDesigner on Thu Jan 02 13:27:33 MSK 2014
 */

package com.archean.jtradegui;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import com.archean.jtradeapi.AccountManager;
import com.archean.jtradeapi.Utils;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import org.apache.commons.io.FileUtils;

/**
 * @author Yarr harr
 */
public class AccountManagerDlg extends JDialog {
    public AccountManager.AccountDb accountDb = new AccountManager.AccountDb();
    protected final static String accountDbFile = "accounts.json";
    protected void loadDb() {
        try {
            accountDb.loadFromJson(FileUtils.readFileToString(new File(accountDbFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected void saveDb() {
        try {
            FileUtils.writeStringToFile(new File(accountDbFile), accountDb.saveToJson());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AccountManagerDlg(Frame owner) {
        super(owner);
        initComponents();
    }

    public AccountManagerDlg(Dialog owner) {
        super(owner);
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.archean.jtradegui.locale");
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPane1 = new JScrollPane();
        listAccounts = new JList();
        labelLabel = new JLabel();
        textFieldLabel = new JTextField();
        labelPublicKey = new JLabel();
        textFieldPublic = new JTextField();
        labelPrivateKey = new JLabel();
        textFieldPrivate = new JTextField();
        buttonAddAccount = new JButton();
        label1 = new JLabel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setModal(true);
        setTitle(bundle.getString("AccountManager.this.title"));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new FormLayout(
                    "75dlu, $lcgap, 41dlu, $lcgap, 110dlu, $lcgap, [55dlu,default]:grow",
                    "15dlu, 2*($lgap, default), $lgap, 25dlu, $lgap, fill:default:grow"));

                //======== scrollPane1 ========
                {
                    scrollPane1.setViewportView(listAccounts);
                }
                contentPanel.add(scrollPane1, CC.xywh(1, 1, 1, 9));

                //---- labelLabel ----
                labelLabel.setText(bundle.getString("AccountManager.labelLabel.text"));
                labelLabel.setLabelFor(textFieldLabel);
                contentPanel.add(labelLabel, CC.xy(3, 1));
                contentPanel.add(textFieldLabel, CC.xywh(5, 1, 3, 1));

                //---- labelPublicKey ----
                labelPublicKey.setText(bundle.getString("AccountManager.labelPublicKey.text"));
                labelPublicKey.setLabelFor(textFieldPublic);
                contentPanel.add(labelPublicKey, CC.xy(3, 3));
                contentPanel.add(textFieldPublic, CC.xywh(5, 3, 3, 1));

                //---- labelPrivateKey ----
                labelPrivateKey.setText(bundle.getString("AccountManager.labelPrivateKey.text"));
                labelPrivateKey.setLabelFor(textFieldPrivate);
                contentPanel.add(labelPrivateKey, CC.xy(3, 5));
                contentPanel.add(textFieldPrivate, CC.xywh(5, 5, 3, 1));

                //---- buttonAddAccount ----
                buttonAddAccount.setText(bundle.getString("AccountManager.buttonAddAccount.text"));
                contentPanel.add(buttonAddAccount, CC.xywh(3, 7, 5, 1));

                //---- label1 ----
                label1.setText(bundle.getString("AccountManager.label1.text"));
                label1.setFont(label1.getFont().deriveFont(label1.getFont().getStyle() | Font.ITALIC));
                contentPanel.add(label1, CC.xywh(3, 9, 5, 1, CC.CENTER, CC.DEFAULT));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.createEmptyBorder("5dlu, 0dlu, 0dlu, 0dlu"));
                buttonBar.setLayout(new FormLayout(
                    "$glue, $button, $rgap, $button",
                    "pref"));

                //---- okButton ----
                okButton.setText(bundle.getString("AccountManager.okButton.text"));
                buttonBar.add(okButton, CC.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText(bundle.getString("AccountManager.cancelButton.text"));
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
    private JScrollPane scrollPane1;
    private JList listAccounts;
    private JLabel labelLabel;
    private JTextField textFieldLabel;
    private JLabel labelPublicKey;
    private JTextField textFieldPublic;
    private JLabel labelPrivateKey;
    private JTextField textFieldPrivate;
    private JButton buttonAddAccount;
    private JLabel label1;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
