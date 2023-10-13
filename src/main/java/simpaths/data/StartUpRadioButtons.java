// define package
package simpaths.data;

// import Java packages
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.*;


/**
 *
 * 	CLASS TO DEFINE RADIO BUTTONS FOR GUI
 *
 * 	extends JPanel from javax.swing (for GUI construction)
 * 	implements ActionListener from java.awt.event (for data entry)
 *
 */
public class StartUpRadioButtons extends JPanel implements ActionListener {

    // variables used in multiple methods
	private int choice;
    private Map<JRadioButton, Integer> buttonsMap;


    /**
     *
     *  CONSTRUCTOR TO INITIATE RADIO BUTTONS
     * @param options
     *
     */
    public StartUpRadioButtons(Map<String, Integer> options) {

        // constructor for a new borderlayout (from java.awt)
        super(new BorderLayout());

        // local variable to organise radio buttons
        buttonsMap = new LinkedHashMap<>();

        // create radio buttons
        for (String ss : options.keySet()) {
            JRadioButton button = new JRadioButton(ss);
            buttonsMap.put(button, options.get(ss));
            button.setActionCommand(ss);
            button.setSelected(true);        	
        }

        // group radio buttons
        ButtonGroup group = new ButtonGroup();
        for(JRadioButton bb: buttonsMap.keySet()) {
        	group.add(bb);
        	bb.addActionListener(this);		//Register a listener for the radio buttons.
        }

        // organise the radio buttons in a column in a panel
        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        for(JRadioButton bb : buttonsMap.keySet()) {
        	radioPanel.add(bb);
        }

        // add panel to borderlayout
        add(radioPanel, BorderLayout.LINE_START);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
    }


    /**
     *
     * Listens to the radio buttons
     *
     */
    public void actionPerformed(ActionEvent e) {
		JRadioButton buttonChosen = (JRadioButton)e.getSource();
		choice = buttonsMap.get(buttonChosen);
    }


    /**
     *
     * Return the chosen option
     * @return
     *
     */
	public int getChoice() {
		return choice;
	}
}