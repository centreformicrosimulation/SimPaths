package simpaths.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class XLSXfileWriter {

	/**
	 * 
	 * Creates a Microsoft Excel (.xlsx) file based on data provided by arrays
	 * 
	 * @param filename - the name of the file.  If a file with the same name already exists, this will be overwritten
	 * @param columnNames - the headings for the table columns
	 * @param data - the arrays carrying the data
	 * 
	 * @author Ross Richardson
	 * 
	 */
	public static void createXLSX(String directoryName, String filename, String worksheetName, String[] columnNames, Object[][] data) {
 
		String extension = ".xlsx"; 
		String filePath = directoryName + File.separator + filename + extension;
		FileOutputStream out = null;
		Workbook wb = null; 
		Path newPath = null;
		
		try { 
        	//Creates file but checks whether a file with the same filename already exists - if so, overwrite. 
        	File f = new File(filePath);

			if (!f.exists()) {  // Create new empty workbook if doesn't exist
        		wb = WorkbookFactory.create(true);
				wb.createSheet(worksheetName);
				out = new FileOutputStream(filePath);
				wb.write(out);
				wb.close();
				out.close();
			}

        	if(f.exists()){
        		Path source = FileSystems.getDefault().getPath(directoryName, filename + extension);        		
        		newPath = Files.copy(source, source.resolveSibling(filename + "_bak" + extension), StandardCopyOption.REPLACE_EXISTING);
        		f.createNewFile();
        	} 
        	else throw new IllegalArgumentException("Error - the file " + (filename+extension) + " cannot be found in " + directoryName);

        	wb = WorkbookFactory.create(newPath.toFile());		//Create workbook from copy        	
    		Sheet s = wb.getSheet(worksheetName);
    		int lastRowNum = s.getLastRowNum();
    		//Delete old rows
    		for(int row = 0; row <= lastRowNum; row++) {
    			s.removeRow(s.getRow(row));
    		}
    		
    		//Create new rows
    		int numColumns = columnNames.length;
    		int numRows = data.length;
    		Row r1 = s.createRow(0);
    		for(int col = 0; col < numColumns; col++) {        		
    			String columnName = columnNames[col];
    			String[] noHTMLcolumnName = columnName.split(">");		//Cuts out any HTML formatting applied to the column name headings
    			String noSpacesColumnName = noHTMLcolumnName[noHTMLcolumnName.length-1].replace(' ', '_');
    			r1.createCell(col).setCellValue(noSpacesColumnName);
    		}
    		for(int row = 0; row < numRows; row++) {
    			Row r = s.createRow(row+1);
    			for(int col = 0; col < numColumns; col++) {	
    				Cell c = r.createCell(col);
    				
    				Object val = data[row][col];
    				if(val == null) {
    					c.setCellValue("");
    				}
    				else {
    					c.setCellValue(val.toString());
    				}
    			}
    		}
    		out = new FileOutputStream(filePath);
    		wb.write(out);    		        	
        }
        catch (Exception e) {

			e.printStackTrace();
        }
        finally {
        	try {
        		out.flush(); 
        		out.close(); 
        		wb.close();
        		newPath.toFile().delete();	//Remove copy
        	} catch (IOException e) { 
        			System.err.println("Error while flushing/closing file writer or worksheet."); 
        			e.printStackTrace(); 
        	} 	
        }
	}
}
