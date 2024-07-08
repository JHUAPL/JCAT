package reader;

import java.util.LinkedHashMap;
import java.util.logging.Level;

import util.JCATLog;

/**
 * The WA level 4 CDR gives the actual center wavelength for each pixel, including smile.
 * 
 * @author nairah1
 *
 */
public class WACDR extends CRISMPDSImageFirstGen {

  public WACDR(String filename) {
    super(filename);
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating WACDR object");
  }

  @Override
  protected void create() {
    super.create();
    System.out.printf("%s: rowNumbers.size() = %d\n", lblFilename, rowNumbers.size());

    if ((rowNumbers != null) && (imageWidth > 0)) {
      int i, j, size;
      size = rowNumbers.size();

      bandMap = new LinkedHashMap<>();

      for (i = 0; i < size; i++) {
        double sum = 0;
        int count = 0;
        int midpt = (int) imageWidth / 2;
        int centerWidth = (int) (50 * imageWidth / 640.);
        for (j = midpt - centerWidth; j < midpt + centerWidth; j++) {
          double thisWavelength = getWavelengthFromFile(rowNumbers.get(i), j);

          // if (j == 270) System.out.println("i, j, wave = "+i+",
          // "+j+", "+thisWavelength+", centerWidth = "+centerWidth);

          if (thisWavelength > 1 && thisWavelength < 65535) {
            sum += thisWavelength;
            count++;
          }
        }

        bandMap.put(size - i - 1, sum / count);
      }
    }

  }

  //
  // retrieves the wavelength of a particular row number and
  // pixel column
  //
  protected double getWavelengthFromFile(int row, int col) {
    double result = -1.0;

    if (rowNumbers != null) {
      int size = rowNumbers.size();

      for (int i = 0; i < size; i++) {
        int rowNum = rowNumbers.get(i);
        if (rowNum == row) {
          result = getPCFloatValue(0, col, i);
          break;
        }
      }
    }

    return result;
  }

}
