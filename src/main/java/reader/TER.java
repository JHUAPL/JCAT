package reader;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import util.JCATLog;

public class TER extends CRISMPDSImageNextGen {

  public TER(String filename, String wavelengthTable) {
    super(filename, wavelengthTable);
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating TER object");
  }

  public static void main(String[] args) {
    String basename = "hrl0002422e_07_if182j_ter3";

    String filename = basename + ".img";
    String wavelengthTable = basename.replaceAll("if", "wv") + ".tab";
    TER image = new TER(filename, wavelengthTable);

    System.out.printf("file %s\nwidth %d height %d numBands %d\n", filename, image.getWidth(),
        image.getHeight(), image.getNumBands());

    int x = 100;
    int y = 100;
    List<Double> wavelengths = image.getWavelengths();
    for (int i = 0; i < wavelengths.size(); i++) {
      double wavelength = wavelengths.get(i);
      double intensity = image.getPCFloatValue(y, x, i);
      System.out.printf("%d %f %f\n", i, wavelength, intensity);
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
