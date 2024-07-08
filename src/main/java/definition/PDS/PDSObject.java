package definition.PDS;

import java.util.Vector;
import java.util.logging.Level;

import org.apache.commons.math3.util.Pair;

import util.FileUtils;
import util.JCATLog;

public class PDSObject {
  protected String objectName = null;
  protected Vector<Pair<String, String>> params = new Vector<Pair<String, String>>();
  protected Vector<PDSObject> subObjects = new Vector<PDSObject>();

  public PDSObject(String objName) {

    // if(objName==null)
    // return;

    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating PDSObject object");

    objectName = new String(objName);



  }

  public PDSObject(PDSObject obj) {
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating PDSObject object");
    copy(obj);
  }

  public String getName() {
    return objectName;
  }

  public void copy(PDSObject obj) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method copy");

    int i, size;

    params.clear();
    subObjects.clear();

    objectName = obj.objectName;

    size = obj.params.size();

    for (i = 0; i < size; i++) {
      Pair<String, String> inPair = obj.params.get(i);

      Pair<String, String> outPair =
          new Pair<String, String>(new String(inPair.getFirst()), new String(inPair.getSecond()));
      params.add(outPair);
    }

    size = obj.subObjects.size();

    for (i = 0; i < size; i++) {
      PDSObject pdsObj = new PDSObject(obj.subObjects.get(i));
      subObjects.add(pdsObj);
    }
  }

  public void addObject(PDSObject obj) {
    subObjects.add(obj);
  }

  public void removeObject(int index) {
    if ((index >= 0) && (index < subObjects.size())) {
      subObjects.remove(index);
    }
  }

  public void clearObjects() {
    subObjects.clear();
  }

  public PDSObject getObject(int index) {
    PDSObject result = null;

    if ((index >= 0) && (index < subObjects.size())) {
      result = subObjects.get(index);
    }

    return result;
  }

  public int getObjectCount() {
    return subObjects.size();
  }

  public PDSObject findObject(String objKey) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method findObject");

    PDSObject result = null;

    int size = subObjects.size();
    for (int i = 0; (i < size) && (result == null); i++) {
      if (objKey.equals(subObjects.get(i).objectName)) {
        result = subObjects.get(i);
      }
    }

    return result;
  }

  public void addParameter(String key, String value) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method addParameter");

    Pair<String, String> p = new Pair<String, String>(key, value);
    params.add(p);
  }

  public void addOrReplaceParameter(String key, String value) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method addOrReplaceParameter");

    Pair<String, String> p = findParameter(key);
    if (p != null) {
      params.remove(p);
    }
    p = new Pair<String, String>(key, value);
    params.add(p);
  }

  public String getString(String key) {
    String result = null;

    Pair<String, String> p = findParameter(key);
    if (p != null) {
      result = new String(p.getSecond());
    }

    return result;
  }

  public String getStringNoQuotes(String key) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getStringNoQuotes");

    String a = getString(key);

    if (a == null)
      return "";

    String result = new String(FileUtils.stripDoubleQuotes(getString(key).trim()));

    return result;
  }

  public double getDouble(String key) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getDouble");

    Pair<String, String> p = findParameter(key);
    String[] values = p.getSecond().split("\\s+");
    double result = Double.valueOf(values[0].trim());
    return result;
  }

  public int getInt(String key) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getInt");
    Pair<String, String> p = findParameter(key);
    String[] values = p.getSecond().split("\\s+");
    int result = Integer.valueOf(values[0].trim());
    return result;
  }

  public boolean hasParameter(String key) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method hasParameter");
    boolean result = false;

    Pair<String, String> p = findParameter(key);
    if (p != null) {
      result = true;
    }

    return result;
  }

  public Pair<String, String> findParameter(String key) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method findParameter");
    Pair<String, String> result = null;

    int size = params.size();

    String upKey = key.trim().toUpperCase();

    for (int i = 0; (i < size) && (result == null); i++) {
      Pair<String, String> p = params.get(i);
      if (p.getFirst().trim().toUpperCase().equals(upKey)) {
        result = p;
      }
    }

    return result;
  }

  public Vector<Pair<String, String>> findParameters(String key) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method findParameters");
    Vector<Pair<String, String>> results = new Vector<Pair<String, String>>();

    int size = params.size();

    String upKey = key.trim().toUpperCase();

    for (int i = 0; i < size; i++) {
      Pair<String, String> p = params.get(i);
      if (p.getFirst().trim().toUpperCase().equals(upKey)) {
        results.add(p);
      }
    }

    return results;
  }

  public boolean removeParameter(String key) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method removeParameter");
    boolean result = false;

    int size = params.size();

    String upKey = key.trim().toUpperCase();

    for (int i = 0; (i < size) && !result; i++) {
      Pair<String, String> p = params.get(i);
      if (p.getFirst().trim().toUpperCase().equals(upKey)) {
        result = true;
        params.remove(i);
      }
    }

    return result;
  }

  public void getObjectStrings(Vector<String> lines) {

    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getObjectStrings");
    int i, n;

    String line;

    n = params.size();

    for (i = 0; i < n; i++) {
      Pair<String, String> p = params.get(i);
      line = p.getFirst();
      line += " = ";
      line += p.getSecond();
      lines.add(line);
    }

    n = subObjects.size();

    for (i = 0; i < n; i++) {
      PDSObject obj = subObjects.get(i);
      line = "OBJECT = ";
      line += obj.getName();
      lines.add(line);

      obj.getObjectStrings(lines);

      line = "END_OBJECT = ";
      line += obj.getName();
      lines.add(line);
    }
  }
}
