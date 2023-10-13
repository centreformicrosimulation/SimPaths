// define package
package simpaths.data;

// import Java packages
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// import bespoke packages
import simpaths.model.enums.Country;


/**
 *
 * CLASS FOR DEFINING COMBO-BOX FOR COUNTRY SELECTION
 *
 */
public class ComboBoxCountry extends JPanel implements ActionListener {

	// variables in multiple methods
	private JLabel picture;
	private String countryName;
	private boolean disp_flag = true;


	/**
	 *
	 * CONSTRUCTOR FOR COMBO-BOX FOR SELECTING A COUNTRY VIA GUI
	 * @param title
	 *
	 */
	public ComboBoxCountry(String title) {

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
		int numCountries = Country.values().length;
	//	numCountries = 1; //Overrides the above to only allow selection of Italy
		String[] countryNames = new String[numCountries];
		for(int i = 0; i < numCountries; i++) {
			countryNames[i] = Country.values()[i].getCountryName();
		}
		JComboBox<String> countryList = new JComboBox<String>(countryNames);
		countryList.setSelectedIndex(Country.IT.ordinal());
		countryList.addActionListener(this);
	
		// set-up display for country flag
		if (disp_flag) {
			picture = new JLabel();
			picture.setFont(picture.getFont().deriveFont(Font.ITALIC));
			picture.setHorizontalAlignment(JLabel.CENTER);
			this.countryName = Country.values()[countryList.getSelectedIndex()].getCountryName();
			updateLabel(countryName);
			picture.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
			picture.setPreferredSize(new Dimension(100, 50));
		}

		// collect display elements
		JPanel combPanel = new JPanel();
		combPanel.setLayout(new BorderLayout());
		if (title != null) combPanel.add(titlePane, BorderLayout.PAGE_START);
		combPanel.add(countryList, BorderLayout.CENTER);
		if (disp_flag) combPanel.add(picture, BorderLayout.PAGE_END);
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
		countryName = (String)cb.getSelectedItem();
		updateLabel(countryName);
	}


	/**
	 *
	 * update displayed label for each selection
	 *
	 */
	protected void updateLabel(String countryName) {
		Country country = Country.IT.getCountryFromNameString(countryName);
		ImageIcon icon = createImageIcon("/images/" + country + ".png");
		String text = "You have selected " + countryName;
		picture.setText(text);
		if (icon != null) {
			picture.setIcon(icon);
		}
		picture.setToolTipText("The country to be simulated is " + countryName);
	}


	/**
	 *
	 * returns an ImageIcon, or null if the path was invalid
	 *
	 */
	protected static ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = ComboBoxCountry.class.getResource(path);
		
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		}
		else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}


	/**
	 *
	 * Return the selections
	 * @return
	 *
	 */
	public Country getCountryEnum() { return Country.IT.getCountryFromNameString(this.countryName); }
}
