package javafx;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.logging.Level;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import com.sun.javafx.charts.Legend;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import reader.ADR;
import reader.ADRPairings;
import reader.ADRVSLibrary;
import reader.CRISMPDSImage;
import util.ImageUtils;
import util.ImageUtils.STRETCH;
import util.JCATConfig;
import util.JCATLog;
import util.JCATMessageWindow;

public class JCATCalls extends Application {

  int FillValue = 65535, MAX_FILES = 10, t = 2;
  double scaleFactor = 1;

  CRISMPDSImage CRISM;
  ADR adr;

  String imgfile = null;
  String ddrFile = null;

  List<String> adrNames = new ArrayList<String>(); // ADR files created BEFORE TRDR file with
                                                   // correct bc, wv
  List<String> adrIDList = new ArrayList<String>(); // ADR ID's as Strings
  List<RadioMenuItem> adrIDItems = new ArrayList<RadioMenuItem>(); // ADR ID's as RadioMenuItems
                                                                   // (for user choice)
  List<String> correctADRfiles = new ArrayList<String>();
  List<String> lownoise = new ArrayList<String>();// correct ADR files for each ADR ID
  ToggleGroup VSTG = new ToggleGroup();

  int bc, wv;

  int red, green, blue;
  double longitude, latitude;

  int firstRow, firstCol, prevRow, prevCol;
  int minX, minY, maxX, maxY;
  List<Double> polyCoordinates = new ArrayList<Double>();
  int visX, visY, visW, visH;

  BufferedImage bImg;
  Image image;
  WritableImage img;

  List<Integer> index = new ArrayList<Integer>();
  List<Double> wavelength = new ArrayList<Double>();
  List<Integer> detector = new ArrayList<Integer>();
  List<Double> intensity = new ArrayList<Double>();
  List<Integer> gbBands = new ArrayList<Integer>();
  List<List<Double>> spectralData = new ArrayList<List<Double>>();
  List<String> spectraName = new ArrayList<String>();

  String fullVSName = "None";
  String optimalADR = "Empty";
  double contin = 0.0;

  // this chart replaces the current chart
  NumberAxis fxAxis = new NumberAxis(400, 4000, 400);
  NumberAxis fyAxis = new NumberAxis(0, 0.4, 0.05);
  LineChart<Number, Number> flc = new LineChart<Number, Number>(fxAxis, fyAxis);
  XYChart.Series<Number, Number> fLCData = new XYChart.Series<Number, Number>();
  XYChart.Series<Number, Number> frm = new XYChart.Series<Number, Number>();
  XYChart.Series<Number, Number> fgm = new XYChart.Series<Number, Number>();
  XYChart.Series<Number, Number> fbm = new XYChart.Series<Number, Number>();
  LineChart<Number, Number> fslc = new LineChart<Number, Number>(fxAxis, fyAxis);

  // defining all menu-related items
  MenuBar mbar = new MenuBar();

  Menu fileMenu = new Menu("File"), zoomMenu = new Menu("Zoom"), plotMenu = new Menu("Plot"),
      controlsMenu = new Menu("Controls");

  MenuItem expandImg = new MenuItem("Expand Image"), saveImg = new MenuItem("Save Image"),
      saveData = new MenuItem("Save Spectral Data"), saveChart = new MenuItem("Save Chart"),
      close = new MenuItem("Close Window");

  ToggleGroup zoomTG = new ToggleGroup();
  RadioMenuItem zoom50 = new RadioMenuItem("50%"), zoom75 = new RadioMenuItem("75%"),
      zoom100 = new RadioMenuItem("100%"), zoom125 = new RadioMenuItem("125%"),
      zoom150 = new RadioMenuItem("150%"), zoom200 = new RadioMenuItem("200%");

  ToggleGroup contrastTG = new ToggleGroup();
  Menu contrast = new Menu("Contrast");
  RadioMenuItem linStretch = new RadioMenuItem("Linear Stretch"),
      per1Stretch = new RadioMenuItem("1 Percentile Stretch"),
      per2Stretch = new RadioMenuItem("2 Percentile Stretch"),
      perCStretch = new RadioMenuItem("Custom Percentile Stretch");
  CheckMenuItem subStretch = new CheckMenuItem("Spatial Subset Stretch");

  ToggleGroup specAvgSizeTG = new ToggleGroup();
  Menu specAvgSize = new Menu("Spectrum Averaging Size");
  RadioMenuItem singlePixel = new RadioMenuItem("Single Pixel"),
      ninePixel = new RadioMenuItem("3x3 Pixels"),
      twentyFivePixel = new RadioMenuItem("5x5 Pixels");

  ToggleGroup specIntTG = new ToggleGroup();
  Menu specInt = new Menu("Spectral Interest");
  RadioMenuItem click = new RadioMenuItem("Click"), drag = new RadioMenuItem("Drag Avg"),
      region = new RadioMenuItem("Region of Interest");
  MenuItem manualInput = new MenuItem("Manual Input"),
      mouseBtn = new MenuItem("Mouse Button Instructions");

  ToggleGroup numSpecTG = new ToggleGroup();
  Menu specNum = new Menu("# of Spectra");
  RadioMenuItem singSpec = new RadioMenuItem("One"), multSpec = new RadioMenuItem("Two+");

  MenuItem specOps = new MenuItem("Spectral Operations");

  Menu ATP = new Menu("ATP Corrections (TRDR Only)"), A = new Menu("Atmospheric");
  CheckMenuItem T = new CheckMenuItem("Thermal"), P = new CheckMenuItem("Photometric");

  RadioMenuItem emp = new RadioMenuItem("Pre-selected optimum wavelength shift"),
      defVS = new RadioMenuItem("Default volcano scan 061C4"), noVS = new RadioMenuItem("None");
  Menu userChoice = new Menu("User-selected volcano scan");

  MenuItem showBP = new MenuItem("View Browse Products");
  MenuItem showSP = new MenuItem("View Summary Parameters");

  MenuItem resetX = new MenuItem("Reset X"), resetY = new MenuItem("Reset Y"),
      resetB = new MenuItem("Reset Both"), setX = new MenuItem("Set X Range"),
      setY = new MenuItem("Set Y Range");

  @Override
  public void start(Stage primaryStage) throws Exception {}

