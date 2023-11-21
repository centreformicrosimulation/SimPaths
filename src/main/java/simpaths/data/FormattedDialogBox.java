// define package
package simpaths.data;

// import Java packages
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;


/**
 *
 * 	CLASS TO DISPLAY FORMATTED DIALOG BOX
 *
 */
public class FormattedDialogBox {


	/**
	 * CREATE METHOD TO DISPLAY DIALOG BOX AND WAIT FOR USER ENTRY
	 * @param title
	 * @param text
	 * @param width
	 * @param height
	 * @param component
	 * @param modal
	 * @param showButton
	 * @param isVisible
	 * @return
	 */
	public static JFrame create(String title, String text, int width, int height, JComponent component, boolean modal, boolean showButton, boolean isVisible) {
		
		// create dialog box
		JFrame frame = new JFrame();
		final JDialog dialog = new JDialog(frame, title);
		dialog.setModal(modal);

		// add label if supplied
		JTextPane label = null;
		if(text != null) {
			label = new JTextPane() {
	
				private static final long serialVersionUID = -6527724767413857638L;
	
				@Override
				public void updateUI() {
				      super.updateUI();
				      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
				}
			};
			label.setContentType("text/html"); // let the text pane know this is what you want
			label.setText(text);
			label.setEditable(false); // as before
			label.setBackground(null); // this is the same as a JLabel
			label.setBorder(null); // remove the border
		}

		// add button for user to indicate that flow should proceed
		JButton closeButton = new JButton("next");
		closeButton.setVisible(showButton);
		closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		closeButton.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
				dialog.dispose();
			}
		});

		// create panel for dialog content and insert component and next button
		JPanel dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		dialogPanel.add(Box.createHorizontalGlue());
		if(component != null) {
			dialogPanel.add(component);
		}
		dialogPanel.add(closeButton);
		dialogPanel.setBorder(BorderFactory.createEmptyBorder(0,10,10,10));

		// create content pane and add panel to it
		JPanel contentPane = new JPanel(new BorderLayout());
		if(text != null) contentPane.add(label, BorderLayout.CENTER);
		contentPane.add(dialogPanel, BorderLayout.PAGE_END);
		contentPane.setOpaque(true);
		dialog.setContentPane(contentPane);

		// show dialog
		dialog.setSize(new Dimension(width, height));
		dialog.setLocationRelativeTo(frame);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setVisible(isVisible);
		dialog.setAlwaysOnTop(true);
		
		return frame;
	}
}
