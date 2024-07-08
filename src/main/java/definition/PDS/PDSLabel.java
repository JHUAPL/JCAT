package definition.PDS;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.logging.Level;

import org.apache.commons.math3.util.Pair;

import util.FileUtils;
import util.JCATLog;

public class PDSLabel extends PDSObject {
  protected String lblFilename = null;
  protected boolean validLabel = false;

  public PDSLabel(String filename) {

    super(filename);
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating PDSLabel object");
    readLabel(filename);

  }

  public boolean isValidLabel() {
    return validLabel;
  }

  public boolean readLabel(String filename) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readLabel");

    boolean result = false;

    // if (!PDSLabel.isLabelFile(filename))
    // return result;

    lblFilename = new String(filename);

    try {
      Vector<String> txtLines = new Vector<String>();// readLabelFile(filename);
      FileUtils.readAsciiFile(filename, txtLines);
      Vector<String> lblLines = new Vector<String>();

      String currentLine = new String("");

      int size = txtLines.size();
      int i;

      boolean inQuote = false;

      for (i = 0; i < size; i++) {
        String s = txtLines.get(i).trim();
        if (s.length() > 0) {
          // strip out comments, from
          // https://blog.ostermiller.org/find-comment
          s = s.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
          int qCount = getQuoteCount(s);
          if (inQuote) {
            currentLine += s;
          } else {
            if (checkForEquals(s)) {
              if (currentLine.length() > 0) {

                lblLines.add(currentLine);
                currentLine = new String("");
              }
            }
            String line = new String(currentLine.trim());
            if (line.startsWith("/*") && line.endsWith("*/")) {
            } else {
              currentLine += ((currentLine.length() > 0) ? " " : "") + s;
            }
          }

          if (qCount == 1) {
            inQuote = false;
          }

          if (s.toUpperCase().equals("END"))
            i = size;
        }
      }

      if (currentLine.trim().length() > 0) {
        lblLines.add(currentLine);
      }

      processLabelLines(lblLines);

      result = true;
    } catch (Exception e) {
      System.out.println("LabelReader.readLabel: " + e);
      e.printStackTrace();
    }

    return result;
  }

  protected void processLabelLines(Vector<String> lines) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method processLabelLines");

    int i;
    int size = lines.size();

    Vector<PDSObject> subObjs = new Vector<PDSObject>();

    for (i = 0; i < size; i++) {
      String line = lines.get(i);

      // System.out.printf("processLabelLines: %s\n", line);


      String parts[] = line.trim().split("=");

      if (parts.length >= 2) {
        if (i == 0) {
          if (parts[0].trim().equals("PDS_VERSION_ID"))
            validLabel = true;
        }

        if (parts[0].trim().toUpperCase().equals("OBJECT")) {
          PDSObject o = new PDSObject(parts[1].trim());
          subObjs.add(o);
        } else if (parts[0].trim().toUpperCase().equals("END_OBJECT")) {
          int subSize = subObjs.size();

          if (subSize == 0) {
            System.out.println("We hit an END_OBJECT without having a corresponding OBJECT.");
          } else if (subSize == 1) {
            addObject(subObjs.get(0));
            subObjs.clear();
          } else if (size > 1) {
            PDSObject lastObj = subObjs.get(subSize - 1);
            PDSObject nextToLast = subObjs.get(subSize - 2);

            nextToLast.addObject(lastObj);
            subObjs.remove(subSize - 1);
          }
        } else {
          int subSize = subObjs.size();

          if (subSize > 0) {
            subObjs.get(subSize - 1).addParameter(parts[0].trim(), parts[1].trim());
            // System.out.println(o.objectName + "." +
            // parts[0].trim() + " = " + parts[1].trim());
          } else {
            params.add(new Pair<String, String>(parts[0].trim(), parts[1].trim()));
            // System.out.println(parts[0].trim() + " = " +
            // parts[1].trim());
          }
        }
      }
    }
  }

  protected boolean checkForEquals(String s) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method checkForEquals");

    boolean result = false;

    boolean inQuote = false;

    int length = s.length();
    for (int i = 0; (i < length) && !result; i++) {
      char c = s.charAt(i);
      if (c == '"') {
        inQuote = inQuote ? false : true;
      }
      if (!inQuote && (c == '=')) {
        result = true;
      }
    }

    return result;
  }

  protected int getQuoteCount(String s) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getQuoteCount");

    int result = 0;

    int length = s.length();
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if (c == '"') {
        result++;
      }
    }

    return result;
  }

  public Vector<String> readLabelFile(String filename) throws Exception {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readLabelFile");

    Vector<String> results = new Vector<String>();

    File f = new File(filename);
    if (f.exists()) {
      FileInputStream fileInput = new FileInputStream(filename);
      BufferedReader fileRdr = new BufferedReader(new InputStreamReader(fileInput));

      String s = new String(fileRdr.readLine().trim());

      if (s.trim().toUpperCase().startsWith("PDS_VERSION_ID")) {
        while (s != null) {

          results.add(s);

          if (s.trim().toUpperCase().equals("END")) {
            break;
          }

          s = fileRdr.readLine();
        }
      }

      fileRdr.close();
      fileInput.close();
    }

    return results;
  }

  public static boolean isLabelFile(String filename) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method isLabelFile");

    boolean result = false;

    File f = new File(filename);
    if (f.exists()) {
      try {
        FileInputStream fileInput = new FileInputStream(filename);
        // DataInputStream fileRdr = new DataInputStream(fileInput);
        BufferedReader fileRdr = new BufferedReader(new InputStreamReader(fileInput));

        try {
          String testStr = new String("");

          final int maxChars = 512;
          int nCharsRead = 0;
          char[] buff = new char[maxChars];

          try {
            nCharsRead = fileRdr.read(buff, 0, maxChars);
          } catch (EOFException eofEx) {
          }

          if (nCharsRead > 0) {
            testStr = new String(buff);
            if (testStr.contains("PDS_VERSION_ID")) {
              result = true;
            }
          }
        } catch (IOException ioEx) {
        }

        try {
          fileRdr.close();
          fileInput.close();
        } catch (IOException io2Ex) {
        }
      } catch (FileNotFoundException fnfEx) {
      }
    }

    return result;
  }

  public void getLabelStrings(String lblFilename, Vector<String> lines) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getLabelStrings");

    getObjectStrings(lines);

    lines.add("END");
  }

  public static void main(String[] argv) {
    // PDSLabel lbl = new
    // PDSLabel("C:\\dev\\apps\\crism\\mola\\megr44s090hb.lbl");
    // PDSLabel lbl = new
    // PDSLabel("C:\\Data\\crism_pds_archive\\2006_347\\EDR\\FRT000035DB_07_SC164L_EDR0.LBL");
    PDSLabel lbl =
        new PDSLabel("C:\\Data\\crism_pds_archive\\2006_347\\DDR\\FRT000035DB_07_DE164S_DDR1.LBL");
    // "C:\\Data\\crism_pds_archive\\2006_347\\DDR\\FRT000035DB_07_DE164S_DDR1.IMG"
    if (lbl != null) {
      System.out.println("");
    }



  }
}
