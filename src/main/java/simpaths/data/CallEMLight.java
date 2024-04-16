package simpaths.data;

import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class CallEMLight {

    //-------------------------------------------------------------------------------------------------------------
    /**
     *	CALLING EUROMOD LIGHT IN THIS SIMULATION RUN TO GIVE THE USER THE OPPORTUNITY TO CREATE EUROMOD OUTPUT POPULATION from which to generate population of households and persons after tax & benefit treatment
     **/
    //-------------------------------------------------------------------------------------------------------------

    public static void run() {
        //EUROMOD Light must be separately installed on the machine where the simulation is run, in the default location (Program Files (x86) on
        //Windows machines).  Note, EUROMOD currently only works on Windows operating systems, not Linux or MAC OS.
        if(SystemUtils.IS_OS_WINDOWS) {
            try {
                String euromodDirectoryString = new String("C:\\Program Files\\EM_LightUI\\EM_LightUI.exe");
                File tempDir = new File(euromodDirectoryString);
                if(tempDir.exists()) {
                    ProcessBuilder euromodProcess =
                            new ProcessBuilder(euromodDirectoryString);
                    euromodProcess.redirectErrorStream(true);
                    try {

                        Process p = euromodProcess.start();
                        assert euromodProcess.redirectInput() == ProcessBuilder.Redirect.PIPE;
                        assert p.getInputStream().read() == -1;

                        String title = "Specify EUROMOD Light Output Path";
                        String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt; padding-bottom: 0;\">"
                                + "When running EUROMOD Light, please save the output data to the following location:<br><br>"
                                + Parameters.EUROMOD_OUTPUT_DIRECTORY + "</h2></html>";
                        FormattedDialogBox.create(title, text, 800, 200, null, false, true, true);

                        //Make process wait until EUROMOD has been terminated
                        p.waitFor();

                    } catch (IOException | InterruptedException e) {

                        e.printStackTrace();
                    }
                } else {

                    boolean pathcorrect = false;
                    while (!pathcorrect) {
                        String euromodInstallPath = null;
                        JFrame frame = new JFrame("EUROMOD Light not found in default location");
                        JOptionPane.showMessageDialog(frame, "EUROMOD Light not found in the default location. Please select the EM_LightUI.exe file on the next screen.");
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
                                assert euromodProcess.redirectInput() == ProcessBuilder.Redirect.PIPE;
                                assert p.getInputStream().read() == -1;
                                pathcorrect = true;
                                //Make process wait until EUROMOD has been terminated

                                String title = "Specify EUROMOD Light Output Path";
                                String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt; padding-bottom: 0;\">"
                                        + "When running EUROMOD Light, please save the output data to the following location:<br><br>"
                                        + Parameters.EUROMOD_OUTPUT_DIRECTORY + "</h2></html>";
                                FormattedDialogBox.create(title, text, 800, 200, null, false, true, true);

                                p.waitFor();

                            } catch (IOException | InterruptedException e) {

                                //PB: If getting IOException, would like to provide an input window for where EUROMOD is installed - doesn't have to be C drive.
                                JOptionPane.showMessageDialog(null,
                                        "Please check the path provided is correct",
                                        "Unable to execute EUROMOD Light",
                                        JOptionPane.INFORMATION_MESSAGE);
                                pathcorrect = false;
                                e.printStackTrace();
                            }
                        }
                        else if (returnValue == JFileChooser.CANCEL_OPTION) {
                            JFrame frameCancel = new JFrame("Choice cancelled");
                            JOptionPane.showMessageDialog(frameCancel, "EUROMOD Light not found in the default location and no location provided. Skipping EUROMOD Light step.");
                            break;
                        }
                    }

                }
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        else {
            JOptionPane.showMessageDialog(null,
                    "<html><p align=center style=\"font-size:120%;\">EUROMOD Light is currently only available on the Windows operating system.<br>"
                            + "Before running the simulation, please ensure that the required files<br>"
                            + "containing the output of EUROMOD Light exist in the following directory:</p><br>"
                            + "<h1 align=center>" + Parameters.EUROMOD_OUTPUT_DIRECTORY + "</h1></html>",
                    "Unable to execute EUROMOD Light",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

}
