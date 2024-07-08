package javafx;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import reader.ADR;
import reader.ADRVSLibrary;
import reader.CRISMPDSImageNextGen;
import reader.TRDR;
import util.JCATConfig;
import util.JCATLog;
import util.JCATMessageWindow;

public class launch extends JCATCalls {

  public boolean create(String imagefile, String basefile) throws IOException {
    ///////////////////////////////////////
    // Establishes rootDir as the JCAT folder in home directory
    ///////////////////////////////////////
    File rootDir = new File(System.getProperty("user.home"), "JCAT");
    if (!rootDir.isDirectory()) {
      rootDir.mkdirs();
    }

    String bFile = basefile;
    ///////////////////////////////////////
    // Instantiates CRISM based on 's', 'l' or 'j' data ID as CRISMPDSImageNextGen
    // or TRDR
    ///////////////////////////////////////
    boolean isTRDR = false;
    basefile = FilenameUtils.getBaseName(basefile);
    imgfile = imagefile;
    if (basefile.substring(20, 21).toLowerCase().equals("j")) {

      CRISM = new CRISMPDSImageNextGen(imgfile);

    } else {
      File parent = new File(FilenameUtils.getFullPath(imgfile));
      String base = FilenameUtils.getName(imgfile);
      ddrFile = new File(parent,
          base.replaceAll("ra", "de").replaceAll("if", "de").replaceAll("trr3", "ddr1")
              .replaceAll("RA", "DE").replaceAll("IF", "DE").replaceAll("TRR3", "DDR1"))
                  .getAbsolutePath();

      CRISM = new TRDR(imgfile, ddrFile);

      if (!((TRDR) CRISM).ddrExists())
        return false;

      if (basefile.substring(20, 21).toLowerCase().equals("l")) {
        // ADRVSLibrary is created for potential volcano scan corrections
        //
        // adrNames becomes a list of correct bin code and wavelength filter
        // created BEFORE the TRDR file
        //
        // adrIDList and adrIDItems contain the ID's of the applicable ADR's
        //
        // correctADRfiles holds the most recent of each ADR ID to
        // be called during volcano scan implementation
        int timeTRR = CRISM.getsclk();
        bc = CRISM.getBC();
        wv = CRISM.getWV();

        ADRVSLibrary adrLib = ADRVSLibrary.getInstance();
        File localPath =
            new File(JCATConfig.getInstance().getLocalArchive() + File.separator + "vs");
        if (!localPath.exists())
          localPath.mkdirs();

        for (String vsName : adrLib.getNames()) {
          String img = (vsName + ".IMG").toUpperCase();
          File f = new File(localPath, img);
          if (!f.exists()) {
            String path = adrLib.getPath();
            String lbl = (vsName + ".LBL").toUpperCase();
            FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(path + "/" + img), f);
            FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(path + "/" + lbl),
                new File(localPath, lbl));
          }
          if (vsName.substring(23).startsWith(Integer.toString(bc))) {
            if (vsName.substring(24).startsWith(Integer.toString(wv))) {
              adr = new ADR(f.getPath());
              int timeADR = adr.getsclk();
              if (timeADR < timeTRR) {
                adrNames.add(vsName);
                String ID = vsName.substring(15, 20);
                if (!adrIDList.contains(ID)) {
                  adrIDList.add(ID);
                  RadioMenuItem adrIDMenu = null;
                  if (ID.equals("06822")) {
                    adrIDMenu = new RadioMenuItem((String) ID + " (Dusty)");
                  } else if (ID.equals("09E04") || ID.equals("0A3F6") || ID.equals("094B5")) {
                    adrIDMenu = new RadioMenuItem((String) ID + " (Icy)");
                  } else {
                    adrIDMenu = new RadioMenuItem((String) ID);
                  }
                  adrIDMenu.setToggleGroup(VSTG);
                  adrIDItems.add(adrIDMenu);
                }
              }
            }
          }
        }
        for (String vsid : adrIDList) {
          String correctADR = adrVS(vsid);
          correctADRfiles.add(correctADR);
        }
        for (String adr : correctADRfiles) {
          String ID = adr.substring(15, 20);
          if (ID.startsWith("0")) { // checks if noisy VS
            lownoise.add(adr);
          }
        }
      }
      isTRDR = true;
    }
    if (!CRISM.validImage)
      return false;

    int height = (int) CRISM.getHeight(), width = (int) CRISM.getWidth();
    int startRow = height / 2, startCol = width / 2;
    //////////////////////////////////////////
    // Looks for a properties file that stores user preferences, extraneous
    // code, etc.
    // If it does not exist, use a default file
    //////////////////////////////////////////
    File r = new File(rootDir, "config.txt");
    if (r.exists()) {
      readPropertiesFile();
    } else {
      red = 304; // these are only three things stored in current properties file
      green = 122;
      blue = 41;
    }

    ///////////////////////////////////////////////////
    // Creates a menu bar with multiple menus
    // Toggle groups/Radio Menu Items ensure only one if selected at a time
    // Disables the zoom menus that will cause sizing issues
    // Check Menu Item for subset/photometric can be selected/de-selected anytime
    ///////////////////////////////////////////////////
    if ((height * 0.5) < 300 || (width * 0.5) < 300) {
      zoom50.setDisable(true);
    }
    if ((height * 0.75) < 300 || (width * 0.75) < 300) {
      zoom75.setDisable(true);
    }
    zoom50.setToggleGroup(zoomTG);
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

    singlePixel.setToggleGroup(specAvgSizeTG);
    singlePixel.setSelected(true);
    ninePixel.setToggleGroup(specAvgSizeTG);
    twentyFivePixel.setToggleGroup(specAvgSizeTG);
    specAvgSize.getItems().addAll(singlePixel, ninePixel, twentyFivePixel);

    click.setToggleGroup(specIntTG);
    click.setSelected(true);
    drag.setToggleGroup(specIntTG);
    region.setToggleGroup(specIntTG);
    specInt.getItems().addAll(click, drag, region, manualInput, mouseBtn);

    singSpec.setToggleGroup(numSpecTG);
    singSpec.setSelected(true);
    multSpec.setToggleGroup(numSpecTG);
    specNum.getItems().addAll(singSpec, multSpec);

    emp.setToggleGroup(VSTG);
    userChoice.getItems().addAll(adrIDItems);
    defVS.setToggleGroup(VSTG);
    noVS.setToggleGroup(VSTG);
    noVS.setSelected(true);
    A.getItems().addAll(emp, userChoice, defVS, noVS);
    ATP.getItems().addAll(A, T, P);
    if (!basefile.substring(22).startsWith("trr")) { // disables ATP if not viewing TRDR
      ATP.setDisable(true);
    }
    if (!basefile.substring(20, 21).equals("l")) { // disables atm correction unless using l TRDR
                                                   // file
      A.setDisable(true);
    }

    fileMenu.getItems().addAll(expandImg, saveImg, saveData, saveChart, new SeparatorMenuItem(),
        close);
    zoomMenu.getItems().addAll(zoom50, zoom75, zoom100, zoom125, zoom150, zoom200);
    plotMenu.getItems().addAll(resetX, resetY, resetB, setX, setY);
    controlsMenu.getItems().addAll(contrast, specAvgSize, specNum, specInt, specOps, ATP);

    if (isTRDR) {
      controlsMenu.getItems().add(showBP);
      controlsMenu.getItems().add(showSP);

      showBP.setOnAction(e -> {
        TRDRBrowseProducts su = new TRDRBrowseProducts();
        try {
          if (!su.create(imgfile, bFile))
            JCATLog.getInstance().getLogger().log(Level.WARNING,
                "Error Opening TRDR Browse Products GUI");
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      });

      showSP.setOnAction(e -> {
        TRDRSummaryParameters sp = new TRDRSummaryParameters();
        try {
          if (!sp.create(imgfile, bFile))
            JCATLog.getInstance().getLogger().log(Level.WARNING,
                "Error Opening Summary Parameters GUI");
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      });
    }

    mbar.getMenus().addAll(fileMenu, zoomMenu, plotMenu, controlsMenu);

    //////////////////////////////////////////////////
    // Creates a list of the indices, wavelengths, detector ID (VNIR=1, IR=0), list
    // of gbBands (g=1, b=0), intensity
    // Adds intensity to a list for reference during potential spectral operations
    //////////////////////////////////////////////////
    index = new ArrayList<Integer>();
    if (!basefile.substring(20).startsWith("j")) {
      for (int i = 0; i < CRISM.getBandIndices().size(); i++) {
        index.add(i);
      }
    } else {
      index = CRISM.getBandIndices();
    }
    wavelength = CRISM.getWavelengths();
    detector = CRISM.getDetector();
    gbBands = (!basefile.substring(22).startsWith("mtr")) ? removeBadBands()
        : new ArrayList<Integer>(Collections.nCopies(wavelength.size(), 1));

    if (gbBands.isEmpty())
      return false;

    int counter = 0;
    for (int i = 0; i < gbBands.size(); i++) {
      if (gbBands.get(i) == 0) {
        index.remove(i - counter);
        wavelength.remove(i - counter);
        counter++;
      }
    }

    singlePixel(intensity, startRow, startCol);
    spectralData.add(intensity);

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
    GraphicsContext gc = canvas.getGraphicsContext2D();

    //////////////////////////////////////////
    // Creates the starting image, a 1 %ile stretch of a standard image
    // Places image in starting image view, and creates a set of groups that will
    // make up the final displayed image
    //////////////////////////////////////////
    if (red > index.size()) {
      red = (int) (index.size() / 1.5); // arbitrary starting points
      green = (int) (index.size() / 2);
      blue = (int) (index.size() / 2.5);
    }
    bImg = percentileStretch(CRISM.getQuickColorImage(red, green, blue), 1);

    ///////////
    // if(bImg==null)
    // return;

    image = SwingFXUtils.toFXImage(bImg, null);

    ImageView iv = new ImageView(image), iv2 = new ImageView(image);
    ImageView iv3 = new ImageView(), iv4 = new ImageView();
    Group finalImg = new Group(iv, iv3), finalImg2 = new Group(iv2, iv4);

    ScrollPane sp = new ScrollPane(finalImg), sp2 = new ScrollPane(finalImg2);
    sp.setPrefSize(300, 300);

    ///////////////////////////////////////////////////////////
    // Blends the image plus user reference (cross-hair, etc.) with a black
    // rectangle to create final display
    //////////////////////////////////////////////////////////
    gc.strokeLine(startCol, startRow - 4, startCol, startRow + 4);
    gc.strokeLine(startCol - 4, startRow, startCol + 4, startRow);
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

    ////////////////////////////////////////////////////
    // Creates a line chart with series for the intensity, RGB markers
    // Adds data, RGB, & saves name of data for reference in spectral operations
    ////////////////////////////////////////////////////
    NumberAxis xAxis = new NumberAxis();
    NumberAxis yAxis = new NumberAxis(0, 0.4, 0.05);
    xAxis.setAutoRanging(true);
    xAxis.setForceZeroInRange(false);
    LineChart<Number, Number> lc = new LineChart<Number, Number>(xAxis, yAxis);
    XYChart.Series<Number, Number> LCData = new XYChart.Series<Number, Number>(),
        rm = new XYChart.Series<Number, Number>(), gm = new XYChart.Series<Number, Number>(),
        bm = new XYChart.Series<Number, Number>();
    rm.setName("Red");
    gm.setName("Green");
    bm.setName("Blue");
    xAxis.setLabel("Wavelength (nm)");
    yAxis.setLabel("Corrected I/F");

    inputData(intensity, LCData, lc);
    inputMarkers(lc, rm, gm, bm);
    lc.setCreateSymbols(false);
    lc.setAnimated(false);

    /////////////////////////////////////////////////
    // Since JavaFX Charts cannot have their data removed & replaced in an event
    // handler, this 'f' line chart acts as a place holder to store the current
    // line chart in order to refer back
    /////////////////////////////////////////////////
    flc = lc;
    frm = rm;
    fgm = gm;
    fbm = bm;

    ///////////////////////////////////////////////////////////
    // Creates labels, sliders, and value labels for RGB sliders
    // Sets wavelengths as values on slider, creates tick marks
    ///////////////////////////////////////////////////////////
    Label redLabel = new Label("Red:"), grnLabel = new Label("Green:"),
        bluLabel = new Label("Blue:");
    Label redValue = new Label(wavelength.get(red).toString() + "nm");
    Label grnValue = new Label(wavelength.get(green).toString() + "nm");
    Label bluValue = new Label(wavelength.get(blue).toString() + "nm");

    Button updateBtn = new Button("Update RGB & Image");
    Label subLabel = new Label();

    double maxSlider = Collections.max(wavelength);
    double minSlider = Collections.min(wavelength);
    double sliderDif = maxSlider - minSlider;
    Slider redSlider = new Slider(minSlider, maxSlider, wavelength.get(red));
    Slider grnSlider = new Slider(minSlider, maxSlider, wavelength.get(green));
    Slider bluSlider = new Slider(minSlider, maxSlider, wavelength.get(blue));

    redSlider.setMajorTickUnit(sliderDif / 10);
    redSlider.setMinorTickCount(5);
    redSlider.setBlockIncrement(5);
    grnSlider.setMajorTickUnit(sliderDif / 10);
    grnSlider.setMinorTickCount(5);
    grnSlider.setBlockIncrement(5);
    bluSlider.setShowTickLabels(true);
    bluSlider.setShowTickMarks(true);
    bluSlider.setMajorTickUnit(sliderDif / 10);
    bluSlider.setMinorTickCount(5);
    bluSlider.setBlockIncrement(5);

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
    LCData.setName(
        "Lat: " + String.format("%.4f", latitude) + ", Lon: " + String.format("%.4f", longitude));
    spectraName.add(LCData.getName());

    ////////////////////////////////////////////////////
    // Sets location of slider, coordinate objects in top-right section of grid pane
    // Sets the content of the different border/scroll panes
    ////////////////////////////////////////////////////
    GridPane.setConstraints(redLabel, 0, 0);
    GridPane.setConstraints(redSlider, 1, 0);
    GridPane.setConstraints(redValue, 2, 0);
    GridPane.setConstraints(grnLabel, 0, 1);
    GridPane.setConstraints(grnSlider, 1, 1);
    GridPane.setConstraints(grnValue, 2, 1);
    GridPane.setConstraints(bluLabel, 0, 2);
    GridPane.setConstraints(bluSlider, 1, 2);
    GridPane.setConstraints(bluValue, 2, 2);
    GridPane.setConstraints(crdValue, 1, 3);
    GridPane.setConstraints(updateBtn, 2, 3);
    GridPane.setConstraints(subLabel, 2, 4);
    controls.getChildren().addAll(redSlider, redLabel, redValue, grnSlider, grnLabel, grnValue,
        bluSlider, bluLabel, bluValue, crdValue, updateBtn, subLabel);

    borderTL.setLeft(sp);
    borderTL.setRight(imgZoom);
    borderLR.setLeft(borderTL);
    borderLR.getChildren().add(rect);
    borderLR.setRight(controls);
    borderTB.setTop(borderLR);
    borderTB.setBottom(lc);
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
    // Color codes markers and makes the RGB labels invisible on the legend
    // NOTE: Chart styling MUST go AFTER stage is shown in order to be applied to
    // the line chart
    //////////////////////////////////////////////
    styleChart(lc, rm, gm, bm);

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

    //////////////////////////////////////////////////
    // Saves the spectra plotted, either choose a single one to save or select to
    // save all of them to one .csv file
    //////////////////////////////////////////////////
    saveData.setOnAction(event -> {
      try {
        spectraName.add("All");
        ChoiceDialog<String> cd = new ChoiceDialog<>(spectraName.get(0), spectraName);
        cd.setTitle("Save Spectrum");
        cd.setHeaderText("Select Spectrum to Save: ");
        Optional<String> result = cd.showAndWait();
        int ndx = cd.getItems().indexOf(tidy(result));

        TextInputDialog input = new TextInputDialog();
        input.setTitle("File Name");
        input.setContentText("Set filename: ");
        String name = tidy(input.showAndWait());

        JCATLog.getInstance().getLogger().log(Level.INFO,
            "Saving plotted spectra: " + name + ".csv");

        BufferedWriter br = new BufferedWriter(new FileWriter(name + ".csv"));
        StringBuilder sb = new StringBuilder();
        if (tidy(result).equals("All")) {
          for (int i = 0; i < index.size(); i++) {
            sb.append(index.get(i));
            sb.append(",");
            sb.append(wavelength.get(i));
            sb.append(",");
            for (int j = 0; j < spectralData.size(); j++) {
              sb.append(spectralData.get(j).get(i));
              sb.append(",");
            }
            sb.append(" \n");
          }
        } else {
          for (int i = 0; i < index.size(); i++) {
            sb.append(index.get(i));
            sb.append(",");
            sb.append(wavelength.get(i));
            sb.append(",");
            sb.append(spectralData.get(ndx).get(i));
            sb.append(" \n");
          }
        }
        br.write(sb.toString());
        br.close();
      } catch (IOException e1) {
        JCATLog.getInstance().getLogger().log(Level.WARNING, "Error saving plotted spectra");
        e1.printStackTrace();
      }
    });

    saveChart.setOnAction(e -> {
      TextInputDialog input = new TextInputDialog();
      input.setTitle("File Name");
      input.setContentText("Set filename: ");
      String result = tidy(input.showAndWait());

      Image chartImg = borderTB.getBottom().snapshot(null, null);
      BufferedImage biSave = SwingFXUtils.fromFXImage(chartImg, null);
      File f = new File(result + ".png");
      JCATLog.getInstance().getLogger().log(Level.INFO, "Saving chart as: " + result + ".png");
      try {
        ImageIO.write(biSave, "PNG", f);
      } catch (IOException e1) {
        JCATLog.getInstance().getLogger().log(Level.WARNING, "Error saving chart");
        e1.printStackTrace();
      }
    });

    close.setOnAction(e -> {
      JCATLog.getInstance().getLogger().log(Level.FINE, "Closing image");
      stage2.close();
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

    //////////////////////////////////////////////////////////////////
    // Auto-scaling sets the axes to their original state where the entire spectrum
    // is in view
    // User can set lower, upper bound on axis to focus on area of intrigue
    /////////////////////////////////////////////////////////////////
    resetX.setOnAction(e -> {
      resetScale(borderTB, 0);
    });

    resetY.setOnAction(e -> {
      resetScale(borderTB, 1);
    });

    resetB.setOnAction(e -> {
      resetScale(borderTB, 2);
    });

    setX.setOnAction(e -> {
      LineChart<Number, Number> newLC = setX();
      borderTB.setBottom(newLC);
    });

    setY.setOnAction(e -> {
      LineChart<Number, Number> newLC = setY();
      borderTB.setBottom(newLC);
    });

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
          + String.format("%.4f", latitude) + ", Longitude: " + String.format("%.4f", longitude));
    });

    finalImg2.setOnMouseMoved(e -> {
      int newRow = (int) (e.getY());
      int newCol = (int) (e.getX());
      double longitude = CRISM.getLon(newRow, newCol);
      double latitude = CRISM.getLat(newRow, newCol);
      crdValue.setText("Data (row, col): (" + newRow + ", " + newCol + ")" + "Latitude: "
          + String.format("%.4f", latitude) + ", Longitude: " + String.format("%.4f", longitude));
    });

    mouseBtn.setOnAction(e -> {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Opening Mouse Button Instructions");
      Dialog<Boolean> d = new Dialog<>();
      VBox vbox = new VBox();
      Button apply = new Button("OK");
      vbox.getChildren().add(apply);
      d.setTitle("Mouse Button Instructions");
      d.setHeaderText("Mouse Button Instructions: ");
      d.setContentText(
          "There are two use cases: 1) adding the first spectrum OR 2) adding an additional spectrum \n\n"
              + "All three options are available for the first case. For 'Click', click anywhere on the image. \n"
              + "For 'Drag Avg', press down, drag and release the mouse. \nFor 'Region of Interest', left click to add points to the region. "
              + "To close the region, right click. This connects the last point to the first point \n\n"
              + "For the second case, only the click and drag options are available. "
              + "To use, follow the same instructions but CENTER click instead \n\n"
              + "To clear, left click anywhere, and a single spectra will appear.");
      d.setGraphic(vbox);
      d.show();
      apply.setOnAction(a -> {
        d.setResult(Boolean.TRUE);
        d.close();
      });
    });

    manualInput.setOnAction(e -> {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Entering Manual Input routine");
      VBox vbox = new VBox();
      ChoiceBox<String> cbSpec = new ChoiceBox<String>();
      cbSpec.getItems().addAll("New Spectrum", "Add Spectrum");

      TextArea ta = new TextArea();
      ta.setPromptText("Enter Point(s): 'row, col' format, one point per line");
      Button OK = new Button("OK");

      vbox.getChildren().addAll(cbSpec, ta, OK);
      Scene scene = new Scene(vbox);
      Stage stage = new Stage();
      stage.setScene(scene);
      stage.show();

      OK.setOnAction(a -> {
        int numPoints = ta.getText().split("\n").length;
        String choice = cbSpec.getValue().toString();
        Dialog<Boolean> wait = waitMessage();

        List<Double> intensity2 = new ArrayList<Double>();

        int newRow = 0;
        int newCol = 0;
        int oldRow = 0;
        int oldCol = 0;
        List<Integer> rows = new ArrayList<Integer>();
        List<Integer> cols = new ArrayList<Integer>();
        List<Double> poly = new ArrayList<Double>();
        List<Integer> roi = new ArrayList<Integer>();
        if (numPoints == 1) {
          newRow = Integer.parseInt(ta.getText().substring(0, ta.getText().indexOf(",")));
          newCol = Integer.parseInt(ta.getText().substring(ta.getText().indexOf(",") + 2));

        } else if (numPoints == 2) {
          for (String line : ta.getText().split("\\n")) {
            newRow = Integer.parseInt(line.substring(0, line.indexOf(",")));
            newCol = Integer.parseInt(line.substring(line.indexOf(",") + 2));
            rows.add(newRow);
            cols.add(newCol);
          }
          newRow = Collections.max(rows);
          newCol = Collections.max(cols);
          oldRow = Collections.min(rows);
          oldCol = Collections.min(cols);
        } else if (numPoints > 2) {
          for (String line : ta.getText().split("\\n")) {
            int row = Integer.parseInt(line.substring(0, line.indexOf(",")));
            int col = Integer.parseInt(line.substring(line.indexOf(",") + 2));
            rows.add(row);
            cols.add(col);
            poly.add((double) col);
            poly.add((double) row);
          }
          minX = Collections.min(cols);
          minY = Collections.min(rows);
          maxX = Collections.max(cols);
          maxY = Collections.max(rows);

          Polygon polygon = new Polygon();
          polygon.getPoints().addAll(poly);

          for (int i = minX; i < maxX; i++) {
            for (int j = minY; j < maxY; j++) {
              if (polygon.contains(i, j)) {
                roi.add(i);
                roi.add(j);
              }
            }
          }
        }

        double newLat = CRISM.getLat(newRow, newCol);
        double newLon = CRISM.getLon(newRow, newCol);
        double oldLat = CRISM.getLat(oldRow, oldCol);
        double oldLon = CRISM.getLon(oldRow, oldCol);

        if (choice.equals("Add Spectrum")) {
          NumberAxis x = (NumberAxis) flc.getXAxis();
          NumberAxis y = (NumberAxis) flc.getYAxis();
          ObservableList<Series<Number, Number>> series = flc.getData();
          flc = new LineChart<Number, Number>(x, y);
          flc.getData().addAll(series);
          flc.getData().remove(frm);
          flc.getData().remove(fbm);
          flc.getData().remove(fgm);

          LineChart<Number, Number> newLC = flc;
          XYChart.Series<Number, Number> rm2 = frm;
          XYChart.Series<Number, Number> gm2 = fgm;
          XYChart.Series<Number, Number> bm2 = fbm;
          rm2.setName("Red");
          gm2.setName("Green");
          bm2.setName("Blue");
          XYChart.Series<Number, Number> newLCData2 = new XYChart.Series<>();

          wait.show();
          if (numPoints == 1) {
            gc.strokeLine(newCol, newRow - 4, newCol, newRow + 4);
            gc.strokeLine(newCol - 4, newRow, newCol + 4, newRow);
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);

            if (singlePixel.isSelected()) {
              singlePixel(intensity2, newRow, newCol);
              newLCData2.setName("Lat: " + String.format("%.4f", newLat) + ", Lon: "
                  + String.format("%.4f", newLon));
            }
            if (ninePixel.isSelected()) {
              ninePixel(intensity2, newRow, newCol);
              newLCData2.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 1, newCol))
                  + "-" + String.format("%.4f", CRISM.getLat(newRow + 1, newCol)) + ", Lon: "
                  + String.format("%.4f", CRISM.getLon(newRow, newCol - 1)) + "-"
                  + String.format("%.4f", CRISM.getLon(newRow, newCol + 1)));
            }
            if (twentyFivePixel.isSelected()) {
              twentyFivePixel(intensity2, newRow, newCol);
              newLCData2.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 2, newCol))
                  + "-" + String.format("%.4f", CRISM.getLat(newRow + 2, newCol)) + ", Lon: "
                  + String.format("%.4f", CRISM.getLon(newRow, newCol - 2)) + "-"
                  + String.format("%.4f", CRISM.getLon(newRow, newCol + 2)));
            }
          } else if (numPoints == 2) {
            gc.strokeRect(oldCol, oldRow, Math.abs(oldCol - newCol), Math.abs(oldRow - newRow));
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);

            dragAvg(intensity2, oldRow, oldCol, newRow, newCol);
            newLCData2.setName("Lat: " + String.format("%.4f", oldLat) + "-"
                + String.format("%.4f", newLat) + ", Lon: " + String.format("%.4f", oldLon) + "-"
                + String.format("%.4f", oldLon));
          } else if (numPoints > 2) {
            for (int i = 0; i < rows.size() - 1; i++) {
              int col = cols.get(i);
              int row = rows.get(i);
              gc.strokeLine(col, row, cols.get(i + 1), rows.get(i + 1));
              gc.strokeLine(col, row - 4, col, row + 4);
              gc.strokeLine(col - 4, row, col + 4, row);
            }
            gc.strokeLine(cols.get(cols.size() - 1), rows.get(rows.size() - 1), cols.get(0),
                rows.get(0));
            gc.strokeLine(cols.get(cols.size() - 1), rows.get(rows.size() - 1) - 4,
                cols.get(cols.size() - 1), rows.get(rows.size() - 1) + 4);
            gc.strokeLine(cols.get(cols.size() - 1) - 4, rows.get(rows.size() - 1),
                cols.get(cols.size() - 1) + 4, rows.get(rows.size() - 1));
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);

            roiAvg(intensity2, roi);
            newLCData2.setName("Center of region: ("
                + String.format("%.4f", (CRISM.getLat(maxY, maxX) + CRISM.getLat(minY, minX)) / 2)
                + ", "
                + String.format("%.4f", (CRISM.getLon(maxY, maxX) + CRISM.getLon(minY, minX)) / 2)
                + ")");
          }
          if (P.isSelected()) {
            pCorr(intensity2);
            newLCData2.setName("PHT " + newLCData2.getName());
          }
          if (!fullVSName.equals("None")) {
            aCorr(newCol, intensity2, correctADRfiles);
            String vsid = fullVSName.substring(15, 20);
            newLCData2.setName("ATM (" + vsid + ") " + newLCData2.getName());
          }
          for (int i = 0; i < index.size(); i++) {
            if (intensity2.get(i) < 1) {
              newLCData2.getData()
                  .add(new XYChart.Data<Number, Number>(wavelength.get(i), intensity2.get(i)));
            }
          }
          flc.getData().add(newLCData2);

          spectralData.add(intensity2);
          spectraName.add(newLCData2.getName());

          flc.getData().add(frm);
          flc.getData().add(fbm);
          flc.getData().add(fgm);
          newLC.setCreateSymbols(false);
          newLC.setAnimated(false);
          styleChart(newLC, rm2, gm2, bm2);

          borderTB.setBottom(newLC);
        } else {
          NumberAxis x = (NumberAxis) flc.getXAxis();
          NumberAxis y = (NumberAxis) flc.getYAxis();
          LineChart<Number, Number> newLC = null;
          flc = new LineChart<Number, Number>(x, y);
          newLC = flc;
          XYChart.Series<Number, Number> newLCData = new XYChart.Series<Number, Number>();
          XYChart.Series<Number, Number> rm2 = new XYChart.Series<Number, Number>();
          XYChart.Series<Number, Number> gm2 = new XYChart.Series<Number, Number>();
          XYChart.Series<Number, Number> bm2 = new XYChart.Series<Number, Number>();
          rm2.setName("Red");
          gm2.setName("Green");
          bm2.setName("Blue");

          intensity.clear();

          gc.clearRect(0, 0, width, height);
          img = canvas.snapshot(null, null);
          iv3.setImage(img);
          iv4.setImage(img);

          wait.show();
          if (numPoints == 1) {
            gc.strokeLine(newCol, newRow - 4, newCol, newRow + 4);
            gc.strokeLine(newCol - 4, newRow, newCol + 4, newRow);
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);

            if (singlePixel.isSelected()) {
              singlePixel(intensity, newRow, newCol);
              newLCData.setName("Lat: " + String.format("%.4f", newLat) + ", Lon: "
                  + String.format("%.4f", newLon));
            }
            if (ninePixel.isSelected()) {
              ninePixel(intensity, newRow, newCol);
              newLCData.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 1, newCol))
                  + "-" + String.format("%.4f", CRISM.getLat(newRow + 1, newCol)) + ", Lon: "
                  + String.format("%.4f", CRISM.getLon(newRow, newCol - 1)) + "-"
                  + String.format("%.4f", CRISM.getLon(newRow, newCol + 1)));
            }
            if (twentyFivePixel.isSelected()) {
              twentyFivePixel(intensity, newRow, newCol);
              newLCData.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 2, newCol))
                  + "-" + String.format("%.4f", CRISM.getLat(newRow + 2, newCol)) + ", Lon: "
                  + String.format("%.4f", CRISM.getLon(newRow, newCol - 2)) + "-"
                  + String.format("%.4f", CRISM.getLon(newRow, newCol + 2)));
            }
          } else if (numPoints == 2) {
            gc.strokeRect(oldCol, oldRow, Math.abs(oldCol - newCol), Math.abs(oldRow - newRow));
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);

            dragAvg(intensity, oldRow, oldCol, newRow, newCol);
            newLCData.setName("Lat: " + String.format("%.4f", oldLat) + "-"
                + String.format("%.4f", newLat) + ", Lon: " + String.format("%.4f", oldLon) + "-"
                + String.format("%.4f", oldLon));
          } else if (numPoints > 2) {
            for (int i = 0; i < rows.size() - 1; i++) {
              int col = cols.get(i);
              int row = rows.get(i);
              gc.strokeLine(col, row, cols.get(i + 1), rows.get(i + 1));
              gc.strokeLine(col, row - 4, col, row + 4);
              gc.strokeLine(col - 4, row, col + 4, row);
            }
            gc.strokeLine(cols.get(cols.size() - 1), rows.get(rows.size() - 1), cols.get(0),
                rows.get(0));
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);

            roiAvg(intensity, roi);
            newLCData.setName("Center of region: ("
                + String.format("%.4f", (CRISM.getLat(maxY, maxX) + CRISM.getLat(minY, minX)) / 2)
                + ", "
                + String.format("%.4f", (CRISM.getLon(maxY, maxX) + CRISM.getLon(minY, minX)) / 2)
                + ")");
          }
          if (P.isSelected()) {
            pCorr(intensity);
            newLCData.setName("PHT " + newLCData.getName());
          }
          if (!fullVSName.equals("None")) {
            aCorr(newCol, intensity, correctADRfiles);
            String vsid = fullVSName.substring(15, 20);
            newLCData.setName("ATM (" + vsid + ") " + newLCData.getName());
          }
          inputData(intensity, newLCData, newLC);
          inputMarkers(newLC, rm2, gm2, bm2);
          newLC.setCreateSymbols(false);
          newLC.setAnimated(false);
          borderTB.setBottom(newLC);

          styleChart(newLC, rm2, gm2, bm2);

          spectralData.clear();
          spectraName.clear();

          spectralData.add(intensity);
          spectraName.add(newLCData.getName());
          flc = newLC;
          frm = rm2;
          fgm = gm2;
          fbm = bm2;
        }
        wait.setResult(Boolean.TRUE);
        wait.close();
        stage.close();
      });
    });

    ////////////////////////////////////////////////////////
    // Creates three choice boxes, puts the stored names of series to choose what to
    // operate on
    // Once chosen, resulting graph is shown with autoscaled axes
    // User has option to change axis ranges
    // save features available
    ////////////////////////////////////////////////////////
    specOps.setOnAction(e -> {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Opening spectral operations window");
      Dialog<Boolean> d = new Dialog<>();
      VBox vbox = new VBox();
      Button apply = new Button("OK");
      Button cancel = new Button("Cancel");
      ChoiceBox<String> cb1 = new ChoiceBox<String>();
      ChoiceBox<String> cbOp = new ChoiceBox<String>();
      ChoiceBox<String> cb2 = new ChoiceBox<String>();
      cb1.getItems().addAll(spectraName);
      cbOp.getItems().addAll("+", "-", "ratio by");
      cb2.getItems().addAll(spectraName);
      vbox.getChildren().addAll(cb1, cbOp, cb2, apply, cancel);
      d.setTitle("Spectra");
      d.setHeaderText("Choose spectra and operation: ");
      d.setGraphic(vbox);
      d.show();
      apply.setOnAction(event -> {
        int spec1 = spectraName.indexOf(cb1.getValue());
        int op = cbOp.getItems().indexOf(cbOp.getValue());
        int spec2 = spectraName.indexOf(cb2.getValue());

        ArrayList<Double> output = new ArrayList<Double>();
        NumberAxis x = new NumberAxis();
        NumberAxis y = new NumberAxis();
        if (op == 0) {
          for (int i = 0; i < spectralData.get(spec1).size(); i++) {
            double a = (spectralData.get(spec1).get(i) + spectralData.get(spec2).get(i));
            if (spectralData.get(spec1).get(i) < 1 && spectralData.get(spec2).get(i) < 1) {
              output.add(a);
            } else {
              output.add((double) FillValue);
            }
          }
        }
        if (op == 1) {
          for (int i = 0; i < spectralData.get(spec1).size(); i++) {
            double a = (spectralData.get(spec1).get(i) - spectralData.get(spec2).get(i));
            if (spectralData.get(spec1).get(i) < 1 && spectralData.get(spec2).get(i) < 1) {
              output.add(a);
            } else {
              output.add((double) FillValue);
            }
          }
        }
        if (op == 2) {
          for (int i = 0; i < spectralData.get(spec1).size(); i++) {
            double a = (spectralData.get(spec1).get(i) / spectralData.get(spec2).get(i));
            if (spectralData.get(spec1).get(i) < 1 && spectralData.get(spec2).get(i) < 1) {
              output.add(a);
            } else {
              output.add((double) FillValue);
            }
          }
        }
        XYChart.Series<Number, Number> opLCData = new XYChart.Series<>();
        for (int i = 0; i < index.size(); i++) {
          if (output.get(i) < FillValue) {
            opLCData.getData()
                .add(new XYChart.Data<Number, Number>(wavelength.get(i), output.get(i)));
          }
        }
        x.setAutoRanging(true);
        x.setForceZeroInRange(false);
        y.setAutoRanging(true);
        y.setForceZeroInRange(false);
        LineChart<Number, Number> opLC = new LineChart<Number, Number>(x, y);
        x.setLabel("Wavelength(nm)");
        opLC.getData().add(opLCData);
        opLC.setCreateSymbols(false);
        opLC.setLegendVisible(false);
        fslc = opLC;
        VBox opvbox = new VBox();

        Scene opScene = new Scene(opvbox, 800, 400);
        Stage opStage = new Stage();
        opStage.setScene(opScene);
        opStage.setTitle(cb1.getValue() + " " + cbOp.getValue() + " " + cb2.getValue());
        opStage.show();

        d.setResult(Boolean.TRUE);
        d.close();

        MenuBar opmBar = new MenuBar();

        Menu file = new Menu("File");
        Menu plot = new Menu("Plot");

        MenuItem saveOpsData = new MenuItem("Save Spectral Data");
        MenuItem saveOpsChart = new MenuItem("Save Chart");
        file.getItems().addAll(saveOpsData, saveOpsChart);

        MenuItem autoscaleX = new MenuItem("Autoscale X");
        MenuItem autoscaleY = new MenuItem("Autoscale Y");
        MenuItem autoscaleB = new MenuItem("Autoscale Both");
        MenuItem setXScale = new MenuItem("Set X Scale");
        MenuItem setYScale = new MenuItem("Set Y Scale");
        plot.getItems().addAll(autoscaleX, autoscaleY, autoscaleB, setXScale, setYScale);

        opmBar.getMenus().addAll(file, plot);
        opvbox.getChildren().addAll(opmBar, opLC);

        saveOpsData.setOnAction(a -> {
          try {
            JCATLog.getInstance().getLogger().log(Level.INFO, "Saving spectral operations data");
            TextInputDialog input = new TextInputDialog();
            input.setTitle("File Name");
            input.setContentText("Set filename: ");
            String result = tidy(input.showAndWait());

            BufferedWriter br = new BufferedWriter(new FileWriter(result + ".csv"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < index.size(); i++) {
              sb.append(index.get(i));
              sb.append(",");
              sb.append(wavelength.get(i));
              sb.append(",");
              sb.append(spectralData.get(spec1).get(i));
              sb.append(",");
              sb.append(spectralData.get(spec2).get(i));
              sb.append(",");
              sb.append(output.get(i));
              sb.append(" \n");
            }
            br.write(sb.toString());
            br.close();
          } catch (IOException e1) {
            JCATLog.getInstance().getLogger().log(Level.WARNING,
                "Something went wrong saving spectral operations data");
            JCATMessageWindow.show(e1);
            e1.printStackTrace();
          }
        });
        saveOpsChart.setOnAction(a -> {
          TextInputDialog input = new TextInputDialog();
          input.setTitle("File Name");
          input.setContentText("Set filename: ");
          String result = tidy(input.showAndWait());

          Image chartImg = opLC.snapshot(null, null);
          BufferedImage biSave = SwingFXUtils.fromFXImage(chartImg, null);
          File f = new File(result + ".png");
          try {
            ImageIO.write(biSave, "PNG", f);
          } catch (IOException e1) {
            JCATLog.getInstance().getLogger().log(Level.WARNING,
                "Something went wrong saving spectral operations chart");
            JCATMessageWindow.show(e1);
            e1.printStackTrace();
          }
        });

        autoscaleX.setOnAction(a -> {
          LineChart<Number, Number> newopLC = autoScaleOP(0);
          opvbox.getChildren().clear();
          opvbox.getChildren().addAll(opmBar, newopLC);
        });
        autoscaleY.setOnAction(a -> {
          LineChart<Number, Number> newopLC = autoScaleOP(1);
          opvbox.getChildren().clear();
          opvbox.getChildren().addAll(opmBar, newopLC);
        });
        autoscaleB.setOnAction(a -> {
          LineChart<Number, Number> newopLC = autoScaleOP(2);
          opvbox.getChildren().clear();
          opvbox.getChildren().addAll(opmBar, newopLC);
        });
        setXScale.setOnAction(a -> {
          LineChart<Number, Number> newopLC = setXScale();
          opvbox.getChildren().clear();
          opvbox.getChildren().addAll(opmBar, newopLC);
        });
        setYScale.setOnAction(a -> {
          LineChart<Number, Number> newopLC = setYScale();
          opvbox.getChildren().clear();
          opvbox.getChildren().addAll(opmBar, newopLC);
        });

      });
      cancel.setOnAction(e1 -> {
        JCATLog.getInstance().getLogger().log(Level.INFO, "Closing spectral operations window");
        d.setResult(Boolean.TRUE);
        d.close();
      });
    });

    //////////////////////////////////////////////////
    // Runs when a new volcano scan option is selected, instantiated as "None"
    // Empirically optimize sets name to "Optimize" as keyword for VS application
    // else, ID gives fullVSName which is used during application to spectrum
    //////////////////////////////////////////////////
    VSTG.selectedToggleProperty().addListener((obs, old_toggle, new_toggle) -> {
      if (emp.isSelected()) {
        fullVSName = "Optimize";
      } else if (defVS.isSelected()) {
        fullVSName = adrVS("061C4");
      } else if (noVS.isSelected()) {
        fullVSName = "None";
      } else {
        int ndx = VSTG.getToggles().indexOf(new_toggle);
        if (ndx >= 0)
          fullVSName = adrVS(adrIDList.get(ndx));
      }
    });

    /////////////////////////////////////////////////////////////////////////////////////
    // Routine when the user clicks on the image:
    // Gets the location of the scroll bars to reset the image once done
    // Shows a wait message while the routine is running (it can take some time)
    // First checks to see if the multiple spectrum is selected, if yes and sensed a
    // center click,then either add the click or drag avg to line chart
    // If not, checks if region of interest, if yes then user can left click to add
    // points until a right click closes the shape
    // If not, checks if drag avg, if yes detects where pressed and released to get
    // size of rectangle
    // Otherwise, add a single click to the chart
    // Puts new chart into the placeholder chart and adds user-reference shape
    // Stores the new intensity in list for possible spectral operations
    // NOTE: New chart must be made because JavaFX bug doesn't allow removal and
    // input of new data within a handler
    /////////////////////////////////////////////////////////////////////////////////////
    finalImg.onMousePressedProperty().set(a -> {
      finalImg.onMouseReleasedProperty().set(b -> {
        int newRow = (int) (Math.max(a.getY(), b.getY()) * (1 / scaleFactor));
        int newCol = (int) (Math.max(a.getX(), b.getX()) * (1 / scaleFactor));
        int oldRow = (int) (Math.min(a.getY(), b.getY()) * (1 / scaleFactor));
        int oldCol = (int) (Math.min(a.getX(), b.getX()) * (1 / scaleFactor));
        double newLat = CRISM.getLat(newRow, newCol);
        double newLon = CRISM.getLon(newRow, newCol);
        double oldLat = CRISM.getLat(oldRow, oldCol);
        double oldLon = CRISM.getLon(oldRow, oldCol);

        int hval = (int) sp.getHvalue();
        int vval = (int) sp.getVvalue();

        Dialog<Boolean> wait = waitMessage();

        List<Double> intensity2 = new ArrayList<Double>();
        if (multSpec.isSelected() && a.getButton() == MouseButton.MIDDLE) {
          NumberAxis x = (NumberAxis) flc.getXAxis();
          NumberAxis y = (NumberAxis) flc.getYAxis();
          ObservableList<Series<Number, Number>> series = flc.getData();
          flc = new LineChart<Number, Number>(x, y);
          flc.getData().addAll(series);
          flc.getData().remove(frm);
          flc.getData().remove(fbm);
          flc.getData().remove(fgm);

          LineChart<Number, Number> newLC = flc;
          XYChart.Series<Number, Number> rm2 = frm;
          XYChart.Series<Number, Number> gm2 = fgm;
          XYChart.Series<Number, Number> bm2 = fbm;
          rm2.setName("Red");
          gm2.setName("Green");
          bm2.setName("Blue");
          XYChart.Series<Number, Number> newLCData2 = new XYChart.Series<>();

          wait.show();
          if (drag.isSelected()) {
            gc.strokeRect(oldCol, oldRow, Math.abs(oldCol - newCol), Math.abs(oldRow - newRow));
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);

            dragAvg(intensity2, oldRow, oldCol, newRow, newCol);
            newLCData2.setName("Lat: " + String.format("%.4f", oldLat) + "-"
                + String.format("%.4f", newLat) + ", Lon: " + String.format("%.4f", oldLon) + "-"
                + String.format("%.4f", oldLon));
            if (P.isSelected()) {
              intensity2 = pCorr(intensity2);
              newLCData2.setName("PHT " + newLCData2.getName());
            }
            if (!fullVSName.equals("None")) {
              intensity2 = aCorr(newCol, intensity2, correctADRfiles);
              String vsid = fullVSName.substring(15, 20);
              newLCData2.setName("ATM (" + vsid + ") " + newLCData2.getName());
            }
            for (int i = 0; i < index.size(); i++) {
              if (intensity2.get(i) < 1) {
                newLCData2.getData()
                    .add(new XYChart.Data<Number, Number>(wavelength.get(i), intensity2.get(i)));
              }
            }
            flc.getData().add(newLCData2);
          } else {
            gc.strokeLine(newCol, newRow - 4, newCol, newRow + 4);
            gc.strokeLine(newCol - 4, newRow, newCol + 4, newRow);
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);

            if (singlePixel.isSelected()) {
              singlePixel(intensity2, newRow, newCol);
              newLCData2.setName("Lat: " + String.format("%.4f", newLat) + ", Lon: "
                  + String.format("%.4f", newLon));
            }
            if (ninePixel.isSelected()) {
              ninePixel(intensity2, newRow, newCol);
              newLCData2.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 1, newCol))
                  + "-" + String.format("%.4f", CRISM.getLat(newRow + 1, newCol)) + ", Lon: "
                  + String.format("%.4f", CRISM.getLon(newRow, newCol - 1)) + "-"
                  + String.format("%.4f", CRISM.getLon(newRow, newCol + 1)));
            }
            if (twentyFivePixel.isSelected()) {
              twentyFivePixel(intensity2, newRow, newCol);
              newLCData2.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 2, newCol))
                  + "-" + String.format("%.4f", CRISM.getLat(newRow + 2, newCol)) + ", Lon: "
                  + String.format("%.4f", CRISM.getLon(newRow, newCol - 2)) + "-"
                  + String.format("%.4f", CRISM.getLon(newRow, newCol + 2)));
            }
            if (P.isSelected()) {
              intensity2 = pCorr(intensity2);
              newLCData2.setName("PHT " + newLCData2.getName());
            }
            if (!fullVSName.equals("None")) {
              intensity2 = aCorr(newCol, intensity2, correctADRfiles);
              String vsid = fullVSName.substring(15, 20);
              newLCData2.setName("ATM (" + vsid + ") " + newLCData2.getName());
            }
            for (int i = 0; i < index.size(); i++) {
              if (intensity2.get(i) < 1) {
                newLCData2.getData()
                    .add(new XYChart.Data<Number, Number>(wavelength.get(i), intensity2.get(i)));
              }
            }
            flc.getData().add(newLCData2);
          }
          spectralData.add(intensity2);
          spectraName.add(newLCData2.getName());

          flc.getData().add(frm);
          flc.getData().add(fbm);
          flc.getData().add(fgm);
          newLC.setCreateSymbols(false);
          newLC.setAnimated(false);
          styleChart(newLC, rm2, gm2, bm2);

          borderTB.setBottom(newLC);
          wait.setResult(Boolean.TRUE);
          wait.close();
        } else {
          NumberAxis x;
          NumberAxis y;
          LineChart<Number, Number> newLC = null;
          XYChart.Series<Number, Number> newLCData = new XYChart.Series<Number, Number>();
          XYChart.Series<Number, Number> rm2 = new XYChart.Series<Number, Number>();
          XYChart.Series<Number, Number> gm2 = new XYChart.Series<Number, Number>();
          XYChart.Series<Number, Number> bm2 = new XYChart.Series<Number, Number>();
          rm2.setName("Red");
          gm2.setName("Green");
          bm2.setName("Blue");

          intensity.clear();
          if (region.isSelected()) {
            if (a.getButton() == MouseButton.SECONDARY) {
              x = (NumberAxis) flc.getXAxis();
              y = (NumberAxis) flc.getYAxis();
              flc = new LineChart<Number, Number>(x, y);
              newLC = flc;

              wait.show();

              gc.strokeLine(prevCol, prevRow, firstCol, firstRow);
              Polygon poly = new Polygon();
              poly.getPoints().addAll(polyCoordinates);
              t = 2;
              List<Integer> roi = new ArrayList<Integer>();
              for (int i = minX; i <= maxX; i++) {
                for (int j = minY; j <= maxY; j++) {
                  if (poly.contains(i, j)) {
                    roi.add(i);
                    roi.add(j);
                  }
                }
              }
              roiAvg(intensity, roi);

              newLCData.setName("Center of region: ("
                  + String.format("%.4f", (CRISM.getLat(maxY, maxX) + CRISM.getLat(minY, minX)) / 2)
                  + ", "
                  + String.format("%.4f", (CRISM.getLon(maxY, maxX) + CRISM.getLon(minY, minX)) / 2)
                  + ")");
              if (P.isSelected()) {
                intensity = pCorr(intensity);
                newLCData.setName("PHT " + newLCData.getName());
              }
              if (!fullVSName.equals("None")) {
                intensity = aCorr(newCol, intensity, correctADRfiles);
                String vsid = fullVSName.substring(15, 20);
                newLCData.setName("ATM (" + vsid + ") " + newLCData.getName());
              }
              inputData(intensity, newLCData, newLC);
              inputMarkers(newLC, rm2, gm2, bm2);
              newLC.setCreateSymbols(false);
              newLC.setAnimated(false);
              borderTB.setBottom(newLC);

              styleChart(newLC, rm2, gm2, bm2);

              spectralData.add(intensity);
              spectraName.add(newLCData.getName());
              flc = newLC;
              frm = rm2;
              fgm = gm2;
              fbm = bm2;
            } else {
              if (t > 0) {
                polyCoordinates.clear();
                firstRow = newRow;
                firstCol = newCol;
                minX = newCol;
                minY = newRow;
                if (t > 1) {
                  gc.clearRect(0, 0, width, height);
                  img = canvas.snapshot(null, null);
                  iv3.setImage(img);
                  iv4.setImage(img);
                }
              } else if (prevCol > 0 && prevRow > 0) {
                gc.strokeLine(newCol, newRow, prevCol, prevRow);
              }
              maxY = (newRow > maxY) ? newRow : maxY;
              maxX = (newCol > maxX) ? newCol : maxX;
              minY = (newRow < minY) ? newRow : minY;
              minX = (newCol < minX) ? newCol : minX;
              t = -1;
              gc.strokeLine(newCol, newRow - 4, newCol, newRow + 4);
              gc.strokeLine(newCol - 4, newRow, newCol + 4, newRow);
              prevRow = newRow;
              prevCol = newCol;
              polyCoordinates.add((double) newCol);
              polyCoordinates.add((double) newRow);
            }
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);
          } else {
            x = (NumberAxis) flc.getXAxis();
            y = (NumberAxis) flc.getYAxis();
            flc = new LineChart<Number, Number>(x, y);
            newLC = flc;

            wait.show();

            t = 2;
            gc.clearRect(0, 0, width, height);
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);
            if (drag.isSelected()) {
              gc.strokeRect(oldCol, oldRow, Math.abs(oldCol - newCol), Math.abs(oldRow - newRow));
              img = canvas.snapshot(null, null);
              iv3.setImage(img);
              iv4.setImage(img);

              dragAvg(intensity, oldRow, oldCol, newRow, newCol);
              newLCData.setName("Lat: " + String.format("%.4f", oldLat) + "-"
                  + String.format("%.4f", newLat) + ", Lon: " + String.format("%.4f", oldLon) + "-"
                  + String.format("%.4f", newLon));
              if (P.isSelected()) {
                intensity = pCorr(intensity);
                newLCData.setName("PHT " + newLCData.getName());
              }
              if (!fullVSName.equals("None")) {
                intensity = aCorr(newCol, intensity, correctADRfiles);
                String vsid = fullVSName.substring(15, 20);
                newLCData.setName("ATM (" + vsid + ") " + newLCData.getName());
              }
              inputData(intensity, newLCData, newLC);
              inputMarkers(newLC, rm2, gm2, bm2);
              newLC.setCreateSymbols(false);
              newLC.setAnimated(false);
              borderTB.setBottom(newLC);

              styleChart(newLC, rm2, gm2, bm2);
            } else {
              gc.strokeLine(newCol, newRow - 4, newCol, newRow + 4);
              gc.strokeLine(newCol - 4, newRow, newCol + 4, newRow);
              img = canvas.snapshot(null, null);
              iv3.setImage(img);
              iv4.setImage(img);
              if (singlePixel.isSelected()) {
                singlePixel(intensity, newRow, newCol);
                newLCData.setName("Lat: " + String.format("%.4f", newLat) + ", Lon: "
                    + String.format("%.4f", newLon));
              }
              if (ninePixel.isSelected()) {
                ninePixel(intensity, newRow, newCol);
                newLCData.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 1, newCol))
                    + "-" + String.format("%.4f", CRISM.getLat(newRow + 1, newCol)) + ", Lon: "
                    + String.format("%.4f", CRISM.getLon(newRow, newCol - 1)) + "-"
                    + String.format("%.4f", CRISM.getLon(newRow, newCol + 1)));
              }
              if (twentyFivePixel.isSelected()) {
                twentyFivePixel(intensity, newRow, newCol);
                newLCData.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 2, newCol))
                    + "-" + String.format("%.4f", CRISM.getLat(newRow + 2, newCol)) + ", Lon: "
                    + String.format("%.4f", CRISM.getLon(newRow, newCol - 2)) + "-"
                    + String.format("%.4f", CRISM.getLon(newRow, newCol + 2)));
              }
              if (P.isSelected()) {
                intensity = pCorr(intensity);
                newLCData.setName("PHT " + newLCData.getName());
              }
              if (!fullVSName.equals("None")) {
                intensity = aCorr(newCol, intensity, correctADRfiles);
                String vsid = fullVSName.substring(15, 20);
                newLCData.setName("ATM (" + vsid + ") " + newLCData.getName());
              }
              inputData(intensity, newLCData, newLC);
              inputMarkers(newLC, rm2, gm2, bm2);
              newLC.setCreateSymbols(false);
              newLC.setAnimated(false);
              borderTB.setBottom(newLC);

              styleChart(newLC, rm2, gm2, bm2);
            }
            flc = newLC;
            frm = rm2;
            fgm = gm2;
            fbm = bm2;
          }
          spectralData.clear();
          spectraName.clear();
          spectralData.add(intensity);
          spectraName.add(newLCData.getName());

          sp.setHvalue(hval);
          sp.setVvalue(vval);
          wait.setResult(Boolean.TRUE);
          wait.close();
        }
      });
    });

    finalImg2.onMousePressedProperty().set(a -> {
      finalImg2.onMouseReleasedProperty().set(b -> {
        int newRow = (int) (Math.max(a.getY(), b.getY()));
        int newCol = (int) (Math.max(a.getX(), b.getX()));
        int oldRow = (int) (Math.min(a.getY(), b.getY()));
        int oldCol = (int) (Math.min(a.getX(), b.getX()));
        double newLat = CRISM.getLat(newRow, newCol);
        double newLon = CRISM.getLon(newRow, newCol);
        double oldLat = CRISM.getLat(oldRow, oldCol);
        double oldLon = CRISM.getLon(oldRow, oldCol);

        int hval = (int) sp.getHvalue();
        int vval = (int) sp.getVvalue();

        Dialog<Boolean> wait = waitMessage();

        List<Double> intensity2 = new ArrayList<Double>();
        if (multSpec.isSelected() && a.getButton() == MouseButton.MIDDLE) {
          NumberAxis x = (NumberAxis) flc.getXAxis();
          NumberAxis y = (NumberAxis) flc.getYAxis();
          ObservableList<Series<Number, Number>> series = flc.getData();
          flc = new LineChart<Number, Number>(x, y);
          flc.getData().addAll(series);
          flc.getData().remove(frm);
          flc.getData().remove(fbm);
          flc.getData().remove(fgm);

          LineChart<Number, Number> newLC = flc;
          XYChart.Series<Number, Number> rm2 = frm;
          XYChart.Series<Number, Number> gm2 = fgm;
          XYChart.Series<Number, Number> bm2 = fbm;
          rm2.setName("Red");
          gm2.setName("Green");
          bm2.setName("Blue");
          XYChart.Series<Number, Number> newLCData2 = new XYChart.Series<>();

          wait.show();
          if (drag.isSelected()) {
            gc.strokeRect(oldCol, oldRow, Math.abs(oldCol - newCol), Math.abs(oldRow - newRow));
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);

            dragAvg(intensity2, oldRow, oldCol, newRow, newCol);
            newLCData2.setName("Lat: " + String.format("%.4f", oldLat) + "-"
                + String.format("%.4f", newLat) + ", Lon: " + String.format("%.4f", oldLon) + "-"
                + String.format("%.4f", oldLon));
            if (P.isSelected()) {
              intensity2 = pCorr(intensity2);
              newLCData2.setName("PHT " + newLCData2.getName());
            }
            if (!fullVSName.equals("None")) {
              intensity2 = aCorr(newCol, intensity2, correctADRfiles);
              String vsid = fullVSName.substring(15, 20);
              newLCData2.setName("ATM (" + vsid + ") " + newLCData2.getName());
            }
            for (int i = 0; i < index.size(); i++) {
              if (intensity2.get(i) < 1) {
                newLCData2.getData()
                    .add(new XYChart.Data<Number, Number>(wavelength.get(i), intensity2.get(i)));
              }
            }
            flc.getData().add(newLCData2);
          } else {
            gc.strokeLine(newCol, newRow - 4, newCol, newRow + 4);
            gc.strokeLine(newCol - 4, newRow, newCol + 4, newRow);
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);
            if (singlePixel.isSelected()) {
              singlePixel(intensity2, newRow, newCol);
              newLCData2.setName("Lat: " + String.format("%.4f", newLat) + ", Lon: "
                  + String.format("%.4f", newLon));
            }
            if (ninePixel.isSelected()) {
              ninePixel(intensity2, newRow, newCol);
              newLCData2.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 1, newCol))
                  + "-" + String.format("%.4f", CRISM.getLat(newRow + 1, newCol)) + ", Lon: "
                  + String.format("%.4f", CRISM.getLon(newRow, newCol - 1)) + "-"
                  + String.format("%.4f", CRISM.getLon(newRow, newCol + 1)));
            }
            if (twentyFivePixel.isSelected()) {
              twentyFivePixel(intensity2, newRow, newCol);
              newLCData2.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 2, newCol))
                  + "-" + String.format("%.4f", CRISM.getLat(newRow + 2, newCol)) + ", Lon: "
                  + String.format("%.4f", CRISM.getLon(newRow, newCol - 2)) + "-"
                  + String.format("%.4f", CRISM.getLon(newRow, newCol + 2)));
            }
            if (P.isSelected()) {
              intensity2 = pCorr(intensity2);
              newLCData2.setName("PHT " + newLCData2.getName());
            }
            if (!fullVSName.equals("None")) {
              intensity2 = aCorr(newCol, intensity2, correctADRfiles);
              String vsid = fullVSName.substring(15, 20);
              newLCData2.setName("ATM (" + vsid + ") " + newLCData2.getName());
            }
            for (int i = 0; i < index.size(); i++) {
              if (intensity2.get(i) < 1) {
                newLCData2.getData()
                    .add(new XYChart.Data<Number, Number>(wavelength.get(i), intensity2.get(i)));
              }
            }
            flc.getData().add(newLCData2);
          }
          spectralData.add(intensity2);
          spectraName.add(newLCData2.getName());

          flc.getData().add(frm);
          flc.getData().add(fbm);
          flc.getData().add(fgm);
          newLC.setCreateSymbols(false);
          newLC.setAnimated(false);
          styleChart(newLC, rm2, gm2, bm2);

          borderTB.setBottom(newLC);
          wait.setResult(Boolean.TRUE);
          wait.close();
        } else {
          NumberAxis x;
          NumberAxis y;
          LineChart<Number, Number> newLC = null;
          XYChart.Series<Number, Number> newLCData = new XYChart.Series<Number, Number>();
          XYChart.Series<Number, Number> rm2 = new XYChart.Series<Number, Number>();
          XYChart.Series<Number, Number> gm2 = new XYChart.Series<Number, Number>();
          XYChart.Series<Number, Number> bm2 = new XYChart.Series<Number, Number>();
          rm2.setName("Red");
          gm2.setName("Green");
          bm2.setName("Blue");

          intensity.clear();
          if (region.isSelected()) {
            if (a.getButton() == MouseButton.SECONDARY) {
              x = (NumberAxis) flc.getXAxis();
              y = (NumberAxis) flc.getYAxis();
              flc = new LineChart<Number, Number>(x, y);
              newLC = flc;

              wait.show();

              gc.strokeLine(prevCol, prevRow, firstCol, firstRow);
              Polygon poly = new Polygon();
              poly.getPoints().addAll(polyCoordinates);
              t = 2;
              List<Integer> roi = new ArrayList<Integer>();
              for (int i = minX; i <= maxX; i++) {
                for (int j = minY; j <= maxY; j++) {
                  if (poly.contains(i, j)) {
                    roi.add(i);
                    roi.add(j);
                  }
                }
              }
              roiAvg(intensity, roi);

              newLCData.setName("Center of region: ("
                  + String.format("%.4f", (CRISM.getLat(maxY, maxX) + CRISM.getLat(minY, minX)) / 2)
                  + ", "
                  + String.format("%.4f", (CRISM.getLon(maxY, maxX) + CRISM.getLon(minY, minX)) / 2)
                  + ")");
              if (P.isSelected()) {
                intensity = pCorr(intensity);
                newLCData.setName("PHT " + newLCData.getName());
              }
              if (!fullVSName.equals("None")) {
                intensity = aCorr(newCol, intensity, correctADRfiles);
                String vsid = fullVSName.substring(15, 20);
                newLCData.setName("ATM (" + vsid + ") " + newLCData.getName());
              }
              inputData(intensity, newLCData, newLC);
              inputMarkers(newLC, rm2, gm2, bm2);
              newLC.setCreateSymbols(false);
              newLC.setAnimated(false);
              borderTB.setBottom(newLC);

              styleChart(newLC, rm2, gm2, bm2);

              spectralData.add(intensity);
              spectraName.add(newLCData.getName());
              flc = newLC;
              frm = rm2;
              fgm = gm2;
              fbm = bm2;
            } else {
              if (t > 0) {
                polyCoordinates.clear();
                firstRow = newRow;
                firstCol = newCol;
                minX = newCol;
                minY = newRow;
                if (t > 1) {
                  gc.clearRect(0, 0, width, height);
                  img = canvas.snapshot(null, null);
                  iv3.setImage(img);
                  iv4.setImage(img);
                }
              } else if (prevCol > 0 && prevRow > 0) {
                gc.strokeLine(newCol, newRow, prevCol, prevRow);
              }
              maxY = (newRow > maxY) ? newRow : maxY;
              maxX = (newCol > maxX) ? newCol : maxX;
              minY = (newRow < minY) ? newRow : minY;
              minX = (newCol < minX) ? newCol : minX;
              t = -1;
              gc.strokeLine(newCol, newRow - 4, newCol, newRow + 4);
              gc.strokeLine(newCol - 4, newRow, newCol + 4, newRow);
              prevRow = newRow;
              prevCol = newCol;
              polyCoordinates.add((double) newCol);
              polyCoordinates.add((double) newRow);
            }
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);
          } else {
            x = (NumberAxis) flc.getXAxis();
            y = (NumberAxis) flc.getYAxis();
            flc = new LineChart<Number, Number>(x, y);
            newLC = flc;

            wait.show();

            t = 2;
            gc.clearRect(0, 0, width, height);
            img = canvas.snapshot(null, null);
            iv3.setImage(img);
            iv4.setImage(img);
            if (drag.isSelected()) {
              gc.strokeRect(oldCol, oldRow, Math.abs(oldCol - newCol), Math.abs(oldRow - newRow));
              img = canvas.snapshot(null, null);
              iv3.setImage(img);
              iv4.setImage(img);

              dragAvg(intensity, oldRow, oldCol, newRow, newCol);
              newLCData.setName("Lat: " + String.format("%.4f", oldLat) + "-"
                  + String.format("%.4f", newLat) + ", Lon: " + String.format("%.4f", oldLon) + "-"
                  + String.format("%.4f", newLon));
              if (P.isSelected()) {
                intensity = pCorr(intensity);
                newLCData.setName("PHT " + newLCData.getName());
              }
              if (!fullVSName.equals("None")) {
                intensity = aCorr(newCol, intensity, correctADRfiles);
                String vsid = fullVSName.substring(15, 20);
                newLCData.setName("ATM (" + vsid + ") " + newLCData.getName());
              }
              inputData(intensity, newLCData, newLC);
              inputMarkers(newLC, rm2, gm2, bm2);
              newLC.setCreateSymbols(false);
              newLC.setAnimated(false);
              borderTB.setBottom(newLC);

              styleChart(newLC, rm2, gm2, bm2);
            } else {
              gc.strokeLine(newCol, newRow - 4, newCol, newRow + 4);
              gc.strokeLine(newCol - 4, newRow, newCol + 4, newRow);
              img = canvas.snapshot(null, null);
              iv3.setImage(img);
              iv4.setImage(img);
              if (singlePixel.isSelected()) {
                singlePixel(intensity, newRow, newCol);
                newLCData.setName("Lat: " + String.format("%.4f", newLat) + ", Lon: "
                    + String.format("%.4f", newLon));
              }
              if (ninePixel.isSelected()) {
                ninePixel(intensity, newRow, newCol);
                newLCData.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 1, newCol))
                    + "-" + String.format("%.4f", CRISM.getLat(newRow + 1, newCol)) + ", Lon: "
                    + String.format("%.4f", CRISM.getLon(newRow, newCol - 1)) + "-"
                    + String.format("%.4f", CRISM.getLon(newRow, newCol + 1)));
              }
              if (twentyFivePixel.isSelected()) {
                twentyFivePixel(intensity, newRow, newCol);
                newLCData.setName("Lat: " + String.format("%.4f", CRISM.getLat(newRow - 2, newCol))
                    + "-" + String.format("%.4f", CRISM.getLat(newRow + 2, newCol)) + ", Lon: "
                    + String.format("%.4f", CRISM.getLon(newRow, newCol - 2)) + "-"
                    + String.format("%.4f", CRISM.getLon(newRow, newCol + 2)));
              }
              if (P.isSelected()) {
                intensity = pCorr(intensity);
                newLCData.setName("PHT " + newLCData.getName());
              }
              if (!fullVSName.equals("None")) {
                intensity = aCorr(newCol, intensity, correctADRfiles);
                String vsid = fullVSName.substring(15, 20);
                newLCData.setName("ATM (" + vsid + ") " + newLCData.getName());
              }
              inputData(intensity, newLCData, newLC);
              inputMarkers(newLC, rm2, gm2, bm2);
              newLC.setCreateSymbols(false);
              newLC.setAnimated(false);
              borderTB.setBottom(newLC);

              styleChart(newLC, rm2, gm2, bm2);
            }
            flc = newLC;
            frm = rm2;
            fgm = gm2;
            fbm = bm2;
          }
          spectralData.clear();
          spectraName.clear();
          spectralData.add(intensity);
          spectraName.add(newLCData.getName());

          sp.setHvalue(hval);
          sp.setVvalue(vval);
          wait.setResult(Boolean.TRUE);
          wait.close();
        }
      });
    });

    /////////////////////////////////////////////////////////////
    // When slider value is changed, set what RGB should be and the output
    // wavelength value
    /////////////////////////////////////////////////////////////
    redSlider.valueProperty().addListener((obs, old_val, new_val) -> {
      double num = (double) new_val;
      red = findIndex(wavelength, num);
      redValue.setText(String.format("%.2f", wavelength.get(red)) + "nm");
    });
    grnSlider.valueProperty().addListener((obs, old_val, new_val) -> {
      double num = (double) new_val;
      green = findIndex(wavelength, num);
      grnValue.setText(String.format("%.2f", wavelength.get(green)) + "nm");
    });
    bluSlider.valueProperty().addListener((obs, old_val, new_val) -> {
      double num = (double) new_val;
      blue = findIndex(wavelength, num);
      bluValue.setText(String.format("%.2f", wavelength.get(blue)) + "nm");
    });

    ////////////////////////////////////////////////////////////
    // Gets the current chart, removes the RGB values and inputs the current values
    // Checks what is selected in contrast menu
    // Applies stretches with new RGB values
    // Checks whether subset stretch is selected,
    // Else use entire image for stretch
    ///////////////////////////////////////////////////////////
    updateBtn.setOnAction(e -> {
      JCATLog.getInstance().getLogger().log(Level.INFO,
          "Updating chart with selected RGB values and subset stretch.");
      XYChart.Series<Number, Number> rm2 = new XYChart.Series<Number, Number>();
      XYChart.Series<Number, Number> gm2 = new XYChart.Series<Number, Number>();
      XYChart.Series<Number, Number> bm2 = new XYChart.Series<Number, Number>();
      rm2.setName("Red");
      gm2.setName("Green");
      bm2.setName("Blue");

      flc.getData().remove(frm);
      flc.getData().remove(fbm);
      flc.getData().remove(fgm);
      LineChart<Number, Number> newLC = flc;

      inputMarkers(newLC, rm2, gm2, bm2);
      newLC.setCreateSymbols(false);
      newLC.setAnimated(false);
      borderTB.setBottom(newLC);

      styleChart(newLC, rm2, gm2, bm2);

      flc = newLC;
      frm = rm2;
      fgm = gm2;
      fbm = bm2;

      BufferedImage newBI = CRISM.getQuickColorImage(red, green, blue);

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
    writePropertiesFile();

    return true;
  }

}
