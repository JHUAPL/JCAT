package reader;

import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import definition.PDS.PDSObject;
import util.JCATLog;

public class CRISMPDSImageFirstGen extends CRISMPDSImage {

  protected List<Integer> rowNumbers;

  public CRISMPDSImageFirstGen(String filename) {
    super(filename);
    JCATLog.getInstance().getLogger().log(Level.FINER,
        "Instantiating CRISMPDSImageFirstGen object");
  }

  @Override
  protected void create() {

    PDSObject fileObj = imgLabel.findObject("FILE");
    if (fileObj == null)
      throw new NullPointerException("No FILE object found in " + lblFilename);

    PDSObject imgObj = fileObj.findObject("IMAGE");
    if (imgObj == null)
      throw new NullPointerException("No IMAGE object found in " + lblFilename);

    createImage(imgFilename, imgObj);

    PDSObject rowObj = fileObj.findObject("ROWNUM_TABLE");
    if (rowObj != null) {
      int rows = rowObj.getInt("ROWS");

      if (rows > 0) {
        if (rowNumbers == null)
          rowNumbers = new Vector<Integer>(rows);

        int recBytes = fileObj.getInt("RECORD_BYTES");
        int numFileRecs = fileObj.getInt("FILE_RECORDS");
        long totalFileSize = recBytes * numFileRecs;

        long numImageRecs = (imageWidth * imageHeight * bandCount * dataBitCount / 8) / recBytes;

        long offset = numImageRecs * recBytes;

        while ((offset < totalFileSize) && (rowNumbers.size() < rows)) {
          int rowNum = getShortValue(offset);

          rowNumbers.add(rowNum);

          offset += 2;
        }
      }
    }
  }

}
