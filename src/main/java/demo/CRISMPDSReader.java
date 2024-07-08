package demo;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

import reader.CRISMPDSImage;
import reader.CRISMPDSImageNextGen;
import reader.TRDR;

public class CRISMPDSReader {

  public static void main(String[] args) {

    String imageFilename = "hrl0002422e_07_if182j_ter3";

    imageFilename = "hrl0002422e_07_if182l_trr3";

    // imageFilename = "frt000251c0_07_if165j_mtr3";

    String filename = imageFilename + ".img";

    CRISMPDSImage image = null;
    String basename = FilenameUtils.getBaseName(imageFilename);
    if (basename.substring(20, 21).equals("j")) {
      String wavelengthTable = imageFilename.replaceAll("if", "wv") + ".tab";
      image = new CRISMPDSImageNextGen(filename, wavelengthTable);
    } else {
      String ddrFile = imageFilename.replaceAll("if", "de").replaceAll("trr3", "ddr1") + ".img";
      image = new TRDR(filename, ddrFile);
    }

    System.out.printf("file %s\nwidth %d height %d numBands %d\n", filename, image.getWidth(),
        image.getHeight(), image.getNumBands());

    if (image.haveMapBounds()) {
      System.out.printf("min, max lat %f, %f (deg)\n", image.getMinLatDegrees(),
          image.getMaxLatDegrees());
      System.out.printf("west, east lon %f, %f (deg)\n", image.getWestLonDegrees(),
          image.getEastLonDegrees());
      System.out.printf("min, max line %d, %d\n", image.getMinLine(), image.getMaxLine());
      System.out.printf("min, max sample %d, %d\n", image.getMinSample(), image.getMaxSample());
    }

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
