package javafx;

import java.awt.Color;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import util.AppVersion;
import util.JCATConfig;
import util.JCATLog;
import util.JCATMessageWindow;

public class buildGUI extends launch {

  public static void main(String args[]) {
    Application.launch(args);
  }

  @Override
  public void start(Stage stage) {
    try {
      JCATLog.getInstance().getLogger().log(Level.INFO, AppVersion.getVersionString());
      JCATLog.getInstance().getLogger().log(Level.INFO,
          "Running Java " + System.getProperty("java.version"));
      JCATLog.getInstance().getLogger().log(Level.INFO, "Initiating UI");
      initUI(stage);
    } catch (Exception e) {
      JCATLog.getInstance().getLogger().log(Level.SEVERE, "UI failed to initiate");
      JCATMessageWindow.show(e);
      e.printStackTrace();
    }
  }

  private void initUI(Stage stage1) throws Exception {
    ///////////////////////////////////////////////////////////
    // Creates main window, adds File, Browser and Help menus with menu items
    ///////////////////////////////////////////////////////////
    HBox root1 = new HBox();
    Scene scene1 = new Scene(root1, 400, 200);
    stage1.setTitle("Java CRISM Analysis Tool");
    stage1.setScene(scene1);
    stage1.setOnCloseRequest(e -> Platform.exit());
    stage1.show();

    MenuBar mbar = new MenuBar();
    mbar.prefWidthProperty().bind(stage1.widthProperty());

    Menu fileMenu = new Menu("File");
    Menu browserMenu = new Menu("Browser");
    Menu helpMenu = new Menu("Help");
    mbar.getMenus().addAll(fileMenu, browserMenu, helpMenu);

    ////////////////////////////////////////////
    // Launches window with filename, from home directory or recent files list
    ////////////////////////////////////////////
    MenuItem newFile = new MenuItem("New");
    fileMenu.getItems().add(newFile);
    newFile.setOnAction(event -> {
      try {
        JCATLog.getInstance().getLogger().log(Level.INFO, "Attempting to open new file");
        FileChooser fc = new FileChooser();
        fc.setTitle("New File");
        configureFileChooser(fc);
        File selectedFile = fc.showOpenDialog(stage1);
        if (selectedFile != null) {
          String imgname = selectedFile.toString();
          String basefile = FilenameUtils.getFullPath(imgname) + FilenameUtils.getBaseName(imgname);
          String imgfile = String.format("%s.%s", basefile, FilenameUtils.getExtension(imgname));

          if (FilenameUtils.getBaseName(imgname).substring(15).toLowerCase().startsWith("su")) {
            MTRDRSummaryParameters j = new MTRDRSummaryParameters();
            if (!j.create(imgfile, basefile)) {
              JCATLog.getInstance().getLogger().log(Level.WARNING,
                  "Error opening file: " + imgname);
              return;
            }
          } else {
            launch j = new launch();
            if (!j.create(imgfile, basefile)) {
              JCATLog.getInstance().getLogger().log(Level.WARNING,
                  "Error opening file: " + imgname);
              return;
            }
          }
          JCATLog.getInstance().getLogger().log(Level.INFO,
              "New file opened successfully: " + imgname);
          JCATConfig.getInstance().setLastDirectory(FilenameUtils.getFullPath(imgname));
        }
      } catch (Exception e) {
        JCATMessageWindow.show(e);
        e.printStackTrace();
      }
    });

    MenuItem recentFile = new MenuItem("Recent");
    fileMenu.getItems().add(recentFile);
    recentFile.setOnAction(event -> {
      try {
        Vector<String> RecentFiles = getRecentFiles();
        if (RecentFiles.size() > 0) {
          String recFiles = RecentFiles.get(0);
          ChoiceDialog<String> dialog = new ChoiceDialog<>(recFiles, RecentFiles);
          dialog.setTitle("Recent Files");
          dialog.setContentText("Choose a file: ");
          Optional<String> imgname = dialog.showAndWait();
          if (imgname.isPresent()) {
            String imgfile = tidy(imgname); // Changes type Optional<String> to String
            JCATLog.getInstance().getLogger().log(Level.INFO, "Opening recent file: " + imgfile);
            String basefile = imgfile.replace(".img", "");

            if (FilenameUtils.getBaseName(basefile).substring(15).toLowerCase().startsWith("su")) {
              MTRDRSummaryParameters j = new MTRDRSummaryParameters();
              if (!j.create(imgfile, basefile)) {
                JCATLog.getInstance().getLogger().log(Level.WARNING,
                    "Error opening file: " + imgfile);
                return;
              }
            } else {
              launch j = new launch();
              if (!j.create(imgfile, basefile)) {
                JCATLog.getInstance().getLogger().log(Level.WARNING,
                    "Error opening file: " + imgfile);
                return;
              }
            }
            JCATLog.getInstance().getLogger().log(Level.INFO, "File opened: " + imgfile);
          }
        }
      } catch (Exception e) {
        JCATLog.getInstance().getLogger().log(Level.WARNING,
            "Something went wrong opening recent file");
        JCATMessageWindow.show(e);
        e.printStackTrace();
      }
    });

    // Creates the menu item that displays the log when pressed
    MenuItem log = new MenuItem("Show Log");
    helpMenu.getItems().add(log);
    log.setOnAction(e -> {
      new logWindow();
    });

    fileMenu.getItems().add(new SeparatorMenuItem());

    /////////////////////////////////////////////////
    // Open browser to CRISM Map, PDS Data Product Repository, PDS Data Product
    ///////////////////////////////////////////////// Search
    /////////////////////////////////////////////////
    MenuItem CRISMBrowser = new MenuItem("CRISM MAP");
    browserMenu.getItems().add(CRISMBrowser);
    CRISMBrowser.setOnAction(event -> {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Opening CRISM Map");
      getHostServices().showDocument("http://crism-map.jhuapl.edu/");
    });

    MenuItem PDSDPRBrowser = new MenuItem("PDS Data Product Repository");
    browserMenu.getItems().add(PDSDPRBrowser);
    PDSDPRBrowser.setOnAction(event -> {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Opening PDS Data Product Repository");
      getHostServices().showDocument("http://pds-geosciences.wustl.edu/missions/mro/crism.htm");
    });

    MenuItem PDSDPSBrowser = new MenuItem("PDS Data Product Search");
    browserMenu.getItems().add(PDSDPSBrowser);
    PDSDPSBrowser.setOnAction(event -> {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Opening PDS Data Product Search");
      getHostServices().showDocument("http://ode.rsl.wustl.edu/mars/indexproductsearch.aspx");
    });

    MenuItem docHelp = new MenuItem("Tutorial");
    helpMenu.getItems().add(docHelp);
    docHelp.setOnAction(event -> {
      try {
        JCATLog.getInstance().getLogger().log(Level.INFO, "Opening tutorial");
        File f = new File(
            JCATConfig.getInstance().getLocalArchive() + File.separator + "JCAT_Tutorial.pdf");
        FileUtils.copyInputStreamToFile(
            getClass().getResourceAsStream("/resources/JCAT_Tutorial.pdf"), f);
        getHostServices().showDocument(f.toURI().toString());
      } catch (Exception e) {
        JCATLog.getInstance().getLogger().log(Level.WARNING, "Error getting documentation");
        JCATMessageWindow.show(e);
        e.printStackTrace();
      }
    });

    //////////////////////////////////////////////////////////////
    // Display current JCAT Version Number
    //////////////////////////////////////////////////////////////
    MenuItem aboutHelp = new MenuItem("About");
    helpMenu.getItems().add(aboutHelp);
    aboutHelp.setOnAction(event -> {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Showing JCAT Version");
      VBox vbox = new VBox();
      Dialog<Boolean> d = new Dialog<Boolean>();
      d.setTitle("About");
      d.setHeaderText(AppVersion.getVersionString());
      d.setContentText(
          "Authors: \nDavid Stephens \nMichael Chen \nAntonio Karides \nHari Nair\n\nFor support, please contact Hari.Nair@jhuapl.edu \n\nJohns Hopkins University Applied Physics Lab");
      d.setGraphic(vbox);
      Button close = new Button("Close");
      vbox.getChildren().add(close);
      d.show();
      close.setOnAction(e -> {
        d.setResult(Boolean.TRUE);
        d.close();
      });
    });

    MenuItem quit = new MenuItem("Quit");
    quit.setOnAction(event -> {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Quitting JCAT");
      Platform.exit();
    });

    fileMenu.getItems().add(quit);
    root1.getChildren().add(mbar);
  }

