package definition.PDS;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;

import util.JCATLog;

public class BlockFile {
  protected RandomAccessFile fileDataInput = null;
  protected String fullPath = null;
  protected int blockSize = PDSImage.typicalPageSize;
  protected byte block[] = null;
  protected int blockOffset = 0;
  protected int fileSize = 0;

  public BlockFile(String filename) throws IOException {

    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating BlockFile object");

    fullPath = filename;

    openFile(filename);
  }

  public BlockFile(String filename, int bufferSize) throws IOException {

    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating BlockFile object");
    fullPath = filename;
    blockSize = bufferSize;

    openFile(filename, bufferSize);
  }

  public void openFile(String filename) throws IOException {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method openFile 1 parameter");
    fullPath = filename;
    readBlock(0, blockSize);

    if (fileDataInput == null) {
      fileDataInput = new RandomAccessFile(filename, "r");
      fileSize = (int) fileDataInput.length();
      blockSize = fileSize / 100;
      fullPath = filename;
      readBlock(0, blockSize);
    }

  }

  public void openFile(String filename, int bufferSize) throws IOException {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method openFile 2 parameter");
    fullPath = filename;
    blockSize = bufferSize;

    readBlock(0, blockSize);


    if (fileDataInput == null) {

      fileDataInput = new RandomAccessFile(filename, "r");
      fileSize = (int) fileDataInput.length();
      blockSize = bufferSize;
      fullPath = filename;
      readBlock(0, blockSize);
    }

  }

  public void openFile() throws IOException {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method openFile 0 paramter");
    openFile(fullPath);
  }

  public void closeFile() throws IOException {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method closeFile");
    if (isOpen()) {
      fileDataInput.close();
      fileDataInput = null;
    }

  }

  public String getFullPath() {
    return fullPath;
  }

  public boolean isOpen() {
    return (block != null);
  }

  public byte readByte(int offset) throws IOException {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readByte");
    byte result = 0;

    readBlock(offset, 1);
    result = block[offset - blockOffset];

    return result;
  }

  public short readShort(int offset) throws IOException {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readShort");
    short result = 0;

    readBlock(offset, 2);
    int v = ((block[offset - blockOffset] & 0xff) << 8) | (block[offset - blockOffset + 1] & 0xff);
    result = (short) v;

    return result;
  }

  public float readFloat(int offset) throws IOException {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readFloat");
    float result = 0;

    readBlock(offset, 4);

    if (block != null)
      result = Float.intBitsToFloat(
          (block[offset - blockOffset + 3] & 0xff) | ((block[offset - blockOffset + 2] & 0xff) << 8)
              | ((block[offset - blockOffset + 1] & 0xff) << 16)
              | ((block[offset - blockOffset] & 0xff) << 24));

    return result;
  }

  public float readPCFloat(int offset) throws IOException {

    float result = 0;

    readBlock(offset, 4);

    if (block != null)
      result = Float.intBitsToFloat(
          (block[offset - blockOffset] & 0xff) | ((block[offset - blockOffset + 1] & 0xff) << 8)
              | ((block[offset - blockOffset + 2] & 0xff) << 16)
              | ((block[offset - blockOffset + 3] & 0xff) << 24));

    return result;
  }

  public int readInt(int offset) throws IOException {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readInt");
    int result = 0;

    readBlock(offset, 4);

    if (block != null) {
      int byte1 = block[offset - blockOffset];
      int byte2 = block[offset - blockOffset + 1];
      int byte3 = block[offset - blockOffset + 2];
      int byte4 = block[offset - blockOffset + 3];

      result =
          (byte4 & 0xff) | ((byte3 & 0xff) << 8) | ((byte2 & 0xff) << 16) | ((byte1 & 0xff) << 24);
    }

    return result;
  }

  public double readDouble(int offset) throws IOException {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readDouble");
    double result = 0;

    readBlock(offset, 8);

    if (block != null) {
      long byte1 = block[offset - blockOffset];
      long byte2 = block[offset - blockOffset + 1];
      long byte3 = block[offset - blockOffset + 2];
      long byte4 = block[offset - blockOffset + 3];
      long byte5 = block[offset - blockOffset + 4];
      long byte6 = block[offset - blockOffset + 5];
      long byte7 = block[offset - blockOffset + 6];
      long byte8 = block[offset - blockOffset + 7];

      long bits = (byte8 & 0xff) | ((byte7 & 0xff) << 8) | ((byte6 & 0xff) << 16)
          | ((byte5 & 0xff) << 24) | ((byte4 & 0xff) << 32) | ((byte3 & 0xff) << 40)
          | ((byte2 & 0xff) << 48) | ((byte1 & 0xff) << 56);

      result = Double.longBitsToDouble(bits);
    }

    return result;
  }

  protected void readBlock(int offset, int length) throws IOException {
    JCATLog.getInstance().getLogger().log(Level.FINEST, "Entering method readBlock");
    if (blockSize <= 0)
      blockSize = PDSImage.typicalPageSize;

    if ((block == null) || (offset > (blockOffset + blockSize)) || (offset < blockOffset)
        || ((offset + length) > (blockOffset + blockSize))) {
      // RandomAccessFile fileDataInput = new
      // RandomAccessFile(filename, "r");
      if (fullPath == null)
        return;

      if (fileDataInput == null)
        fileDataInput = new RandomAccessFile(fullPath, "r");

      fileSize = (int) fileDataInput.length();

      int off =
          (offset >= 0) ? ((offset < (fileSize - blockSize)) ? offset : (fileSize - blockSize)) : 0;

      if (off < 0)
        off = 0;

      blockOffset = off;

      if (block == null)
        block = new byte[blockSize];

      // fileDataInput.seek(blockOffset);
      // fileDataInput.read(block, 0, blockSize);
      // fileDataInput.close();

      FileChannel fc = fileDataInput.getChannel();
      ByteBuffer byteBuffer = ByteBuffer.wrap(block);
      Buffer buffer = (Buffer) byteBuffer;
      fc.read(byteBuffer, blockOffset);
      buffer.clear();
      byteBuffer.get(block);
    }
  }
}

