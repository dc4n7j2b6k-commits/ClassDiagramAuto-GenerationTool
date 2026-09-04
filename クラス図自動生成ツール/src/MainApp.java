package src;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

import src.infos.*;
import src.draw.*;

public class MainApp{
    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
                File dir = fc.getSelectedFile();
                DiagramBuilder builder = new DiagramBuilder();
                List<ClassInfo> infos = builder.parseDirectory(dir);
                List<ClassRelation> rels = builder.detectRelations(infos);
                List<ClassBox> boxes = builder.layout(infos);
                List<PackageFolder> folders = builder.getPackageFolderList();

                JFrame frame = new JFrame("Class Diagram");

                ClassDiagramPanel panel = new ClassDiagramPanel(boxes, rels, folders);
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                panel.setPreferredSize(new Dimension((int)screenSize.getWidth() * 3, (int)screenSize.getHeight() * 3));

                JButton reset = new JButton("リセット");
                JButton edit = new JButton("編集非表示");

                JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                topPanel.setBackground(Color.GRAY);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(panel, BorderLayout.CENTER);
                frame.add(new JScrollPane(panel));
                frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                reset.addActionListener(e -> {
                    panel.scale = 1.0;
                    panel.offsetX = 0; panel.offsetY = 0;
                    panel.repaint();
                });
                edit.addActionListener(e -> {
                    panel.isVisible = !panel.isVisible;
                    edit.setText(panel.isVisible ? "編集非表示" : "編集表示");
                    panel.repaint();
                });
                topPanel.add(reset);
                topPanel.add(edit);
                JLabel statusLabel = new JLabel();
                Timer timer = new Timer(100, e -> {
                    statusLabel.setText(String.format("ズーム： %.2f | オフセット： (%.1f, %.1f)", panel.scale, panel.offsetX, panel.offsetY));
                });
                timer.start();
                frame.add(statusLabel, BorderLayout.SOUTH);
                frame.add(topPanel, BorderLayout.NORTH);
                frame.setVisible(true);
            }
        });
    }
}
//https://us02web.zoom.us/rec/share/tbrMyDtGCANsImgucE7bbhgwdh-IPTQcmyo3UxC9dpG9ULl9wNTGzSQSegQ_pokA.wYElDS010l9P5gEe