  ///////////////////////////////////////////////////////////
  // Sets initial directory and .img extension filters
  ///////////////////////////////////////////////////////////
  public void configureFileChooser(final FileChooser fileChooser) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method configureFileChooser in buildGUI class");
    fileChooser.setInitialDirectory(JCATConfig.getInstance().getLastWorkingDirectory());
    fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("IMG", "*.img"));
  }

  public class logWindow {

    private JFrame frmLog;
    private JComboBox<Level> levels;
    private TextArea logMessages;

    public logWindow() {
      levels = new JComboBox<Level>();
      levels.setBounds(12, 13, 223, 36);
      levels.setModel(new DefaultComboBoxModel<Level>(new Level[] {Level.SEVERE, Level.WARNING,
          Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST}));

      levels.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          ArrayList<String> messages =
              (JCATLog.getInstance().getLog((Level) levels.getSelectedItem()));
          String allMessages = "";
          for (String a : messages)
            allMessages += a.toString() + "\n";

          logMessages.setText(allMessages);
        }
      });

      initialize();

      JButton btnSave = new JButton("Save");
      btnSave.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          JFileChooser chooser = new JFileChooser();

          int retrival = chooser.showSaveDialog(null);
          if (retrival == JFileChooser.APPROVE_OPTION) {

            try (FileWriter fw = new FileWriter(chooser.getSelectedFile() + ".txt")) {
              fw.write(logMessages.getText());
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      });
      btnSave.setBounds(523, 377, 97, 25);
      frmLog.getContentPane().add(btnSave);

      frmLog.setLocationRelativeTo(null);
      frmLog.setVisible(true);
    }

    public TextArea getText() {
      return logMessages;
    }

    private void initialize() {
      frmLog = new JFrame();
      frmLog.setResizable(false);
      frmLog.setTitle("JCAT Log");
      frmLog.setBounds(100, 100, 650, 444);
      frmLog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frmLog.getContentPane().setLayout(null);

      frmLog.getContentPane().add(levels);

      JPanel panel = new JPanel();
      panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Log",
          TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      panel.setBounds(12, 51, 608, 319);
      frmLog.getContentPane().add(panel);
      panel.setLayout(null);

      logMessages = new TextArea();
      logMessages.setEditable(false);
      logMessages.setBounds(6, 20, 592, 293);
      panel.add(logMessages);

      levels.setSelectedItem(Level.INFO);
      levels.getActionListeners()[0].actionPerformed(null);

    }
  }
}
