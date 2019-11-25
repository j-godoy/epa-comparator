package main;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import static main.Options.*;

public class Main {

    static final String EPAComparator = "EPAComparator";
    static final String SuperEPA = "SuperEPA";

    public static void main(String[] args) throws Exception {
        if(args.length != 1)
        {
            System.err.println("You must enter 1 parameter: properties file path");
            System.exit(1);
        }

        String propertyFile = args[0];
        Properties properties = loadProperty(propertyFile);

        if(properties.getProperty(RUN_MODE).trim().contains(EPAComparator) && properties.getProperty(RUN_MODE).trim().contains(SuperEPA)) {
            EPAComparator epaComparator = new EPAComparator();
            epaComparator.run(properties);

            SuperEPA superEPA = new SuperEPA();
            superEPA.run(properties);
        }
        else if(properties.getProperty(RUN_MODE).trim().equalsIgnoreCase(EPAComparator)) {
            EPAComparator epaComparator = new EPAComparator();
            epaComparator.run(properties);
        }
        else if(properties.getProperty(RUN_MODE).trim().equalsIgnoreCase(SuperEPA)) {
            SuperEPA superEPA = new SuperEPA();
            superEPA.run(properties);
        }
    }

    private static Properties loadProperty(String arg) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(arg));
        } catch (IOException e) {
            System.err.println("File does not exists: " + arg + ". Error: " + e.getMessage());
        }

        for(String property : properties.stringPropertyNames()) {
            switch (property) {
                case OUTPUT:
                case MAX_ID:
                case BUDGETS:
                case R_OUTPUT_FILE:
                case INFERRED_XML_EPA_NAME:
                case CRITERIA:
                case SUBJECTS:
                case R_PATH:
                case STRATEGY:
                case RUN_MODE:
                    break;
                case SUBJECTS_FOLDER_EPA_PATH:
                case METRICS_FOLDER_PATH:
                    if (!checkFolder(property, properties))
                        System.exit(1);
                    break;
                case BUG_TYPES:
                    if (!Arrays.stream(properties.getProperty(property).split(",")).allMatch(b ->
                            b.toUpperCase().equals("ALL") || b.toUpperCase().equals("ERRPROT"))) {
                        System.err.println("Bug type does not exists: " + properties.getProperty(property));
                        System.exit(1);
                    }
                    break;
                case R_SCRIPT:
                    if (!checkFolder(property, properties))
                        System.err.println("R script will not be executed!");
                    break;
                default:
                    System.err.println("Unknown defined property: " + property);
                    break;
            }
        }

        //obligatorios
        if(properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(RUN_MODE)))) {
            System.err.printf("Input value '%s' not defined in properties file", RUN_MODE);
            System.exit(1);
        }
        if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(OUTPUT)))) {
            System.err.printf("Input value '%s' not defined in properties file", OUTPUT);
            System.exit(1);
        }
        if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(MAX_ID)))) {
            System.err.printf("Input value '%s' not defined in properties file", MAX_ID);
            System.exit(1);
        }
        if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(SUBJECTS_FOLDER_EPA_PATH)))) {
            System.err.printf("Input value '%s' not defined in properties file", SUBJECTS_FOLDER_EPA_PATH);
            System.exit(1);
        }
        if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(METRICS_FOLDER_PATH)))) {
            System.err.printf("Input value '%s' not defined in properties file", METRICS_FOLDER_PATH);
            System.exit(1);
        }
        if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(INFERRED_XML_EPA_NAME)))) {
            System.err.printf("Input value '%s' not defined in properties file", INFERRED_XML_EPA_NAME);
            System.exit(1);
        }
        if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(CRITERIA)))) {
            System.err.printf("Input value '%s' not defined in properties file", CRITERIA);
            System.exit(1);
        }
        if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(SUBJECTS)))) {
            System.err.printf("Input value '%s' not defined in properties file", SUBJECTS);
            System.exit(1);
        }
        if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(BUG_TYPES)))) {
            System.err.printf("Input value '%s' not defined in properties file", BUG_TYPES);
            System.exit(1);
        }
        if (properties.stringPropertyNames().stream().noneMatch(p -> (p.equals(BUDGETS)))) {
            System.err.printf("Input value '%s' not defined in properties file", BUDGETS);
            System.exit(1);
        }

        return properties;
    }

    private static boolean checkFolder(String property, Properties properties) {
        String path = completeHomeUserPath(properties.getProperty(property));
        if(!new File(path).exists()) {
            System.err.println("(PROPERTY ERROR - " + property + "). File does not exists: " + properties.getProperty(property));
            return false;
        }
        return true;
    }

    static String completeHomeUserPath(String path) {
        if(new File(path).exists() || path.startsWith("/") || path.startsWith("C:"))
            return path;
        String home_dir = System.getProperty("user.home");
        return Paths.get(home_dir, path).toString();
    }
}
