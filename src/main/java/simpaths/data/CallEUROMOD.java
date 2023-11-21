package simpaths.data;

import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

public class CallEUROMOD {

	//-------------------------------------------------------------------------------------------------------------
    /**
    *	CALLING EUROMOD IN THIS SIMULATION RUN TO GIVE THE USER THE OPPORTUNITY TO CREATE EUROMOD OUTPUT POPULATION from which to generate population of households and persons after tax & benefit treatment 
    **/
    //-------------------------------------------------------------------------------------------------------------    		
	public static void run() {
		
		//EUROMOD must be separately installed on the machine where the simulation is run, in the default location (Program Files (x86) on 
		//Windows machines).  Note, EUROMOD currently only works on Windows operating systems, not Linux or MAC OS.
		if(SystemUtils.IS_OS_WINDOWS) {

//			String euromodInstallPath;
//			euromodInstallPath = JOptionPane.showInputDialog("Please provide path to EM_UI.exe file");
			
								
			
			//Call EUROMOD - EUROMOD must be separately installed on the machine where the simulation is run, in the default location (Program Files (x86) on 
			//Windows machines).  Note, EUROMOD currently only works on Windows operating systems, not Linux (or MAC OS?)
			
			try {
				String euromodDirectoryString = new String("C:\\Program Files\\EUROMOD\\EUROMOD\\EM_UI.exe");
				File tempDir = new File(euromodDirectoryString);
				if(tempDir.exists()) {
					ProcessBuilder euromodProcess =
							new ProcessBuilder(euromodDirectoryString);
					euromodProcess.redirectErrorStream(true);
					try {
						
						Process p = euromodProcess.start();
						assert euromodProcess.redirectInput() == Redirect.PIPE;
						assert p.getInputStream().read() == -1;
						 
						String title = "Specify EUROMOD Output Path";
						String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt; padding-bottom: 0;\">"
								+ "When running EUROMOD, please ensure that the 'Output path' in<br>"
								+ "the 'Run EUROMOD' window is set to the following location:<br><br>"
								+ Parameters.EUROMOD_OUTPUT_DIRECTORY + "</h2></html>";
						JFrame csvFrame = FormattedDialogBox.create(title, text, 800, 120, null, false, true, true);

						//Make process wait until EUROMOD has been terminated
						p.waitFor();
						 
					} catch (IOException | InterruptedException e) { 

						e.printStackTrace();
					}	
				} else {
					
					boolean pathcorrect = false; 
					while (!pathcorrect) {
						String euromodInstallPath = null;
						JFrame frame = new JFrame("EUROMOD not found in default location");
						JOptionPane.showMessageDialog(frame, "EUROMOD not found in the default location. Please select the EM_UI.exe file on the next screen.");
						final JFileChooser fc = new JFileChooser();
						fc.setCurrentDirectory(new File(System.getProperty("user.home")));
						int returnValue = fc.showOpenDialog(fc);
						if (returnValue == JFileChooser.APPROVE_OPTION) {
							File fileEMUI = fc.getSelectedFile(); 
							euromodInstallPath = fileEMUI.getPath(); //Get path of the selected EM_UI file
						
						ProcessBuilder euromodProcess =
								new ProcessBuilder(euromodInstallPath);
						euromodProcess.redirectErrorStream(true);
							try {
								
								Process p = euromodProcess.start();
								assert euromodProcess.redirectInput() == Redirect.PIPE;
								assert p.getInputStream().read() == -1;
								pathcorrect = true;
								//Make process wait until EUROMOD has been terminated
								
								String title = "Specify EUROMOD Output Path";
								String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt; padding-bottom: 0;\">"
										+ "When running EUROMOD, please ensure that the 'Output path' in<br>"
										+ "the 'Run EUROMOD' window is set to the following location:<br><br>"
										+ Parameters.EUROMOD_OUTPUT_DIRECTORY + "</h2></html>";
								JFrame csvFrame = FormattedDialogBox.create(title, text, 800, 120, null, false, true, true);

								p.waitFor();
								 
							} catch (IOException | InterruptedException e) { 

								//PB: If getting IOException, would like to provide an input window for where EUROMOD is installed - doesn't have to be C drive. 
								JOptionPane.showMessageDialog(null, 
							 			"Please check the path provided is correct",
										"Unable to execute EUROMOD",					 
							            JOptionPane.INFORMATION_MESSAGE);
								pathcorrect = false;
								e.printStackTrace();
							}
						}
						else if (returnValue == JFileChooser.CANCEL_OPTION) {
							JFrame frameCancel = new JFrame("Choice cancelled");
							JOptionPane.showMessageDialog(frameCancel, "EUROMOD not found in the default location and no location provided. Skipping EUROMOD step.");
							break;
						}
					}
						
				}
			} catch (Exception e) {

				e.printStackTrace();
			}
	
				
			
/*			//ProcessBuilder euromodProcess =
			//	new ProcessBuilder("C:\\Program Files (x86)\\EUROMOD\\EUROMOD\\EM_UI.exe");		//EUROMOD is supposed to always be stored at this location!
			ProcessBuilder euromodProcess =
					new ProcessBuilder(euromodInstallPath);	//PB: modified path to EM. Seems to work although throws an exception, should be done in nicer way 
				euromodProcess.redirectErrorStream(true);

				try {
					
					Process p = euromodProcess.start();
					assert euromodProcess.redirectInput() == Redirect.PIPE;
					assert p.getInputStream().read() == -1;
					 
					//Make process wait until EUROMOD has been terminated
					p.waitFor();
					 
				} catch (IOException | InterruptedException e) { 

					//PB: If getting IOException, would like to provide an input window for where EUROMOD is installed - doesn't have to be C drive. 
					e.printStackTrace();
				}				
*/				
		}
		else {			
			 JOptionPane.showMessageDialog(null, 
			 			"<html><p align=center style=\"font-size:120%;\">EUROMOD is currently only available on the Windows operating system.<br>"
					 	+ "Before running the simulation, please ensure that the required files<br>"
					 	+ "containing the output of EUROMOD exist in the following directory:</p><br>"
					 	+ "<h1 align=center>" + Parameters.EUROMOD_OUTPUT_DIRECTORY + "</h1></html>",
						"Unable to execute EUROMOD",					 
			            JOptionPane.INFORMATION_MESSAGE);
		}	
	}

}
