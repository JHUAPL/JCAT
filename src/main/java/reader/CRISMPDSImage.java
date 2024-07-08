package reader;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import definition.PDS.PDSImage;
import definition.PDS.PDSLabel;
import util.JCATLog;

public abstract class CRISMPDSImage extends PDSImage {
  protected PDSLabel imgLabel = null;
  protected PDSLabel waLabel = null;
  protected String imgFilename = "";
  protected String lblFilename = "";

  protected LinkedHashMap<Integer, Double> bandMap;

  protected int numBands;
  protected double[] bands;

  protected double westLon;
  protected double eastLon;
  protected double maxLat;
  protected double minLat;
  protected int minLine;
  protected int maxLine;
  protected int minSample;
  protected int maxSample;

  protected double[][] lat;
  protected double[][] lon;
  protected int inaIndex;
  protected int bc;
  protected int wv;
  protected int sclk;
  protected List<Integer> detector;

  public int getsclk() {
    return sclk;
  }

  public int getINA() {
    return inaIndex;
  }

  public int getBC() {
    return bc;
  }

  public int getWV() {
    return wv;
  }

  protected boolean haveMapBounds;

  public double getWestLonDegrees() {
    return westLon;
  }

  public double getMaxLatDegrees() {
    return maxLat;
  }

  public double getEastLonDegrees() {
    return eastLon;
  }

  public double getMinLatDegrees() {
    return minLat;
  }

  public int getMinLine() {
    return minLine;
  }

  public int getMaxLine() {
    return maxLine;
  }

  public int getMinSample() {
    return minSample;
  }

  public int getMaxSample() {
    return maxSample;
  }

  public boolean haveMapBounds() {
    return haveMapBounds;
  }

  public double getLat(int row, int col) {
    if (haveMapBounds && (row < lon.length && col < lon[row].length))
      return lat[row][col];
    else
      return Double.NaN;
  }

  public double getLon(int row, int col) {

    if (haveMapBounds && (row < lon.length && col < lon[row].length))
      return lon[row][col];
    else
      return Double.NaN;
  }

  public CRISMPDSImage(String filename) {

    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating CRISMPDSImage object");
    haveMapBounds = false;

    String name = filename.trim();

    if (name.toUpperCase().endsWith(".IMG")) {

      imgFilename = name;
      lblFilename = FilenameUtils.getFullPath(name) + FilenameUtils.getBaseName(name);

      if (name.endsWith(".IMG"))
        lblFilename += ".LBL";
      else
        lblFilename += ".lbl";

      //

    } else if (name.toUpperCase().endsWith(".LBL")) {
      // imgLabel = new PDSLabel(name);

      lblFilename = name;
      imgFilename = FilenameUtils.getFullPath(name) + FilenameUtils.getBaseName(name);

      if (name.endsWith(".LBL"))
        imgFilename += ".IMG";
      else
        imgFilename += ".img";
    }

    JCATLog.getInstance().getLogger().log(Level.INFO, "Reading Label file from: " + lblFilename);
    imgLabel = new PDSLabel(lblFilename);

    if (!imgLabel.isValidLabel()) {
      String s = "Invalid/missing label file: " + FilenameUtils.getName(lblFilename);
      JCATLog.getInstance().getLogger().log(Level.WARNING, s);
    } else
      create();

  }

  abstract protected void create();

  public double getWavelength(int band) {
    return bandMap.get(band);
  }

  public List<Integer> getDetector() {
    return detector;
  }

  public List<Integer> getBandIndices() {
    return new ArrayList<Integer>(bandMap.keySet());
  }

  public List<Double> getWavelengths() {
    List<Double> wavelengths = new ArrayList<>(bandMap.values());
    return wavelengths;
  }

  public int getNumBands() {
    return bandMap.keySet().size();
  }

  public double[][] getBandData(int bandIndex) {
    int w = (int) getWidth();
    int h = (int) getHeight();
    double[][] array = new double[w][h];

    for (int row = 0; row < h; row++) {
      for (int col = 0; col < w; col++) {
        array[col][row] = getPCFloatValue(row, col, bandIndex);
      }
    }
    return array;
  }

  public BufferedImage getQuickMonoImage(int bandIndex) {

    BufferedImage result = null;

    int w = (int) getWidth();
    int h = (int) getHeight();

    if ((w > 0) && (h > 0)) {

      DescriptiveStatistics xStats = new DescriptiveStatistics();

      result = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);

      Graphics gfx = result.getGraphics();
      gfx.setColor(Color.black);
      gfx.fillRect(0, 0, w, h);

      for (int row = 0; row < h; row++) {
        for (int col = 0; col < w; col++) {
          float x = getPCFloatValue(row, col, bandIndex);
          if (x > 32768)
            continue;
          xStats.addValue(x);
        }
      }

      final double minVal = xStats.getMin();
      final double maxVal = xStats.getMax();
      for (int row = 0; row < h; row++) {
        for (int col = 0; col < w; col++) {
          float x = getPCFloatValue(row, col, bandIndex);
          int R = (int) (255 * (x - minVal) / (maxVal - minVal));
          result.setRGB(col, row, ((R << 16) | (R << 8) | R));
        }
      }
    }

