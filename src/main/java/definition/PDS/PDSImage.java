package definition.PDS;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;

import util.JCATLog;

public class PDSImage {
  public enum ImageDataTypes {
    IntegerType, ShortType, ByteType, FloatType, DoubleType, UnsignedShortType, UnsignedIntType, UnknownDataType
  };

  public enum ImageBandTypes {
    BandInterleaved, BandSequential, LineInterleaved, LineSequential, UnknownBandType
  };

  public enum ImageByteOrderTypes {
    Intel, NonIntel, UnknownByteOrderType
  };

  public static String linesKey = "LINES";
  public static String samplesKey = "LINE_SAMPLES";
  public static String sampleTypeKey = "SAMPLE_TYPE";
  public static String sampleBitsKey = "SAMPLE_BITS";
  public static String bandCountKey = "BANDS";
  public static String bandNameKey = "BAND_NAME";
  public static String bandStorageKey = "BAND_STORAGE_TYPE";

  public static int typicalPageSize = 4096;
  public static int smallPageSize = 512;

  public int imageDataOffset = 0;

  public long imageWidth = 0;
  public long imageHeight = 0;
  public int dataBitCount = 8;
  public String dataTypeString = "";

  public String bandStorageType = "";
  public int bandCount = 1;
  public Vector<String> bandNames = new Vector<String>();

  public boolean unsignedFlag = false;
  public boolean pcFlag = false;

  public ImageDataTypes dataType = ImageDataTypes.ByteType;
  public ImageBandTypes bandType = ImageBandTypes.BandSequential;
  public ImageByteOrderTypes byteOrderType = ImageByteOrderTypes.NonIntel;

  public String filename = null;
  // public DataInputStream fileDataInput = null;
  // public RandomAccessFile fileDataInput = null;
  public BlockFile imgBlockFile = null;
  public int pdsBlockSize = 4096;
  public PDSLabel pdsLabel = null;
  public boolean validImage = false;

  public PDSObject imgObject = null;

  public PDSImage() {
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating PDSObject object");
  }
  // creates object

  public PDSImage(String imgFilename, PDSObject imgObj) {
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating PDSObject object");
    createImage(imgFilename, imgObj);
  }
  // creates object & executes createImage method from explicit parameters

  public PDSImage(String imgFilename, PDSObject imgObj, int bufferSize) {
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating PDSObject object");
    pdsBlockSize = bufferSize;
    createImage(imgFilename, imgObj);
  }
  // creates object & executes createImage method from first 2 explicit
  // parameters
  // sets size of image block

  public PDSImage(String imgFilename) {
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating PDSObject object");
  }
  // creates object & does nothing with filename

  public PDSImage(String imgFilename, long w, long h, int bitsPerPixel) {
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating PDSObject object");
    createImage(imgFilename, w, h, bitsPerPixel);
  }
  // creates object & executes createImage method w/ name, width, height, and
  // bits per pixel

