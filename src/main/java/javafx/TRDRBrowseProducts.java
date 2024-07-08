package javafx;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import reader.TRDR;
import util.JCATConfig;
import util.JCATLog;
import util.JCATMessageWindow;
import util.RBmaker;
import util.arrayMedian;
import util.spBandDepth;
import util.spShoulder;

public class TRDRBrowseProducts extends JCATCalls {

  private int bpCode = 1;
  private ComboBox<String> bpChoices;
  private String detectorID = null;
  private int redBand;
  private int grnBand;
  private int bluBand;

  private int toggle = 0;

  protected float[][][] mappedData_;
  private float[][][] raster_ = new float[3][][];

  private float[][] R770;
  private float[][] RBR;
  private float[][] BD530_2;
  private float[][] SH600_2;
  private float[][] SH770;
  private float[][] BD640_2;
  private float[][] BD860_2;
  private float[][] BD920_2;
  private float[][] RPEAK1;
  private float[][] BDI1000VIS;
  private float[][] BDI1000IR;
  private float[][] R1330;
  private float[][] BD1300;
  private float[][] OLINDEX3;
  private float[][] LCPINDEX2;
  private float[][] HCPINDEX2;
  private float[][] VAR;
  private float[][] ISLOPE1;
  private float[][] BD1400;
  private float[][] BD1435;
  private float[][] BD1500_2;
  private float[][] ICER1_2;
  private float[][] BD1750_2;
  private float[][] BD1900_2;
  private float[][] BD1900r2;
  private float[][] BDI2000;
  private float[][] BD2100_2;
  private float[][] BD2165;
  private float[][] BD2190;
  private float[][] MIN2200;
  private float[][] BD2210_2;
  private float[][] D2200;
  private float[][] BD2230;
  private float[][] BD2250;
  private float[][] MIN2250;
  private float[][] BD2265;
  private float[][] BD2290;
  private float[][] D2300;
  private float[][] BD2355;
  private float[][] SINDEX2;
  private float[][] ICER2_2;
  private float[][] MIN2295_2480;
  private float[][] MIN2345_2537;
  private float[][] BD2500_2;
  private float[][] BD3000;
  private float[][] BD3100;
  private float[][] BD3200;
  private float[][] BD3400_2;
  private float[][] CINDEX2;
  private float[][] R440;
  private float[][] R530;
  private float[][] R600;
  private float[][] IRR1;
  private float[][] R1080;
  private float[][] R1506;
  private float[][] R2529;
  private float[][] BD2600;
  private float[][] IRR2;
  private float[][] IRR3;
  private float[][] R3920;
  private float[][] R614;
  private float[][] R533;
  private float[][] R716;
  private float[][] R920;
  private float[][] R807;
  private float[][] R984;
  private float[][] slope;
  private float[][] yint;

  public TRDRBrowseProducts() {
    // TODO Auto-generated constructor stub
  }