    return result;
  }

  public BufferedImage getQuickColorImage(int red, int green, int blue) {

    BufferedImage result = null;

    int w = (int) getWidth();
    int h = (int) getHeight();

    int row, col, rgb;
    float x, y, z;// , v1, v2, v3;
    int R, G, B;

    cacheEntireImage();

    if ((w > 0) && (h > 0)) {

      DescriptiveStatistics xStats = new DescriptiveStatistics();
      DescriptiveStatistics yStats = new DescriptiveStatistics();
      DescriptiveStatistics zStats = new DescriptiveStatistics();

      result = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);

      Graphics gfx = result.getGraphics();
      gfx.setColor(Color.black);
      gfx.fillRect(0, 0, w, h);

      ArrayList<Float> redBand = new ArrayList<>();
      ArrayList<Float> grnBand = new ArrayList<>();
      ArrayList<Float> bluBand = new ArrayList<>();

      for (row = 0; row < h; row++) {
        for (col = 0; col < w; col++) {
          x = getPCFloatValue(row, col, red);
          if (x != 65535)
            xStats.addValue(x);
          redBand.add(x);
        }
      }

      for (row = 0; row < h; row++) {
        for (col = 0; col < w; col++) {
          y = getPCFloatValue(row, col, green);
          if (y != 65535)
            yStats.addValue(y);
          grnBand.add(y);
        }
      }

      for (row = 0; row < h; row++) {
        for (col = 0; col < w; col++) {
          z = getPCFloatValue(row, col, blue);
          if (z != 65535)
            zStats.addValue(z);
          bluBand.add(z);
        }
      }

      JCATLog.getInstance().getLogger().log(Level.FINE, "red stats:\n" + xStats);
      JCATLog.getInstance().getLogger().log(Level.FINE, "green stats:\n" + yStats);
      JCATLog.getInstance().getLogger().log(Level.FINE, "blue stats:\n" + zStats);

      final double xMin = xStats.getMin();
      final double yMin = yStats.getMin();
      final double zMin = zStats.getMin();
      double scale = 255 / (Math.max(xStats.getMax() - xMin,
          Math.max(yStats.getMax() - yMin, zStats.getMax() - zMin)));
      float min = (float) Math.min(xStats.getMin(), Math.min(yStats.getMin(), zStats.getMin()));

      for (int i = 0; i < redBand.size(); i++) {
        col = i % w;
        row = i / w;
        x = redBand.get(i) - min;
        y = grnBand.get(i) - min;
        z = bluBand.get(i) - min;
        R = Math.min((int) (0.50 + scale * x), 255);
        G = Math.min((int) (0.50 + scale * y), 255);
        B = Math.min((int) (0.50 + scale * z), 255);

        rgb = (R << 16) | (G << 8) | B;
        result.setRGB(col, row, rgb);
      }
    }
    return result;
  }

  public BufferedImage getQuickColorImage(float[][] redBand, float[][] grnBand, float[][] bluBand) {

    BufferedImage result = null;

    int w = (int) getWidth();
    int h = (int) getHeight();

    int rgb;
    float x, y, z;// , v1, v2, v3;
    int R, G, B;

    cacheEntireImage();

    if ((w > 0) && (h > 0)) {

      DescriptiveStatistics xStats = new DescriptiveStatistics();
      DescriptiveStatistics yStats = new DescriptiveStatistics();
      DescriptiveStatistics zStats = new DescriptiveStatistics();

      for (int row = 0; row < h; row++) {
        for (int col = 0; col < w; col++) {
          x = redBand[row][col];
          if (x != 65535)
            xStats.addValue(x);
        }
      }

      for (int row = 0; row < h; row++) {
        for (int col = 0; col < w; col++) {
          y = grnBand[row][col];
          if (y != 65535)
            yStats.addValue(y);
        }
      }

      for (int row = 0; row < h; row++) {
        for (int col = 0; col < w; col++) {
          z = bluBand[row][col];
          if (z != 65535)
            zStats.addValue(z);
        }
      }

      JCATLog.getInstance().getLogger().log(Level.FINE, "red stats:\n" + xStats);
      JCATLog.getInstance().getLogger().log(Level.FINE, "green stats:\n" + yStats);
      JCATLog.getInstance().getLogger().log(Level.FINE, "blue stats:\n" + zStats);

      result = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);

      Graphics gfx = result.getGraphics();
      gfx.setColor(Color.black);
      gfx.fillRect(0, 0, w, h);

      final double xMin = xStats.getMin();
      final double yMin = yStats.getMin();
      final double zMin = zStats.getMin();
      double scale = 255 / (Math.max(xStats.getMax() - xMin,
          Math.max(yStats.getMax() - yMin, zStats.getMax() - zMin)));
      float min = (float) Math.min(xStats.getMin(), Math.min(yStats.getMin(), zStats.getMin()));

      for (int row = 0; row < h; row++) {
        for (int col = 0; col < w; col++) {

          x = redBand[row][col] - min;
          y = grnBand[row][col] - min;
          z = bluBand[row][col] - min;
          R = Math.min((int) (0.50 + scale * x), 255);
          G = Math.min((int) (0.50 + scale * y), 255);
          B = Math.min((int) (0.50 + scale * z), 255);

          rgb = (R << 16) | (G << 8) | B;
          result.setRGB(col, row, rgb);

        }
      }
    }
    return result;

  }

  public String getImgname() {
    return imgFilename;
  }

}