  public void cacheEntireImage() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method cacheEntireImage ");
    if (imgBlockFile != null) {
      try {
        // imgBlockFile.readEntireFile();
        imgBlockFile.openFile();
      } catch (IOException ioex) {
        System.out.println("PDSImage.cacheEntireImage: " + ioex);
        ioex.printStackTrace();
      }
    }
  }
  // if a blockfile exists, and it won't open, display the error

  public void createImage(String imgFilename, PDSObject imgObj, int bufferSize) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method createImage 3 parameters ");
    pdsBlockSize = bufferSize;
    createImage(imgFilename, imgObj);
  }
  // runs the create image command but also sets PDSblocksize as buffersize

  public void createImage(String imgFilename, PDSObject imgObj) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method createImage 2 parameters ");
    if (imgObj != null) // if imgObj exists run the following
    {
      if (imgObj.hasParameter(PDSImage.linesKey) && imgObj.hasParameter(PDSImage.samplesKey)
          && imgObj.hasParameter(PDSImage.sampleTypeKey)
          && imgObj.hasParameter(PDSImage.sampleBitsKey)) {
        imageHeight = imgObj.getInt(PDSImage.linesKey);
        imageWidth = imgObj.getInt(PDSImage.samplesKey);
        dataTypeString = imgObj.getString(PDSImage.sampleTypeKey);
        dataBitCount = imgObj.getInt(sampleBitsKey);
        // reads LINES, LINE_SAMPLES, SAMPLE_TYPE & SAMPLE_BITS

        if (dataTypeString.contains("UNSIGNED"))
          unsignedFlag = true;
        // if there the sample type is unsigned, set the flag
        if (dataTypeString.contains("PC"))
          byteOrderType = ImageByteOrderTypes.Intel;
        // if the data type contains PC set byte order type to Intel

        if (dataTypeString.contains("INTEGER")) {
          if (dataBitCount == 16)
            dataType = ImageDataTypes.ShortType;
          else if (dataBitCount == 8)
            dataType = ImageDataTypes.ByteType;
          else if (dataBitCount == 32)
            dataType = ImageDataTypes.IntegerType;
        } // if the SAMPLE_TYPE has INTEGER, it finds out which type
        // using SAMPLE_BITS and then sets data type
        else if (dataTypeString.contains("REAL")) {
          if (dataBitCount == 32)
            dataType = ImageDataTypes.FloatType;
          else if (dataBitCount == 64)
            dataType = ImageDataTypes.DoubleType;
        } // if the SAMPLE_TYPE contains REAL, the datatype is set
        // according to SAMPLE_BITS
        else if (dataTypeString.contains("DOUBLE")) {
          dataType = ImageDataTypes.DoubleType;
        } // if the SAMPLE_TYPE is double, sets datatype to double

        if (imgObj.hasParameter(PDSImage.bandCountKey)) // tests for
        // BANDS in lbl
        // file
        {
          bandCount = imgObj.getInt(PDSImage.bandCountKey);
          // sets bandCount to whatever follows BANDS in lbl file
          String storageType = imgObj.getString(PDSImage.bandStorageKey);
          // sets storagetype to BAND_STORAGE_TYPE in lbl file
          if (storageType.contains("BAND_SEQUENTIAL"))
            bandType = ImageBandTypes.BandSequential; // sets as
          // BandSequential
          // if so
          else if (storageType.contains("LINE_INTERLEAVED"))
            bandType = ImageBandTypes.LineInterleaved; // sets as
          // LineInterleaved
          // if so
          else
            bandType = ImageBandTypes.BandInterleaved; // sets as
          // BandInterleaved
          // if so

          if (imgObj.hasParameter(PDSImage.bandNameKey)) // does stuff
          // if there
          // is a band
          // name, but
          // I don't
          // have an
          // LBL with
          // one
          // sooo...
          {
            String bandString =
                imgObj.getString(PDSImage.bandNameKey).replace('(', ' ').replace(')', ' ');
            // split on ", then strip off the leading quote
            String bandSplit[] = bandString.split("\",");
            for (int i = 0; i < bandSplit.length; i++)
              bandNames.add(bandSplit[i].replace("\"", ""));
          }
        }

        File f = new File(imgFilename);
        // creates a File obj named f with the directory path specified
        if (f.exists()) // if that file exists do the following
        {
          validImage = true;
          // it is a valid image
          filename = new String(imgFilename);
          // a string with the filename is created
          openImageFile();
          // opens the image file
          if (PDSLabel.isLabelFile(imgFilename)) // if a label file
          // exists with
          // filename
          {
            // RECORD_TYPE = FIXED_LENGTH
            // RECORD_BYTES = 2880
            // FILE_RECORDS = 1922
            // LABEL_RECORDS = 2

            PDSLabel lbl = new PDSLabel(imgFilename);

            int recBytes = lbl.getInt("RECORD_BYTES");
            int lblRecs = lbl.getInt("LABEL_RECORDS");
            //
            imageDataOffset = recBytes * lblRecs;
            // set a new image data offset
          }
        }
      }
    }
  }
  //
  // creates an image with the provided information
  //

  public void createImage(String imgFilename, long w, long h, int bitsPerPixel) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method createImage ");
    File f = new File(imgFilename);
    if (f.exists()) {
      filename = new String(imgFilename);
      long fileSize = f.length();
      long calculatedFileSize = w * h;

      openImageFile();

      switch (bitsPerPixel) {
        case 8:
          dataType = ImageDataTypes.ByteType;
          break;
        case 16:
          dataType = ImageDataTypes.ShortType;
          break;
        case 32:
          dataType = ImageDataTypes.IntegerType;
          break;
        case 64:
          dataType = ImageDataTypes.DoubleType;
          break;
        default:
          dataType = ImageDataTypes.ByteType;
          break;
      }
      // breaks image file up by pixel according to the datatype specified

      switch (dataType) {
        case IntegerType:
          calculatedFileSize *= 4;
          break;
        case ShortType:
          calculatedFileSize *= 2;
          break;
        case FloatType:
          calculatedFileSize *= 4;
          break;
        case DoubleType:
          calculatedFileSize *= 8;
          break;
        case ByteType:
        default:
          break;
      }
      // breaks up data type based on calculated file size

      if (calculatedFileSize > 0)// && (calculatedFileSize == fileSize))
      {
        imageWidth = w;
        imageHeight = h;
        validImage = true;
      } // tests to make sure file size is positive
      else if (fileSize > calculatedFileSize) // if actual filesize is
      // bigger than calculated
      // size
      {
        // image data may be preceded by a label, so first we want to
        // decode the label
        pdsLabel = new PDSLabel(imgFilename);
        if (pdsLabel.isValidLabel()) {
          int recordBytes = pdsLabel.getInt("RECORD_BYTES");
          int numRecords = pdsLabel.getInt("FILE_RECORDS");
          PDSObject imgObj = pdsLabel.findObject("IMAGE");

          if (imgObj != null) {
            int numLines = imgObj.getInt("LINES");
            int samples = imgObj.getInt("LINE_SAMPLES");
            int sampleBits = imgObj.getInt("SAMPLE_BITS");

            if ((sampleBits == 0) && (samples != 0)) {
              sampleBits = recordBytes / samples;
            }

            imageWidth = samples;
            imageHeight = numLines;
            imageDataOffset = (numRecords - numLines) * recordBytes;

            validImage = true;

            switch (sampleBits) {
              case 8:
                dataType = ImageDataTypes.ByteType;
                break;
              case 16:
                dataType = ImageDataTypes.ShortType;
                break;
              case 32:
                dataType = ImageDataTypes.IntegerType;
                break;
              case 64:
                dataType = ImageDataTypes.DoubleType;
                break;
            }
          }
        }
      }
    }
  }
  //
  // this method enables the retrieval of a band index from a name
  //

  public int findBandNumber(String name) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method findBandNumber ");
    int result = -1;

    int size = bandNames.size();

    String upperName = name.trim().toUpperCase();

    for (int i = 0; (i < size) && (result < 0); i++) {
      String itemName = bandNames.get(i).trim().toUpperCase();
      if (upperName.equals(itemName)) {
        result = i;
      }
    }

    return result;
  }

  public boolean isValidImage() {
    return validImage;
  }

  public long getWidth() {
    return imageWidth;
  }

  public long getHeight() {
    return imageHeight;
  }

  public boolean openImageFile() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method openImageFile ");
    boolean result = false;

    if (imgBlockFile != null) {
      result = true;
    } else {
      try {
        // fileDataInput = new DataInputStream(new
        // FileInputStream(filename));
        if (filename != null)
          imgBlockFile = new BlockFile(filename, pdsBlockSize);
        // fileDataInput.seek(imageDataOffset);
        result = true;
      } catch (Exception e) {
        System.out.println("PDSImage.openImageFile: " + e);
        e.printStackTrace();
        System.exit(0);
      }
    }

    return result;
  }

  public void closeImageFile() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method closeImageFile ");
    if (imgBlockFile != null) {
      try {
        imgBlockFile.closeFile();
      } catch (Exception e) {
        System.out.println("PDSImage.closeImageFile: " + e);
        e.printStackTrace();
      }

      imgBlockFile = null;
    }
  }

  public byte readByte() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readByte ");
    byte result = 0;

    if (openImageFile()) {
      try {
        result = imgBlockFile.readByte(imgBlockFile.blockOffset);
      } catch (IOException e) {
        System.out.println("PDSImage.readByte: " + e);
        e.printStackTrace();
      }
    }

    return result;
  }

  public short readShort() {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readShort ");
    short result = 0;

    if (openImageFile()) {
      try {
        result = imgBlockFile.readShort(imgBlockFile.blockOffset);
      } catch (IOException e) {
        System.out.println("PDSImage.readByte: " + e);
        e.printStackTrace();
      }
    }

    return result;
  }
  //
  // reading a byte from the file given the zero-based row and column indices
  //

  public byte getByteValue(int row, int col) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getByteValue 2 parameters ");
    byte result = 0;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        // long offset = (row * imageWidth + col) * pixelSize +
        // imageDataOffset;
        long offset = (row * imageWidth * bandCount + col) * pixelSize + imageDataOffset;

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readByte((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }

      }
    }

    return result;
  }
  //
  // reading a short from the file given the zero-based row and column indices
  //

  public short getShortValue(int row, int col) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getShortValue 2 parameters ");
    short result = 0;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        // long offset = (row * imageWidth + col) * pixelSize +
        // imageDataOffset;
        long offset = (row * imageWidth * bandCount + col) * pixelSize + imageDataOffset;

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readShort((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }
      }
    }

    return result;
  }

  //
  // reading a short from the file given the zero-based row and column indices
  //
  public short getShortValue(long offset) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getShortValue 1 parameter ");
    short result = 0;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        // long pixelSize = getPixelSize();

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readShort((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }
      }
    }

    return result;
  }
  //
  // reading an int from the file given the zero-based row and column indices
  //

  public int getIntValue(int row, int col) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getIntValue 2 parameters ");
    int result = 0;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        // long offset = (row * imageWidth + col) * pixelSize +
        // imageDataOffset;
        long offset = (row * imageWidth * bandCount + col) * pixelSize + imageDataOffset;

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readInt((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }
      }
    }

    return result;
  }
  //
  // reading a float from the file given the zero-based row and column indices
  //

  public float getFloatValue(int row, int col) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getFloatValue 2 parameters ");
    float result = 0.0f;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        // long offset = (row * imageWidth + col) * pixelSize +
        // imageDataOffset;
        long offset = (row * imageWidth * bandCount + col) * pixelSize + imageDataOffset;

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readFloat((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }
      }
    }

    return result;
  }
  //
  // reading a double from the file given the zero-based row and column
  // indices
  //

  public double getDoubleValue(int row, int col) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getDoubleValue 2 parameters ");
    double result = 0.0;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        // long offset = (row * imageWidth + col) * pixelSize +
        // imageDataOffset;
        long offset = (row * imageWidth * bandCount + col) * pixelSize + imageDataOffset;

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readDouble((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }
      }
    }

    return result;
  }
  //
  // reading a byte from the file given the zero-based row, column, and band
  // indices
  //

  public byte getByteValue(int row, int col, int band) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getByteValue 3 parameters ");
    byte result = 0;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        long bandSize = (imageWidth * imageHeight) * pixelSize;
        long offset = bandSize * band + (row * imageWidth + col) * pixelSize + imageDataOffset;
        if (bandType == ImageBandTypes.BandInterleaved)
          offset = ((row * imageWidth + col) * bandCount + band) * pixelSize + imageDataOffset;

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readByte((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }

      }
    }

    return result;
  }
  //
  // reading a short from the file given the zero-based row, column, and band
  // indices
  //

  public short getShortValue(int row, int col, int band) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getShortValue 3 parameters ");
    short result = 0;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        long bandSize = (imageWidth * imageHeight) * pixelSize;
        long offset = bandSize * band + (row * imageWidth + col) * pixelSize + imageDataOffset;
        if (bandType == ImageBandTypes.BandInterleaved)
          offset = ((row * imageWidth + col) * bandCount + band) * pixelSize + imageDataOffset;

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readShort((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }
      }
    }

    return result;
  }
  //
  // reading an integer from the file given the zero-based row, column, and
  // band indices
  //

  public int getIntValue(int row, int col, int band) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getIntValue 3 parameters ");
    int result = 0;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        long bandSize = (imageWidth * imageHeight) * pixelSize;
        long offset = bandSize * band + (row * imageWidth + col) * pixelSize + imageDataOffset;
        if (bandType == ImageBandTypes.BandInterleaved)
          offset = ((row * imageWidth + col) * bandCount + band) * pixelSize + imageDataOffset;

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readInt((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }
      }
    }

    return result;
  }
  //
  // reading a float from the file given the zero-based row, column, and band
  // indices
  //

  public float getFloatValue(int row, int col, int band) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getFloatValue 3 parameters ");
    float result = 0.0f;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        long bandSize = (imageWidth * imageHeight) * pixelSize;
        long offset = bandSize * band + (row * imageWidth + col) * pixelSize + imageDataOffset;
        if (bandType == ImageBandTypes.BandInterleaved) {
          // offset = row * imageWidth * pixelSize * bandCount + band
          // * imageWidth * pixelSize + col * pixelSize;
          offset = ((row * bandCount + band) * imageWidth + col) * pixelSize;
          // offset = ((row * imageWidth + col) * bandCount + band) *
          // pixelSize + imageDataOffset;
        } else if (bandType == ImageBandTypes.LineInterleaved) {
          // long lineSize = imageWidth * pixelSize;
          // offset = row * bandCount * lineSize + band*lineSize + col
          // * pixelSize;
          // offset = row * bandCount * imageWidth * pixelSize + band
          // * imageWidth * pixelSize + col * pixelSize;
          offset = (row * bandCount * imageWidth + band * imageWidth + col) * pixelSize;
        }

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readFloat((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }
      }
    }

    return result;
  }
  //
  // reading a float from the file given the zero-based row, column, and band
  // indices
  //

  public float getPCFloatValue(int row, int col, int band) {

    float result = 0.0f;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        long bandSize = (imageWidth * imageHeight) * pixelSize;
        long offset = bandSize * band + (row * imageWidth + col) * pixelSize + imageDataOffset;
        if (bandType == ImageBandTypes.BandInterleaved) {
          // offset = row * imageWidth * pixelSize * bandCount + band
          // * imageWidth * pixelSize + col * pixelSize;
          offset = ((row * bandCount + band) * imageWidth + col) * pixelSize;
          // offset = ((row * imageWidth + col) * bandCount + band) *
          // pixelSize + imageDataOffset;
        } else if (bandType == ImageBandTypes.LineInterleaved) {
          // long lineSize = imageWidth * pixelSize;
          // offset = row * bandCount * lineSize + band*lineSize + col
          // * pixelSize;
          // offset = row * bandCount * imageWidth * pixelSize + band
          // * imageWidth * pixelSize + col * pixelSize;
          offset = (row * bandCount * imageWidth + band * imageWidth + col) * pixelSize;
        }

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          if (imgBlockFile != null)
            result = imgBlockFile.readPCFloat((int) offset);
        } catch (Exception e) {
          System.out.println("PDSImage.getShortValue: " + e);
          System.out.printf("Row %d Col %d Band %d offset %d\n", row, col, band, offset);
          e.printStackTrace();
          System.exit(0);
        }
      }
    }

    return result;
  }
  //
  // reading a double from the file given the zero-based row, column, and band
  // indices
  //

  public double getDoubleValue(int row, int col, int band) {
    JCATLog.getInstance().getLogger().log(Level.FINEST,
        "Entering method getDoubleValue 3 parameters ");
    double result = 0.0;

    if (openImageFile()) {
      // if (dataType == ImageDataTypes.ShortType)
      {
        long pixelSize = getPixelSize();

        long bandSize = (imageWidth * imageHeight) * pixelSize;
        long offset = bandSize * band + (row * imageWidth + col) * pixelSize + imageDataOffset;
        if (bandType == ImageBandTypes.BandInterleaved)
          offset = ((row * imageWidth + col) * bandCount + band) * pixelSize + imageDataOffset;

        try {
          // fileDataInput.skip(offset);
          // fileDataInput.seek(offset);
          result = imgBlockFile.readDouble((int) offset);
        } catch (IOException e) {
          System.out.println("PDSImage.getShortValue: " + e);
          e.printStackTrace();
        }
      }
    }

    return result;
  }

  public int getPixelSize() {
    int result = 1;

    switch (dataType) {
      case IntegerType:
        result = 4;
        break;
      case ShortType:
        result = 2;
        break;
      case FloatType:
        result = 4;
        break;
      case DoubleType:
        result = 8;
        break;
      case ByteType:
      default:
        break;
    }

    return result;
  }

  public static BufferedImage pdsImage2Image(String filename) {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method pdsImage2Image ");
    BufferedImage result = null;

    PDSImage img = new PDSImage(filename, 0, 0, 8);
    if (img.isValidImage()) {
      long w = img.getWidth();
      long h = img.getHeight();

      if ((w > 0) && (h > 0)) {
        result = new BufferedImage((int) w, (int) h, BufferedImage.TYPE_3BYTE_BGR);
        if (result != null) {
          for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
              int v = img.readByte();
              result.setRGB(j, i, (int) (((v & 0xff) << 16) | ((v & 0xff) << 8) | (v & 0xff)));
            }
          }
        }
      }
    }

    return result;
  }

}
