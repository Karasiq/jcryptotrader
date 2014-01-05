/*
 * Created by JFormDesigner on Thu Jan 02 14:52:49 MSK 2014
 */

package com.archean.jtradegui;

import com.archean.jtradeapi.AccountManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author Yarr harr
 */
public class Controller extends JFrame {
    public AccountManager.AccountDb accountDb = new AccountManager.AccountDb();
    protected final static String settingsDir = System.getProperty("user.home") + "/jCryptoTrader/";
    protected final static String accountDbFile = settingsDir + "accounts.json";
    protected final static String tabsDbFile = settingsDir + "tabs.json";

    public void loadAccountDb() {
        try {
            accountDb.loadFromJson(FileUtils.readFileToString(new File(accountDbFile)));
        } catch (IOException e) {
            // nothing
        }
    }

    public void saveAccountDb() {
        try {
            FileUtils.writeStringToFile(new File(accountDbFile), accountDb.saveToJson());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class SavedTab {
        String accountLabel;
        double feePercent;
        int timeInterval;
        String pair;

        public SavedTab(String accountLabel, TraderMainForm traderPanel) {
            this.accountLabel = accountLabel;
            Map<String, Object> settings = traderPanel.getSettings();
            feePercent = (Double) settings.get("feePercent");
            timeInterval = (Integer) settings.get("timeInterval");
            pair = (String) settings.get("currentPair");
        }
    }

    private void saveTabs() {
        List<SavedTab> savedTabList = new ArrayList<>();
        for (int i = 0; i < tabbedPaneTraders.getTabCount(); i++) {
            savedTabList.add(new SavedTab(tabbedPaneTraders.getTitleAt(i), (TraderMainForm) tabbedPaneTraders.getComponentAt(i)));
        }
        try {
            FileUtils.writeStringToFile(new File(tabsDbFile), new Gson().toJson(savedTabList));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTabs() {
        try {
            String json = FileUtils.readFileToString(new File(tabsDbFile));
            List<SavedTab> savedTabList = new Gson().fromJson(json, new TypeToken<List<SavedTab>>() {
            }.getType());
            for (SavedTab tab : savedTabList) {
                TraderMainForm traderMainForm = new TraderMainForm(accountDb.get(tab.accountLabel));
                tabbedPaneTraders.add(tab.accountLabel, traderMainForm);
                traderMainForm.setSettings(tab.feePercent, tab.timeInterval);
                traderMainForm.setPair(tab.pair);
            }
        } catch (IOException e) {
            // nothing
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.jtattoo.plaf.acryl.AcrylLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean success = new File(settingsDir).mkdirs();

        Controller controller = new Controller();
        controller.loadAccountDb();
        controller.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        controller.pack();
        controller.setIconImage(new ImageIcon(controller.getClass().getResource("/icons/logo.png")).getImage());
        controller.setVisible(true);
        TrayIconController.createTrayIcon(controller.popupMenuTray, "jTrader", new ImageIcon(controller.getClass().getResource("/icons/logo.png")).getImage());
        controller.loadTabs();
    }

    public Controller() {
        initComponents();
    }

    public void addTab(String label, AccountManager.Account account) {
        TraderMainForm panel = new TraderMainForm(account);
        tabbedPaneTraders.addTab(label, panel);
        panel.repaint();
    }

    private void buttonAddActionPerformed(ActionEvent e) {
        final Controller frame = this;
        SwingWorker swingWorker = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                AccountManagerDlg accountManagerDlg = new AccountManagerDlg(frame, accountDb);
                accountManagerDlg.setVisible(true);
                do {
                    Thread.sleep(100);
                } while (accountManagerDlg.isVisible());
                if (accountManagerDlg.selectedAccount != null) {
                    saveAccountDb();
                    frame.addTab(accountManagerDlg.selectedAccount, accountDb.get(accountManagerDlg.selectedAccount));
                    saveTabs();
                }
                return null;
            }
        };
        swingWorker.execute();
    }

    private void buttonDeleteActionPerformed(ActionEvent e) {
        if (tabbedPaneTraders.getTabCount() > 0 && tabbedPaneTraders.getSelectedIndex() >= 0) {
            ((TraderMainForm) tabbedPaneTraders.getSelectedComponent()).traderRemoved = true;
            tabbedPaneTraders.remove(tabbedPaneTraders.getSelectedIndex());
            saveTabs();
        }
    }

    private void menuItemExitActionPerformed(ActionEvent e) {
        System.exit(0);
    }

    private void thisWindowClosing(WindowEvent e) {
        saveAccountDb();
        saveTabs();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.archean.jtradegui.locale", new UTF8Control());
        toolBar1 = new JToolBar();
        buttonAdd = new JButton();
        buttonDelete = new JButton();
        tabbedPaneTraders = new JTabbedPane();
        popupMenuTray = new JPopupMenu();
        menuItemExit = new JButton();

        //======== this ========
        setTitle(bundle.getString("Controller.this.title"));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "[350dlu,default]:grow",
                "top:16dlu, $lgap, fill:[420dlu,default]:grow"));

        //======== toolBar1 ========
        {
            toolBar1.setFloatable(false);

            //---- buttonAdd ----
            buttonAdd.setIcon(new ImageIcon(getClass().getResource("/icons/plus.png")));
            buttonAdd.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    buttonAddActionPerformed(e);
                }
            });
            toolBar1.add(buttonAdd);

            //---- buttonDelete ----
            buttonDelete.setIcon(new ImageIcon(getClass().getResource("/icons/delete.png")));
            buttonDelete.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    buttonDeleteActionPerformed(e);
                }
            });
            toolBar1.add(buttonDelete);
        }
        contentPane.add(toolBar1, CC.xy(1, 1, CC.FILL, CC.TOP));
        contentPane.add(tabbedPaneTraders, CC.xy(1, 3));
        pack();
        setLocationRelativeTo(getOwner());

        //======== popupMenuTray ========
        {

            //---- menuItemExit ----
            menuItemExit.setText(bundle.getString("Controller.menuItemExit.text"));
            menuItemExit.setIcon(new ImageIcon(getClass().getResource("/icons/delete.png")));
            menuItemExit.setHorizontalAlignment(SwingConstants.LEFT);
            menuItemExit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    menuItemExitActionPerformed(e);
                }
            });
            popupMenuTray.add(menuItemExit);
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JToolBar toolBar1;
    private JButton buttonAdd;
    private JButton buttonDelete;
    private JTabbedPane tabbedPaneTraders;
    private JPopupMenu popupMenuTray;
    private JButton menuItemExit;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}