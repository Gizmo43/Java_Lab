package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class LeaderboardDialog extends JDialog {
    public LeaderboardDialog(Frame owner, List<PlayerEntity> leaders) {
        super(owner, "Таблица лидеров", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(300, 400);
        setLocationRelativeTo(owner);

        String[] columns = {"Имя", "Победы"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        for (PlayerEntity p : leaders) {
            model.addRow(new Object[]{p.getUsername(), p.getWins()});
        }
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }
}