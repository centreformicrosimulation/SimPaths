// define package
package simpaths.data;

// import Java packages
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.*;


/**
 *
 * 	CLASS TO DEFINE CHECK BOXES FOR GUI
 *
 * 	extends JPanel from javax.swing (for GUI construction)
 * 	implements ActionListener from java.awt.event (for data entry)
 *
 */
public class StartUpCheckBoxes extends JPanel implements ActionListener {

    private boolean[] choices;
    private Map<JCheckBox, Integer> boxesMap;


    /**
     *
     *  CONSTRUCTOR TO INITIATE RADIO BUTTONS
     * @param options
     *
     */
    public StartUpCheckBoxes(Map<String, Integer> options) {

        // constructor for a new borderlayout (from java.awt)
        super(new BorderLayout());

        // local variable to organise radio buttons
        choices = new boolean[options.size()];
        boxesMap = new LinkedHashMap<>();

        // create check boxes
        for (String ss : options.keySet()) {
            JCheckBox box = new JCheckBox(ss);
            boxesMap.put(box, options.get(ss));
            box.setActionCommand(ss);
        }

//        // group check boxes
//        ButtonGroup group = new ButtonGroup();
//        for(JCheckBox cb: boxesMap.keySet()) {
//            group.add(cb);
//            cb.addActionListener(this);		//Register a listener for the radio buttons.
//        }

        // organise the check boxes in a column in a panel
        JPanel checkBoxPanel = new JPanel(new GridLayout(0, 1));
        for(JCheckBox cb : boxesMap.keySet()) {
            checkBoxPanel.add(cb);
            cb.addActionListener(this);		//Register a listener for the radio buttons.
        }

        // add panel to borderlayout
        add(checkBoxPanel, BorderLayout.LINE_START);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
    }


    /**
     *
     * Listens to the radio buttons
     *
     */
    public void actionPerformed(ActionEvent e) {
        JCheckBox boxChosen = (JCheckBox)e.getSource();
        int index = boxesMap.get(boxChosen);
        if (boxChosen.isSelected())
            choices[index] = true;
        else
            choices[index] = false;
    }


    /**
     *
     * Return the chosen option
     * @return
     *
     */
    public boolean[] getChoices() {
        return choices;
    }
}
