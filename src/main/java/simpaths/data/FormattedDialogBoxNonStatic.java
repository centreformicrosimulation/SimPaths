// define package
package simpaths.data;

// import Java packages
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;


/**
 *
 * CLASS TO DISPLAY DIALOG BOX FOR SELECTING POLICY SCENARIO
 *
 */
public class FormattedDialogBoxNonStatic {

	// variables used in multiple methods
	private boolean skip = false;


	/**
	 *
	 * CONSTRUCTOR FOR DIALOG BOX
	 * @param title
	 * @param text
	 * @param width
	 * @param height
	 * @param component
	 * @param modal
	 *
	 */
	public FormattedDialogBoxNonStatic(String title, String text, int width, int height, JComponent component, boolean modal, boolean keepExistingButton) {
		
		// create dialog box
		JFrame frame = new JFrame();
		final JDialog dialog = new JDialog(frame, title);
		dialog.setModal(modal);

		// add in text (if provided)
		JTextPane label = null;
		if (text != null) {
			label = new JTextPane() {
				private static final long serialVersionUID = -6527724767413857638L;
				@Override
				public void updateUI() {
				      super.updateUI();
				      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
				}
			};
			label.setContentType("text/html");
			label.setText(text);
			label.setEditable(false);
			label.setBackground(null);
			label.setBorder(null);
		}

		// add button to revise policy scenario
		JButton updateButton = new JButton("Build new Policy Schedule");
		updateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
				dialog.dispose();
			}
		});

		// add button to use existing policy scenario
		JButton skipButton = new JButton("Keep existing Policy Schedule");
		skipButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				skip = true;
				dialog.setVisible(false);
				dialog.dispose();
			}
		});

		// add content to dialog box
		JPanel dialogPanel = new JPanel();
		if (component != null) dialogPanel.add(component);
		dialogPanel.add(updateButton);
		if (keepExistingButton) { // Only show "keep existing policy schedule" button if EM schedule builder not called as part of the rebuild of all databases
			dialogPanel.add(skipButton);
		}
		dialogPanel.setBorder(BorderFactory.createEmptyBorder(20,20,70,20));

		// complete construction
		JPanel contentPane = new JPanel(new BorderLayout());
		if(text != null) {
			contentPane.add(label, BorderLayout.CENTER);
		}
		contentPane.add(dialogPanel, BorderLayout.PAGE_END);
		contentPane.setOpaque(true);
		dialog.setContentPane(contentPane);

		// display dialog box
		dialog.setSize(new Dimension(width, height+100));
		dialog.setLocationRelativeTo(frame);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setVisible(true);
		dialog.setAlwaysOnTop(true);
	}


	/**
	 *
	 * METHOD TO RETURN FLAG IF EXISTING POLICY SCENARIO HAS BEEN SELECTED
	 * @return
	 *
	 */
	public boolean isSkip() {
		return skip;
	}
}
