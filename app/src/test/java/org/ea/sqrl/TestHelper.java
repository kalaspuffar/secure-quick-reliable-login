package org.ea.sqrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A utility class providing common functionality for the project's
 * unit tests.
 */
public class TestHelper {

    /**
     * Parses a text file containing test vectors stored in csv format.
     *
     * @param fileName The name of the vector file saved in the project's resource directory.
     * @param skipFirstLine If set to true, the first line (header line/field descriptions) will be ignored.
     * @param removeQuotes If set to true, double quotes around field values will be removed.
     * @return A list of test vectors, where each vector again contains a list of individual fields.
     * @throws FileNotFoundException
     * @throws URISyntaxException
     */
    public static List<List<String>> parseVectorCsvFile(String fileName, boolean skipFirstLine, boolean removeQuotes)
    throws FileNotFoundException, URISyntaxException {
        List<List<String>> result = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(fileName);

        Scanner s = new Scanner(new File(url.toURI()));

        int i=0;
        while (s.hasNextLine()){
            String line = s.nextLine();

            if (skipFirstLine && i==0) {
                i++;
                continue;
            }

            String[] fields = line.split(",");
            List<String> lineResult = new ArrayList<>();

            for (String field : fields) {
                if (field.length() > 1) {
                    if (field.charAt(field.length()-1) == '\r') {
                        field = field.substring(0, field.length()-1);
                    }

                    if (removeQuotes) {
                        if (field.charAt(0) == '"') field = field.substring(1, field.length()-1);
                        if (field.charAt(field.length()-1) == '"') field = field.substring(0, field.length()-1);
                    }
                }

                lineResult.add(field);
            }

            result.add(lineResult);
            i++;
        }
        s.close();

        return result;
    }
}
