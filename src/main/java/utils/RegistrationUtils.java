package utils;

import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;

public class AnmeldungUtils {

    final static Logger logger = Logger.getLogger(AnmeldungUtils.class);

    private AnmeldungUtils() {
    }

    public static Properties readPropertyFile(String fileName) {
        Properties props = new Properties();
        try {
            props.load(AnmeldungUtils.class.getResourceAsStream(fileName));
        } catch (FileNotFoundException e) {
            logger.debug("Property file " + fileName + " is missing");
        } catch (IOException ioe) {
            logger.debug("Can't load  " + fileName);
        }
        return props;
    }
}
