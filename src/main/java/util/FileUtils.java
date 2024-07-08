package util;

import java.io.*;
import java.util.Vector;

public class FileUtils {


    public static boolean readAsciiFile(String filename, Vector<String> results) throws Exception {
        boolean result = false;

        results.clear();

        File f = new File(filename);
        if (f.exists()) {
            result = true;
            FileInputStream fileInput = new FileInputStream(filename);
            try (BufferedReader fileRdr = new BufferedReader(new InputStreamReader(fileInput))) {
                String s = fileRdr.readLine().trim();

                while (s != null) {

                    results.add(s);

                    s = fileRdr.readLine();
                }
            }
        }

        return result;
    }

    public static String stripDoubleQuotes(String s) {
        StringBuilder r = new StringBuilder(s.length());
        r.setLength(s.length());
        int current = 0;
        for (int i = 0; i < s.length(); i++) {
            char cur = s.charAt(i);
            if (cur != '"') r.setCharAt(current++, cur);
        }
        return r.toString();
    }

    public static InputStreamReader getInputStream(String file) {
        JCATConfig config = JCATConfig.getInstance();
        InputStreamReader reader = null;
        try {
            if (config.fromJar()) {
                reader = new InputStreamReader(config.getClass().getResourceAsStream(file));
            } else {
                reader = new FileReader(file);
            }
        } catch (FileNotFoundException e) {
            JCATMessageWindow.show(e);
            e.printStackTrace();
        }
        return reader;
    }
}
