package com.archean.jtradegui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TrayIconController {
    static TrayIcon trayIcon;

    public static void showMessage(final String title, final String message, final TrayIcon.MessageType messageType) {
        trayIcon.displayMessage(title,
                message,
                messageType);
    }

    public static boolean createTrayIcon(final JPopupMenu popup, final String hint, final Image image) {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            ActionListener exitListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("Exiting...");
                    System.exit(0);
                }
            };
            trayIcon = new TrayIcon(image, hint);
            /* ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    trayIcon.displayMessage("Action Event",
                            "An Action Event Has Been Performed!",
                            TrayIcon.MessageType.INFO);
                }
            }; */

            trayIcon.setImageAutoSize(true);
            // trayIcon.addActionListener(actionListener);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        popup.setLocation(e.getX(), e.getY());
                        popup.setInvoker(popup);
                        popup.setVisible(true);
                    }
                }
            });

            try {
                tray.add(trayIcon);
                return true;
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }
        }
        return false;
    }
}