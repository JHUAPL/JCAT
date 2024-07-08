package reader;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import util.JCATLog;

public class MRDR extends CRISMPDSImageNextGen {

  public MRDR(String filename, String wavelengthTable) {
    super(filename, wavelengthTable);
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating MRDR object");
  }

  public static void main(String[] args) {
    String basename = "t1463_mrrif_35n063_0256_3";

    String filename = basename + ".img";
    String wavelengthTable = basename.replaceAll("if", "wv") + ".tab";
    MRDR image = new MRDR(filename, wavelengthTable);

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
