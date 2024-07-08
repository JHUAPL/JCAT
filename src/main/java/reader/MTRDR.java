package reader;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import definition.PDS.PDSObject;
import util.JCATLog;

public class MTRDR extends CRISMPDSImageNextGen {

  public MTRDR(String filename, String wavelengthTable) {
    super(filename, wavelengthTable);
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating MTRDR object");
  }

  @Override
  protected void create() {
    super.create();

    if (haveMapBounds) {
      lat = new double[maxLine][maxSample];
      lon = new double[maxLine][maxSample];

      PDSObject mapObj = imgLabel.findObject("IMAGE_MAP_PROJECTION");
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

  public static void main(String[] args) {
    String basename = "frt000251c0_07_if165j_mtr3";

    String filename = basename + ".img";
    String wavelengthTable = basename.replaceAll("if", "wv") + ".tab";
    MTRDR image = new MTRDR(filename, wavelengthTable);

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
      System.out.printf("%d %d %f %f\n", i, bandIndex, wavelength, intensity);
    }

    int red = 150;
    int green = 300;
    int blue = 400;

    BufferedImage bi = image.getQuickColorImage(red, green, blue);
    try {
      ImageIO.write(bi, "JPEG", new FileOutputStream(basename + ".jpg"));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
