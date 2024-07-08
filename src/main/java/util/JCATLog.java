package util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class JCATLog {


  // Set logging level for JCAT (Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE,
  // Level.FINER, Level.FINEST)
  private static final Level loggingLevel = Level.FINER;

  private static JCATLog instance = null;

  private final Logger logger;
  private final LogRecordHandler handler;

  private JCATLog() {

    System.setProperty("java.util.logging.SimpleFormatter.format",
        "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
    logger = Logger.getGlobal();
    handler = new LogRecordHandler();
    logger.addHandler(handler);

    logger.setLevel(loggingLevel);

  }


  public static JCATLog getInstance() {

    if (instance == null)
      instance = new JCATLog();

    return instance;

  }

  public ArrayList<String> getLog(Level l) {

    ArrayList<LogRecord> messages = handler.getMessages(l);
    ArrayList<String> toReturn = new ArrayList<>();

    for (LogRecord a : messages)
      toReturn.add(
          String.format("%-23.23s\t%-30.30s\t%-15.15s\t%s", new Timestamp(a.getMillis()),
              a.getSourceClassName(), a.getLevel(), a.getMessage()));

    return toReturn;

  }

  public Logger getLogger() {

    return logger;

  }

}
