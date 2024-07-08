package reader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import definition.PDS.PDSObject;
import util.JCATLog;

public class TRDR extends CRISMPDSImageFirstGen {

  private DDR ddr;

  public TRDR(String filename) {
    this(filename, null);
  }

  public TRDR(String filename, String ddrFilename) {
    super(filename);

    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating TRDR object");

    this.ddr = (new File(ddrFilename).exists() ? new DDR(ddrFilename) : null);

    if (ddr != null) {
      int latIndex = ddr.findBandNumber("Latitude, areocentric, deg N");
      int lonIndex = ddr.findBandNumber("Longitude, areocentric, deg E");

      lat = new double[(int) ddr.getHeight()][(int) ddr.getWidth()];
      lon = new double[(int) ddr.getHeight()][(int) ddr.getWidth()];

      String sensor = imgLabel.getStringNoQuotes("MRO:SENSOR_ID").toLowerCase().trim();
      PDSObject fileObj = imgLabel.findObject("FILE");

      if (fileObj == null)
        return;

      PDSObject imgObj = fileObj.findObject("IMAGE");
      int bands = imgObj.getInt("BANDS");
      sensor = (sensor.equals("s")) ? Integer.toString(1) : sensor;
      sensor = (sensor.equals("l")) ? Integer.toString(0) : sensor;
      detector = (sensor.equals(Integer.toString(1)) || sensor.equals(Integer.toString(0)))
          ? new ArrayList<Integer>(Collections.nCopies(bands, Integer.parseInt(sensor)))
          : null;

      inaIndex = ddr.findBandNumber("INA at areoid, deg");

      bc = Integer.parseInt(imgLabel.getStringNoQuotes("PIXEL_AVERAGING_WIDTH").trim());
      bc = (bc == 1) ? 0 : bc;
      bc = (bc == 10) ? 3 : bc;
      bc = (bc == 2) ? 1 : bc;
      bc = (bc == 5) ? 2 : bc;

      wv = Integer.parseInt(imgLabel.getStringNoQuotes("MRO:WAVELENGTH_FILTER").trim());

      String tick = imgLabel.getStringNoQuotes("SPACECRAFT_CLOCK_START_COUNT");
      int p = tick.indexOf("/");
      sclk = Integer.parseInt(tick.substring(p + 1, 11));

      minLat = Double.MAX_VALUE;
      maxLat = -Double.MAX_VALUE;
      westLon = Double.MAX_VALUE;
      eastLon = -Double.MAX_VALUE;

      for (int row = 0; row < ddr.getHeight(); row++) {
        for (int col = 0; col < ddr.getWidth(); col++) {
          float thisLat = ddr.getPCFloatValue(row, col, latIndex);
          if (thisLat < 32768) {
            maxLat = Math.max(maxLat, thisLat);
            minLat = Math.min(minLat, thisLat);
            lat[row][col] = thisLat;
          }

          float thisLon = ddr.getPCFloatValue(row, col, lonIndex);
          if (thisLon < 32768) {
            eastLon = Math.max(eastLon, thisLon);
            westLon = Math.min(westLon, thisLon);
            lon[row][col] = thisLon;
            // check if we're crossing the 0 degree line
            if (eastLon - westLon > 180) {
              double tmp = eastLon;
              eastLon = westLon;
              westLon = tmp;
            }
          }
        }
      }

      // int x = 100;
      // int y = 100;
      // for (int band = 0; band < ddr.getNumBands(); band++) {
      // System.out.printf("%s %f\n", ddr.bandNames.get(band),
      // ddr.getPCFloatValue(x, y, band));
      // }

      haveMapBounds = true;
    } else {
      JCATLog.getInstance().getLogger().log(Level.WARNING,
          "Missing DDR file " + ddrFilename.substring(ddrFilename.lastIndexOf("\\") + 1));
    }

  }

  public boolean ddrExists() {
    return ddr != null;
  }

  @Override
  protected void create() {
    super.create();

    String startTime = imgLabel.getStringNoQuotes("SPACECRAFT_CLOCK_START_COUNT");
    if (startTime == null)
      throw new NullPointerException("No SPACECRAFT_CLOCK_START_COUNT in " + lblFilename);

    String[] parts = startTime.split("/");
    int partition = Integer.parseInt(parts[0]);
    double sclk = Double.parseDouble(parts[1]);

    String sensor = imgLabel.getStringNoQuotes("MRO:SENSOR_ID").toLowerCase();

    // System.out.printf("SCLK %d/%10.0f sensor %s\n", partition, sclk,
    // sensor);

    SWCDRLibrary swLib = SWCDRLibrary.getInstance();
    bandMap = swLib.getBandMap(sensor, partition, sclk);
  }

  public static void main(String[] args) {
    String basename = "hrl0002422e_07_if182l_trr3";
    basename = "frt000063cb_07_if164s_trr3";

    String filename = basename + ".img";
    TRDR image = new TRDR(filename);

    System.out.printf("file %s\nwidth %d height %d numBands %d\n", filename, image.getWidth(),
        image.getHeight(), image.getNumBands());

    int x = 100;
    int y = 100;
    List<Integer> bandIndices = image.getBandIndices();
    List<Double> wavelengths = image.getWavelengths();
    for (int i = 0; i < wavelengths.size(); i++) {
      int bandIndex = bandIndices.get(i);
      double wavelength = wavelengths.get(i);
      double intensity = image.getPCFloatValue(y, x, bandIndex);
      System.out.printf("%d %f %f\n", bandIndex, wavelength, intensity);
    }

    int red = 25;
    int green = 48;
    int blue = 65;
    BufferedImage bi = image.getQuickColorImage(red, green, blue);
    try {
      ImageIO.write(bi, "JPEG", new FileOutputStream(basename + ".jpg"));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