  public boolean create(String imgfile, String basefile) throws IOException {

    ///////////////////////////////////////
    // Establishes rootDir as the JCAT folder in home directory
    ///////////////////////////////////////
    File rootDir = new File(System.getProperty("user.home"), "JCAT");
    if (!rootDir.isDirectory()) {
      rootDir.mkdirs();
    }

    basefile = FilenameUtils.getBaseName(basefile);

    ///////////////////////////////////////
    // Instantiates CRISM based on 's', 'l' data ID as TRDR
    // If TRDR, ADRVSLibrary is created for potential volcano scan corrections
    // adrNames becomes a list of correct bin code and wavelength filter
    // created BEFORE the TRDR file
    // adrIDList and adrIDItems contain the ID's of the applicable ADR's
    // correctADRfiles holds the most recent of each ADR ID to
    // be called during volcano scan implementation
    ///////////////////////////////////////
    ddrFile = imgfile.replaceAll("if", "de").replaceAll("trr3", "ddr1");

    CRISM = new TRDR(imgfile, ddrFile);

    if (!((TRDR) CRISM).ddrExists())
      return false;

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
    ObservableList<String> options;
    File userBP = null;
    Vector<String[]> list = null;
    if (detectorID == "VNIR") {

      options = FXCollections.observableArrayList("TRU", "VNA", "FEM", "FM2");
      detectorID = "VNIR";
      userBP = new File(rootDir, "userBP_sTRDR.txt");
      list = getRecentBP_s();
    } else {

      options = FXCollections.observableArrayList("IRA", "FAL", "MAF", "HYD", "PHY", "PFM", "PAL",
          "HYS", "ICE", "IC2", "CHL", "CAR", "CR2");
      detectorID = "IR";
      userBP = new File(rootDir, "userBP_lTRDR.txt");
      list = getRecentBP_l();
    }

    bpChoices = new ComboBox<String>(options);

    List<String> userName = new ArrayList<String>();
    if (userBP.exists()) {
      JCATLog.getInstance().getLogger().log(Level.INFO,
          "Adding user-created browse products to menu");
      for (int i = 0; i < list.size(); i++) {
        String[] BP = list.get(i);
        String name = BP[0];
        userName.add(name);
      }
    }
    bpChoices.getItems().addAll(userName);

    Label nameSP = new Label("Current: " + bpChoices.getValue());

    Button okBtn = new Button("Set RGB");
    ObservableList<String> items = null;
    if (detectorID == "VNIR") {
      items = FXCollections.observableArrayList("R770", "RBR", "BD530_2", " SH600_2", "SH770",
          "BD640_2", "BD860_2", "BD920_2", "R600", "R530", "R440", "IRR1");
    } else if (detectorID == "IR") {
      items = FXCollections.observableArrayList("R1330", "BD1300", "OLINDEX3", "LCPINDEX2",
          "HCPINDEX2", "ISLOPE1", "BD1400", "BD1435", "BD1500_2", "ICER1_2", "BD1750_2", "BD1900_2",
          "BD1900r2", "BD2100_2", "BD2165", "BD2190", "MIN2200", "BD2210_2", "D2200", "BD2230",
          "BD2250", "MIN2250", "BD2265", "BD2290", "D2300", "BD2355", "SINDEX2", "ICER2_2",
          "MIN2295_2480", "MIN2345_2537", "BD2500_2", "BD3000", "BD3100", "BD3200", "BD3400_2",
          "CINDEX2", "R1080", "R1506", "R2529", "BD2600", "IRR2", "IRR3", "R3920");
    }

    ComboBox<String> red = new ComboBox<>(items);
    ComboBox<String> green = new ComboBox<>(items);
    ComboBox<String> blue = new ComboBox<>(items);

    Button updateBtn = new Button("Update Image");
    Button addBtn = new Button("Add new browse product");
    Button saveBtn = new Button("Save new browse product");
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
    GridPane.setConstraints(addBtn, 2, 4);
    GridPane.setConstraints(saveBtn, 2, 5);
    GridPane.setConstraints(subLabel, 2, 6);

    GridPane.setConstraints(bpChoices, 1, 2);
    GridPane.setConstraints(nameSP, 1, 4);
    controls.getChildren().addAll(crdValue, updateBtn, subLabel, addBtn, saveBtn, nameSP,
        bpChoices);

    borderTL.setLeft(sp);
    borderTL.setRight(imgZoom);
    borderLR.setLeft(borderTL);
    borderLR.getChildren().add(rect);
    borderLR.setRight(controls);
    borderTB.setTop(borderLR);
    mainSP.setContent(borderTB);
    mainSP.setFitToHeight(true);
    mainSP.setFitToWidth(true);

    mappedData_ = this.getMappedData();
    int rasterH = mappedData_[0].length;
    int rasterW = mappedData_[0][0].length;

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
          "Saving shown image as: " + result + ".png");

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
          + "\nRed: " + raster_[0][newRow][newCol] + " Green:" + raster_[1][newRow][newCol]
          + " Blue:" + raster_[2][newRow][newCol]);
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
      JCATLog.getInstance().getLogger().log(Level.INFO, "Updating browse product");
      bpCode = bpChoices.getItems().indexOf(bpChoices.getValue()) + 1;

      calculateBands(imgfile);
      ObservableList<float[][]> floats = (detectorID == "VNIR")
          ? FXCollections.observableArrayList(R770, RBR, BD530_2, SH600_2, SH770, BD640_2, BD860_2,
              BD920_2, R600, R530, R440, IRR1)
          : FXCollections.observableArrayList(R1330, BD1300, OLINDEX3, LCPINDEX2, HCPINDEX2,
              ISLOPE1, BD1400, BD1435, BD1500_2, ICER1_2, BD1750_2, BD1900_2, BD1900r2, BD2100_2,
              BD2165, BD2190, MIN2200, BD2210_2, D2200, BD2230, BD2250, MIN2250, BD2265, BD2290,
              D2300, BD2355, SINDEX2, ICER2_2, MIN2295_2480, MIN2345_2537, BD2500_2, BD3000, BD3100,
              BD3200, BD3400_2, CINDEX2, R1080, R1506, R2529, BD2600, IRR2, IRR3, R3920);

      // removed BDI1000IR, VAR, BDI2000 from IR
      // removed RPEAK1, BDI1000VIS
      // if including them, be sure to check spCode executes correct summary parameter

      // removed BP FEM, FM2 for use of BDI1000VIS

      if (toggle == 0) {
        nameSP.setText("Current: " + bpChoices.getValue());

        if (detectorID == "VNIR") {
          // TRU
          if (bpCode == 1) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = R600[i][j];
                raster_[1][i][j] = R530[i][j];
                raster_[2][i][j] = R440[i][j];
              }
            }
          }
          // VNA
          else if (bpCode == 2) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = R770[i][j];
                raster_[1][i][j] = R770[i][j];
                raster_[2][i][j] = R770[i][j];
              }
            }
          }
          // // FEM
          // else if (bpCode == 3) {
          // for (int i = 0; i < rasterH; i++) {
          // for (int j = 0; j < rasterW; j++) {
          // raster_[0][i][j] = BD530_2[i][j];
          // raster_[1][i][j] = SH600_2[i][j];
          // raster_[2][i][j] = BDI1000VIS[i][j];
          // }
          // }
          //
          // }
          // // FM2
          // else if (bpCode == 4) {
          // for (int i = 0; i < rasterH; i++) {
          // for (int j = 0; j < rasterW; j++) {
          // raster_[0][i][j] = BD530_2[i][j];
          // raster_[1][i][j] = BD920_2[i][j];
          // raster_[2][i][j] = BDI1000VIS[i][j];
          // }
          // }
          // }
        } else {
          // IRA
          if (bpCode == 1) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = R1330[i][j];
                raster_[1][i][j] = R1330[i][j];
                raster_[2][i][j] = R1330[i][j];
              }
            }
          }
          // FAL
          else if (bpCode == 2) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = R2529[i][j];
                raster_[1][i][j] = R1506[i][j];
                raster_[2][i][j] = R1080[i][j];
              }
            }
          }
          // MAF
          else if (bpCode == 3) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = OLINDEX3[i][j];
                raster_[1][i][j] = LCPINDEX2[i][j];
                raster_[2][i][j] = HCPINDEX2[i][j];
              }
            }
          }
          // HYD
          else if (bpCode == 4) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = SINDEX2[i][j];
                raster_[1][i][j] = BD2100_2[i][j];
                raster_[2][i][j] = BD1900_2[i][j];
              }
            }
          }
          // PHY
          else if (bpCode == 5) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = D2300[i][j];
                raster_[1][i][j] = D2200[i][j];
                raster_[2][i][j] = BD1900r2[i][j];
              }
            }
          }
          // PFM
          else if (bpCode == 6) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = BD2355[i][j];
                raster_[1][i][j] = D2300[i][j];
                raster_[2][i][j] = BD2290[i][j];
              }
            }
          }
          // PAL
          else if (bpCode == 7) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = BD2210_2[i][j];
                raster_[1][i][j] = BD2190[i][j];
                raster_[2][i][j] = BD2165[i][j];
              }
            }
          }
          // HYS
          else if (bpCode == 8) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = MIN2250[i][j];
                raster_[1][i][j] = BD2250[i][j];
                raster_[2][i][j] = BD1900r2[i][j];
              }
            }
          }
          // ICE
          else if (bpCode == 9) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = BD1900_2[i][j];
                raster_[1][i][j] = BD1500_2[i][j];
                raster_[2][i][j] = BD1435[i][j];
              }
            }
          }
          // IC2
          else if (bpCode == 10) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = R3920[i][j];
                raster_[1][i][j] = BD1500_2[i][j];
                raster_[2][i][j] = BD1435[i][j];
              }
            }
          }
          // CHL
          else if (bpCode == 11) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = ISLOPE1[i][j];
                raster_[1][i][j] = BD3000[i][j];
                raster_[2][i][j] = IRR2[i][j];
              }
            }

          }
          // CAR
          else if (bpCode == 12) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = D2300[i][j];
                raster_[1][i][j] = BD2500_2[i][j];
                raster_[2][i][j] = BD1900_2[i][j];
              }
            }

          }
          // CR2
          else if (bpCode == 13) {
            for (int i = 0; i < rasterH; i++) {
              for (int j = 0; j < rasterW; j++) {
                raster_[0][i][j] = MIN2295_2480[i][j];
                raster_[1][i][j] = MIN2345_2537[i][j];
                raster_[2][i][j] = CINDEX2[i][j];
              }
            }
          }

        }
      } else {
        float[][] R = floats.get(redBand - 1);
        float[][] G = floats.get(grnBand - 1);
        float[][] B = floats.get(bluBand - 1);
        if (toggle == 1) {
          nameSP.setText(
              "Current: R " + red.getValue() + " G " + green.getValue() + " B " + blue.getValue());
        } else {
          nameSP.setText("Current: " + bpChoices.getItems().get(bpCode - 1));

        }
        for (int i = 0; i < rasterH; i++) {
          for (int j = 0; j < rasterW; j++) {
            raster_[0][i][j] = R[i][j];
            raster_[1][i][j] = G[i][j];
            raster_[2][i][j] = B[i][j];
          }
        }
      }

      toggle = 0;
      if (raster_[0] == null || raster_[1] == null || raster_[2] == null) {
        JCATLog.getInstance().getLogger().log(Level.WARNING,
            bpChoices.getItems().get(bpCode - 1) + " is currently unavailable.");
      } else {
        BufferedImage newBI = CRISM.getQuickColorImage(raster_[0], raster_[1], raster_[2]);
        bImg = stretchImage(newBI);
        if (bImg != null && subStretch.isSelected())
          subLabel.setText(
              "Row: " + visX + "-" + (visX + visW) + ", Col: " + visY + "-" + (visY + visH));
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
        			subLabel.setText(
        					"Row: " + visX + "-" + (visX + visW) + ", Col: " + visY + "-" + (visY + visH));
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
      }
    });

    GridPane bpGrid = new GridPane();
    GridPane.setConstraints(red, 1, 1);
    GridPane.setConstraints(green, 2, 1);
    GridPane.setConstraints(blue, 3, 1);
    GridPane.setConstraints(okBtn, 4, 1);
    bpGrid.getChildren().addAll(red, green, blue, okBtn);

    HBox newRoot = new HBox();
    newRoot.getChildren().add(bpGrid);
    Scene scene4 = new Scene(newRoot);
    Stage stage4 = new Stage();
    stage4.setTitle("Choose RGB for New Browse Product");
    stage4.setScene(scene4);

    addBtn.setOnAction(e -> {
      stage4.show();
    });

    okBtn.setOnAction(event -> {
      JCATLog.getInstance().getLogger().log(Level.FINEST, "Adjusting RGB for new browse product");
      redBand = red.getItems().indexOf(red.getValue()) + 1;
      grnBand = green.getItems().indexOf(green.getValue()) + 1;
      bluBand = blue.getItems().indexOf(blue.getValue()) + 1;

      toggle = 1;
      stage4.close();
    });

    saveBtn.setOnAction(e -> {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Saving new browse product");
      TextInputDialog input = new TextInputDialog();
      input.setTitle("New Browse Product");
      input.setContentText("Name new browse product: ");
      String result = tidy(input.showAndWait());

      String[] bp = {result, Integer.toString(red.getItems().indexOf(red.getValue())),
          Integer.toString(green.getItems().indexOf(green.getValue())),
          Integer.toString(blue.getItems().indexOf(blue.getValue()))};
      if (detectorID == "VNIR") {
        saveNewBP_s(bp);
      } else {
        saveNewBP_l(bp);
      }
    });

    return true;
  }

  public float[][][] getMappedData() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getMappedData");

    int w = (int) CRISM.getWidth();
    int h = (int) CRISM.getHeight();

    float[][][] mappedData_ = new float[CRISM.bandCount][][];

    // float zmax = (float) 0.0;

    for (int bandNum = 0; bandNum < CRISM.bandCount; bandNum++) {

      mappedData_[bandNum] = new float[h][w];

      for (int row = 0; row < h; row++) {
        for (int col = 0; col < w; col++) {
          float z = CRISM.getPCFloatValue(row, col, bandNum);
          // if (z > zmax && z != 65535) {
          // zmax = z;
          // }
          mappedData_[bandNum][row][col] = z;

        }
      }
    }
    return mappedData_;

  }

  private void calculateBands(String imgfile) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method calculateBands");

    int rasterH = mappedData_[0].length;
    int rasterW = mappedData_[0][0].length;
    raster_[0] = new float[rasterH][rasterW];
    raster_[1] = new float[rasterH][rasterW];
    raster_[2] = new float[rasterH][rasterW];

    if (toggle == 0) {
      if (detectorID == "VNIR") {
        if (bpCode == 1) {
          // TRU
          r600(imgfile);
          r530(imgfile);
          r440(imgfile);

        } else if (bpCode == 2) {
          // VNA
          r770(imgfile);

          // } else if (bpCode == 3) {
          // // FEM
          // bd530_2(imgfile);
          // sh600_2(imgfile);
          // bdi1000vis(imgfile);
          //
          // } else if (bpCode == 4) {
          // // FM2
          // bd530_2(imgfile);
          // bd920_2(imgfile);
          // bdi1000vis(imgfile);

        } else {
          int index = bpCode - 5;
          Vector<String[]> list = getRecentBP_s();
          String[] BP = list.get(index);
          redBand = Integer.parseInt(BP[1]) + 1;
          grnBand = Integer.parseInt(BP[2]) + 1;
          bluBand = Integer.parseInt(BP[3]) + 1;
          toggle = 2;
        }
      } else {
        if (bpCode == 1) {
          // IRA
          r1330(imgfile);

        } else if (bpCode == 2) {
          // FAL
          r2529(imgfile);
          r1506(imgfile);
          r1080(imgfile);

        } else if (bpCode == 3) {
          // MAF
          olindex3(imgfile);
          lcpindex2(imgfile);
          hcpindex2(imgfile);

        } else if (bpCode == 4) {
          // HYD
          sindex2(imgfile);
          bd2100_2(imgfile);
          bd1900_2(imgfile);

        } else if (bpCode == 5) {
          // PHY
          d2300(imgfile);
          d2200(imgfile);
          bd1900r2(imgfile);

        } else if (bpCode == 6) {
          // PFM
          bd2355(imgfile);
          d2300(imgfile);
          bd2290(imgfile);

        } else if (bpCode == 7) {
          // PAL
          bd2210_2(imgfile);
          bd2190(imgfile);
          bd2165(imgfile);

        } else if (bpCode == 8) {
          // HYS
          min2250(imgfile);
          bd2250(imgfile);
          bd1900r2(imgfile);

        } else if (bpCode == 9) {
          // ICE
          bd1900_2(imgfile);
          bd1500_2(imgfile);
          bd1435(imgfile);

        } else if (bpCode == 10) {
          // IC2
          r3920(imgfile);
          bd1500_2(imgfile);
          bd1435(imgfile);

        } else if (bpCode == 11) {
          // CHL
          islope1(imgfile);
          irr2(imgfile);
          bd3000(imgfile);

        } else if (bpCode == 12) {
          // CAR
          d2300(imgfile);
          bd2500_2(imgfile);
          bd1900_2(imgfile);

        } else if (bpCode == 13) {
          // CR2
          min2295_2480(imgfile);
          min2345_2537(imgfile);
          cindex2(imgfile);

        } else {
          int index = bpCode - 14;
          Vector<String[]> list = getRecentBP_l();
          String[] BP = list.get(index);
          redBand = Integer.parseInt(BP[1]) + 1;
          grnBand = Integer.parseInt(BP[2]) + 1;
          bluBand = Integer.parseInt(BP[3]) + 1;
          toggle = 2;
        }
      }
    }
    if (toggle >= 1) {
      if (detectorID == "VNIR") {
        if (redBand == 1 || grnBand == 1 || bluBand == 1) {
          r770(imgfile);
        }
        if (redBand == 2 || grnBand == 2 || bluBand == 2) {
          rbr(imgfile);
        }
        if (redBand == 3 || grnBand == 3 || bluBand == 3) {
          bd530_2(imgfile);
        }
        if (redBand == 4 || grnBand == 4 || bluBand == 4) {
          sh600_2(imgfile);
        }
        if (redBand == 5 || grnBand == 5 || bluBand == 5) {
          sh770(imgfile);
        }
        if (redBand == 6 || grnBand == 6 || bluBand == 6) {
          bd640_2(imgfile);
        }
        if (redBand == 7 || grnBand == 7 || bluBand == 7) {
          bd860_2(imgfile);
        }
        if (redBand == 8 || grnBand == 8 || bluBand == 8) {
          bd920_2(imgfile);
        }
        // if (redBand == 9 || grnBand == 9 || bluBand == 9) {
        // rpeak1(imgfile);
        // }
        // if (redBand == 10 || grnBand == 10 || bluBand == 10) {
        // bdi1000vis(imgfile);
        // }
        if (redBand == 9 || grnBand == 9 || bluBand == 9) {
          r600(imgfile);
        }
        if (redBand == 10 || grnBand == 10 || bluBand == 10) {
          r530(imgfile);
        }
        if (redBand == 11 || grnBand == 11 || bluBand == 11) {
          r440(imgfile);
        }
        if (redBand == 12 || grnBand == 12 || bluBand == 12) {
          irr1(imgfile);
        }
      } else {
        // if (redBand == 1 || grnBand == 1 || bluBand == 1) {
        // bdi1000ir(imgfile);
        // }
        if (redBand == 1 || grnBand == 1 || bluBand == 1) {
          r1330(imgfile);
        }
        if (redBand == 2 || grnBand == 2 || bluBand == 2) {
          bd1300(imgfile);
        }
        if (redBand == 3 || grnBand == 3 || bluBand == 3) {
          olindex3(imgfile);
        }
        if (redBand == 4 || grnBand == 4 || bluBand == 4) {
          lcpindex2(imgfile);
        }
        if (redBand == 5 || grnBand == 5 || bluBand == 5) {
          hcpindex2(imgfile);
        }
        // if (redBand == 7 || grnBand == 7 || bluBand == 7) {
        // var(imgfile);
        // }
        if (redBand == 6 || grnBand == 6 || bluBand == 6) {
          islope1(imgfile);
        }
        if (redBand == 7 || grnBand == 7 || bluBand == 7) {
          bd1400(imgfile);
        }
        if (redBand == 8 || grnBand == 8 || bluBand == 8) {
          bd1435(imgfile);
        }
        if (redBand == 9 || grnBand == 9 || bluBand == 9) {
          bd1500_2(imgfile);
        }
        if (redBand == 10 || grnBand == 10 || bluBand == 10) {
          icer1_2(imgfile);
        }
        if (redBand == 11 || grnBand == 11 || bluBand == 11) {
          bd1750_2(imgfile);
        }
        if (redBand == 12 || grnBand == 12 || bluBand == 12) {
          bd1900_2(imgfile);
        }
        if (redBand == 13 || grnBand == 13 || bluBand == 13) {
          bd1900r2(imgfile);
        }
        // if (redBand == 16 || grnBand == 16 || bluBand == 16) {
        // bdi2000(imgfile);
        // }
        if (redBand == 14 || grnBand == 14 || bluBand == 14) {
          bd2100_2(imgfile);
        }
        if (redBand == 15 || grnBand == 15 || bluBand == 15) {
          bd2165(imgfile);
        }
        if (redBand == 16 || grnBand == 16 || bluBand == 16) {
          bd2190(imgfile);
        }
        if (redBand == 17 || grnBand == 17 || bluBand == 17) {
          min2200(imgfile);
        }
        if (redBand == 18 || grnBand == 18 || bluBand == 18) {
          bd2210_2(imgfile);
        }
        if (redBand == 19 || grnBand == 19 || bluBand == 19) {
          d2200(imgfile);
        }
        if (redBand == 20 || grnBand == 20 || bluBand == 20) {
          bd2230(imgfile);
        }
        if (redBand == 21 || grnBand == 21 || bluBand == 21) {
          bd2250(imgfile);
        }
        if (redBand == 22 || grnBand == 22 || bluBand == 22) {
          min2250(imgfile);
        }
        if (redBand == 23 || grnBand == 23 || bluBand == 23) {
          bd2265(imgfile);
        }
        if (redBand == 24 || grnBand == 24 || bluBand == 24) {
          bd2290(imgfile);
        }
        if (redBand == 25 || grnBand == 25 || bluBand == 25) {
          d2300(imgfile);
        }
        if (redBand == 26 || grnBand == 26 || bluBand == 26) {
          bd2355(imgfile);
        }
        if (redBand == 27 || grnBand == 27 || bluBand == 27) {
          sindex2(imgfile);
        }
        if (redBand == 28 || grnBand == 28 || bluBand == 28) {
          icer2_2(imgfile);
        }
        if (redBand == 29 || grnBand == 29 || bluBand == 29) {
          min2295_2480(imgfile);
        }
        if (redBand == 30 || grnBand == 30 || bluBand == 30) {
          min2345_2537(imgfile);
        }
        if (redBand == 31 || grnBand == 31 || bluBand == 31) {
          bd2500_2(imgfile);
        }
        if (redBand == 32 || grnBand == 32 || bluBand == 32) {
          bd3000(imgfile);
        }
        if (redBand == 33 || grnBand == 33 || bluBand == 33) {
          bd3100(imgfile);
        }
        if (redBand == 34 || grnBand == 34 || bluBand == 34) {
          bd3200(imgfile);
        }
        if (redBand == 35 || grnBand == 35 || bluBand == 35) {
          bd3400_2(imgfile);
        }
        if (redBand == 36 || grnBand == 36 || bluBand == 36) {
          cindex2(imgfile);
        }
        if (redBand == 37 || grnBand == 37 || bluBand == 37) {
          r1080(imgfile);
        }
        if (redBand == 38 || grnBand == 38 || bluBand == 38) {
          r1506(imgfile);
        }
        if (redBand == 39 || grnBand == 39 || bluBand == 39) {
          r2529(imgfile);
        }
        if (redBand == 40 || grnBand == 40 || bluBand == 40) {
          bd2600(imgfile);
        }
        if (redBand == 41 || grnBand == 41 || bluBand == 41) {
          irr2(imgfile);
        }
        if (redBand == 42 || grnBand == 42 || bluBand == 42) {
          irr3(imgfile);
        }
        if (redBand == 43 || grnBand == 43 || bluBand == 43) {
          r3920(imgfile);
        }
      }
    }

  }

  public void saveNewBP_s(String[] bp) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method saveNewBP_s");
    Vector<String[]> lines = getRecentBP_s();
    try {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Adding name to userBP: " + bp[0]);
      String path =
          JCATConfig.getInstance().getLocalArchive() + File.separator + "userBP_sTRDR.txt";
      File f = new File(path);
      PrintWriter out;
      out = new PrintWriter(new FileWriter(f));
      StringBuilder sb = new StringBuilder();
      sb.append(bp[0] + " " + bp[1] + " " + bp[2] + " " + bp[3]);
      out.println(sb);
      for (int i = 0; i < lines.size(); i++) {
        String[] current = lines.elementAt(i);
        String s = null;
        if (!Arrays.equals(bp, current))
          s = convertArraytoString(current, ",");
        out.print(s + "\n");
      }
      out.close();
    } catch (IOException e) {
      JCATLog.getInstance().getLogger().log(Level.WARNING, "Error adding browse product");
      JCATMessageWindow.show(e);
      e.printStackTrace();
    }
  }

  public Vector<String[]> getRecentBP_s() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getRecentBP_s");
    Vector<String[]> lines = new Vector<String[]>();
    try {

      JCATLog.getInstance().getLogger().log(Level.INFO, "Displaying recent BP");
      String path =
          JCATConfig.getInstance().getLocalArchive() + File.separator + "userBP_sTRDR.txt";
      File f = new File(path);
      if (f.exists()) {
        BufferedReader in = new BufferedReader(new FileReader(f));
        String line = null;
        while ((line = in.readLine()) != null) {
          String strArray[] = line.split(" ");
          lines.add(strArray);
        }
        in.close();
      }
    } catch (IOException e) {
      JCATLog.getInstance().getLogger().log(Level.WARNING,
          "Error retriving recent browse products.");
      JCATMessageWindow.show(e);
      e.printStackTrace();
    }
    return lines;
  }

  public void saveNewBP_l(String[] bp) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method saveNewBP_l");
    Vector<String[]> lines = getRecentBP_l();
    try {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Adding name to userBP: " + bp[0]);
      String path =
          JCATConfig.getInstance().getLocalArchive() + File.separator + "userBP_lTRDR.txt";
      File f = new File(path);
      PrintWriter out;
      out = new PrintWriter(new FileWriter(f));
      StringBuilder sb = new StringBuilder();
      sb.append(bp[0] + " " + bp[1] + " " + bp[2] + " " + bp[3]);
      out.println(sb);
      for (int i = 0; i < lines.size(); i++) {
        String[] current = lines.elementAt(i);
        String s = null;
        if (!Arrays.equals(bp, current))
          s = convertArraytoString(current, ",");
        out.print(s + "\n");
      }
      out.close();
    } catch (IOException e) {
      JCATLog.getInstance().getLogger().log(Level.WARNING, "Error adding browse product");
      JCATMessageWindow.show(e);
      e.printStackTrace();
    }
  }

  public Vector<String[]> getRecentBP_l() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getRecentBP_l");
    Vector<String[]> lines = new Vector<String[]>();
    try {

      JCATLog.getInstance().getLogger().log(Level.INFO, "Displaying recent BP");
      String path =
          JCATConfig.getInstance().getLocalArchive() + File.separator + "userBP_lTRDR.txt";
      File f = new File(path);
      if (f.exists()) {
        BufferedReader in = new BufferedReader(new FileReader(f));
        String line = null;
        while ((line = in.readLine()) != null) {
          String strArray[] = line.split(" ");
          lines.add(strArray);
        }
        in.close();
      }
    } catch (IOException e) {
      JCATLog.getInstance().getLogger().log(Level.WARNING,
          "Error retriving recent browse products.");
      JCATMessageWindow.show(e);
      e.printStackTrace();
    }
    return lines;
  }

  public String convertArraytoString(String[] array, String delimiter) {
    StringBuilder sb = new StringBuilder();
    for (String str : array)
      sb.append(str).append(delimiter);
    String s = sb.toString().replaceAll(",", " ");
    return s.substring(0, sb.length() - 1);
  }

  class spBDI1000VIS extends JCATCalls {

    private float[][] summaryProduct;
    private float[][][] newMapped;

    public spBDI1000VIS(String imgfile, float[][][] rawmapped, float[][] sp) {
      summaryProduct = new float[rawmapped[0].length][rawmapped[0][0].length];
      newMapped = new float[rawmapped.length][rawmapped[0].length][rawmapped[0][0].length];

      String ddrFile = imgfile.replaceAll("if", "de").replaceAll("trr3", "ddr1");
      CRISM = new TRDR(imgfile, ddrFile);
      wavelength = CRISM.getWavelengths();
      int startBand = findIndex(wavelength, 833.0);
      int endBand = findIndex(wavelength, 1023.0);

      for (int x = 0; x < rawmapped[0].length; x++) {
        for (int y = 0; y < rawmapped[0][0].length; y++) {
          for (int bnds = startBand; bnds < endBand; bnds++) {
            newMapped[bnds - startBand][x][y] = (float) rawmapped[bnds][x][y] / sp[x][y];
          }
          // int inteBands = newMapped.length;
          // float bdi = (float) 0.0;
          // for (int integral = 0; integral < inteBands - 1; integral++) {
          // bdi = (float) bdi
          // + (float) 6.55 * Math.abs(newMapped[integral + 1][x][y] - newMapped[integral][x][y]);
          // } // TODO fix 6.55 so that it is the accurate wavelength difference
          //
          // summaryProduct[x][y] = bdi;

        }
      }

      int length = 0;
      for (int i = 0; i < wavelength.size(); i++) {
        if (wavelength.get(i) > 833.0 && wavelength.get(i) < 1023.0) {
          length++;
        }
      }

      List<Double> waverange = new ArrayList<Double>();
      double[] wv_vec_um = new double[length];
      int counter = 0;
      for (int i = 0; i < wavelength.size(); i++) {
        if (wavelength.get(i) > 833.0 && wavelength.get(i) < 1023.0) {
          waverange.add(wavelength.get(i));
          wv_vec_um[counter] = wavelength.get(i) / 1000.0;
          counter++;
        }
      }
      Arrays.sort(wv_vec_um);

      double[] adj_spec_vec = new double[length];
      for (int i = 0; i < rawmapped[0].length; i++) {
        for (int j = 0; j < rawmapped[0][0].length; j++) {
          for (int k = startBand; k < endBand; k++) {
            adj_spec_vec[k - startBand] = 1.0 - newMapped[k - startBand][i][j];
          }
          summaryProduct[i][j] = int_tabulated(waverange, wv_vec_um, adj_spec_vec);
        }
      }

    }

    public float[][] getBDI1000VIS() {
      return summaryProduct;
    }

  }

  class spRPEAK1 {

    private float[][] summaryProduct;

    public spRPEAK1(float[][][] rawmapped) {

      summaryProduct = new float[rawmapped[0].length][];
      for (int x = 0; x < rawmapped[0].length; x++) {
        summaryProduct[x] = new float[rawmapped[0][0].length];
        for (int y = 0; y < rawmapped[0][0].length; y++) {
          final WeightedObservedPoints scX = new WeightedObservedPoints();
          for (int bnds = 0; bnds < rawmapped.length; bnds++) {
            scX.add(bnds, rawmapped[bnds][x][y]);
          }
          final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(5);
          double[] coeff = fitter.fit(scX.toList());
          double[] d_coeff = new double[coeff.length - 1];
          d_coeff[0] = coeff[1];
          d_coeff[1] = coeff[2] * 2.0;
          d_coeff[2] = coeff[3] * 3.0;
          d_coeff[3] = coeff[4] * 4.0;
          d_coeff[4] = coeff[5] * 5.0;
          LaguerreSolver solver = new LaguerreSolver();
          Complex[] result = solver.solveAllComplex(d_coeff, 0);
          int until = result.length;
          double[] realresult = new double[until];
          for (int a = 0; a < until; a++) {
            realresult[a] = result[a].getReal();
          }
          Arrays.sort(realresult);
          double reallyresult = realresult[1];
          if (realresult[1] < 7.0 || realresult[1] > 100.0) {
            if (realresult[1] < 50) {
              reallyresult = realresult[2];
            } else {
              reallyresult = realresult[0];
            }
          }
          float x1 = (float) reallyresult;
          float x2 = (float) reallyresult * (float) reallyresult;
          float x3 = (float) reallyresult * (float) reallyresult * (float) reallyresult;
          float x4 = (float) reallyresult * (float) reallyresult * (float) reallyresult
              * (float) reallyresult;
          float x5 = (float) reallyresult * (float) reallyresult * (float) reallyresult
              * (float) reallyresult * (float) reallyresult;
          float fill = (float) coeff[0] + (float) coeff[1] * x1 + (float) coeff[2] * x2
              + (float) coeff[3] * x3 + (float) coeff[4] * x4 + (float) coeff[5] * x5;
          summaryProduct[x][y] = fill;
        }
      }
    }

    public float[][] getRPEAK1() {
      return summaryProduct;
    }

  }

  public void r770(String imgfile) {
    arrayMedian r770 = new arrayMedian(imgfile, mappedData_, 770, 5);
    R770 = r770.getToast();
  }

  public void rbr(String imgfile) {
    arrayMedian r770 = new arrayMedian(imgfile, mappedData_, 770, 5);
    float[][] R770 = r770.getToast();
    arrayMedian r440 = new arrayMedian(imgfile, mappedData_, 440, 5);
    float[][] R440 = r440.getToast();
    RBR = new float[R440.length][];
    for (int i = 0; i < R440.length; i++) {
      RBR[i] = new float[R440[0].length];
      for (int j = 0; j < R440[0].length; j++) {
        RBR[i][j] = (float) R770[i][j] / R440[i][j];
      }
    }
  }

  public void bd530_2(String imgfile) {
    spBandDepth bd530_2 = new spBandDepth(imgfile, mappedData_, 440, 5, 530, 5, 614, 5);
    BD530_2 = bd530_2.getSummaryProduct();
  }

  public void sh600_2(String imgfile) {
    spShoulder sh600_2 = new spShoulder(imgfile, mappedData_, 533, 5, 600, 5, 716, 3);
    SH600_2 = sh600_2.getSummaryProduct();
  }

  public void sh770(String imgfile) {
    spShoulder sh770 = new spShoulder(imgfile, mappedData_, 716, 3, 775, 5, 860, 5);
    SH770 = sh770.getSummaryProduct();
  }

  public void bd640_2(String imgfile) {
    spBandDepth bd640_2 = new spBandDepth(imgfile, mappedData_, 600, 5, 624, 3, 760, 5);
    BD640_2 = bd640_2.getSummaryProduct();
  }

  public void bd860_2(String imgfile) {
    spBandDepth bd860_2 = new spBandDepth(imgfile, mappedData_, 755, 5, 860, 5, 977, 5);
    BD860_2 = bd860_2.getSummaryProduct();
  }

  public void bd920_2(String imgfile) {
    spBandDepth bd920_2 = new spBandDepth(imgfile, mappedData_, 807, 5, 920, 5, 984, 5);
    BD920_2 = bd920_2.getSummaryProduct();
  }

  public void rpeak1(String imgfile) {
    spRPEAK1 rpeak1 = new spRPEAK1(mappedData_);
    RPEAK1 = rpeak1.getRPEAK1();
  }

  public void bdi1000vis(String imgfile) {
    spRPEAK1 rpeak1 = new spRPEAK1(mappedData_);
    spBDI1000VIS bdi1000vis = new spBDI1000VIS(imgfile, mappedData_, rpeak1.getRPEAK1());
    BDI1000VIS = bdi1000vis.getBDI1000VIS();
  }

  public void r600(String imgfile) {
    arrayMedian r600 = new arrayMedian(imgfile, mappedData_, 600, 5);
    R600 = r600.getToast();
  }

  public void r530(String imgfile) {
    arrayMedian r530 = new arrayMedian(imgfile, mappedData_, 530, 5);
    R530 = r530.getToast();
  }

  public void r440(String imgfile) {
    arrayMedian r440 = new arrayMedian(imgfile, mappedData_, 440, 5);
    R440 = r440.getToast();
  }

  public void irr1(String imgfile) {
    arrayMedian r800 = new arrayMedian(imgfile, mappedData_, 800, 5);
    float[][] R800 = r800.getToast();
    arrayMedian r997 = new arrayMedian(imgfile, mappedData_, 997, 5);
    float[][] R997 = r997.getToast();
    IRR1 = new float[R997.length][];
    for (int i = 0; i < R997.length; i++) {
      IRR1[i] = new float[R997[0].length];
      for (int j = 0; j < R997[0].length; j++) {
        IRR1[i][j] = (float) R800[i][j] / R997[i][j];
      }
    }
  }

  public void bdi1000ir(String imgfile) {
    wavelength = CRISM.getWavelengths();

    int h = mappedData_[0].length;
    int w = mappedData_[0][0].length;
    bdiCont bdicont = new bdiCont(CRISM, h, w, wavelength);

    int length = 0;
    for (int i = 0; i < wavelength.size(); i++) {
      if (wavelength.get(i) > 1020.0 && wavelength.get(i) < 1255.0) {
        length++;
      }
    }

    int[] indices = new int[length];
    List<Double> waverange = new ArrayList<Double>();
    double[] wv_vec = new double[length];
    double[] wv_vec_um = new double[length];
    int counter = 0;
    for (int i = 0; i < wavelength.size(); i++) {
      if (wavelength.get(i) > 1020.0 && wavelength.get(i) < 1255.0) {
        indices[counter] = findIndex(wavelength, wavelength.get(i));
        waverange.add(wavelength.get(i));
        wv_vec[counter] = wavelength.get(i);
        wv_vec_um[counter] = wv_vec[counter] / 1000.0;
        counter++;
      }
    }
    Arrays.sort(wv_vec_um);

    double[][][] bdi1000ir_cube = new double[h][w][indices.length];
    double[][][] cont_cube = new double[h][w][indices.length];
    double[][][] normalized_cube = new double[h][w][indices.length];
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        for (int k = 0; k < indices.length; k++) {
          bdi1000ir_cube[i][j][k] = CRISM.getPCFloatValue(i, j, indices[k]);
          cont_cube[i][j][k] =
              (bdicont.getContSlopeFrame()[i][j] * (wv_vec[k] - bdicont.getMedian2Wavelength())
                  + bdicont.getMedian2Frame()[i][j]);
          normalized_cube[i][j][k] = bdi1000ir_cube[i][j][k] / cont_cube[i][j][k];
        }
      }
    }

    double[] adj_spec_vec = new double[indices.length];
    double[] check_vec = new double[indices.length];
    BDI1000IR = new float[h][w];
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        for (int k = 0; k < indices.length; k++) {
          adj_spec_vec[k] = 1.0 - normalized_cube[i][j][k];
          check_vec[k] = bdi1000ir_cube[i][j][k];
        }
        if (Arrays.binarySearch(check_vec, FillValue) != check_vec.length) {
          BDI1000IR[i][j] = int_tabulated(waverange, wv_vec_um, adj_spec_vec);
        }
      }
    }
  }

  public void r1330(String imgfile) {
    arrayMedian r1330 = new arrayMedian(imgfile, mappedData_, 1330, 11);
    R1330 = r1330.getToast();
  }

  public void bd1300(String imgfile) {
    spBandDepth bd1300 = new spBandDepth(imgfile, mappedData_, 1080, 5, 1320, 15, 1750, 5);
    BD1300 = bd1300.getSummaryProduct();
  }

  public void olindex3(String imgfile) {
    RBmaker maker = new RBmaker(imgfile, mappedData_, 1750, 1862, 7);
    float[][] RB1210 = maker.findNewRB(1210, 7);
    float[][] RB1250 = maker.findNewRB(1250, 7);
    float[][] RB1263 = maker.findNewRB(1263, 7);
    float[][] RB1276 = maker.findNewRB(1276, 7);
    float[][] RB1330 = maker.findNewRB(1330, 7);
    OLINDEX3 = new float[RB1210.length][];
    for (int i = 0; i < RB1210.length; i++) {
      OLINDEX3[i] = new float[RB1210[0].length];
      for (int j = 0; j < RB1210[0].length; j++) {
        OLINDEX3[i][j] = RB1210[i][j] * (float) 0.1 + RB1250[i][j] * (float) 0.1
            + RB1263[i][j] * (float) 0.2 + RB1276[i][j] * (float) 0.2 + RB1330[i][j] * (float) 0.4;
      }
    }
  }

  public void lcpindex2(String imgfile) {
    RBmaker maker2 = new RBmaker(imgfile, mappedData_, 1560, 2450, 7);
    float[][] RB1690 = maker2.findNewRB(1690, 7);
    float[][] RB1750 = maker2.findNewRB(1750, 7);
    float[][] RB1810 = maker2.findNewRB(1810, 7);
    float[][] RB1870 = maker2.findNewRB(1870, 7);
    LCPINDEX2 = new float[RB1690.length][];
    for (int i = 0; i < RB1690.length; i++) {
      LCPINDEX2[i] = new float[RB1690[0].length];
      for (int j = 0; j < RB1690[0].length; j++) {
        LCPINDEX2[i][j] = RB1690[i][j] * (float) 0.20 + RB1750[i][j] * (float) 0.20
            + RB1810[i][j] * (float) 0.30 + RB1870[i][j] * (float) 0.30;
      }
    }
  }

  public void hcpindex2(String imgfile) {
    RBmaker maker3 = new RBmaker(imgfile, mappedData_, 1690, 2530, 7);
    float[][] RB2120 = maker3.findNewRB(2120, 5);
    float[][] RB2140 = maker3.findNewRB(2140, 7);
    float[][] RB2230 = maker3.findNewRB(2230, 7);
    float[][] RB2250 = maker3.findNewRB(2250, 7);
    float[][] RB2430 = maker3.findNewRB(2430, 7);
    float[][] RB2460 = maker3.findNewRB(2460, 7);

    HCPINDEX2 = new float[RB2120.length][];
    for (int i = 0; i < RB2120.length; i++) {
      HCPINDEX2[i] = new float[RB2120[0].length];
      for (int j = 0; j < RB2120[0].length; j++) {
        HCPINDEX2[i][j] = RB2120[i][j] * (float) 0.10 + RB2140[i][j] * (float) 0.10
            + RB2230[i][j] * (float) 0.15 + RB2250[i][j] * (float) 0.30
            + RB2430[i][j] * (float) 0.20 + RB2460[i][j] * (float) 0.15;
      }
    }
  }

  public void var(String imgfile) {
    wavelength = CRISM.getWavelengths();

    int h = mappedData_[0].length;
    int w = mappedData_[0][0].length;

    int[] varslopewl = new int[2];
    varslopewl[0] = findIndex(wavelength, 1014.00);
    varslopewl[1] = findIndex(wavelength, 2287.00);

    double[] varslopelams = new double[2];
    varslopelams[0] = wavelength.get(varslopewl[0]);
    varslopelams[1] = wavelength.get(varslopewl[1]);

    double[][][] varslopers = new double[h][w][2];
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        for (int k = 0; k < varslopewl.length; k++) {
          varslopers[i][j][k] = CRISM.getPCFloatValue(i, j, varslopewl[k]);
        }
      }
    }

    int length = 0;
    for (int i = 0; i < wavelength.size(); i++) {
      if (wavelength.get(i) > 1021.0 && wavelength.get(i) < 2287.0) {
        length++;
      }
    }

    int[] indices = new int[length];
    double[] wvs = new double[length];
    int counter = 0;
    for (int i = 0; i < wavelength.size(); i++) {
      if (wavelength.get(i) > 1021.0 && wavelength.get(i) < 2287.0) {
        indices[counter] = findIndex(wavelength, wavelength.get(i));
        wvs[counter] = wavelength.get(i);
        counter++;
      }
    }

    double[][][] obsrs = new double[h][w][indices.length];
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        for (int k = 0; k < indices.length; k++) {
          obsrs[i][j][k] = CRISM.getPCFloatValue(i, j, indices[k]);
        }
      }
    }

    double[] fit = new double[2];
    double[] predrs = new double[wvs.length];
    double[] sub = new double[wvs.length];
    double[] array = new double[wvs.length];
    VAR = new float[h][w];
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        fit = linfit(varslopelams, varslopers[i][j]);
        for (int k = 0; k < wvs.length; k++) {
          predrs[k] = fit[0] + (fit[1] * wvs[k]);
          sub[k] = predrs[k] - obsrs[i][j][k];
          array[k] = Math.pow(sub[k], 2);
        }
        VAR[i][j] = (float) Arrays.stream(array).sum();
      }
    }
  }

  public void islope1(String imgfile) {
    wavelength = CRISM.getWavelengths();

    arrayMedian r1815 = new arrayMedian(imgfile, mappedData_, 1815, 5);
    float[][] R1815 = r1815.getToast();
    arrayMedian r2530 = new arrayMedian(imgfile, mappedData_, 2530, 5);
    float[][] R2530 = r2530.getToast();
    float denom = (float) (wavelength.get(findIndex(wavelength, 2530.0))
        - wavelength.get(findIndex(wavelength, 1815.0)));
    ISLOPE1 = new float[R1815.length][];
    for (int i = 0; i < R1815.length; i++) {
      ISLOPE1[i] = new float[R1815[0].length];
      for (int j = 0; j < R1815[0].length; j++) {
        ISLOPE1[i][j] = (float) 1000.0 * ((float) (R1815[i][j] - R2530[i][j])) / denom;
      }
    }
  }

  public void bd1400(String imgfile) {
    spBandDepth bd1400 = new spBandDepth(imgfile, mappedData_, 1330, 5, 1395, 3, 1467, 5);
    BD1400 = bd1400.getSummaryProduct();
  }

  public void bd1435(String imgfile) {
    spBandDepth bd1435 = new spBandDepth(imgfile, mappedData_, 1370, 3, 1434, 1, 1470, 3);
    BD1435 = bd1435.getSummaryProduct();
  }

  public void bd1500_2(String imgfile) {
    spBandDepth bd1500_2 = new spBandDepth(imgfile, mappedData_, 1367, 5, 1525, 11, 1808, 5);
    BD1500_2 = bd1500_2.getSummaryProduct();
  }

  public void icer1_2(String imgfile) {
    spBandDepth bd1435_ = new spBandDepth(imgfile, mappedData_, 1370, 3, 1435, 1, 1470, 3);
    float[][] bd1435 = bd1435_.getSummaryProduct();
    spBandDepth bd1500_2_ = new spBandDepth(imgfile, mappedData_, 1367, 5, 1525, 11, 1808, 5);
    float[][] bd1500_2 = bd1500_2_.getSummaryProduct();
    ICER1_2 = new float[bd1435.length][];
    for (int i = 0; i < bd1435.length; i++) {
      ICER1_2[i] = new float[bd1435[0].length];
      for (int j = 0; j < bd1435[0].length; j++) {
        ICER1_2[i][j] =
            (float) 1 - (float) (((float) 1 - bd1435[i][j]) / ((float) 1 - bd1500_2[i][j]));
      }
    }
  }

  public void bd1750_2(String imfile) {
    spBandDepth bd1750_2 = new spBandDepth(imgfile, mappedData_, 1690, 5, 1750, 3, 1815, 5);
    BD1750_2 = bd1750_2.getSummaryProduct();
  }

  public void bd1900_2(String imgfile) {
    spBandDepth bd1900_2 = new spBandDepth(imgfile, mappedData_, 1850, 5, 1930, 5, 2067, 5);
    BD1900_2 = bd1900_2.getSummaryProduct();
  }

  public void bd1900r2(String imgfile) {
    RBmaker droptop = new RBmaker(imgfile, mappedData_, 1815, 2132, 5);
    float[][] rat1908 = droptop.findRRCratio(1908, 1);
    float[][] rat1914 = droptop.findRRCratio(1914, 1);
    float[][] rat1921 = droptop.findRRCratio(1921, 1);
    float[][] rat1928 = droptop.findRRCratio(1928, 1);
    float[][] rat1934 = droptop.findRRCratio(1934, 1);
    float[][] rat1941 = droptop.findRRCratio(1941, 1);
    float[][] rat1862 = droptop.findRRCratio(1862, 1);
    float[][] rat1869 = droptop.findRRCratio(1869, 1);
    float[][] rat1875 = droptop.findRRCratio(1875, 1);
    float[][] rat2112 = droptop.findRRCratio(2112, 1);
    float[][] rat2120 = droptop.findRRCratio(2120, 1);
    float[][] rat2126 = droptop.findRRCratio(2126, 1);
    BD1900r2 = new float[rat1908.length][];
    for (int i = 0; i < rat1908.length; i++) {
      BD1900r2[i] = new float[rat1908[0].length];
      for (int j = 0; j < rat1908[0].length; j++) {
        BD1900r2[i][j] = (float) 1 - (float) ((float) (rat1908[i][j] + rat1914[i][j] + rat1921[i][j]
            + rat1928[i][j] + rat1934[i][j] + rat1941[i][j])
            / (float) (rat1862[i][j] + rat1869[i][j] + rat1875[i][j] + rat2112[i][j] + rat2120[i][j]
                + rat2126[i][j]));
      }
    }
  }

  public void bdi2000(String imgfile) {
    wavelength = CRISM.getWavelengths();

    int h = mappedData_[0].length;
    int w = mappedData_[0][0].length;

    bdiCont bdicont = new bdiCont(CRISM, h, w, wavelength);

    int length = 0;
    for (int i = 0; i < wavelength.size(); i++) {
      if (wavelength.get(i) > 1660.0 && wavelength.get(i) < 2390.0) {
        length++;
      }
    }

    int[] indices = new int[length];
    List<Double> waverange = new ArrayList<Double>();
    double[] wv_vec = new double[length];
    double[] wv_vec_um = new double[length];
    int counter = 0;
    for (int i = 0; i < wavelength.size(); i++) {
      if (wavelength.get(i) > 1660.0 && wavelength.get(i) < 2390.0) {
        indices[counter] = findIndex(wavelength, wavelength.get(i));
        waverange.add(wavelength.get(i));
        wv_vec[counter] = wavelength.get(i);
        wv_vec_um[counter] = wv_vec[counter] / 1000.0;
        counter++;
      }
    }
    Arrays.sort(wv_vec_um);

    double[][][] bdi2000_cube = new double[h][w][indices.length];
    double[][][] cont_cube = new double[h][w][indices.length];
    double[][][] normalized_cube = new double[h][w][indices.length];
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        for (int k = 0; k < indices.length; k++) {
          bdi2000_cube[i][j][k] = CRISM.getPCFloatValue(i, j, indices[k]);
          cont_cube[i][j][k] =
              (bdicont.getContSlopeFrame()[i][j] * (wv_vec[k] - bdicont.getMedian2Wavelength())
                  + bdicont.getMedian2Frame()[i][j]);
          normalized_cube[i][j][k] = bdi2000_cube[i][j][k] / cont_cube[i][j][k];
        }
      }
    }

    double[] adj_spec_vec = new double[indices.length];
    BDI2000 = new float[h][w];
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        for (int k = 0; k < indices.length; k++) {
          adj_spec_vec[k] = 1.0 - normalized_cube[i][j][k];
        }
        BDI2000[i][j] = int_tabulated(waverange, wv_vec_um, adj_spec_vec);
      }
    }
  }

  public void bd2100_2(String imgfile) {
    spBandDepth bd2100 = new spBandDepth(imgfile, mappedData_, 1930, 3, 2132, 5, 2250, 3);
    BD2100_2 = bd2100.getSummaryProduct();
  }

  public void bd2165(String imgfile) {
    spBandDepth bd2165 = new spBandDepth(imgfile, mappedData_, 2120, 5, 2165, 3, 2230, 3);
    BD2165 = bd2165.getSummaryProduct();
  }

  public void bd2190(String imgfile) {
    spBandDepth bd2190 = new spBandDepth(imgfile, mappedData_, 2120, 5, 2185, 3, 2250, 3);
    BD2190 = bd2190.getSummaryProduct();
  }

  public void min2200(String imgfile) {
    spBandDepth min1 = new spBandDepth(imgfile, mappedData_, 2120, 5, 2210, 3, 2350, 5);
    float[][] MIN1 = min1.getSummaryProduct();
    spBandDepth min2 = new spBandDepth(imgfile, mappedData_, 2120, 5, 2165, 3, 2350, 5);
    float[][] MIN2 = min2.getSummaryProduct();
    MIN2200 = new float[MIN1.length][];
    for (int i = 0; i < MIN1.length; i++) {
      MIN2200[i] = new float[MIN1[0].length];
      for (int j = 0; j < MIN1[0].length; j++) {
        if (MIN1[i][j] < MIN2[i][j]) {
          MIN2200[i][j] = MIN1[i][j];
        } else {
          MIN2200[i][j] = MIN2[i][j];
        }
      }
    }
  }

  public void bd2210_2(String imgfile) {
    spBandDepth bd2210 = new spBandDepth(imgfile, mappedData_, 2165, 5, 2210, 5, 2290, 5);
    BD2210_2 = bd2210.getSummaryProduct();
  }

  public void d2200(String imgfile) {
    RBmaker drop2 = new RBmaker(imgfile, mappedData_, 1815, 2430, 7);
    float[][] rat2210_ = drop2.findRRCratio(2210, 7);
    float[][] rat2230 = drop2.findRRCratio(2230, 7);
    float[][] rat2165 = drop2.findRRCratio(2165, 5);
    D2200 = new float[rat2210_.length][];
    for (int i = 0; i < rat2210_.length; i++) {
      D2200[i] = new float[rat2210_[0].length];
      for (int j = 0; j < rat2210_[0].length; j++) {
        D2200[i][j] = (float) 1 - (float) ((float) (rat2210_[i][j] + rat2230[i][j])
            / (float) (rat2165[i][j] + rat2165[i][j]));
      }
    }
  }

  public void bd2230(String imgfile) {
    spBandDepth bd2230 = new spBandDepth(imgfile, mappedData_, 2210, 3, 2235, 3, 2252, 3);
    BD2230 = bd2230.getSummaryProduct();
  }

  public void bd2250(String imgfile) {
    spBandDepth bd2250 = new spBandDepth(imgfile, mappedData_, 2120, 5, 2245, 7, 2340, 3);
    BD2250 = bd2250.getSummaryProduct();
  }

  public void min2250(String imgfile) {
    spBandDepth min1 = new spBandDepth(imgfile, mappedData_, 2165, 5, 2210, 3, 2350, 5);
    float[][] MIN1 = min1.getSummaryProduct();
    spBandDepth min2 = new spBandDepth(imgfile, mappedData_, 2165, 5, 2265, 3, 2350, 5);
    float[][] MIN2 = min2.getSummaryProduct();
    MIN2250 = new float[MIN1.length][];
    for (int i = 0; i < MIN1.length; i++) {
      MIN2250[i] = new float[MIN1[0].length];
      for (int j = 0; j < MIN1[0].length; j++) {
        if (MIN1[i][j] < MIN2[i][j]) {
          MIN2250[i][j] = MIN1[i][j];
        } else {
          MIN2250[i][j] = MIN2[i][j];
        }
      }
    }
  }

  public void bd2265(String imgfile) {
    spBandDepth bd2265 = new spBandDepth(imgfile, mappedData_, 2210, 5, 2265, 3, 2340, 5);
    BD2265 = bd2265.getSummaryProduct();
  }

  public void bd2290(String imgfile) {
    spBandDepth bd2290_ = new spBandDepth(imgfile, mappedData_, 2250, 5, 2290, 5, 2350, 5);
    BD2290 = bd2290_.getSummaryProduct();
  }

  public void d2300(String imgfile) {
    RBmaker drop = new RBmaker(imgfile, mappedData_, 1815, 2530, 5);
    float[][] rat2120 = drop.findRRCratio(2120, 5);
    float[][] rat2170 = drop.findRRCratio(2170, 5);
    float[][] rat2210 = drop.findRRCratio(2210, 5);
    float[][] rat2290 = drop.findRRCratio(2290, 3);
    float[][] rat2320 = drop.findRRCratio(2320, 3);
    float[][] rat2330 = drop.findRRCratio(2330, 3);
    D2300 = new float[rat2120.length][];
    for (int i = 0; i < rat2120.length; i++) {
      D2300[i] = new float[rat2120[0].length];
      for (int j = 0; j < rat2120[0].length; j++) {
        D2300[i][j] = (float) 1 - (float) ((float) (rat2290[i][j] + rat2320[i][j] + rat2330[i][j])
            / (float) (rat2120[i][j] + rat2170[i][j] + rat2210[i][j]));
      }
    }
  }

  public void bd2355(String imgfile) {
    spBandDepth bd2355_ = new spBandDepth(imgfile, mappedData_, 2300, 5, 2355, 5, 2450, 5);
    BD2355 = bd2355_.getSummaryProduct();
  }

  public void sindex2(String imgfile) {
    spShoulder sind = new spShoulder(imgfile, mappedData_, 2120, 5, 2290, 7, 2400, 3);
    SINDEX2 = sind.getSummaryProduct();
  }

  public void icer2_2(String imgfile) {
    RBmaker icer2 = new RBmaker(imgfile, mappedData_, 2456, 2530, 5);
    float[][] RB2600 = icer2.findNewRB(2600, 5);
    ICER2_2 = new float[RB2600.length][];
    for (int i = 0; i < RB2600.length; i++) {
      ICER2_2[i] = new float[RB2600[0].length];
      for (int j = 0; j < RB2600[0].length; j++) {
        ICER2_2[i][j] = RB2600[i][j];
      }
    }
  }

  public void min2295_2480(String imgfile) {
    spBandDepth min2295 = new spBandDepth(imgfile, mappedData_, 2165, 5, 2295, 5, 2364, 5);
    float[][] MIN1 = min2295.getSummaryProduct();
    spBandDepth min2480 = new spBandDepth(imgfile, mappedData_, 2364, 5, 2480, 5, 2570, 5);
    float[][] MIN2 = min2480.getSummaryProduct();
    MIN2295_2480 = new float[MIN1.length][];
    for (int i = 0; i < MIN1.length; i++) {
      MIN2295_2480[i] = new float[MIN1[0].length];
      for (int j = 0; j < MIN1[0].length; j++) {
        if (MIN1[i][j] < MIN2[i][j]) {
          MIN2295_2480[i][j] = MIN1[i][j];
        } else {
          MIN2295_2480[i][j] = MIN2[i][j];
        }
      }
    }
  }

  public void min2345_2537(String imgfile) {
    spBandDepth min2345 = new spBandDepth(imgfile, mappedData_, 2250, 5, 2345, 5, 2430, 5);
    float[][] MIN1_ = min2345.getSummaryProduct();
    spBandDepth min2537 = new spBandDepth(imgfile, mappedData_, 2430, 5, 2537, 5, 2602, 5);
    float[][] MIN2_ = min2537.getSummaryProduct();
    MIN2345_2537 = new float[MIN1_.length][];
    for (int i = 0; i < MIN1_.length; i++) {
      MIN2345_2537[i] = new float[MIN1_[0].length];
      for (int j = 0; j < MIN1_[0].length; j++) {
        if (MIN1_[i][j] < MIN2_[i][j]) {
          MIN2345_2537[i][j] = MIN1_[i][j];
        } else {
          MIN2345_2537[i][j] = MIN2_[i][j];
        }
      }
    }
  }

  public void bd2500_2(String imgfile) {
    spBandDepth bd2500 = new spBandDepth(imgfile, mappedData_, 2364, 5, 2480, 5, 2570, 5);
    BD2500_2 = bd2500.getSummaryProduct();
  }

  public void bd3000(String imgfile) {
    arrayMedian r2530 = new arrayMedian(imgfile, mappedData_, 2530, 5);
    float[][] R2530 = r2530.getToast();
    arrayMedian r3000 = new arrayMedian(imgfile, mappedData_, 3000, 5);
    float[][] R3000 = r3000.getToast();
    arrayMedian r2210 = new arrayMedian(imgfile, mappedData_, 2210, 5);
    float[][] R2210 = r2210.getToast();
    float[][] IRR2 = new float[R2210.length][];
    for (int i = 0; i < R2210.length; i++) {
      IRR2[i] = new float[R2210[0].length];
      for (int j = 0; j < R2210[0].length; j++) {
        IRR2[i][j] = (float) R2530[i][j] / R2210[i][j];
      }
    }

    BD3000 = new float[R3000.length][];
    for (int i = 0; i < R3000.length; i++) {
      BD3000[i] = new float[R3000[0].length];
      for (int j = 0; j < R3000[0].length; j++) {
        BD3000[i][j] =
            (float) 1 - (float) ((float) R3000[i][j] / (float) (R2530[i][j] * IRR2[i][j]));
      }
    }
  }

  public void bd3100(String imgfile) {
    spBandDepth bd3100 = new spBandDepth(imgfile, mappedData_, 3000, 5, 3120, 5, 3250, 5);
    BD3100 = bd3100.getSummaryProduct();
  }

  public void bd3200(String imgfile) {
    spBandDepth bd3200 = new spBandDepth(imgfile, mappedData_, 3250, 5, 3320, 5, 3390, 5);
    BD3200 = bd3200.getSummaryProduct();
  }

  public void bd3400_2(String imgfile) {
    spBandDepth bd3400_2 = new spBandDepth(imgfile, mappedData_, 3250, 10, 3420, 15, 3630, 10);
    BD3400_2 = bd3400_2.getSummaryProduct();
  }

  public void cindex2(String imgfile) {
    spShoulder cindex2 = new spShoulder(imgfile, mappedData_, 3450, 9, 3610, 11, 3875, 7);
    CINDEX2 = cindex2.getSummaryProduct();
  }

  public void r1080(String imgfile) {
    arrayMedian r1080 = new arrayMedian(imgfile, mappedData_, 1080, 5);
    R1080 = r1080.getToast();
  }

  public void r1506(String imgfile) {
    arrayMedian r1506 = new arrayMedian(imgfile, mappedData_, 1506, 5);
    R1506 = r1506.getToast();
  }

  public void r2529(String imgfile) {
    arrayMedian r2529 = new arrayMedian(imgfile, mappedData_, 2529, 5);
    R2529 = r2529.getToast();
  }

  public void bd2600(String imgfile) {
    spBandDepth bd2600 = new spBandDepth(imgfile, mappedData_, 2530, 5, 2600, 5, 2630, 5);
    BD2600 = bd2600.getSummaryProduct();
  }

  public void irr2(String imgfile) {
    arrayMedian r2530 = new arrayMedian(imgfile, mappedData_, 2530, 5);
    float[][] R2530 = r2530.getToast();
    arrayMedian r2210 = new arrayMedian(imgfile, mappedData_, 2210, 5);
    float[][] R2210 = r2210.getToast();
    IRR2 = new float[R2210.length][];
    for (int i = 0; i < R2210.length; i++) {
      IRR2[i] = new float[R2210[0].length];
      for (int j = 0; j < R2210[0].length; j++) {
        IRR2[i][j] = (float) R2530[i][j] / R2210[i][j];
      }
    }
  }

  public void irr3(String imgfile) {
    arrayMedian r3500 = new arrayMedian(imgfile, mappedData_, 3500, 7);
    float[][] R3500 = r3500.getToast();
    arrayMedian r3390 = new arrayMedian(imgfile, mappedData_, 3390, 7);
    float[][] R3390 = r3390.getToast();
    IRR3 = new float[R3390.length][];
    for (int i = 0; i < R3390.length; i++) {
      IRR3[i] = new float[R3390[0].length];
      for (int j = 0; j < R3390[0].length; j++) {
        IRR3[i][j] = (float) R3500[i][j] / R3390[i][j];
      }
    }
  }

  public void r3920(String imgfile) {
    arrayMedian r3920 = new arrayMedian(imgfile, mappedData_, 3920, 5);
    R3920 = r3920.getToast();
  }

  public float int_tabulated(List<Double> waverange, double[] wv_vec_um, double[] adj_spec_vec) {
    int counter = 0;
    for (int i = 1; i < adj_spec_vec.length; i++) { // removes duplicate wavelength entries
      if (wv_vec_um[i - counter] == wv_vec_um[i - counter - 1]) {
        wv_vec_um = ArrayUtils.remove(wv_vec_um, i - counter);
        adj_spec_vec = ArrayUtils.remove(adj_spec_vec, i - counter);
        counter++;
      }
    }

    double h = (wv_vec_um[wv_vec_um.length - 1] - wv_vec_um[0]) / 4.0; // newton-cotes 5-point
                                                                       // closed integration
    double[] f = new double[5];
    for (int i = 0; i < 5; i++) {
      int idx = findIndex(waverange, wv_vec_um[0] + (i * h));
      f[i] = adj_spec_vec[idx];
    }

    float newSP =
        (float) ((2.0 / 45.0) * h * (7 * f[0] + 32 * f[1] + 12 * f[2] + 32 * f[3] + 7 * f[4]));
    return newSP;

  }

  public double[] linfit(double[] x, double[] y) {
    int n = x.length;
    double sumx = Arrays.stream(x).sum();
    double sumy = Arrays.stream(y).sum();
    double xbar = sumx / n;
    double ybar = sumy / n;

    double xxbar = 0.0, xybar = 0.0;
    for (int i = 0; i < n; i++) {
      xxbar += Math.pow((x[i] - xbar), 2);
      xybar += (x[i] - xbar) * (y[i] - ybar);
    }

    double[] fit = new double[2];
    fit[1] = xybar / xxbar;
    fit[0] = ybar - fit[1] * xbar;
    return fit;
  }

}
