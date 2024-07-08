package reader;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import util.JCATLog;

public class DDR extends CRISMPDSImageFirstGen {

  public DDR(String filename) {
    super(filename);
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating DDR object");
  }

  @Override
  protected void create() {
    super.create();
  }

  @Override
  public int getNumBands() {
    return bandCount;
  }

  public static void main(String[] args) {
    String basename = "hrl0002422e_07_de182l_ddr1";

    String filename = basename + ".img";
    DDR image = new DDR(filename);

    System.out.printf("file %s\nwidth %d height %d numBands %d\n", filename, image.getWidth(),
        image.getHeight(), image.getNumBands());

    // for (int i = 0; i < image.getNumBands(); i++)
    // System.out.printf("band %d %s\n", i, image.bandNames.get(i));

    int band = 9;// elevation
    System.out.printf("Plotting %s\n", image.bandNames.get(band));
    BufferedImage bi = image.getQuickMonoImage(band);
    try {
      ImageIO.write(bi, "JPEG", new FileOutputStream(basename + ".jpg"));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
