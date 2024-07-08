package reader;

import java.util.logging.Level;

import util.JCATLog;

public class WARecord implements Comparable<WARecord> {

  private int detector;
  private int band;
  private double wavelength;
  private double fwhm;
  private int badBand;

  /**
   * Spectrometer identifier; 0 = IR; 1 = VNIR
   * 
   * @return
   */
  public int getDetector() {
    return detector;
  }

  /**
   * detector row number (0-479)
   * 
   * @return
   */
  public int getBand() {
    return band;
  }

  /**
   * Standard sampling center wavelength in nm
   * 
   * @return
   */
  public double getWavelength() {
    return wavelength;
  }

  /**
   * Full Width Half Max in nm
   * 
   * @return
   */
  public double getFwhm() {
    return fwhm;
  }

  /**
   * Bad Band Identifier; 0 = BAD; 1 = GOOD
   * 
   * @return
   */
  public boolean isBadBand() {
    return badBand == 0;
  }

  public WARecord(int detector, int band, double wavelength, double fwhm, int badBand) {

    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating WARecord object");
    this.detector = detector;
    this.band = band;
    this.wavelength = wavelength;
    this.fwhm = fwhm;
    this.badBand = badBand;
  }

  @Override
  public int compareTo(WARecord o) {
    return Double.compare(wavelength, o.wavelength);
  }

}
