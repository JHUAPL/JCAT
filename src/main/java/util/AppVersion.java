
package util;

public class AppVersion {
  private static String gitRevision = new String("5da0c38");
  private static String applicationName = new String("JCAT");
  private static String dateString = new String("2021-Jul-11 13:12:44 EDT");

  private AppVersion() {}

  /**
   * version 5da0c38 (built 2021-Jul-11 13:12:44 EDT)
   */
  public static String getVersionString() {
    return String.format("%s version %s (built %s)", applicationName, gitRevision, dateString);
  }
}
