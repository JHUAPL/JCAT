package util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class LogRecordHandler extends StreamHandler {

    private final ArrayList<LogRecord> messages;

    public LogRecordHandler() {
        super();

        messages = new ArrayList<>();

        // write messages to stdout, not stderr
        LogManager.getLogManager().reset();
        setOutputStream(System.out);

    }

    public void publish(LogRecord record) {

        if (record.getLevel() == Level.WARNING || record.getLevel() == Level.SEVERE) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Error Message");
            alert.setHeaderText(null);
            alert.setContentText(record.getMessage());
            alert.showAndWait();
        }

        messages.add(record);
        super.publish(record);
        flush();

    }

    public ArrayList<LogRecord> getMessages(Level l) {

        ArrayList<LogRecord> toReturn = new ArrayList<>();
        int level = l.intValue();

        for (LogRecord a : messages) {

            if (a.getLevel().intValue() >= level) toReturn.add(a);

        }

        return toReturn;

    }

}
