// define package
package simpaths.data;

// import Java packages
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;


/**
 *
 * CLASS FOR DEFINING COMBO-BOX FOR START YEAR SELECTION
 *
 */
public class ComboBoxYear extends JPanel implements ActionListener {

	// variables in multiple methods
	private static final long serialVersionUID = 1137706260047776530L;
	String startYearName;


	/**
	 *
	 * CONSTRUCTOR FOR COMBO-BOX FOR SELECTING A START YEAR VIA GUI
	 * @param title
	 *
	 */
	public ComboBoxYear(String title) {

		super(new BorderLayout());

		// set up title element
		JTextPane titlePane = null;
		if(title != null) {
			titlePane = new JTextPane() {

				private static final long serialVersionUID = -6527724767413857638L;

				@Override
				public void updateUI() {
					super.updateUI();
					putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
				}
			};
			titlePane.setContentType("text/html");
			titlePane.setText(title);
			titlePane.setEditable(false);
			titlePane.setBackground(null);
			titlePane.setBorder(null);
		}

		// generate combo-box
		int minYear = Parameters.getMinStartYear();
		int maxYear = Parameters.getMaxStartYear();
		String[] possibleStartYears = new String[1 + maxYear - minYear];
		int count = 0;
		for(Integer ii = minYear; ii <= maxYear; ii++) {
			possibleStartYears[count] = ii.toString();
			count++;
		}
		JComboBox<String> startYearList = new JComboBox<String>(possibleStartYears);
		startYearList.setSelectedIndex(0);
		startYearList.addActionListener(this);
		this.startYearName = possibleStartYears[startYearList.getSelectedIndex()];
		updateLabel(startYearName);

		// collect display elements
		JPanel combPanel = new JPanel();
		combPanel.setLayout(new BorderLayout());
		if (title != null) combPanel.add(titlePane, BorderLayout.PAGE_START);
		combPanel.add(startYearList, BorderLayout.CENTER);
		combPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		add(combPanel, BorderLayout.PAGE_START);
	}


	/**
	 *
	 * Listens to the combo box.
	 *
	 */
	public void actionPerformed(ActionEvent e) {
		JComboBox cb = (JComboBox)e.getSource();
		startYearName = (String)cb.getSelectedItem();
		updateLabel(startYearName);
	}


	/**
	 *
	 * update displayed label for each selection
	 *
	 */
	protected void updateLabel(String startYearName) {
		String text = "<html><p align=center style=\"font-size:120%;\">You have selected " + startYearName + "</p></html>";
	}


	/**
	 *
	 * Return the selections
	 * @return
	 *
	 */
	public int getYear() {
		return Integer.parseInt(startYearName);
	}
}