  ////////////////////////////////////////////
  // Converts from Optional<String> to String
  ////////////////////////////////////////////
  public String tidy(Optional<String> filename) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method tidy");
    String file = filename.toString();
    int filelength = file.length();
    return file.substring(9, filelength - 1);
  }

  //////////////////////////////////////////////
  // Writes out properties to config.txt in JCAT folder
  //////////////////////////////////////////////
  public void writePropertiesFile() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method writePropertiesFile");
    JCATConfig.getInstance().save();
  }

  ///////////////////////////////////////////////////////
  // Reads in config.txt from JCAT folder
  // Instantiates RGB values
  ///////////////////////////////////////////////////////
  public void readPropertiesFile() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readPropertiesFile");
    red = JCATConfig.getInstance().getRed();
    green = JCATConfig.getInstance().getGreen();
    blue = JCATConfig.getInstance().getBlue();
  }

  /////////////////////////////////////////////////////////
  // Reads in recentFiles.txt file, places filenames into vector for choiceBox
  /////////////////////////////////////////////////////////
  public Vector<String> getRecentFiles() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getRecentFiles");
    Vector<String> lines = new Vector<String>();
    try {

      JCATLog.getInstance().getLogger().log(Level.INFO, "Displaying recent files");
      String path = JCATConfig.getInstance().getLocalArchive() + File.separator + "recentFiles.txt";
      File f = new File(path);
      if (f.exists()) {
        BufferedReader in = new BufferedReader(new FileReader(f));
        String line = null;
        while ((line = in.readLine()) != null) {
          lines.add(line);
          if (lines.size() == MAX_FILES)
            break;
        }
        in.close();
      }
    } catch (IOException e) {
      JCATLog.getInstance().getLogger().log(Level.WARNING, "Error retriving recent files.");
      JCATMessageWindow.show(e);
      e.printStackTrace();
    }
    return lines;
  }

  //////////////////////////////////////////////////////////////////
  // Adds the recently used file to the vector to save in recentFiles.txt
  // Ensures no duplicates
  //////////////////////////////////////////////////////////////////
  public void saveRecentFiles(String newFile) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method saveRecentFiles");
    Vector<String> lines = getRecentFiles();
    try {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Adding file to recent files: " + newFile);
      String path = JCATConfig.getInstance().getLocalArchive() + File.separator + "recentFiles.txt";
      File f = new File(path);
      PrintWriter out;
      out = new PrintWriter(new FileWriter(f));
      out.println(newFile);
      for (int i = 0; i < lines.size(); i++) {
        if (!lines.elementAt(i).equals(newFile))
          out.print(lines.elementAt(i) + "\n");
      }
      out.close();
    } catch (IOException e) {
      JCATLog.getInstance().getLogger().log(Level.WARNING, "Error adding file to recents.");
      JCATMessageWindow.show(e);
      e.printStackTrace();
    }
  }

  public HashMap<String, String> getPairedFiles() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getPairedFiles");
    HashMap<String, String> adrPairs = new HashMap<String, String>();
    try {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Getting paired files");
      String path =
          JCATConfig.getInstance().getLocalArchive() + File.separator + "crism_paired_files.txt";
      File f = new File(path);
      if (f.exists()) {

        BufferedReader in = new BufferedReader(new FileReader(f));
        String line = null;
        while ((line = in.readLine()) != null) {
          String[] parts = line.split("\\s+");
          adrPairs.put(parts[0], parts[1]);
        }
        in.close();
      }
    } catch (IOException e) {
      JCATLog.getInstance().getLogger().log(Level.WARNING, "Error getting paired files");
      JCATMessageWindow.show(e);
      e.printStackTrace();
    }
    return adrPairs;
  }

  public void savePairedFiles(String obsID, String ADR) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method savePairedFiles");
    HashMap<String, String> adrPairs = getPairedFiles();
    try {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Saving paired files.");
      String path =
          JCATConfig.getInstance().getLocalArchive() + File.separator + "crism_paired_files.txt";
      File f = new File(path);
      PrintWriter out;
      out = new PrintWriter(new FileWriter(f));
      out.println(obsID + ADR.substring(15, 20));
      for (String key : adrPairs.keySet()) {
        if (!key.equals(obsID)) {
          out.print(key + " " + adrPairs.get(key) + "\n");
        }
      }
      out.close();
    } catch (IOException e) {
      JCATLog.getInstance().getLogger().log(Level.WARNING, "Error saving paired files");
      JCATMessageWindow.show(e);
      e.printStackTrace();
    }
  }

  //////////////////////////////////////////////////
  // Creates an alert that appears during long wait times
  // d.setResult(Boolean.TRUE); used to close when finished
  //////////////////////////////////////////////////
  public Dialog<Boolean> waitMessage() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method waitMessage");
    Dialog<Boolean> alert = new Dialog<>();
    alert.setHeaderText("Loading...");
    alert.setContentText("Please wait - JCAT is Loading...");
    return alert;
  }

  //////////////////////////////////////////////////
  // Returns spectrum for a single pixel in an arrayList
  // Includes fillValue to not put values in wrong indices
  //////////////////////////////////////////////////
  public List<Double> singlePixel(List<Double> intensity, int newRow, int newCol) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method singlePixel");
    JCATLog.getInstance().getLogger().log(Level.INFO, "Determining spectrum for single pixel");

    for (int i : index) {
      float a = CRISM.getPCFloatValue(newRow, newCol, i);
      intensity.add(Double.valueOf(a));
    }
    return intensity;
  }

  ///////////////////////////////////////////////////
  // Returns averaged spectrum of 3x3 box around selected pixel
  // Checks each pixel to not include any fillValues in averages
  ///////////////////////////////////////////////////
  public List<Double> ninePixel(List<Double> intensity, int newRow, int newCol) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method ninePixel");
    JCATLog.getInstance().getLogger().log(Level.INFO, "Averaging spectrum of 3x3 box");

    for (int i : index) {
      double total = 0.0;
      double counter = 0.0;
      for (int j = 0; j < 3; j++) {
        for (int k = 0; k < 3; k++) {
          float a = CRISM.getPCFloatValue(newRow - 1 + k, newCol - 1 + j, i);
          if (a < FillValue) {
            total = total + a;
            counter++;
          }
        }
      }
      double avg = total / counter;
      intensity.add(avg);
    }
    return intensity;
  }

  ///////////////////////////////////////////////////
  // Returns averaged spectrum of 5x5 box around selected pixel
  // Checks each pixel to not include any fillValues in averages
  ///////////////////////////////////////////////////
  public List<Double> twentyFivePixel(List<Double> intensity, int newRow, int newCol) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method twentyFivePixel");
    JCATLog.getInstance().getLogger().log(Level.INFO, "Averaging spectrum of 5x5 box.");

    for (int i : index) {
      double total = 0.0;
      double counter = 0.0;
      for (int j = 0; j < 5; j++) {
        for (int k = 0; k < 5; k++) {
          float a = CRISM.getPCFloatValue(newRow - 2 + k, newCol - 2 + j, i);
          if (a < FillValue) {
            total = total + a;
            counter++;
          }
        }
      }
      double avg = total / counter;
      intensity.add(avg);
    }
    return intensity;
  }

  ///////////////////////////////////////////////////
  // Returns averaged spectrum of user-specified rectangle
  // Checks each pixel to not include any fillValues in averages
  ///////////////////////////////////////////////////
  public List<Double> dragAvg(List<Double> intensity, int startDragRow, int startDragCol,
      int newRow, int newCol) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method dragAvg");
    JCATLog.getInstance().getLogger().log(Level.INFO, "Averaging spectrum of selected area");

    for (int i : index) {
      double total = 0.0;
      double counter = 0.0;
      for (int j = startDragCol; j <= newCol; j++) {
        for (int k = startDragRow; k <= newRow; k++) {
          float a = CRISM.getPCFloatValue(k, j, i);
          if (a < FillValue) {
            total = total + a;
            counter++;
          }
        }
      }
      double avg = total / counter;
      intensity.add(avg);
    }
    return intensity;
  }

  ///////////////////////////////////////////////////
  // Returns averaged spectrum of user-specified region of interest
  // roi points placed in alternating order so (j, j+1) will be a coordinate
  // Checks each pixel to not include any fillValues in averages
  ///////////////////////////////////////////////////
  public List<Double> roiAvg(List<Double> intensity, List<Integer> roi) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method roiAvg");
    JCATLog.getInstance().getLogger().log(Level.INFO, "Averaging spectrum for region of interest");

    for (int i : index) {
      double total = 0.0;
      double counter = 0.0;
      for (int j = 0; j < roi.size(); j = j + 2) {
        float a = CRISM.getPCFloatValue(roi.get(j + 1), roi.get(j), i);
        if (a < FillValue) {
          total = total + a;
          counter++;
        }
      }
      double avg = total / counter;
      intensity.add(avg);
    }
    return intensity;
  }

  ////////////////////////////////////////////////////
  // Adjustment for incidence angle of sun at time of image
  // Gets the INA index at center of image, divides intensity by it
  // INA is gotten from TRDR DDR file at the "INA at areoid, deg"
  ////////////////////////////////////////////////////
  public List<Double> pCorr(List<Double> intensity) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method pCorr");
    for (int i = 0; i < intensity.size(); i++) {
      double INA = ((CRISM.getPCFloatValue((int) CRISM.getHeight() / 2, (int) CRISM.getWidth() / 2,
          CRISM.getINA())) * Math.PI) / 180;
      double a = intensity.get(i) / (Math.cos(INA));
      intensity.remove(i);
      intensity.add(i, a);
    }
    return intensity;
  }

  /////////////////////////////////////////////////////
  // Adjustment for atmospheric CO2 absorption with volcano scan
  // Checks for keyword "Optimize" which runs through each VS in correctADRfiles
  // Chooses best one, checks if a less noisy comparable one if available
  // Then applies VS to entire spectrum, returns as an arrayList
  /////////////////////////////////////////////////////
  public List<Double> aCorr(int col, List<Double> intensity, List<String> correctADRfiles) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method aCorr");
    if (fullVSName.equals("Optimize")) { // Looks for keyword optimize
      if (optimalADR.equals("Empty")) { // Checks if optimalADR has been filled yet or not
        HashMap<String, String> pairedFiles = getPairedFiles();
        String obsID = FilenameUtils.getBaseName(ddrFile).substring(3, 11);
        String adrPair = null;
        if (pairedFiles.keySet().contains(obsID)) {
          optimalADR = adrVS(pairedFiles.get(obsID));
        } else {
          ADRPairings pairings = new ADRPairings();
          adrPair = pairings.getADRPair(obsID);
          if (adrPair != null) {
            optimalADR = adrVS(adrPair);
          } else {
            // modeled after measure_shit_artifact.pro
            double wave1 = 1947.0; // wave range
            double wave2 = 2066.0;

            int b1 = findIndex(wavelength, wave1);
            int b2 = findIndex(wavelength, wave2);

            // subsampling from image cube based on size
            List<Integer> subIdx = new ArrayList<Integer>();
            if (CRISM.getWidth() > 600) {
              for (int i = 270; i < 370; i++) {
                subIdx.add(i);
              }
            } else if (CRISM.getWidth() > 300) {
              for (int i = 135; i < 185; i++) {
                subIdx.add(i);
              }
            }

            double QUIET_THRESH = 0.002;
            String best = null;
            String lownoisebest = null;
            double minShift = FillValue;
            double lownoiseShift = FillValue;

            for (String ADR : correctADRfiles) { // iterate through each ADR and check the artifact
                                                 // size
              fullVSName = ADR;
              double shift = measureShiftArtifact(subIdx, b1, b2);
              if (contin > 0.0) {
                shift = shift / contin;
              }
              if (shift < minShift) {
                if (lownoise.contains(ADR)) {
                  lownoisebest = ADR;
                  lownoiseShift = shift;
                }
                best = ADR;
                minShift = shift;
              }
            }

            if (minShift == lownoiseShift) { // if smallest artifact came from lownoise
              optimalADR = best;
            } else {
              if ((lownoiseShift - minShift) < QUIET_THRESH) { // if lownoise is comparable to noisy
                                                               // VS
                optimalADR = lownoisebest;
              }
            }
            savePairedFiles(obsID, optimalADR);
          }
        }
        fullVSName = optimalADR;
      }
    }
    List<Double> vsintensity = applyVS(col, intensity);
    return vsintensity;
  }

  public ArrayList<Double> applyVS(int col, List<Double> intensity) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method applyVS");
    //////////////////////////////////////////////////////////////////
    // Establishes ADRVSLibrary in order to get full path for fullVSName
    // Retrieves both transmission spectrum and artifact spectrum (McGuire) from ADR
    //////////////////////////////////////////////////////////////////
    ADR adr =
        new ADR(ADRVSLibrary.getInstance().getLocalPath() + File.separator + fullVSName + ".IMG");

    List<Double> vstrans = adr.getTrans(col, gbBands);
    List<Double> vsart = adr.getArt(col, gbBands);

    ///////////////////////////////////////////////////////////////
    // Modeled after scaleatm_pcm.pro in CAT code
    // Finds the indices of closest wavelengths to interpolation wavelengths and
    // McGuire wavelengths
    // Uses variable expon to divide scene spectrum by scaled transmission spectrum
    ////////////////////////////////////////////////////////////////
    List<Integer> iwaveIdx = new ArrayList<Integer>();
    double iwave1 = 1764.0;
    double iwave2 = 2239.0;

    int iwave1Idx = findIndex(wavelength, iwave1);
    iwaveIdx.add(iwave1Idx);
    int iwave2Idx = findIndex(wavelength, iwave2);
    iwaveIdx.add(iwave2Idx);

    double correctingBand = 2007.0; // McGuire wavelengths
    double neighborBand = 1980.0;

    int corrIdx = findIndex(wavelength, correctingBand);
    int neighborIdx = findIndex(wavelength, neighborBand);

    double R_CORR = intensity.get(corrIdx);
    double R_NEIGHBOR = intensity.get(neighborIdx);

    double arg3 = R_CORR / R_NEIGHBOR;
    double arg4 = vstrans.get(corrIdx) / vstrans.get(neighborIdx);
    double expon = Math.log(arg3) / Math.log(arg4);

    ArrayList<Double> vsintensity = new ArrayList<Double>();
    for (int i = 0; i < wavelength.size(); i++) {
      if (intensity.get(i) < 65534.0) { // ensures no fill values get divided
        vsintensity.add(intensity.get(i) / Math.pow(vstrans.get(i), expon));
      } else {
        vsintensity.add(Double.valueOf(FillValue));
      }
    }

    ///////////////////////////////////////////////////////////////////
    // Modeled after patch_vs_arifact.pro in CAT code
    // variable avg_cont averages left and right of the artifact
    // median over +/-2 contiguous bands to estimate continuum level at the two ends
    // Essentially, adjust scale factor in order to minimize correlation
    ////////////////////////////////////////////////////////////////////
    // ArrayList<Double> vsintensity = new ArrayList<Double>(vsintensityArray);

    double medians = 0;
    for (int i = 0; i < iwaveIdx.size(); i++) {
      int b1 = Math.max(iwaveIdx.get(i) - 2, 0);
      int b2 = Math.min(iwaveIdx.get(i) + 2, wavelength.size() - 1);
      medians += getMedian(vsintensity.subList(b1, b2));
    }
    double avg_cont = 0.5 * medians;

    // These get bands of interest for the artifact
    int na = 1 + Collections.max(iwaveIdx) - Collections.min(iwaveIdx);
    List<Integer> xa = new ArrayList<Integer>();
    for (int i = 0; i < na; i++) {
      xa.add(i + Collections.min(iwaveIdx));
    }

    // Extracts the interesting part of each spectrum using xa bands
    List<Double> artx = vsart.subList(xa.get(0), xa.get(xa.size() - 1));
    List<Double> csx = vsintensity.subList(xa.get(0), xa.get(xa.size() - 1));

    // Adjusts scale
    double merit = 1.0e23;
    double dscl = 1.0e23;
    double scl_fac = 1.0;
    while (Math.abs(merit) > 1.0e-6 && (Math.abs(dscl) > 1.0e-4)) {
      List<Double> patchx = new ArrayList<Double>();
      for (int i = 0; i < artx.size(); i++) {
        patchx.add(csx.get(i) + (avg_cont * scl_fac * artx.get(i))); // newtons method-like approach
      }
      merit = evaluateArtifact(patchx, artx); // Evaluate the derivative

      double delta = Math.max(Math.abs(scl_fac) * 1.0e-3, 0.0003);
      double scl_fac2 = scl_fac + delta;

      List<Double> patch2 = new ArrayList<Double>();
      for (int i = 0; i < artx.size(); i++) {
        patch2.add(csx.get(i) + (avg_cont * scl_fac2 * artx.get(i)));
      }
      double merit2 = evaluateArtifact(patch2, artx);

      double dmds = (merit2 - merit) / delta;
      dscl = -merit / dmds;
      scl_fac += dscl;
    }

    // Adds the patch to the corrected spectrum, returns as completed volcano scan
    for (int i : xa) {
      double corrected = vsintensity.get(i);
      if (corrected < 65534) {
        double patched = corrected + (avg_cont * scl_fac * vsart.get(i));
        vsintensity.remove(i);
        vsintensity.add(i, patched);
      }
    }
    return vsintensity;
  }

  public double evaluateArtifact(List<Double> patch, List<Double> art) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method evaluateArtifact");
    ////////////////////////////////////////////////////////////
    // Modeled after evaluate_vatvs_patch in patch_vs_artifact.pro in CAT code
    // Subtract smoothed spectrum from original to do a filter
    // Multiply the differences, then total excluding highest&lowest N_DROP points
    /////////////////////////////////////////////////////////////
    double total = 0;
    int MERIT_SMW = 3;
    int N_DROP = 3;

    List<Double> fPatch = new ArrayList<Double>();
    List<Double> fArt = new ArrayList<Double>();
    int ngood = 0;
    for (int i = 0; i < patch.size(); i++) {
      double a = patch.get(i);
      double b = art.get(i);
      if (a < 65534.0 && b < 66534.0) {
        fPatch.add(a);
        fArt.add(b);
        ngood++;
      }
    }
    if (ngood < 8) {
      return 66535.;
    }

    List<Double> dPatch = new ArrayList<Double>();
    List<Double> dArt = new ArrayList<Double>();
    List<Double> smoothPatch = smooth(fPatch, MERIT_SMW);
    List<Double> smoothArt = smooth(fArt, MERIT_SMW);
    for (int i = 0; i < fPatch.size(); i++) {
      double a = fPatch.get(i) - smoothPatch.get(i);
      dPatch.add(a);
      double b = fArt.get(i) - smoothArt.get(i);
      dArt.add(b);
    }

    List<Double> correlation = new ArrayList<Double>();
    for (int i = 3; i < dPatch.size() - 3; i++) {
      double a = dPatch.get(i);
      double b = dArt.get(i);
      correlation.add(a * b); // multiplying the differences
    }
    Collections.sort(correlation);
    int nc = correlation.size();
    int ndrop = Math.max(Math.min(N_DROP, (int) ((nc - 5.) / 2.)), 0); // limit number dropped so 5+
                                                                       // remain
    for (int i = ndrop; i < nc - ndrop; i++) {
      total += correlation.get(i);
    }

    return total;
  }

  public List<Double> smooth(List<Double> list, int MERIT_SMW) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method smooth");
    ///////////////////////////////////////////////////////////////
    // Does a boxcar averaging based on width specified by MERIT_SMW
    ///////////////////////////////////////////////////////////////
    List<Double> result = new ArrayList<Double>();
    int w = MERIT_SMW;
    for (int i = 0; i < list.size(); i++) {
      if ((i >= (w - 1) / 2) && (i <= list.size() - ((w + 1) / 2))) {
        double total = 0;
        for (int j = 0; j < w; j++) {
          total += list.get(i + j - 1);
        }
        result.add((1. / w) * total);
      } else {
        result.add(list.get(i));
      }
    }
    return result;
  }

  public double getMedian(List<Double> list) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method getMedian");
    double median = 0;
    Collections.sort(list);
    int s = list.size();
    if (s % 2 == 0) {
      median = (list.get(s / 2) + list.get((s / 2) - 1)) / 2.0;
    } else {
      median = list.get(s / 2);
    }
    return median;
  }

  // modeled after measure_shift_artifact.pro
  public double measureShiftArtifact(List<Integer> subIdx, int b1, int b2) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method measureShiftArtifact");
    List<Double> pixart = new ArrayList<Double>();
    List<Double> sub = new ArrayList<Double>();

    for (int i = 0; i < CRISM.getHeight(); i++) { // all rows
      for (int j : subIdx) { // subIdx of columns
        List<Double> dummy = new ArrayList<Double>();
        for (int k : index) {
          dummy.add((Double.valueOf(CRISM.getPCFloatValue(i, j, k)))); // retrieve I/F values
        }
        if (getMedian(dummy) < 65534.0) {
          List<Double> vsdummy = applyVS(j, dummy); // correct with VS
          List<Double> xa = new ArrayList<Double>();
          for (int l = b2; l <= b1; l++) {
            xa.add(Math.abs(
                vsdummy.get(l - 1) - vsdummy.get(l) - vsdummy.get(l + 1) + vsdummy.get(l + 2)));
            if (vsdummy.get(l) < 65534.0 && !vsdummy.get(l).isNaN()) {
              sub.add(vsdummy.get(l));
            }
          }
          pixart.add(Collections.max(xa));
        }
      }
    }

    contin = 0.0;
    contin = (sub.size() > 0) ? getMedian(sub) : contin;

    List<Double> gPixart = new ArrayList<Double>();
    gPixart.add((double) FillValue);
    for (int i = 0; i < pixart.size(); i++) {
      if (pixart.get(i) < 65534 && !pixart.get(i).isNaN()) {
        gPixart.add(pixart.get(i));
      }
    }
    return getMedian(gPixart);
  }

  public int findIndex(List<Double> wavelength, double num) {
    double distance = Math.abs(wavelength.get(0) - num);
    int idx = 0;
    for (int c = 1; c < wavelength.size(); c++) {
      double cdistance = Math.abs(wavelength.get(c) - num);
      if (cdistance < distance) {
        idx = c;
        distance = cdistance;
      }
    }
    return idx;
  }

  //////////////////////////////////////////////////////
  // Performs a min/max stretch using the entire bufferedImage
  //////////////////////////////////////////////////////
  public BufferedImage linearStretch(BufferedImage bi) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method linearStretch");
    BufferedImage result = null;

    int w = bi.getWidth();
    int h = bi.getHeight();
    int row, col, rgb;
    float x, y, z;
    int R, G, B;

    float xmax = (float) 0.0;
    float ymax = (float) 0.0;
    float zmax = (float) 0.0;

    result = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);

    ArrayList<Float> redBand = new ArrayList<>();
    ArrayList<Float> grnBand = new ArrayList<>();
    ArrayList<Float> bluBand = new ArrayList<>();

    DescriptiveStatistics r = new DescriptiveStatistics();
    DescriptiveStatistics g = new DescriptiveStatistics();
    DescriptiveStatistics b = new DescriptiveStatistics();

    for (int i = 0; i < bi.getHeight(); i++) {
      for (int j = 0; j < bi.getWidth(); j++) {
        rgb = bi.getRGB(j, i);
        r.addValue((rgb >> 16) & 0x000000FF);
        g.addValue((rgb >> 8) & 0x000000FF);
        b.addValue((rgb) & 0x000000FF);
      }
    }
    int rMin = (int) r.getMin();
    int gMin = (int) g.getMin();
    int bMin = (int) b.getMin();
    int rDiff = (int) (r.getMax() - r.getMin());
    int gDiff = (int) (g.getMax() - g.getMin());
    int bDiff = (int) (b.getMax() - b.getMin());

    for (int i = 0; i < r.getN(); i++) {
      x = (float) ((r.getElement(i) - rMin) / rDiff);
      y = (float) ((g.getElement(i) - gMin) / gDiff);
      z = (float) ((b.getElement(i) - bMin) / bDiff);
      redBand.add(Math.min(Math.max(x, 0), 1));
      grnBand.add(Math.min(Math.max(y, 0), 1));
      bluBand.add(Math.min(Math.max(z, 0), 1));
      if (x > xmax && x != 65535) {
        xmax = x;
      }
      if (y > ymax && y != 65535) {
        ymax = y;
      }
      if (z > zmax && z != 65535) {
        zmax = z;
      }
    }
    float scale = 255 / (Math.max(xmax, Math.max(ymax, zmax)));
    for (int i = 0; i < redBand.size(); i++) {
      col = i % w;
      row = i / w;
      x = redBand.get(i);
      y = grnBand.get(i);
      z = bluBand.get(i);
      R = Math.min((int) (0.50 + scale * x), 255);
      G = Math.min((int) (0.50 + scale * y), 255);
      B = Math.min((int) (0.50 + scale * z), 255);

      rgb = (R << 16) | (G << 8) | B;
      result.setRGB(col, row, rgb);
    }
    return result;
  }

  //////////////////////////////////////////////////////
  // Performs a min/max stretch on entire image while subStretch is selected
  // Min/max values are taken using the stretchValues from the subImage
  //////////////////////////////////////////////////////
  public BufferedImage linSubStretch(BufferedImage bi, List<Double> stretchValues) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method linSubStretch");
    BufferedImage result = null;

    int w = bi.getWidth();
    int h = bi.getHeight();
    int row, col, rgb;
    float x, y, z;
    int R, G, B;

    float xmax = (float) 0.0;
    float ymax = (float) 0.0;
    float zmax = (float) 0.0;

    result = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);

    ArrayList<Float> redBand = new ArrayList<>();
    ArrayList<Float> grnBand = new ArrayList<>();
    ArrayList<Float> bluBand = new ArrayList<>();

    DescriptiveStatistics r = new DescriptiveStatistics();
    DescriptiveStatistics g = new DescriptiveStatistics();
    DescriptiveStatistics b = new DescriptiveStatistics();

    for (int i = 0; i < bi.getHeight(); i++) {
      for (int j = 0; j < bi.getWidth(); j++) {
        rgb = bi.getRGB(j, i);
        r.addValue((rgb >> 16) & 0x000000FF);
        g.addValue((rgb >> 8) & 0x000000FF);
        b.addValue((rgb) & 0x000000FF);
      }
    }

    double rMin = stretchValues.get(0);
    double gMin = stretchValues.get(1);
    double bMin = stretchValues.get(2);
    double rDiff = stretchValues.get(3);
    double gDiff = stretchValues.get(4);
    double bDiff = stretchValues.get(5);

    for (int i = 0; i < r.getN(); i++) {
      x = (float) ((r.getElement(i) - rMin) / rDiff);
      y = (float) ((g.getElement(i) - gMin) / gDiff);
      z = (float) ((b.getElement(i) - bMin) / bDiff);
      redBand.add(Math.min(Math.max(x, 0), 1));
      grnBand.add(Math.min(Math.max(y, 0), 1));
      bluBand.add(Math.min(Math.max(z, 0), 1));
      if (x > xmax && x != 65535) {
        xmax = x;
      }
      if (y > ymax && y != 65535) {
        ymax = y;
      }
      if (z > zmax && z != 65535) {
        zmax = z;
      }
    }
    xmax = (xmax > 1) ? 1 : xmax;
    ymax = (ymax > 1) ? 1 : ymax;
    zmax = (zmax > 1) ? 1 : zmax;
    float scale = 255 / (Math.max(xmax, Math.max(ymax, zmax)));

    for (int i = 0; i < redBand.size(); i++) {
      col = i % w;
      row = i / w;
      x = redBand.get(i);
      y = grnBand.get(i);
      z = bluBand.get(i);
      R = Math.min((int) (0.50 + scale * x), 255);
      G = Math.min((int) (0.50 + scale * y), 255);
      B = Math.min((int) (0.50 + scale * z), 255);

      rgb = (R << 16) | (G << 8) | B;
      result.setRGB(col, row, rgb);
    }
    return result;
  }

  /////////////////////////////////////////////////////////
  // Performs a percentileStretch with set (1, 2) or custom stretch percentile
  /////////////////////////////////////////////////////////
  public BufferedImage percentileStretch(BufferedImage bi, double stretch) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method percentileStretch");
    // if(bi==null)
    // return null;

    BufferedImage result = null;

    int w = bi.getWidth();
    int h = bi.getHeight();
    int row, col, rgb;
    float x, y, z;
    int R, G, B;

    float xmax = (float) 0.0;
    float ymax = (float) 0.0;
    float zmax = (float) 0.0;

    result = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);

    ArrayList<Float> redBand = new ArrayList<>();
    ArrayList<Float> grnBand = new ArrayList<>();
    ArrayList<Float> bluBand = new ArrayList<>();

    DescriptiveStatistics r = new DescriptiveStatistics();
    DescriptiveStatistics g = new DescriptiveStatistics();
    DescriptiveStatistics b = new DescriptiveStatistics();

    for (int i = 0; i < bi.getHeight(); i++) {
      for (int j = 0; j < bi.getWidth(); j++) {
        rgb = bi.getRGB(j, i);
        r.addValue((rgb >> 16) & 0x000000FF);
        g.addValue((rgb >> 8) & 0x000000FF);
        b.addValue((rgb) & 0x000000FF);
      }
    }
    double rPer = r.getPercentile(stretch);
    double gPer = g.getPercentile(stretch);
    double bPer = b.getPercentile(stretch);
    double rDiff = (r.getPercentile(100 - stretch) - rPer);
    double gDiff = (g.getPercentile(100 - stretch) - gPer);
    double bDiff = (b.getPercentile(100 - stretch) - bPer);

    for (int i = 0; i < r.getN(); i++) {
      x = (float) ((r.getElement(i) - rPer) / rDiff);
      y = (float) ((g.getElement(i) - gPer) / gDiff);
      z = (float) ((b.getElement(i) - bPer) / bDiff);
      redBand.add(Math.min(Math.max(x, 0), 1));
      grnBand.add(Math.min(Math.max(y, 0), 1));
      bluBand.add(Math.min(Math.max(z, 0), 1));
      if (x > xmax && x != 65535) {
        xmax = x;
      }
      if (y > ymax && y != 65535) {
        ymax = y;
      }
      if (z > zmax && z != 65535) {
        zmax = z;
      }
    }
    float scale = 255 / (Math.max(xmax, Math.max(ymax, zmax)));
    for (int i = 0; i < redBand.size(); i++) {
      col = i % w;
      row = i / w;
      x = redBand.get(i);
      y = grnBand.get(i);
      z = bluBand.get(i);
      R = Math.min((int) (0.50 + scale * x), 255);
      G = Math.min((int) (0.50 + scale * y), 255);
      B = Math.min((int) (0.50 + scale * z), 255);

      rgb = (R << 16) | (G << 8) | B;
      result.setRGB(col, row, rgb);
    }
    return result;
  }

  ////////////////////////////////////////////////////////
  // Performs the same percentileStrech but with stretchValues taken from a
  //////////////////////////////////////////////////////// subImage
  ////////////////////////////////////////////////////////
  public BufferedImage percentileSubStretch(BufferedImage bi, List<Double> stretchValues) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method percentileSubStretch");
    BufferedImage result = null;

    int w = bi.getWidth();
    int h = bi.getHeight();
    int row, col, rgb;
    float x, y, z;
    int R, G, B;

    float xmax = (float) 0.0;
    float ymax = (float) 0.0;
    float zmax = (float) 0.0;

    result = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);

    ArrayList<Float> redBand = new ArrayList<>();
    ArrayList<Float> grnBand = new ArrayList<>();
    ArrayList<Float> bluBand = new ArrayList<>();

    DescriptiveStatistics r = new DescriptiveStatistics();
    DescriptiveStatistics g = new DescriptiveStatistics();
    DescriptiveStatistics b = new DescriptiveStatistics();

    for (int i = 0; i < bi.getHeight(); i++) {
      for (int j = 0; j < bi.getWidth(); j++) {
        rgb = bi.getRGB(j, i);
        r.addValue((rgb >> 16) & 0x000000FF);
        g.addValue((rgb >> 8) & 0x000000FF);
        b.addValue((rgb) & 0x000000FF);
      }
    }
    double rMin = stretchValues.get(0);
    double gMin = stretchValues.get(1);
    double bMin = stretchValues.get(2);
    double rDiff = stretchValues.get(3);
    double gDiff = stretchValues.get(4);
    double bDiff = stretchValues.get(5);

    for (int i = 0; i < r.getN(); i++) {
      x = (float) ((r.getElement(i) - rMin) / rDiff);
      y = (float) ((g.getElement(i) - gMin) / gDiff);
      z = (float) ((b.getElement(i) - bMin) / bDiff);
      redBand.add(Math.min(Math.max(x, 0), 1));
      grnBand.add(Math.min(Math.max(y, 0), 1));
      bluBand.add(Math.min(Math.max(z, 0), 1));
      if (x > xmax && x != 65535) {
        xmax = x;
      }
      if (y > ymax && y != 65535) {
        ymax = y;
      }
      if (z > zmax && z != 65535) {
        zmax = z;
      }
    }
    xmax = (xmax > 1) ? 1 : xmax;
    ymax = (ymax > 1) ? 1 : ymax;
    zmax = (zmax > 1) ? 1 : zmax;
    float scale = 255 / (Math.max(xmax, Math.max(ymax, zmax)));
    for (int i = 0; i < redBand.size(); i++) {
      col = i % w;
      row = i / w;
      x = redBand.get(i);
      y = grnBand.get(i);
      z = bluBand.get(i);
      R = Math.min((int) (0.50 + scale * x), 255);
      G = Math.min((int) (0.50 + scale * y), 255);
      B = Math.min((int) (0.50 + scale * z), 255);

      rgb = (R << 16) | (G << 8) | B;
      result.setRGB(col, row, rgb);
    }
    return result;
  }

  ////////////////////////////////////////////////////////
  // Color-codes markers to be red, green and blue
  // Makes RGB legend items invisible
  ////////////////////////////////////////////////////////
  public void styleChart(LineChart<Number, Number> lc, XYChart.Series<Number, Number> rm,
      XYChart.Series<Number, Number> gm, XYChart.Series<Number, Number> bm) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method styleChart");
    for (Node n : lc.getChildrenUnmodifiable()) {
      if (n instanceof Legend) {
        for (Legend.LegendItem legendItem : ((Legend) n).getItems()) {
          if (legendItem.getText() == "Red") {
            legendItem.getSymbol().setVisible(false);
            legendItem.setText("");
          }
          if (legendItem.getText() == "Green") {
            legendItem.getSymbol().setVisible(false);
            legendItem.setText("");
          }
          if (legendItem.getText() == "Blue") {
            legendItem.getSymbol().setVisible(false);
            legendItem.setText("");
          }
        }
      }
    }
    rm.nodeProperty().get().setStyle("-fx-stroke: #FF0000;");
    gm.nodeProperty().get().setStyle("-fx-stroke: #00FF00;");
    bm.nodeProperty().get().setStyle("-fx-stroke: #0000FF;");
  }

  ///////////////////////////////////////////////////////
  // Inputs the (wavelength, intensity) to chart
  // Intensity must be less than one (to account for fillValues
  ///////////////////////////////////////////////////////
  public void inputData(List<Double> intensity, XYChart.Series<Number, Number> LCData,
      LineChart<Number, Number> lc) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method inputData");
    for (int i = 0; i < index.size(); i++) {
      if (intensity.get(i) < 65534) {
        LCData.getData().add(new XYChart.Data<Number, Number>(wavelength.get(i), intensity.get(i)));
      }
    }
    lc.getData().add(LCData);
  }

  ///////////////////////////////////////////////////////
  // Inputs the RGB markers into chart, vertical lines with length 1
  ///////////////////////////////////////////////////////
  public void inputMarkers(LineChart<Number, Number> lc, XYChart.Series<Number, Number> rm,
      XYChart.Series<Number, Number> gm, XYChart.Series<Number, Number> bm) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method inputMarkers");

    for (int i = 0; i < 2; i++) {
      rm.getData().add(new XYChart.Data<Number, Number>(wavelength.get(red), i));
      gm.getData().add(new XYChart.Data<Number, Number>(wavelength.get(green), i));
      bm.getData().add(new XYChart.Data<Number, Number>(wavelength.get(blue), i));
    }
    lc.getData().add(rm);
    lc.getData().add(gm);
    lc.getData().add(bm);
  }

  ////////////////////////////////////////////////////////
  // Scales line chart to revert x, y or both axes back to original ranges
  ////////////////////////////////////////////////////////
  public void resetScale(BorderPane borderTB, int axis) {

    NumberAxis x = null;
    NumberAxis y = null;
    if (axis == 0) {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Resetting X-axis");
      x = new NumberAxis();
      x.setAutoRanging(true);
      x.setForceZeroInRange(false);
      y = (NumberAxis) flc.getYAxis();
    }
    if (axis == 1) {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Resetting Y-axis");
      x = (NumberAxis) flc.getXAxis();
      y = new NumberAxis(0, 0.4, 0.05);
    }
    if (axis == 2) {
      JCATLog.getInstance().getLogger().log(Level.INFO, "Resetting both axes");
      x = new NumberAxis();
      x.setAutoRanging(true);
      x.setForceZeroInRange(false);
      y = new NumberAxis(0, 0.4, 0.05);
    }
    x.setLabel("Wavelength(nm)");
    y.setLabel("Corrected I/F");
    ObservableList<Series<Number, Number>> data = flc.getData();
    flc = new LineChart<Number, Number>(x, y);
    flc.getData().addAll(data);
    LineChart<Number, Number> newLC = flc;
    XYChart.Series<Number, Number> rm2 = frm, gm2 = fgm, bm2 = fbm;
    rm2.setName("Red");
    gm2.setName("Green");
    bm2.setName("Blue");
    newLC.setCreateSymbols(false);
    newLC.setAnimated(false);
    styleChart(newLC, rm2, gm2, bm2);

    borderTB.setBottom(newLC);
  }

  /////////////////////////////////////////////////////////////
  // Allows user to set the X-axis of the line chart with lower and upper bound
  /////////////////////////////////////////////////////////////
  public LineChart<Number, Number> setX() {
    JCATLog.getInstance().getLogger().log(Level.INFO,
        "Setting the bounds for X-axis to selected values");
    double lower = 0;
    double upper = 0;
    LineChart<Number, Number> newLC = null;
    TextInputDialog input1 = new TextInputDialog();
    input1.setTitle("Set X Scale");
    input1.setContentText("Input Lower Bound: ");
    Optional<String> result1 = input1.showAndWait();
    lower = Double.parseDouble(tidy(result1));
    if (result1.isPresent()) {
      JCATLog.getInstance().getLogger().log(Level.FINE, "Lower bound for X-axis set to " + lower);
      TextInputDialog input2 = new TextInputDialog();
      input2.setTitle("Set X Scale");
      input2.setContentText("Input Upper Bound: ");
      Optional<String> result2 = input2.showAndWait();
      upper = Double.parseDouble(tidy(result2));
      if (result2.isPresent()) {
        JCATLog.getInstance().getLogger().log(Level.FINE, "Lower bound for X-axis set to " + upper);
        NumberAxis x = new NumberAxis();
        x.setAutoRanging(false);
        x.setLowerBound(lower);
        x.setUpperBound(upper);
        x.setTickUnit((upper - lower) / 5);
        x.setLabel("Wavelength(nm)");
        NumberAxis y = (NumberAxis) flc.getYAxis();
        ObservableList<Series<Number, Number>> data = flc.getData();
        flc = new LineChart<Number, Number>(x, y);
        flc.getData().addAll(data);
        newLC = flc;
        XYChart.Series<Number, Number> rm2 = frm, gm2 = fgm, bm2 = fbm;
        rm2.setName("Red");
        gm2.setName("Green");
        bm2.setName("Blue");
        newLC.setCreateSymbols(false);
        newLC.setAnimated(false);
        styleChart(newLC, rm2, gm2, bm2);
      }
    }
    return newLC;
  }

  ///////////////////////////////////////////////////////////
  // Allows user to set Y-axis of line chart with lower and upper bound input
  ///////////////////////////////////////////////////////////
  public LineChart<Number, Number> setY() {
    JCATLog.getInstance().getLogger().log(Level.INFO,
        "Setting the bounds for Y-axis to selected values");
    double lower = 0;
    double upper = 0;
    LineChart<Number, Number> newLC = null;
    TextInputDialog input1 = new TextInputDialog();
    input1.setTitle("Set Y Scale");
    input1.setContentText("Input Lower Bound: ");
    Optional<String> result1 = input1.showAndWait();
    lower = Double.parseDouble(tidy(result1));
    if (result1.isPresent()) {
      JCATLog.getInstance().getLogger().log(Level.FINE, "Lower bound for Y-axis set to " + lower);
      TextInputDialog input2 = new TextInputDialog();
      input2.setTitle("Set Y Scale");
      input2.setContentText("Input Upper Bound: ");
      Optional<String> result2 = input2.showAndWait();
      upper = Double.parseDouble(tidy(result2));
      if (result2.isPresent()) {
        JCATLog.getInstance().getLogger().log(Level.FINE, "Upper bound for Y-axis set to " + upper);
        NumberAxis x = (NumberAxis) flc.getXAxis();
        NumberAxis y = new NumberAxis();
        y.setAutoRanging(false);
        y.setLowerBound(lower);
        y.setUpperBound(upper);
        y.setTickUnit((upper - lower) / 5);
        y.setLabel("Corrected I/F");
        ObservableList<Series<Number, Number>> data = flc.getData();
        flc = new LineChart<Number, Number>(x, y);
        flc.getData().addAll(data);
        newLC = flc;
        XYChart.Series<Number, Number> rm2 = frm, gm2 = fgm, bm2 = fbm;
        rm2.setName("Red");
        gm2.setName("Green");
        bm2.setName("Blue");
        newLC.setCreateSymbols(false);
        newLC.setAnimated(false);
        styleChart(newLC, rm2, gm2, bm2);
      }
    }
    return newLC;
  }

  /////////////////////////////////////////////////////////
  // Reverts spectral operations chart back to old x, y or both axes
  ////////////////////////////////////////////////////////
  public LineChart<Number, Number> autoScaleOP(int axis) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method autoScaleOP");
    NumberAxis x = null;
    NumberAxis y = null;
    if (axis == 0) {
      x = new NumberAxis();
      x.setAutoRanging(true);
      x.setForceZeroInRange(false);
      y = (NumberAxis) fslc.getYAxis();
    }
    if (axis == 1) {
      x = (NumberAxis) fslc.getXAxis();
      y = new NumberAxis();
      y.setAutoRanging(true);
      y.setForceZeroInRange(false);
    }
    if (axis == 2) {
      x = new NumberAxis();
      x.setAutoRanging(true);
      x.setForceZeroInRange(false);
      y = new NumberAxis();
      y.setAutoRanging(true);
      y.setForceZeroInRange(false);
    }
    x.setLabel("Wavelength(nm)");
    ObservableList<Series<Number, Number>> data = fslc.getData();
    fslc = new LineChart<Number, Number>(x, y);
    fslc.getData().addAll(data);
    LineChart<Number, Number> newopLC = fslc;
    newopLC.setCreateSymbols(false);
    newopLC.setLegendVisible(false);
    newopLC.setAnimated(false);
    return newopLC;
  }

  /////////////////////////////////////////////////////////////
  // Allows user to set the X-axis of the specOps chart with lower and upper bound
  /////////////////////////////////////////////////////////////
  public LineChart<Number, Number> setXScale() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method setXScale");
    double lower = 0;
    double upper = 0;
    LineChart<Number, Number> newopLC = null;
    TextInputDialog input1 = new TextInputDialog();
    input1.setTitle("Set X Scale");
    input1.setContentText("Input Lower Bound: ");
    Optional<String> result1 = input1.showAndWait();
    lower = Double.parseDouble(tidy(result1));
    if (result1.isPresent()) {
      TextInputDialog input2 = new TextInputDialog();
      input2.setTitle("Set X Scale");
      input2.setContentText("Input Upper Bound: ");
      Optional<String> result2 = input2.showAndWait();
      upper = Double.parseDouble(tidy(result2));
      if (result2.isPresent()) {
        NumberAxis x = new NumberAxis();
        x.setAutoRanging(false);
        x.setLowerBound(lower);
        x.setUpperBound(upper);
        x.setTickUnit((upper - lower) / 5);
        x.setLabel("Wavelength(nm)");
        NumberAxis y = (NumberAxis) fslc.getYAxis();
        ObservableList<Series<Number, Number>> data = fslc.getData();
        fslc = new LineChart<Number, Number>(x, y);
        fslc.getData().addAll(data);
        newopLC = fslc;
        newopLC.setCreateSymbols(false);
        newopLC.setLegendVisible(false);
        newopLC.setAnimated(false);
      }
    }
    return newopLC;
  }

  /////////////////////////////////////////////////////////////
  // Allows user to set the Y-axis of the specOps chart with lower and upper bound
  /////////////////////////////////////////////////////////////
  public LineChart<Number, Number> setYScale() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method setYScale");
    double lower = 0;
    double upper = 0;
    LineChart<Number, Number> newopLC = null;
    TextInputDialog input1 = new TextInputDialog();
    input1.setTitle("Set Y Scale");
    input1.setContentText("Input Lower Bound: ");
    Optional<String> result1 = input1.showAndWait();
    lower = Double.parseDouble(tidy(result1));
    if (result1.isPresent()) {
      TextInputDialog input2 = new TextInputDialog();
      input2.setTitle("Set Y Scale");
      input2.setContentText("Input Upper Bound: ");
      Optional<String> result2 = input2.showAndWait();
      upper = Double.parseDouble(tidy(result2));
      if (result2.isPresent()) {
        NumberAxis x = (NumberAxis) fslc.getXAxis();
        NumberAxis y = new NumberAxis();
        y.setAutoRanging(false);
        y.setLowerBound(lower);
        y.setUpperBound(upper);
        y.setTickUnit((upper - lower) / 5);
        ObservableList<Series<Number, Number>> data = fslc.getData();
        fslc = new LineChart<Number, Number>(x, y);
        fslc.getData().addAll(data);
        newopLC = fslc;
        newopLC.setCreateSymbols(false);
        newopLC.setLegendVisible(false);
        newopLC.setAnimated(false);
      }
    }
    return newopLC;
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  // Creates a list of 0's and 1's which represent bad and good bands,
  // respectively (based on TER wavelength table, NOT crism_bad_bands.pro)
  /////////////////////////////////////////////////////////////////////////////////////////
  public List<Integer> removeBadBands() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method removeBadBands");
    int good = 0;
    List<Integer> bbl = new ArrayList<Integer>(Collections.nCopies(wavelength.size(), 1));
    for (Double wv : wavelength) {
      if (Math.abs(wv - 65535.) > 0.1) {
        good++;
      }
    }
    if (good < 1) {
      Alert alert = new Alert(AlertType.INFORMATION);
      alert.setTitle("Error Message");
      alert.setHeaderText(null);
      alert.setContentText("BAD BANDS FAIL: no valid wavelengths");
      alert.showAndWait();

    } else {
      for (int i = 0; i < wavelength.size(); i++) {
        double j = wavelength.get(i);
        if (detector.get(i) == 1) {
          if (j < 433 || (j > 634 && j < 707) || j > 1013) {
            bbl.remove(i);
            bbl.add(i, 0);
          }
        }
        if (detector.get(i) == 0) {
          if (j < 1044 || (j > 2658 && j < 2796) || j > 3900) {
            bbl.remove(i);
            bbl.add(i, 0);
          }
        }
      }
    }
    return bbl;
  }

  ///////////////////////////////////////////////////////////////////
  // With the VS ID, returns the correct full correct ADR file
  // Checks the ID, then gets the most recent since adrNames are before TRDR
  // Gets the latest version
  // Used to determine upon load-in which files can be eventually used
  ///////////////////////////////////////////////////////////////////
  public String adrVS(String vsid) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method adrVS");
    List<String> corrIDList = new ArrayList<String>();
    List<Integer> sclkValList = new ArrayList<Integer>();
    for (int i = 0; i < adrNames.size(); i++) {
      String testID = adrNames.get(i).substring(15, 20);
      if (testID.equals(vsid)) {
        corrIDList.add(adrNames.get(i));
        sclkValList.add(Integer.parseInt(adrNames.get(i).substring(4, 13)));
      }
    }
    int maxsclk = 0;
    List<String> corrsclkList = new ArrayList<String>();
    for (int i = 0; i < sclkValList.size(); i++) {
      if (sclkValList.get(i) >= maxsclk) {
        maxsclk = sclkValList.get(i);
        corrsclkList.add(corrIDList.get(i));
      }
    }
    int maxversion = 0;
    int idx = 0;
    for (int i = 0; i < corrsclkList.size(); i++) {
      int version = Integer.parseInt(corrsclkList.get(i).substring(27, 28));
      if (version > maxversion) {
        maxversion = version;
        idx = i;
      }
    }
    String sclkRecent = corrsclkList.get(idx);
    return sclkRecent;
  }

  public BufferedImage stretchImage(BufferedImage inputImage) {
    BufferedImage outputImage = null;
    List<Double> stretchValues = null;
    if (perCStretch.isSelected()) {
      TextInputDialog input = new TextInputDialog();
      input.setTitle("Percentile Stretch");
      input.setContentText("Input Percentile (Value): ");
      Optional<String> result = input.showAndWait();
      double stretch = Double.parseDouble(tidy(result));
      if (result.isPresent()) {
        if (subStretch.isSelected()) {
          stretchValues = ImageUtils.getStretchValues(
              inputImage.getSubimage(visX, visY, visW, visH), STRETCH.LINEAR, stretch);
          outputImage = percentileSubStretch(inputImage, stretchValues);
        } else {
          outputImage = percentileStretch(inputImage, stretch);
        }
      }
    } else {
      if (subStretch.isSelected()) {
        if (linStretch.isSelected()) {

          stretchValues = ImageUtils
              .getStretchValues(inputImage.getSubimage(visX, visY, visW, visH), STRETCH.LINEAR, 0);
          outputImage = linSubStretch(inputImage, stretchValues);
        } else if (per1Stretch.isSelected()) {

          stretchValues = ImageUtils
              .getStretchValues(inputImage.getSubimage(visX, visY, visW, visH), STRETCH.LINEAR, 1);
          outputImage = percentileSubStretch(inputImage, stretchValues);
        } else if (per2Stretch.isSelected()) {

          stretchValues = ImageUtils
              .getStretchValues(inputImage.getSubimage(visX, visY, visW, visH), STRETCH.LINEAR, 2);
          outputImage = percentileSubStretch(inputImage, stretchValues);
        }
      } else {
        outputImage = (linStretch.isSelected()) ? linearStretch(inputImage) : outputImage;
        outputImage = (per1Stretch.isSelected()) ? percentileStretch(inputImage, 1) : outputImage;
        outputImage = (per2Stretch.isSelected()) ? percentileStretch(inputImage, 2) : outputImage;
      }
    }
    return outputImage;
  }
}
