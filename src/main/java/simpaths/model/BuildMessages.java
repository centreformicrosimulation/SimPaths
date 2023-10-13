package simpaths.model;

import javax.swing.*;


public class BuildMessages {

    public static void run() {

        JTextArea textArea = new JTextArea(8, 60);
        //textArea.setText("test test test");
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);

        JPanel panel = new JPanel();
        panel.add(new JScrollPane(textArea));
        MessageConsole mc = new MessageConsole(textArea);
        mc.redirectOut(null, System.out);
        //mc.redirectOut();
        mc.redirectErr();

        JFrame frame = new JFrame();
        frame.setTitle("run-time progress");
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
