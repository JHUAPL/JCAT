package javafx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import reader.CRISMPDSImageNextGen;
import util.JCATLog;

public class MTRDRSummaryParameters extends JCATCalls {

  private int bandNum;
  private ComboBox<String> summaryParameters;
  private int spCode = 1;

  protected float[][][] mappedData_;
  private float[][] currentSP;

  public boolean create(String imgfile, String basefile) throws IOException {

    String baseF = basefile;
    ///////////////////////////////////////
    // Establishes rootDir as the JCAT folder in home directory
    ///////////////////////////////////////
    File rootDir = new File(System.getProperty("user.home"), "JCAT");
    if (!rootDir.isDirectory()) {
      rootDir.mkdirs();
    }

    ///////////////////////////////////////
    // Instantiates CRISM based on 's', 'l' or 'j' data ID as CRISMPDSImageNextGen
    ///////////////////////////////////////
    basefile = FilenameUtils.getBaseName(basefile);
    if (basefile.substring(20, 21).toLowerCase().equals("j")) {

      CRISM = new CRISMPDSImageNextGen(imgfile);
    }

    if (!CRISM.validImage)
      return false;

    int height = (int) CRISM.getHeight(), width = (int) CRISM.getWidth();
    int startRow = height / 2, startCol = width / 2;

    ///////////////////////////////////////////////////
    // Creates a menu bar with multiple menus
    // Toggle groups/Radio Menu Items ensure only one if selected at a time
    // Disables the zoom menus that will cause sizing issues
    // Check Menu Item for subset/photometric can be selected/de-selected anytime
    ///////////////////////////////////////////////////
    if ((height * 0.5) < 300 || (width * 0.5) < 300) {
      zoom50.setDisable(true);
    }
    zoom50.setToggleGroup(zoomTG);
    if ((height * 0.75) < 300 || (width * 0.75) < 300) {
      zoom75.setDisable(true);
    }
    zoom75.setToggleGroup(zoomTG);
    zoom100.setToggleGroup(zoomTG);
    zoom100.setSelected(true);
    zoom125.setToggleGroup(zoomTG);
    zoom150.setToggleGroup(zoomTG);
    zoom200.setToggleGroup(zoomTG);

    linStretch.setToggleGroup(contrastTG);
    per1Stretch.setToggleGroup(contrastTG);
    per1Stretch.setSelected(true);
    per2Stretch.setToggleGroup(contrastTG);
    perCStretch.setToggleGroup(contrastTG);
    contrast.getItems().addAll(linStretch, per1Stretch, per2Stretch, perCStretch, subStretch);

    fileMenu.getItems().addAll(expandImg, saveImg, new SeparatorMenuItem(), close);
    zoomMenu.getItems().addAll(zoom50, zoom75, zoom100, zoom125, zoom150, zoom200);
    controlsMenu.getItems().addAll(contrast);

    Menu browseProducts = new Menu("View Browse Products");
    controlsMenu.getItems().add(browseProducts);
    browseProducts.setOnAction(e -> {
      try {
        new MTRDRBrowseProducts().create(imgfile, baseF);
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    });

    mbar.getMenus().addAll(fileMenu, zoomMenu, controlsMenu);

    /////////////////////////////////////////
    // Creates the layout for the image frame:
    // borderTB: top, bottom - bottom will be line chart
    // borderLR: left, right - R will hold slider, coordinates, update RGB button
    // border TL: top left (left side), top left (right side) - houses two images
    // Housed in a scroll pane to allow users to move about the window
    /////////////////////////////////////////
    BorderPane borderTB = new BorderPane(), borderLR = new BorderPane(),
        borderTL = new BorderPane();
    GridPane controls = new GridPane();
    ScrollPane mainSP = new ScrollPane();

    VBox root2 = new VBox();
    root2.getChildren().addAll(mbar, mainSP);

    controls.setPadding(new Insets(10, 10, 10, 10));
    controls.setVgap(5);
    controls.setHgap(30);

    //////////////////////////////////////////
    // Creates a canvas for cross-hair, rectangle and region of interest will be
    // drawn on
    //////////////////////////////////////////
    Canvas canvas = new Canvas(width, height);

    bImg = percentileStretch(CRISM.getQuickMonoImage(0), 1);

    image = SwingFXUtils.toFXImage(bImg, null);

    ImageView iv = new ImageView(image), iv2 = new ImageView(image);
    ImageView iv3 = new ImageView(), iv4 = new ImageView();
    Group blend = new Group(iv, iv3), blend2 = new Group(iv2, iv4);
    Group finalImg = new Group(blend), finalImg2 = new Group(blend2);

    ScrollPane sp = new ScrollPane(finalImg), sp2 = new ScrollPane(finalImg2);
    sp.setPrefSize(300, 300);

    ///////////////////////////////////////////////////////////
    // Blends the image plus user reference (cross-hair, etc.) with a black
    // rectangle to create final display
    //////////////////////////////////////////////////////////
    img = canvas.snapshot(null, null);
    iv3.setImage(img);
    iv4.setImage(img);
    Rectangle black = new Rectangle(width, height);

    iv3.setBlendMode(BlendMode.DIFFERENCE);
    black.setFill(Color.BLACK);
    finalImg.setBlendMode(BlendMode.DIFFERENCE);
    sp.setContent(finalImg);

    iv4.setBlendMode(BlendMode.DIFFERENCE);
    black.setFill(Color.BLACK);
    finalImg2.setBlendMode(BlendMode.DIFFERENCE);
    sp2.setContent(finalImg2);

    /////////////////////////////////////////////
    // Creates another image view for the shrunken second image
    // Preserves the width-height ratio, but makes the larger quantity = 300
    // Does some math to figure what range-of-view rectangle height&width should be
    // and how the scroll bars will line up with the image
    /////////////////////////////////////////////
    ImageView imgZoom = new ImageView(image);
    imgZoom.setPreserveRatio(true);
    imgZoom.setFitHeight(300);
    imgZoom.setFitWidth(300);

    double ratio = (double) 300 / (Math.max(height, width));
    double adjHeight = height * ratio, adjWidth = width * ratio;
    double rectHeight = 300 * ratio, rectWidth = 300 * ratio;

    sp.setHmax(adjWidth - rectWidth);
    sp.setVmax(adjHeight - rectHeight);
    sp.setHvalue(sp.getHmax() / 2);
    sp.setVvalue(sp.getVmax() / 2);

    Rectangle rect = new Rectangle(300 + sp.getHvalue(), sp.getVvalue(), rectWidth, rectHeight);
    rect.setStroke(Color.BLACK);
    rect.setFill(null);
    rect.setStrokeWidth(3);

    ///////////////////////////////////////////////////////////
    // Creates labels, sliders, and value labels for RGB sliders
    // Sets wavelengths as values on slider, creates tick marks
    ///////////////////////////////////////////////////////////
    ObservableList<String> items = FXCollections.observableArrayList("R770", "RBR", "BD530_2",
        "SH600_2", "SH770", "BD640_2", "BD860_2", "BD920_2", "RPEAK1", "BDI1000VIS", "R440", "IRR1",
        "BDI1000IR", "OLINDEX3", "R1330", "BD1300", "LCPINDEX2", "HCPINDEX2", "VAR", "ISLOPE1",
        "BD1400", "BD1435", "BD1500_2", "ICER1_2", "BD1750_2", "BD1900_2", "BD1900R2", "BDI2000",
        "BD2100_2", "BD2165", "BD2190", "MIN2200", "BD2210_2", "D2200", "BD2230", "BD2250",
        "MIN2250", "BD2265", "BD2290", "D2300", "BD2355", "SINDEX2", "ICER2_2", "MIN2295_2480",
        "MIN2345_2537", "BD2500_2", "BD3000", "BD3100", "BD3200", "BD3400_2", "CINDEX2", "BD2600",
        "IRR2", "IRR3", "R530", "R600", "R1080", "R1506", "R2529", "R3920");

    summaryParameters = new ComboBox<>(items);
    Label nameSP = new Label("Current: None");

    Button updateBtn = new Button("Update Image");
    Label subLabel = new Label();

    ////////////////////////////////////////////////////
    // Creates coordinate label
    // Displays start row and column at center of image,
    // longitude and latitude
    /////////////////////////////////////////////////////
    longitude = CRISM.getLon(startRow, startCol);
    latitude = CRISM.getLat(startRow, startCol);
    Label crdValue =
        new Label("Data (row, col): (" + startRow + ", " + startCol + ")" + ", Latitude: "
            + String.format("%.4f", latitude) + ", Longitude: " + String.format("%.4f", longitude));
    crdValue.setMinWidth(325);

    ////////////////////////////////////////////////////
    // Sets location of slider, coordinate objects in top-right section of grid pane
    // Sets the content of the different border/scroll panes
    ////////////////////////////////////////////////////
    GridPane.setConstraints(crdValue, 1, 3);
    GridPane.setConstraints(updateBtn, 2, 3);
    GridPane.setConstraints(subLabel, 2, 4);

    GridPane.setConstraints(summaryParameters, 1, 2);

    GridPane.setConstraints(nameSP, 1, 4);

    controls.getChildren().addAll(summaryParameters, crdValue, updateBtn, subLabel, nameSP);

    borderTL.setLeft(sp);
    borderTL.setRight(imgZoom);
    borderLR.setLeft(borderTL);
    borderLR.getChildren().add(rect);
    borderLR.setRight(controls);
    borderTB.setTop(borderLR);
    mainSP.setContent(borderTB);
    mainSP.setFitToHeight(true);
    mainSP.setFitToWidth(true);

    //////////////////////////////////////////////
    // Creates the scene and stage for the second window to be shown
    // Creates the scene for the expanded image, can close and reopen
    //////////////////////////////////////////////
    Scene scene2 = new Scene(root2);
    Stage stage2 = new Stage();
    stage2.setTitle(basefile);
    stage2.setScene(scene2);
    stage2.show();

    Scene scene3 = new Scene(sp2, width, height);
    Stage stage3 = new Stage();
    stage3.setTitle(basefile);
    stage3.setScene(scene3);

    //////////////////////////////////////////////
    // Gets the x, y, w, h of visible area in scroll pane
    // This is used during a subset stretch
    // NOTE: Must be run AFTER scene is shown for viewportBounds to exist
    //////////////////////////////////////////////
    double hmin = sp.getHmin(), hmax = sp.getHmax(), hvalue = sp.getHvalue();
    double contentWidth = iv.getLayoutBounds().getWidth() * (1 / scaleFactor);
    visW = (int) (sp.getViewportBounds().getWidth() * (1 / scaleFactor));
    visX = (int) (Math.max(0, contentWidth - visW) * (hvalue - hmin) / (hmax - hmin));

    double vmin = sp.getVmin(), vmax = sp.getVmax(), vvalue = sp.getVvalue();
    double contentHeight = iv.getLayoutBounds().getHeight() * (1 / scaleFactor);
    visH = (int) (sp.getViewportBounds().getHeight() * (1 / scaleFactor));
    visY = (int) (Math.max(0, contentHeight - visH) * (vvalue - vmin) / (vmax - vmin));

    ////////////////////////////////////////////////
    // Opens a third window and expands image to its full size
    ///////////////////////////////////////////////
    expandImg.setOnAction(event -> {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Expanding Image");
      stage3.show();
    });

    //////////////////////////////////////////////////
    // Saves the currently shown image, with RGB values in filename
    //////////////////////////////////////////////////
    saveImg.setOnAction(event -> {

      TextInputDialog input = new TextInputDialog();
      input.setTitle("File Name");
      input.setContentText("Set filename: ");
      String result = tidy(input.showAndWait());

      JCATLog.getInstance().getLogger().log(Level.INFO,
          "Saving shown image to: " + result + ".png");

      File f = new File(result + ".png");
      BufferedImage biSave = SwingFXUtils.fromFXImage(iv.getImage(), null);
      try {
        ImageIO.write(biSave, "PNG", f);
      } catch (IOException e1) {
        JCATLog.getInstance().getLogger().log(Level.WARNING, "Error saving image");
        e1.printStackTrace();
      }
    });

    //////////////////////////////////////////////////////
    // Adjusts zoom of image, range-of-view rectangle size and sets scroll pane to
    // the same position
    //////////////////////////////////////////////////////
    zoomTG.selectedToggleProperty().addListener((obs, old_toggle, new_toggle) -> {
      JCATLog.getInstance().getLogger().log(Level.FINE, "Adjusting Zoom to selected level");
      scaleFactor = (zoom50.isSelected()) ? 0.5 : scaleFactor;
      scaleFactor = (zoom75.isSelected()) ? 0.75 : scaleFactor;
      scaleFactor = (zoom100.isSelected()) ? 1 : scaleFactor;
      scaleFactor = (zoom125.isSelected()) ? 1.25 : scaleFactor;
      scaleFactor = (zoom150.isSelected()) ? 1.5 : scaleFactor;
      scaleFactor = (zoom200.isSelected()) ? 2 : scaleFactor;
      int hVal = (int) sp.getHvalue();
      int vVal = (int) sp.getVvalue();
      int hMax = (int) sp.getHmax();
      int vMax = (int) sp.getVmax();
      iv.setPreserveRatio(true);
      iv.setFitHeight(height * scaleFactor);
      iv.setFitWidth(width * scaleFactor);
      iv3.setPreserveRatio(true);
      iv3.setFitHeight(height * scaleFactor);
      iv3.setFitWidth(width * scaleFactor);
      rect.setHeight(rectHeight * (1 / scaleFactor));
      rect.setWidth(rectWidth * (1 / scaleFactor));
      sp.setHmax(adjWidth - rect.getWidth());
      sp.setVmax(adjHeight - rect.getHeight());
      sp.setHvalue(hVal * (sp.getHmax() / hMax));
      sp.setVvalue(vVal * (sp.getVmax() / vMax));
    });

    ////////////////////////////////////////////////////////////////////
    // Sets the rectangle to correct position after scroll bars have been moved
    // Whenever scale or scroll bars change, the visible bounds adjust accordingly
    // This is important for the subset image stretches
    ////////////////////////////////////////////////////////////////////
    ChangeListener<Object> changeListener = new ChangeListener<Object>() {
      @Override
      public void changed(ObservableValue<? extends Object> observable, Object oldValue,
          Object newValue) {
        JCATLog.getInstance().getLogger().log(Level.FINEST,
            "Entering method changed in launch class");
        double hmin = sp.getHmin(), hmax = sp.getHmax(), hvalue = sp.getHvalue();
        double contentWidth = iv.getLayoutBounds().getWidth() * (1 / scaleFactor);
        visW = (int) (sp.getViewportBounds().getWidth() * (1 / scaleFactor));
        visX = (int) (Math.max(0, contentWidth - visW) * (hvalue - hmin) / (hmax - hmin));
        rect.setX(300 + hvalue);

        double vmin = sp.getVmin(), vmax = sp.getVmax(), vvalue = sp.getVvalue();
        double contentHeight = iv.getLayoutBounds().getHeight() * (1 / scaleFactor);
        visH = (int) (sp.getViewportBounds().getHeight() * (1 / scaleFactor));
        visY = (int) (Math.max(0, contentHeight - visH) * (vvalue - vmin) / (vmax - vmin));
        rect.setY(vvalue);
      }
    };
    sp.viewportBoundsProperty().addListener(changeListener);
    sp.hvalueProperty().addListener(changeListener);
    sp.vvalueProperty().addListener(changeListener);

    ////////////////////////////////////////////////////
    // Displays new row/col & lat/lon location whenever mouse moves on image
    // Row/col are 1:1 ratio with image, with (0, 0) at top left of both
    ////////////////////////////////////////////////////
    finalImg.setOnMouseMoved(e -> {
      int newRow = (int) (e.getY() * (1 / scaleFactor));
      int newCol = (int) (e.getX() * (1 / scaleFactor));
      double longitude = CRISM.getLon(newRow, newCol);
      double latitude = CRISM.getLat(newRow, newCol);
      crdValue.setText("Data (row, col): (" + newRow + ", " + newCol + ")" + ", Latitude: "
          + String.format("%.4f", latitude) + ", Longitude: " + String.format("%.4f", longitude)
          + "\nValue: " + CRISM.getPCFloatValue(newRow, newCol, bandNum));
    });

    finalImg2.setOnMouseMoved(e -> {
      int newRow = (int) (e.getY());
      int newCol = (int) (e.getX());
      double longitude = CRISM.getLon(newRow, newCol);
      double latitude = CRISM.getLat(newRow, newCol);
      crdValue.setText("Data (row, col): (" + newRow + ", " + newCol + ")" + "Latitude: "
          + String.format("%.4f", latitude) + ", Longitude: " + String.format("%.4f", longitude));
    });

    ////////////////////////////////////////////////////////////
    // Gets the current chart, removes the RGB values and inputs the current values
    // Checks what is selected in contrast menu
    // Applies stretches with new RGB values
    // Checks whether subset stretch is selected,
    // Else use entire image for stretch
    ///////////////////////////////////////////////////////////
    updateBtn.setOnAction(e -> {
      spCode = summaryParameters.getItems().indexOf(summaryParameters.getValue());

      nameSP.setText("Current: " + CRISM.bandNames.get(spCode));

      BufferedImage newBI = CRISM.getQuickColorImage(spCode, spCode, spCode);// CRISM.getQuickMonoImage(spNum);
      bImg = stretchImage(newBI);
      if (bImg != null && subStretch.isSelected())
        subLabel
            .setText("Row: " + visX + "-" + (visX + visW) + ", Col: " + visY + "-" + (visY + visH));
      else
        subLabel.setText(null);
      /*-
      List<Double> stretchValues = null;
      if (perCStretch.isSelected()) {
      	TextInputDialog input = new TextInputDialog();
      	input.setTitle("Percentile Stretch");
      	input.setContentText("Input Percentile (Value): ");
      	Optional<String> result = input.showAndWait();
      	double stretch = Double.parseDouble(tidy(result));
      	if (result.isPresent()) {
      		if (subStretch.isSelected()) {
      			stretchValues = getStretchValues(newBI.getSubimage(visX, visY, visW, visH), stretch);
      			bImg = percentileSubStretch(newBI, stretchValues);
      			subLabel.setText("Row: " + visX + "-" + (visX + visW) + ", Col: " + visY + "-" + (visY + visH));
      		} else {
      			bImg = percentileStretch(newBI, stretch);
      		}
      	}
      } else {
      	if (subStretch.isSelected()) {
      		if (linStretch.isSelected()) {
      
      			stretchValues = getStretchValues(newBI.getSubimage(visX, visY, visW, visH), 0);
      			bImg = linSubStretch(newBI, stretchValues);
      		} else if (per1Stretch.isSelected()) {
      
      			stretchValues = getStretchValues(newBI.getSubimage(visX, visY, visW, visH), 1);
      			bImg = percentileSubStretch(newBI, stretchValues);
      		} else if (per2Stretch.isSelected()) {
      
      			stretchValues = getStretchValues(newBI.getSubimage(visX, visY, visW, visH), 2);
      			bImg = percentileSubStretch(newBI, stretchValues);
      		}
      		subLabel.setText("Row: " + visX + "-" + (visX + visW) + ", Col: " + visY + "-" + (visY + visH));
      	} else {
      		bImg = (linStretch.isSelected()) ? linearStretch(newBI) : bImg;
      		bImg = (per1Stretch.isSelected()) ? percentileStretch(newBI, 1) : bImg;
      		bImg = (per2Stretch.isSelected()) ? percentileStretch(newBI, 2) : bImg;
      		subLabel.setText(null);
      	}
      }
      */
      image = SwingFXUtils.toFXImage(bImg, null);
      iv.setImage(image);
      iv2.setImage(image);
      imgZoom.setImage(image);
    });

    /////////////////////////////////////////////////
    // Saves the input file to the recentFile.txt document for future use
    // Writes the property file for JCAT folder
    /////////////////////////////////////////////////
    saveRecentFiles(imgfile);

    return true;
  }

}
