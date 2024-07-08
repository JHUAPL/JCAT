package javafx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;

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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import reader.CRISMPDSImage;
import reader.TRDR;
import util.JCATLog;
import util.RBmaker;
import util.arrayMedian;
import util.spBandDepth;
import util.spShoulder;

public class TRDRSummaryParameters extends TRDRBrowseProducts {

  private ComboBox<String> summaryParameters;
  private int spCode = 1;

  protected float[][][] mappedData_;
  private float[][] currentSP;

  public TRDRSummaryParameters() {
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

    ///////////////////////////////////////
    // Instantiates CRISM based on 's', 'l' or 'j' data ID as CRISMPDSImageNextGen
    // or TRDR
    // If TRDR, ADRVSLibrary is created for potential volcano scan corrections
    // adrNames becomes a list of correct bin code and wavelength filter
    // created BEFORE the TRDR file
    // adrIDList and adrIDItems contain the ID's of the applicable ADR's
    // correctADRfiles holds the most recent of each ADR ID to
    // be called during volcano scan implementation
    ///////////////////////////////////////
    basefile = FilenameUtils.getBaseName(basefile);

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

    if (mappedData_ == null)
      mappedData_ = getMappedData();

    ObservableList<String> items;
    if (CRISM.filename.substring(CRISM.filename.length() - 10, CRISM.filename.length() - 9)
        .toLowerCase().equals("l")) {
      items = FXCollections.observableArrayList("R1330", "BD1300", "OLINDEX3", "LCPINDEX2",
          "HCPINDEX2", "ISLOPE1", "BD1400", "BD1435", "BD1500_2", "ICER1_2", "BD1750_2", "BD1900_2",
          "BD1900r2", "BD2100_2", "BD2165", "BD2190", "MIN2200", "BD2210_2", "D2200", "BD2230",
          "BD2250", "MIN2250", "BD2265", "BD2290", "D2300", "BD2355", "SINDEX2", "ICER2_2",
          "MIN2295_2480", "MIN2345_2537", "BD2500_2", "BD3000", "BD3100", "BD3200", "BD3400_2",
          "CINDEX2", "R1080", "R1506", "R2529", "BD2600", "IRR2", "IRR3", "R3920");
    } else {
      items = FXCollections.observableArrayList("R770", "RBR", "BD530_2", "SH600_2", "SH770",
          "BD640_2", "BD860_2", "BD920_2", "R600", "R530", "R440", "IRR1");
    }
    // removed BDI1000IR, VAR, BDI2000 from IR
    // removed RPEAK1, BDI1000VIS
    // if including them, be sure to check spCode executes correct summary parameter

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

    GridPane.setConstraints(summaryParameters, 1, 2);
    GridPane.setConstraints(crdValue, 1, 3);
    GridPane.setConstraints(nameSP, 1, 4);
    GridPane.setConstraints(updateBtn, 2, 3);
    GridPane.setConstraints(subLabel, 2, 4);
    controls.getChildren().addAll(crdValue, updateBtn, subLabel, nameSP, summaryParameters);

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
      String value = "Value: " + (currentSP == null ? "None" : currentSP[newRow][newCol]);
      crdValue.setText("Data (row, col): (" + newRow + ", " + newCol + ")" + ", Latitude: "
          + String.format("%.4f", latitude) + ", Longitude: " + String.format("%.4f", longitude)
          + "\n" + value);
    });

    finalImg2.setOnMouseMoved(e -> {
      int newRow = (int) (e.getY());
      int newCol = (int) (e.getX());
      double longitude = CRISM.getLon(newRow, newCol);
      double latitude = CRISM.getLat(newRow, newCol);
      crdValue.setText("Data (row, col): (" + newRow + ", " + newCol + ")" + "Latitude: "
          + String.format("%.4f", latitude) + ", Longitude: " + String.format("%.4f", longitude));
    });

    updateBtn.setOnAction(e -> {
      spCode = summaryParameters.getItems().indexOf(summaryParameters.getValue()) + 1;

      float[][] newSP = null;

      if (CRISM.filename.substring(CRISM.filename.length() - 10, CRISM.filename.length() - 9)
          .equals("s")) {

        if (spCode == 1) {
          // R770
          arrayMedian r770 = new arrayMedian(imgfile, mappedData_, 770, 5);
          newSP = r770.getToast();
        } else if (spCode == 2) {
          // RBR
          arrayMedian r770 = new arrayMedian(imgfile, mappedData_, 770, 5);
          float[][] R770 = r770.getToast();
          arrayMedian r440 = new arrayMedian(imgfile, mappedData_, 440, 5);
          float[][] R440 = r440.getToast();
          newSP = new float[R440.length][];
          for (int i = 0; i < R440.length; i++) {
            newSP[i] = new float[R440[0].length];
            for (int j = 0; j < R440[0].length; j++) {
              newSP[i][j] = (float) R770[i][j] / R440[i][j];
            }
          }
        } else if (spCode == 3) {
          // BD530_2
          spBandDepth bd530_2 = new spBandDepth(imgfile, mappedData_, 440, 5, 530, 5, 614, 5);
          newSP = bd530_2.getSummaryProduct();
        } else if (spCode == 4) {
          // SH600_2
          spShoulder sh600_2 = new spShoulder(imgfile, mappedData_, 533, 5, 600, 5, 716, 3);
          newSP = sh600_2.getSummaryProduct();
        } else if (spCode == 5) {
          // SH770
          spShoulder sh770 = new spShoulder(imgfile, mappedData_, 716, 3, 775, 5, 860, 5);
          newSP = sh770.getSummaryProduct();
        } else if (spCode == 6) {
          // BD640_2
          spBandDepth bd640_2 = new spBandDepth(imgfile, mappedData_, 600, 5, 624, 3, 760, 5);
          newSP = bd640_2.getSummaryProduct();
        } else if (spCode == 7) {
          // BD860_2
          spBandDepth bd860_2 = new spBandDepth(imgfile, mappedData_, 755, 5, 860, 5, 977, 5);
          newSP = bd860_2.getSummaryProduct();
        } else if (spCode == 8) {
          // BD920_2
          spBandDepth bd920_2 = new spBandDepth(imgfile, mappedData_, 807, 5, 920, 5, 984, 5);
          newSP = bd920_2.getSummaryProduct();
          // } else if (spCode == 9) {
          // // RPEAK1
          // spRPEAK1 rpeak1 = new spRPEAK1(mappedData_);
          // newSP = rpeak1.getRPEAK1();
          // } else if (spCode == 10) {
          // // BDI1000VIS
          // spRPEAK1 rpeak1 = new spRPEAK1(mappedData_);
          // spBDI1000VIS bdi1000vis = new spBDI1000VIS(imgfile, mappedData_, rpeak1.getRPEAK1());
          // newSP = bdi1000vis.getBDI1000VIS();
        } else if (spCode == 9) {
          // R600
          arrayMedian r600 = new arrayMedian(imgfile, mappedData_, 600, 5);
          newSP = r600.getToast();
        } else if (spCode == 10) {
          // R530
          arrayMedian r530 = new arrayMedian(imgfile, mappedData_, 530, 5);
          newSP = r530.getToast();
        } else if (spCode == 11) {
          // R440
          arrayMedian r440 = new arrayMedian(imgfile, mappedData_, 440, 5);
          newSP = r440.getToast();
        } else {
          // IRR1
          arrayMedian r800 = new arrayMedian(imgfile, mappedData_, 800, 5);
          float[][] R800 = r800.getToast();
          arrayMedian r997 = new arrayMedian(imgfile, mappedData_, 997, 5);
          float[][] R997 = r997.getToast();
          newSP = new float[R997.length][];
          for (int i = 0; i < R997.length; i++) {
            newSP[i] = new float[R997[0].length];
            for (int j = 0; j < R997[0].length; j++) {
              newSP[i][j] = (float) R800[i][j] / R997[i][j];
            }
          }
        }

      } else {
        // if (spCode == 1) {
        // // BDI1000IR
        // wavelength = CRISM.getWavelengths();
        //
        // int h = mappedData_[0].length;
        // int w = mappedData_[0][0].length;
        // bdiCont bdicont = new bdiCont(CRISM, h, w, wavelength);
        //
        // int length = 0;
        // for (int i = 0; i < wavelength.size(); i++) {
        // if (wavelength.get(i) > 1020.0 && wavelength.get(i) < 1255.0) {
        // length++;
        // }
        // }
        //
        // int[] indices = new int[length];
        // List<Double> waverange = new ArrayList<Double>();
        // double[] wv_vec = new double[length];
        // double[] wv_vec_um = new double[length];
        // int counter = 0;
        // for (int i = 0; i < wavelength.size(); i++) {
        // if (wavelength.get(i) > 1020.0 && wavelength.get(i) < 1255.0) {
        // indices[counter] = findIndex(wavelength, wavelength.get(i));
        // waverange.add(wavelength.get(i));
        // wv_vec[counter] = wavelength.get(i);
        // wv_vec_um[counter] = wv_vec[counter] / 1000.0;
        // counter++;
        // }
        // }
        // Arrays.sort(wv_vec_um);
        //
        // double[][][] bdi1000ir_cube = new double[h][w][indices.length];
        // double[][][] cont_cube = new double[h][w][indices.length];
        // double[][][] normalized_cube = new double[h][w][indices.length];
        // for (int i = 0; i < h; i++) {
        // for (int j = 0; j < w; j++) {
        // for (int k = 0; k < indices.length; k++) {
        // bdi1000ir_cube[i][j][k] = CRISM.getPCFloatValue(i, j, indices[k]);
        // cont_cube[i][j][k] = (bdicont.getContSlopeFrame()[i][j]
        // * (wv_vec[k] - bdicont.getMedian2Wavelength())
        // + bdicont.getMedian2Frame()[i][j]);
        // normalized_cube[i][j][k] = bdi1000ir_cube[i][j][k] / cont_cube[i][j][k];
        // }
        // }
        // }
        //
        // double[] adj_spec_vec = new double[indices.length];
        // double[] check_vec = new double[indices.length];
        // newSP = new float[h][w];
        // for (int i = 0; i < h; i++) {
        // for (int j = 0; j < w; j++) {
        // for (int k = 0; k < indices.length; k++) {
        // adj_spec_vec[k] = 1.0 - normalized_cube[i][j][k];
        // check_vec[k] = bdi1000ir_cube[i][j][k];
        // }
        // if (Arrays.binarySearch(check_vec, FillValue) != check_vec.length) {
        // newSP[i][j] = int_tabulated(waverange, wv_vec_um, adj_spec_vec);
        // }
        // }
        // }
        //
        // } else if (spCode == 2) {
        if (spCode == 1) {
          // R1330
          arrayMedian r1330 = new arrayMedian(imgfile, mappedData_, 1330, 11);
          newSP = r1330.getToast();
        } else if (spCode == 2) {
          // BD1300
          spBandDepth bd1300 = new spBandDepth(imgfile, mappedData_, 1080, 5, 1320, 15, 1750, 5);
          newSP = bd1300.getSummaryProduct();
        } else if (spCode == 3) {
          // OLINDEX3
          RBmaker maker = new RBmaker(imgfile, mappedData_, 1750, 1862, 7);
          float[][] RB1210 = maker.findNewRB(1210, 7);
          float[][] RB1250 = maker.findNewRB(1250, 7);
          float[][] RB1263 = maker.findNewRB(1263, 7);
          float[][] RB1276 = maker.findNewRB(1276, 7);
          float[][] RB1330 = maker.findNewRB(1330, 7);

          newSP = new float[RB1210.length][RB1210[0].length];
          for (int i = 0; i < RB1210.length; i++) {
            for (int j = 0; j < RB1210[0].length; j++) {
              newSP[i][j] = RB1210[i][j] * (float) 0.1 + RB1250[i][j] * (float) 0.1
                  + RB1263[i][j] * (float) 0.2 + RB1276[i][j] * (float) 0.2
                  + RB1330[i][j] * (float) 0.4;
            }
          }
        } else if (spCode == 4) {
          // LCPINDEX2
          RBmaker maker2 = new RBmaker(imgfile, mappedData_, 1560, 2450, 7);
          float[][] RB1690 = maker2.findNewRB(1690, 7);
          float[][] RB1750 = maker2.findNewRB(1750, 7);
          float[][] RB1810 = maker2.findNewRB(1810, 7);
          float[][] RB1870 = maker2.findNewRB(1870, 7);

          newSP = new float[RB1690.length][RB1690[0].length];
          for (int i = 0; i < RB1690.length; i++) {
            for (int j = 0; j < RB1690[0].length; j++) {
              newSP[i][j] = RB1690[i][j] * (float) 0.20 + RB1750[i][j] * (float) 0.20
                  + RB1810[i][j] * (float) 0.30 + RB1870[i][j] * (float) 0.30;
            }
          }
        } else if (spCode == 5) {
          // HCPINDEX2
          RBmaker maker3 = new RBmaker(imgfile, mappedData_, 1690, 2530, 7);
          float[][] RB2120 = maker3.findNewRB(2120, 5);
          float[][] RB2140 = maker3.findNewRB(2140, 7);
          float[][] RB2230 = maker3.findNewRB(2230, 7);
          float[][] RB2250 = maker3.findNewRB(2250, 7);
          float[][] RB2430 = maker3.findNewRB(2430, 7);
          float[][] RB2460 = maker3.findNewRB(2460, 7);

          newSP = new float[RB2120.length][RB2120[0].length];
          for (int i = 0; i < RB2120.length; i++) {
            for (int j = 0; j < RB2120[0].length; j++) {
              newSP[i][j] = RB2120[i][j] * (float) 0.10 + RB2140[i][j] * (float) 0.10
                  + RB2230[i][j] * (float) 0.15 + RB2250[i][j] * (float) 0.30
                  + RB2430[i][j] * (float) 0.20 + RB2460[i][j] * (float) 0.15;
            }
          }
          // } else if (spCode == 7) {
          // // VAR
          // wavelength = CRISM.getWavelengths();
          //
          // int h = mappedData_[0].length;
          // int w = mappedData_[0][0].length;
          //
          // int[] varslopewl = new int[2];
          // varslopewl[0] = findIndex(wavelength, 1014.00);
          // varslopewl[1] = findIndex(wavelength, 2287.00);
          //
          // double[] varslopelams = new double[2];
          // varslopelams[0] = wavelength.get(varslopewl[0]);
          // varslopelams[1] = wavelength.get(varslopewl[1]);
          //
          // double[][][] varslopers = new double[h][w][2];
          // for (int i = 0; i < h; i++) {
          // for (int j = 0; j < w; j++) {
          // for (int k = 0; k < varslopewl.length; k++) {
          // varslopers[i][j][k] = CRISM.getPCFloatValue(i, j, varslopewl[k]);
          // }
          // }
          // }
          //
          // int length = 0;
          // for (int i = 0; i < wavelength.size(); i++) {
          // if (wavelength.get(i) > 1021.0 && wavelength.get(i) < 2287.0) {
          // length++;
          // }
          // }
          //
          // int[] indices = new int[length];
          // double[] wvs = new double[length];
          // int counter = 0;
          // for (int i = 0; i < wavelength.size(); i++) {
          // if (wavelength.get(i) > 1021.0 && wavelength.get(i) < 2287.0) {
          // indices[counter] = findIndex(wavelength, wavelength.get(i));
          // wvs[counter] = wavelength.get(i);
          // counter++;
          // }
          // }
          //
          // double[][][] obsrs = new double[h][w][indices.length];
          // for (int i = 0; i < h; i++) {
          // for (int j = 0; j < w; j++) {
          // for (int k = 0; k < indices.length; k++) {
          // obsrs[i][j][k] = CRISM.getPCFloatValue(i, j, indices[k]);
          // }
          // }
          // }
          //
          // double[] fit = new double[2];
          // double[] predrs = new double[wvs.length];
          // double[] sub = new double[wvs.length];
          // double[] array = new double[wvs.length];
          // newSP = new float[h][w];
          // for (int i = 0; i < h; i++) {
          // for (int j = 0; j < w; j++) {
          // fit = linfit(varslopelams, varslopers[i][j]);
          // for (int k = 0; k < wvs.length; k++) {
          // predrs[k] = fit[0] + (fit[1] * wvs[k]);
          // sub[k] = predrs[k] - obsrs[i][j][k];
          // array[k] = Math.pow(sub[k], 2);
          // }
          // newSP[i][j] = (float) Arrays.stream(array).sum();
          // }
          // }

        } else if (spCode == 6) {
          // ISLOPE1
          wavelength = CRISM.getWavelengths();

          arrayMedian r1815 = new arrayMedian(imgfile, mappedData_, 1815, 5);
          float[][] R1815 = r1815.getToast();
          arrayMedian r2530 = new arrayMedian(imgfile, mappedData_, 2530, 5);
          float[][] R2530 = r2530.getToast();
          float denom = (float) (wavelength.get(findIndex(wavelength, 2530.0))
              - wavelength.get(findIndex(wavelength, 1815.0)));
          newSP = new float[R1815.length][];
          for (int i = 0; i < R1815.length; i++) {
            newSP[i] = new float[R1815[0].length];
            for (int j = 0; j < R1815[0].length; j++) {
              newSP[i][j] = (float) 1000.0 * ((float) (R1815[i][j] - R2530[i][j])) / denom;
            }
          }
        } else if (spCode == 7) {
          // BD1400
          spBandDepth bd1400 = new spBandDepth(imgfile, mappedData_, 1330, 5, 1395, 3, 1467, 5);
          newSP = bd1400.getSummaryProduct();
        } else if (spCode == 8) {
          // BD1435
          spBandDepth bd1435 = new spBandDepth(imgfile, mappedData_, 1370, 3, 1434, 1, 1470, 3);
          newSP = bd1435.getSummaryProduct();
        } else if (spCode == 9) {
          // BD1500_2
          spBandDepth bd1500_2 = new spBandDepth(imgfile, mappedData_, 1367, 5, 1525, 11, 1808, 5);
          newSP = bd1500_2.getSummaryProduct();
        } else if (spCode == 10) {
          // ICER1_2
          spBandDepth bd1435_ = new spBandDepth(imgfile, mappedData_, 1370, 3, 1435, 1, 1470, 3);
          float[][] bd1435 = bd1435_.getSummaryProduct();
          spBandDepth bd1500_2_ = new spBandDepth(imgfile, mappedData_, 1367, 5, 1525, 11, 1808, 5);
          float[][] bd1500_2 = bd1500_2_.getSummaryProduct();
          newSP = new float[bd1435.length][];
          for (int i = 0; i < bd1435.length; i++) {
            newSP[i] = new float[bd1435[0].length];
            for (int j = 0; j < bd1435[0].length; j++) {
              newSP[i][j] =
                  (float) 1 - (float) (((float) 1 - bd1435[i][j]) / ((float) 1 - bd1500_2[i][j]));
            }
          }
        } else if (spCode == 11) {
          // BD1750_2
          spBandDepth bd1750_2 = new spBandDepth(imgfile, mappedData_, 1690, 5, 1750, 3, 1815, 5);
          newSP = bd1750_2.getSummaryProduct();
        } else if (spCode == 12) {
          // BD1900_2
          spBandDepth bd1900_2 = new spBandDepth(imgfile, mappedData_, 1850, 5, 1930, 5, 2067, 5);
          newSP = bd1900_2.getSummaryProduct();
        } else if (spCode == 13) {
          // BD1900r2
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
          newSP = new float[rat1908.length][];
          for (int i = 0; i < rat1908.length; i++) {
            newSP[i] = new float[rat1908[0].length];
            for (int j = 0; j < rat1908[0].length; j++) {
              newSP[i][j] = (float) 1 - (float) ((float) (rat1908[i][j] + rat1914[i][j]
                  + rat1921[i][j] + rat1928[i][j] + rat1934[i][j] + rat1941[i][j])
                  / (float) (rat1862[i][j] + rat1869[i][j] + rat1875[i][j] + rat2112[i][j]
                      + rat2120[i][j] + rat2126[i][j]));
            }
          }
          // } else if (spCode == 16) {
          // // BDI2000
          // wavelength = CRISM.getWavelengths();
          //
          // int h = mappedData_[0].length;
          // int w = mappedData_[0][0].length;
          //
          // bdiCont bdicont = new bdiCont(CRISM, h, w, wavelength);
          //
          // int length = 0;
          // for (int i = 0; i < wavelength.size(); i++) {
          // if (wavelength.get(i) > 1660.0 && wavelength.get(i) < 2390.0) {
          // length++;
          // }
          // }
          //
          // int[] indices = new int[length];
          // List<Double> waverange = new ArrayList<Double>();
          // double[] wv_vec = new double[length];
          // double[] wv_vec_um = new double[length];
          // int counter = 0;
          // for (int i = 0; i < wavelength.size(); i++) {
          // if (wavelength.get(i) > 1660.0 && wavelength.get(i) < 2390.0) {
          // indices[counter] = findIndex(wavelength, wavelength.get(i));
          // waverange.add(wavelength.get(i));
          // wv_vec[counter] = wavelength.get(i);
          // wv_vec_um[counter] = wv_vec[counter] / 1000.0;
          // counter++;
          // }
          // }
          // Arrays.sort(wv_vec_um);
          //
          // double[][][] bdi2000_cube = new double[h][w][indices.length];
          // double[][][] cont_cube = new double[h][w][indices.length];
          // double[][][] normalized_cube = new double[h][w][indices.length];
          // for (int i = 0; i < h; i++) {
          // for (int j = 0; j < w; j++) {
          // for (int k = 0; k < indices.length; k++) {
          // bdi2000_cube[i][j][k] = CRISM.getPCFloatValue(i, j, indices[k]);
          // cont_cube[i][j][k] = (bdicont.getContSlopeFrame()[i][j]
          // * (wv_vec[k] - bdicont.getMedian2Wavelength())
          // + bdicont.getMedian2Frame()[i][j]);
          // normalized_cube[i][j][k] = bdi2000_cube[i][j][k] / cont_cube[i][j][k];
          // }
          // }
          // }
          //
          // double[] adj_spec_vec = new double[indices.length];
          // newSP = new float[h][w];
          // for (int i = 0; i < h; i++) {
          // for (int j = 0; j < w; j++) {
          // for (int k = 0; k < indices.length; k++) {
          // adj_spec_vec[k] = 1.0 - normalized_cube[i][j][k];
          // }
          // newSP[i][j] = int_tabulated(waverange, wv_vec_um, adj_spec_vec);
          // }
          // }

        } else if (spCode == 14) {
          // BD2100_2
          spBandDepth bd2100 = new spBandDepth(imgfile, mappedData_, 1930, 3, 2132, 5, 2250, 3);
          newSP = bd2100.getSummaryProduct();
        } else if (spCode == 15) {
          // BD2165
          spBandDepth bd2165 = new spBandDepth(imgfile, mappedData_, 2120, 5, 2165, 3, 2230, 3);
          newSP = bd2165.getSummaryProduct();
        } else if (spCode == 16) {
          // BD2190
          spBandDepth bd2190 = new spBandDepth(imgfile, mappedData_, 2120, 5, 2185, 3, 2250, 3);
          newSP = bd2190.getSummaryProduct();
        } else if (spCode == 17) {
          // MIN2200
          spBandDepth min1 = new spBandDepth(imgfile, mappedData_, 2120, 5, 2210, 3, 2350, 5);
          float[][] MIN1 = min1.getSummaryProduct();
          spBandDepth min2 = new spBandDepth(imgfile, mappedData_, 2120, 5, 2165, 3, 2350, 5);
          float[][] MIN2 = min2.getSummaryProduct();
          newSP = new float[MIN1.length][];
          for (int i = 0; i < MIN1.length; i++) {
            newSP[i] = new float[MIN1[0].length];
            for (int j = 0; j < MIN1[0].length; j++) {
              if (MIN1[i][j] < MIN2[i][j]) {
                newSP[i][j] = MIN1[i][j];
              } else {
                newSP[i][j] = MIN2[i][j];
              }
            }
          }
        } else if (spCode == 18) {
          // BD2210_2
          spBandDepth bd2210 = new spBandDepth(imgfile, mappedData_, 2165, 5, 2210, 5, 2290, 5);
          newSP = bd2210.getSummaryProduct();
        } else if (spCode == 19) {
          // D2200
          RBmaker drop2 = new RBmaker(imgfile, mappedData_, 1815, 2430, 7);
          float[][] rat2210_ = drop2.findRRCratio(2210, 7);
          float[][] rat2230 = drop2.findRRCratio(2230, 7);
          float[][] rat2165 = drop2.findRRCratio(2165, 5);
          newSP = new float[rat2210_.length][];
          for (int i = 0; i < rat2210_.length; i++) {
            newSP[i] = new float[rat2210_[0].length];
            for (int j = 0; j < rat2210_[0].length; j++) {
              newSP[i][j] = (float) 1 - (float) ((float) (rat2210_[i][j] + rat2230[i][j])
                  / (float) (rat2165[i][j] + rat2165[i][j]));
            }
          }
        } else if (spCode == 20) {
          // BD2230
          spBandDepth bd2230 = new spBandDepth(imgfile, mappedData_, 2210, 3, 2235, 3, 2252, 3);
          newSP = bd2230.getSummaryProduct();
        } else if (spCode == 21) {
          // BD2250
          spBandDepth bd2250 = new spBandDepth(imgfile, mappedData_, 2120, 5, 2245, 7, 2340, 3);
          newSP = bd2250.getSummaryProduct();
        } else if (spCode == 22) {
          // MIN2250
          spBandDepth min1 = new spBandDepth(imgfile, mappedData_, 2165, 5, 2210, 3, 2350, 5);
          float[][] MIN1 = min1.getSummaryProduct();
          spBandDepth min2 = new spBandDepth(imgfile, mappedData_, 2165, 5, 2265, 3, 2350, 5);
          float[][] MIN2 = min2.getSummaryProduct();
          newSP = new float[MIN1.length][];
          for (int i = 0; i < MIN1.length; i++) {
            newSP[i] = new float[MIN1[0].length];
            for (int j = 0; j < MIN1[0].length; j++) {
              if (MIN1[i][j] < MIN2[i][j]) {
                newSP[i][j] = MIN1[i][j];
              } else {
                newSP[i][j] = MIN2[i][j];
              }
            }
          }
        } else if (spCode == 23) {
          // BD2265
          spBandDepth bd2265 = new spBandDepth(imgfile, mappedData_, 2210, 5, 2265, 3, 2340, 5);
          newSP = bd2265.getSummaryProduct();
        } else if (spCode == 24) {
          // BD2290
          spBandDepth bd2290_ = new spBandDepth(imgfile, mappedData_, 2250, 5, 2290, 5, 2350, 5);
          newSP = bd2290_.getSummaryProduct();
        } else if (spCode == 25) {
          // D2300
          RBmaker drop = new RBmaker(imgfile, mappedData_, 1815, 2530, 5);
          float[][] rat2120 = drop.findRRCratio(2120, 5);
          float[][] rat2170 = drop.findRRCratio(2170, 5);
          float[][] rat2210 = drop.findRRCratio(2210, 5);
          float[][] rat2290 = drop.findRRCratio(2290, 3);
          float[][] rat2320 = drop.findRRCratio(2320, 3);
          float[][] rat2330 = drop.findRRCratio(2330, 3);
          newSP = new float[rat2120.length][];
          for (int i = 0; i < rat2120.length; i++) {
            newSP[i] = new float[rat2120[0].length];
            for (int j = 0; j < rat2120[0].length; j++) {
              newSP[i][j] =
                  (float) 1 - (float) ((float) (rat2290[i][j] + rat2320[i][j] + rat2330[i][j])
                      / (float) (rat2120[i][j] + rat2170[i][j] + rat2210[i][j]));

            }
          }
        } else if (spCode == 26) {
          // BD2355
          spBandDepth bd2355_ = new spBandDepth(imgfile, mappedData_, 2300, 5, 2355, 5, 2450, 5);
          newSP = bd2355_.getSummaryProduct();
        } else if (spCode == 27) {
          // SINDEX2
          spShoulder sind = new spShoulder(imgfile, mappedData_, 2120, 5, 2290, 7, 2400, 3);
          newSP = sind.getSummaryProduct();
        } else if (spCode == 28) {
          // ICER2_2
          RBmaker icer2 = new RBmaker(imgfile, mappedData_, 2456, 2530, 5);
          float[][] RB2600 = icer2.findNewRB(2600, 5);
          newSP = new float[RB2600.length][RB2600[0].length];
          for (int i = 0; i < RB2600.length; i++) {
            for (int j = 0; j < RB2600[0].length; j++) {
              newSP[i][j] = RB2600[i][j];
            }
          }
        } else if (spCode == 29) {
          // MIN2295_2480
          spBandDepth min2295 = new spBandDepth(imgfile, mappedData_, 2165, 5, 2295, 5, 2364, 5);
          float[][] MIN1 = min2295.getSummaryProduct();
          spBandDepth min2480 = new spBandDepth(imgfile, mappedData_, 2364, 5, 2480, 5, 2570, 5);
          float[][] MIN2 = min2480.getSummaryProduct();
          newSP = new float[MIN1.length][];
          for (int i = 0; i < MIN1.length; i++) {
            newSP[i] = new float[MIN1[0].length];
            for (int j = 0; j < MIN1[0].length; j++) {
              if (MIN1[i][j] < MIN2[i][j]) {
                newSP[i][j] = MIN1[i][j];
              } else {
                newSP[i][j] = MIN2[i][j];
              }
            }
          }
        } else if (spCode == 30) {
          // MIN2345_2537
          spBandDepth min2345 = new spBandDepth(imgfile, mappedData_, 2250, 5, 2345, 5, 2430, 5);
          float[][] MIN1_ = min2345.getSummaryProduct();
          spBandDepth min2537 = new spBandDepth(imgfile, mappedData_, 2430, 5, 2537, 5, 2602, 5);
          float[][] MIN2_ = min2537.getSummaryProduct();
          newSP = new float[MIN1_.length][];
          for (int i = 0; i < MIN1_.length; i++) {
            newSP[i] = new float[MIN1_[0].length];
            for (int j = 0; j < MIN1_[0].length; j++) {
              if (MIN1_[i][j] < MIN2_[i][j]) {
                newSP[i][j] = MIN1_[i][j];
              } else {
                newSP[i][j] = MIN2_[i][j];
              }
            }
          }
        } else if (spCode == 31) {
          // BD2500_2
          spBandDepth bd2500 = new spBandDepth(imgfile, mappedData_, 2364, 5, 2480, 5, 2570, 5);
          newSP = bd2500.getSummaryProduct();
        } else if (spCode == 32) {
          // BD3000
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

          newSP = new float[R3000.length][];
          for (int i = 0; i < R3000.length; i++) {
            newSP[i] = new float[R3000[0].length];
            for (int j = 0; j < R3000[0].length; j++) {
              newSP[i][j] =
                  (float) 1 - (float) ((float) R3000[i][j] / (float) (R2530[i][j] * IRR2[i][j]));
            }
          }
        } else if (spCode == 33) {
          // BD3100
          spBandDepth bd3100 = new spBandDepth(imgfile, mappedData_, 3000, 5, 3120, 5, 3250, 5);
          newSP = bd3100.getSummaryProduct();
        } else if (spCode == 34) {
          // BD3200
          spBandDepth bd3200 = new spBandDepth(imgfile, mappedData_, 3250, 5, 3320, 5, 3390, 5);
          newSP = bd3200.getSummaryProduct();
        } else if (spCode == 35) {
          // BD3400_2
          spBandDepth bd3400_2 =
              new spBandDepth(imgfile, mappedData_, 3250, 10, 3420, 15, 3630, 10);
          newSP = bd3400_2.getSummaryProduct();
        } else if (spCode == 36) {
          // CINDEX2
          spShoulder cindex2 = new spShoulder(imgfile, mappedData_, 3450, 9, 3610, 11, 3875, 7);
          newSP = cindex2.getSummaryProduct();
        } else if (spCode == 37) {
          // R1080
          arrayMedian r1080 = new arrayMedian(imgfile, mappedData_, 1080, 5);
          newSP = r1080.getToast();
        } else if (spCode == 38) {
          // R1506
          arrayMedian r1506 = new arrayMedian(imgfile, mappedData_, 1506, 5);
          newSP = r1506.getToast();
        } else if (spCode == 39) {
          // R2529
          arrayMedian r2529 = new arrayMedian(imgfile, mappedData_, 2529, 5);
          newSP = r2529.getToast();
        } else if (spCode == 40) {
          // BD2600
          spBandDepth bd2600 = new spBandDepth(imgfile, mappedData_, 2530, 5, 2600, 5, 2630, 5);
          newSP = bd2600.getSummaryProduct();
        } else if (spCode == 41) {
          // IRR2
          arrayMedian r2530 = new arrayMedian(imgfile, mappedData_, 2530, 5);
          float[][] R2530 = r2530.getToast();
          arrayMedian r2210 = new arrayMedian(imgfile, mappedData_, 2210, 5);
          float[][] R2210 = r2210.getToast();
          newSP = new float[R2210.length][];
          for (int i = 0; i < R2210.length; i++) {
            newSP[i] = new float[R2210[0].length];
            for (int j = 0; j < R2210[0].length; j++) {
              newSP[i][j] = (float) R2530[i][j] / R2210[i][j];
            }
          }
        } else if (spCode == 42) {
          // IRR3
          arrayMedian r3500 = new arrayMedian(imgfile, mappedData_, 3500, 7);
          float[][] R3500 = r3500.getToast();
          arrayMedian r3390 = new arrayMedian(imgfile, mappedData_, 3390, 7);
          float[][] R3390 = r3390.getToast();
          newSP = new float[R3390.length][];
          for (int i = 0; i < R3390.length; i++) {
            newSP[i] = new float[R3390[0].length];
            for (int j = 0; j < R3390[0].length; j++) {
              newSP[i][j] = (float) R3500[i][j] / R3390[i][j];
            }
          }
        } else {
          // R3920
          arrayMedian r3920 = new arrayMedian(imgfile, mappedData_, 3920, 5);
          newSP = r3920.getToast();
        }
      }

      if (newSP == null) {
        JCATLog.getInstance().getLogger().log(Level.WARNING,
            summaryParameters.getItems().get(spCode - 1) + " is currently unavailable.");
      } else {

        nameSP.setText("Current: " + summaryParameters.getValue());
        currentSP = new float[newSP.length][newSP[0].length];

        for (int row = 0; row < newSP.length; row++)
          for (int col = 0; col < newSP[row].length; col++)
            currentSP[row][col] = newSP[row][col];

        BufferedImage newBI = CRISM.getQuickColorImage(newSP, newSP, newSP);
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

    return true;

  }

  public float int_tabulated(List<Double> waverange, double[] wv_vec_um, double[] adj_spec_vec) {

    int counter = 0;
    for (int i = 1; i < wv_vec_um.length; i++) { // removes duplicate wavelength entries
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


class bdiCont extends TRDRSummaryParameters {

  private double[][] cont_slope_frame;
  private double[][] median2_frame;
  private double median2_wavelength;

  public bdiCont(CRISMPDSImage CRISM, int h, int w, List<Double> wavelength) {
    int length1 = 0;
    int length2 = 0;
    for (int i = 0; i < wavelength.size(); i++) {
      if (wavelength.get(i) > 1330.0 && wavelength.get(i) < 1875.0) {
        length1++;
      } else if (wavelength.get(i) > 2430.0 && wavelength.get(i) < 2600.0) {
        length2++;
      }
    }

    int[] indices1 = new int[length1];
    int[] indices2 = new int[length2];
    double[] wv_vec1 = new double[length1];
    double[] wv_vec2 = new double[length2];
    int counter1 = 0;
    int counter2 = 0;
    for (int i = 0; i < wavelength.size(); i++) {
      if (wavelength.get(i) > 1330.0 && wavelength.get(i) < 1875.0) {
        indices1[counter1] = findIndex(wavelength, wavelength.get(i));
        wv_vec1[counter1] = wavelength.get(i);
        counter1++;
      } else if (wavelength.get(i) > 2430.0 && wavelength.get(i) < 2600.0) {
        indices2[counter2] = findIndex(wavelength, wavelength.get(i));
        wv_vec2[counter2] = wavelength.get(i);
        counter2++;
      }
    }

    double[][][] spec_vec1 = new double[h][w][length1];
    double[][][] spec_vec2 = new double[h][w][length2];
    double[][] percentile1_frame = new double[h][w];
    double percentile1_wavelength = 0.0;
    median2_frame = new double[h][w];
    cont_slope_frame = new double[h][w];
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        for (int k = 0; k < indices1.length; k++) {
          spec_vec1[i][j][k] = CRISM.getPCFloatValue(i, j, indices1[k]);
        }
        for (int l = 0; l < indices2.length; l++) {
          spec_vec2[i][j][l] = CRISM.getPCFloatValue(i, j, indices2[l]);
        }
        Arrays.sort(spec_vec1[i][j]);
        percentile1_frame[i][j] = spec_vec1[i][j][spec_vec1.length * (3 / 4)];

        median2_frame[i][j] = findMedian(spec_vec2[i][j], "odd");
      }
    }

    percentile1_wavelength = findMedian(wv_vec1, "even");
    median2_wavelength = findMedian(wv_vec2, "even");

    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        cont_slope_frame[i][j] = (median2_frame[i][j] - percentile1_frame[i][j])
            / (median2_wavelength - percentile1_wavelength);
      }
    }

  }

  private double findMedian(double[] list, String key) {
    double median = 0.0;
    int size = list.length;
    if (size % 2 == 0) {
      if (key == "even") {
        median = (list[size / 2] + list[size / 2 - 1]) * 0.5;
      } else {
        median = list[size / 2];
      }
    } else {
      median = list[size / 2 + 1];
    }
    return median;
  }

  public double[][] getContSlopeFrame() {
    return cont_slope_frame;
  }

  public double getMedian2Wavelength() {
    return median2_wavelength;
  }

  public double[][] getMedian2Frame() {
    return median2_frame;
  }
}
