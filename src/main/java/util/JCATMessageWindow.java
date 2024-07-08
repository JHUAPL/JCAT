package util;

import org.apache.commons.lang3.exception.ExceptionUtils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;

public class JCATMessageWindow {

  public static void show(Exception e) {
    TextArea textArea = new TextArea(ExceptionUtils.getStackTrace(e));
    textArea.setEditable(false);
    textArea.setWrapText(true);
    GridPane gridPane = new GridPane();
    gridPane.setMaxWidth(Double.MAX_VALUE);
    gridPane.add(textArea, 0, 0);

    Alert alert = new Alert(AlertType.ERROR);
    alert.setHeaderText("An error occurred ... stack trace below:");
    alert.setTitle("Java Exception");
    alert.getDialogPane().setContent(gridPane);
    alert.showAndWait();
  }

  public static void show(String s) {
    show(new RuntimeException(s));
  }

}
