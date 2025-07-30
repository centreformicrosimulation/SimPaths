package simpaths.model.lifetime_incomes;

import jakarta.persistence.Transient;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import simpaths.data.Parameters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class ExportCSV {

    //Fields for exporting tables to output .csv files
    final static String newLine = "\n";
    final static String delimiter = ",";
    final static String directory = Parameters.getInputDirectory() + "csv";

    private Set<String> fieldsForExport;
    private BufferedWriter bufferWriter;
    private String idFieldName;

    private Collection<?> targetCollection;

    public ExportCSV(int birthYear, Object target) {
        try {

            targetCollection = (Collection<?>) target;
            Object obj = targetCollection.iterator().next();

            //Set up file and fileWriter - create new file and directory if required
            String filename = obj.getClass().getSimpleName() + birthYear;
            File ff = new File(directory + File.separator + filename + ".csv");
            boolean fAlreadyExists = ff.exists();
            if (!fAlreadyExists) {
                File dir = new File(directory);
                dir.mkdirs();
                ff.createNewFile();
            }
            else {
                ff.delete();
                ff.createNewFile();
            }
            bufferWriter = new BufferedWriter(new FileWriter(ff, true));

            //Create alphabetically sorted list of fields including private and inherited fields that belong to the target class.
            List<Field> declaredFields = new ArrayList<Field>();
            List<Field> allFields = getAllFields(declaredFields, obj.getClass());
            TreeSet<String> fieldNames = new TreeSet<String>();
            for (Field field : allFields) {

                if (field.getType().isPrimitive() || Number.class.isAssignableFrom(field.getType()) ||
                        field.getType().equals(String.class)|| field.getType().equals(Boolean.class) ||
                        field.getType().isEnum() || field.getType().equals(Character.class) ||
                        field.getType().equals(BirthCohort.class) || field.getType().equals(Individual.class) ||
                        field.getType().equals(AnnualIncome.class)
                ) {

                    String name = field.getName();
                    fieldNames.add(name);
                }
            }
            fieldsForExport = new LinkedHashSet<String>();
            for(String fieldName : fieldNames) {		//Iterated in correct order
                fieldsForExport.add(fieldName);
                bufferWriter.append(fieldName + delimiter);
            }

            for (Object oo : targetCollection) {

                bufferWriter.append(newLine);
                for (String fieldName : fieldsForExport) {
                    Field thisField = findUnderlyingField(oo.getClass(), fieldName);
                    thisField.setAccessible(true);
                    Object value = thisField.get(oo);
                    if (value == null) {
                        bufferWriter.append("null");
                    }
                    else {
                        bufferWriter.append(value.toString());
                    }
                    bufferWriter.append(delimiter);
                }
            }
            bufferWriter.flush();
            bufferWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }


    private static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));
        if (type.getSuperclass() != null) {
            fields = getAllFields(fields, type.getSuperclass());
        }
        return fields;
    }


    private static Field findUnderlyingField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        do {
            try {
                return current.getDeclaredField(fieldName);
            } catch(Exception e) {}
        } while((current = current.getSuperclass()) != null);
        return null;
    }
}
