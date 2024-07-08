package reader;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import definition.PDS.PDSObject;
import util.JCATLog;

public class ADR extends CRISMPDSImageFirstGen {

  public ADR(String filename) {
    super(filename);
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating ADR object");
    String tick = imgLabel.getStringNoQuotes("SPACECRAFT_CLOCK_START_COUNT");
    int p = tick.indexOf("/");
    sclk = Integer.parseInt(tick.substring(p + 1, 11));
  }

  public List<Double> getTrans(int col, List<Integer> gbBands) {
    List<Double> vstrans = new ArrayList<Double>();
    for (int i = 0; i < this.totalBands(); i++) {
      float a = this.getPCFloatValue(0, col, i); // first line is transmission spectrum
      if (gbBands.get(i) == 1) {
        vstrans.add(Double.valueOf(a));
      }
    }
    return vstrans;
  }

  public List<Double> getArt(int col, List<Integer> gbBands) {
    List<Double> vsart = new ArrayList<Double>();
    for (int i = 0; i < this.totalBands(); i++) {
      float a = this.getPCFloatValue(1, col, i); // second line is McGuire artifact
      if (gbBands.get(i) == 1) {
        vsart.add(Double.valueOf(a));
      }
    }
    return vsart;
  }

  public int totalBands() {
    PDSObject fileObj = imgLabel.findObject("FILE");
    PDSObject imgObj = fileObj.findObject("IMAGE");
    int bands = imgObj.getInt("BANDS");
    return bands;
  }

}
