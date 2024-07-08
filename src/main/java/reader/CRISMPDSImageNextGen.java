package reader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;

import definition.PDS.PDSObject;
import util.JCATLog;

public class CRISMPDSImageNextGen extends CRISMPDSImage {

  private DDR ddr;
  public LinkedHashMap<Integer, String> bandMapSU;

  public CRISMPDSImageNextGen(String filename) {

    this(filename, null);

  }

  public CRISMPDSImageNextGen(String filename, String ddrFilename) {
    super(filename);

    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating CRISMPDSImageNextGen object");

    JCATLog.getInstance().getLogger().log(Level.FINER, "Creating CRISMPDSImageNextGen object");
    String wavelengthTable =
        FilenameUtils.getFullPath(filename) + FilenameUtils.getName(filename).replaceAll("if", "wv")
            .replaceAll(".img", ".tab").replaceAll("IF", "WV").replaceAll(".IMG", ".TAB");

    if (FilenameUtils.getBaseName(filename).substring(22).toLowerCase().startsWith("ter")) {
      ddrFilename = filename.replaceAll("if", "de").replaceAll("j", "l").replaceAll("ter3", "ddr1")
          .replaceAll("IF", "DE").replaceAll("J", "L").replaceAll("TER3", "DDR1");
      ddr = (new File(ddrFilename).exists() ? new DDR(ddrFilename) : null);

      if (ddr != null) {
        int latIndex = ddr.findBandNumber("Latitude, areocentric, deg N");
        int lonIndex = ddr.findBandNumber("Longitude, areocentric, deg E");

        lat = new double[(int) ddr.getHeight()][(int) ddr.getWidth()];
        lon = new double[(int) ddr.getHeight()][(int) ddr.getWidth()];

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

        haveMapBounds = true;
      }

    }

    detector = new ArrayList<Integer>();

    if (FilenameUtils.getBaseName(filename).substring(15).toLowerCase().startsWith("if")) {
      List<WARecord> wrs = new ArrayList<>();
      bandMap = new LinkedHashMap<>();

      try (Reader in = new FileReader(wavelengthTable)) {

        JCATLog.getInstance().getLogger().log(Level.FINE, "Reading wavelengths from: " + filename);

        Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
        for (CSVRecord record : records) {
          detector.add(Integer.parseInt(record.get(0).trim()));
          int detector = Integer.parseInt(record.get(0).trim());
          int band = Integer.parseInt(record.get(1).trim());
          double wavelength = Double.valueOf(record.get(2).trim());
          double fwhm = Double.valueOf(record.get(3).trim());
          int badBand = Integer.parseInt(record.get(4).trim());
          wrs.add(new WARecord(detector, band, wavelength, fwhm, badBand));
        }
      } catch (IOException e) {

        JCATLog.getInstance().getLogger().log(Level.WARNING, "Cannot read wavelength file "
            + wavelengthTable.substring(wavelengthTable.lastIndexOf('\\') + 1));
        return;
      }
      for (int i = 0; i < wrs.size(); i++) {
        bandMap.put(i, wrs.get(i).getWavelength());
      }
    } else {
      // summary product routine, drop down list instead of sliders
      // need to work around bandMap routines
      bandMapSU = new LinkedHashMap<Integer, String>();
      for (int i = 0; i < bandNames.size(); i++) {
        bandMapSU.put(i, bandNames.get(i));
      }
    }

  }

  public List<Integer> getBandIndices() {

    if (bandMapSU == null)
      return super.getBandIndices();
    else
      return new ArrayList<Integer>(bandMapSU.keySet());
  }

  @Override
  protected void create() {
    if ((imgLabel != null) && imgLabel.isValidLabel()) {
      PDSObject imgObj = imgLabel.findObject("IMAGE");
      if (imgObj != null) {
        createImage(imgFilename, imgObj);
      }

      PDSObject mapObj = imgLabel.findObject("IMAGE_MAP_PROJECTION");
      if (mapObj != null) {
        maxLat = mapObj.getDouble("MAXIMUM_LATITUDE");
        minLat = mapObj.getDouble("MINIMUM_LATITUDE");
        westLon = mapObj.getDouble("WESTERNMOST_LONGITUDE");
        eastLon = mapObj.getDouble("EASTERNMOST_LONGITUDE");
        minLine = mapObj.getInt("LINE_FIRST_PIXEL");
        maxLine = mapObj.getInt("LINE_LAST_PIXEL");
        minSample = mapObj.getInt("SAMPLE_FIRST_PIXEL");
        maxSample = mapObj.getInt("SAMPLE_LAST_PIXEL");

        haveMapBounds = true;
      }

      if (haveMapBounds) {
        lat = new double[maxLine][maxSample];
        lon = new double[maxLine][maxSample];

        if (mapObj != null) {
          double degreesPerPixel = 1 / mapObj.getDouble("MAP_RESOLUTION");

          for (int row = minLine; row < maxLine; row++) {
            for (int col = minSample; col < maxSample; col++) {
              lat[row][col] = minLat + (row - minLine) * degreesPerPixel;
              lon[row][col] = westLon + (col - minSample) * degreesPerPixel;
            }
          }
        }
      }

    }
  }

}
