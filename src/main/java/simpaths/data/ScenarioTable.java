// define package
package simpaths.data;

// import Java packages
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 *
 * 	CLASS TO GATHER CONTENT FOR TABLE TO DEFINE POLICY SCENARIO
 *
 */
public class ScenarioTable extends JPanel implements ActionListener {

    // variables used in multiple methods
	private static final long serialVersionUID = -1848950117035152245L;
	private JTable table;
    private JCheckBox cellCheck;


    /**
     *
     *  CONSTRUCTOR TO INITIATE TABLE
     * @param title
     * @param columnNames
     * @param data
     *
     */
    public ScenarioTable(String tableText, String[] columnNames, Object[][] data) {

        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // define table text
		JTextPane textPane = new JTextPane() {
			private static final long serialVersionUID = 1L;
			@Override
            public void updateUI() {
			    super.updateUI();
			    putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
			}
		};
        textPane.setContentType("text/html");
        textPane.setText(tableText);
        textPane.setEditable(false);
		textPane.setBackground(null);
		textPane.setBorder(BorderFactory.createEmptyBorder(0,0,20,0));
        
        // prepare table content
        int numberRows = data.length;
        table = new JTable(new MyTableModel(columnNames, data));
        table.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 14));
        table.setPreferredScrollableViewportSize(new Dimension(750, 20 + 11*numberRows));
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(new RowListener());
        table.getColumnModel().getSelectionModel().addListSelectionListener(new ColumnListener());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // add spacer pane
        JPanel spacePanel = new JPanel();
        spacePanel.setLayout(new BorderLayout());
        spacePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // add content to display
        add(textPane);
        add(new JScrollPane(table));
        add(spacePanel);
    }

    /**
     *
     * CHECKBOX NOT CURRENTLY USED HERE
     * @param text
     * @return
     *
     */
    private JCheckBox addCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.addActionListener(this);
        add(checkBox);
        return checkBox;
    }


    /**
     * TODO: PROBLEM WITH LISTENER - DOESN'T CAPTURE LAST EDIT IN TABLE
     * @param event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
//        //Cell selection is disabled in Multiple Interval Selection
//        //mode. The enabled state of cellCheck is a convenient flag
//        //for this status.
//        if ("Row Selection" == command) {
//            table.setRowSelectionAllowed(rowCheck.isSelected());
//            //In MIS mode, column selection allowed must be the
//            //opposite of row selection allowed.
//            if (!cellCheck.isEnabled()) {
//                table.setColumnSelectionAllowed(!rowCheck.isSelected());
//            }
//        } else if ("Column Selection" == command) {
//            table.setColumnSelectionAllowed(columnCheck.isSelected());
//            //In MIS mode, row selection allowed must be the
//            //opposite of column selection allowed.
//            if (!cellCheck.isEnabled()) {
//                table.setRowSelectionAllowed(!columnCheck.isSelected());
//            }
//        } else 
        if ("Cell Selection" == command) {
            table.setCellSelectionEnabled(cellCheck.isSelected());
        }
//       else if ("Multiple Interval Selection" == command) { 
//            table.setSelectionMode(
//                    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
//            //If cell selection is on, turn it off.
//            if (cellCheck.isSelected()) {
//                cellCheck.setSelected(false);
//                table.setCellSelectionEnabled(false);
//            }
//            //And don't let it be turned back on.
//            cellCheck.setEnabled(false);
//        } else if ("Single Interval Selection" == command) {
//            table.setSelectionMode(
//                    ListSelectionModel.SINGLE_INTERVAL_SELECTION);
//            //Cell selection is ok in this mode.
//            cellCheck.setEnabled(true);
//        } 
            else if ("Single Selection" == command) {
            table.setSelectionMode(
                    ListSelectionModel.SINGLE_SELECTION);
            //Cell selection is ok in this mode.
            cellCheck.setEnabled(true);
        }

        //Update checkboxes to reflect selection mode side effects.
//        rowCheck.setSelected(table.getRowSelectionAllowed());
//        columnCheck.setSelected(table.getColumnSelectionAllowed());
        if (cellCheck.isEnabled()) {
            cellCheck.setSelected(table.getCellSelectionEnabled());
        }
    }

//    private void outputSelection() {
//        output.append(String.format("Lead: %d, %d. ",
//                    table.getSelectionModel().getLeadSelectionIndex(),
//                    table.getColumnModel().getSelectionModel().
//                        getLeadSelectionIndex()));
//        output.append("Rows:");
//        for (int c : table.getSelectedRows()) {
//            output.append(String.format(" %d", c));
//        }
//        output.append(". Columns:");
//        for (int c : table.getSelectedColumns()) {
//            output.append(String.format(" %d", c));
//        }
//        output.append(".\n");
//    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
//            output.append("ROW SELECTION EVENT. ");
//            outputSelection();
        }
    }

    private class ColumnListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
//            output.append("COLUMN SELECTION EVENT. ");
//            outputSelection();
        }
    }

    class MyTableModel extends AbstractTableModel {
    	
        private String[] columnNames;

        private Object[][] data;// = new Object[][]();        
//	    {"Kathy", "Smith",
//	     "Snowboarding", new Integer(5), new Boolean(false)},
//	    {"John", "Doe",
//	     "Rowing", new Integer(3), new Boolean(true)},
//	    {"Sue", "Black",
//	     "Knitting", new Integer(2), new Boolean(false)},
//	    {"Jane", "White",
//	     "Speed reading", new Integer(20), new Boolean(true)},
//	    {"Joe", "Brown",
//	     "Pool", new Integer(10), new Boolean(false)}
//        };
        
        MyTableModel(String[] columnNames, Object[][] data) {
        	this.columnNames = columnNames;
        	this.data = data;
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col < 1) {
                return false;
            } else {
                return true;
            }
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }

    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    public static JFrame createAndShowGUI(String title, String text, String[] columnNames, Object[][] data) {
        //Disable boldface controls.
        UIManager.put("swing.boldMetal", Boolean.FALSE); 

        //Create and set up the window.
//        JFrame frame = new JFrame("Select EUROMOD policies and the years they begin");
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        JTextPane textArea = new JTextPane() {
        	@Override
			  public void updateUI() {
			      super.updateUI();
			      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
			  }
		};
        textArea.setContentType("text/html"); // let the text pane know this is what you want
//        String text = "<html><h2 align=center style=\"font-size:120%;\">"
//			+ "Select EUROMOD policies to include in this simulation by typing in the year a policy begins.</h2>"
//			+ "<p align=center style=\"font-size:120%;\">Note that policies not containing a valid year entry will not be included in the simulation.<br>"
//			+ "Additionally, please add a description of the scenario policy to store this information";

        textArea.setText(text);
        contentPane.add(textArea);
        
        //Create and set up the content pane.
        ScenarioTable scenarioTable = new ScenarioTable(text, columnNames, data);
//        newContentPane.setOpaque(true); //content panes must be opaque
//        frame.setContentPane(newContentPane);
        contentPane.add(scenarioTable);
        contentPane.setOpaque(true);
        frame.setContentPane(contentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        
        return frame;
    }

    public static void create(String title, String text, String[] columnNames, Object[][] data) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(title, text, columnNames, data);
            }
        });
    }
}
