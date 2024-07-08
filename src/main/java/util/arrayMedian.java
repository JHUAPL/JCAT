package util;

import java.util.Arrays;
import java.util.List;

import reader.TRDR;

public class arrayMedian {

  public float[][][] loaf;
  public float[][] toast;
  public float[][][] nextLoaf;
  public int medianIndex;
  public int medianSpan;
  public double realWave;

  public arrayMedian(String imgfile, float[][][] array, int wave, int span) {
    String ddrFile = imgfile.replaceAll("if", "de").replaceAll("trr3", "ddr1");
    TRDR trdr_ = new TRDR(imgfile, ddrFile);
    List<Double> wavelength = trdr_.getWavelengths();

    double distance = Math.abs(wavelength.get(0) - wave);
    int idx = 0;
    for (int c = 1; c < wavelength.size(); c++) {
      double cdistance = Math.abs(wavelength.get(c) - wave);
      if (cdistance < distance) {
        idx = c;
        distance = cdistance;
      }
    }
    medianIndex = idx;
    realWave = wavelength.get(medianIndex);

    loaf = array;
    medianSpan = span;
    int h = loaf[0].length;
    int w = loaf[0][0].length;
    float[][][] slice = new float[h][w][medianSpan];
    int knife = (medianSpan - 1) / 2;
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        if (knife == 0) {
          slice[y][x][0] = loaf[medianIndex][y][x];
        } else {
          for (int z = medianIndex - knife; z <= medianIndex + knife; z++) {
            slice[y][x][z - (medianIndex - knife)] = loaf[z][y][x];
          }
        }
      }
    }

    toast = new float[h][w];
    nextLoaf = loaf;
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        Arrays.sort(slice[i][j]);
        int midindex = (slice[i][j].length - 1) / 2;
        toast[i][j] = slice[i][j][midindex];
        nextLoaf[medianIndex][i][j] = toast[i][j];
      }
    }

  }

  public void resetArrayMedian(String imgfile, float[][][] array, int wave, int span) {
    String ddrFile = imgfile.replaceAll("if", "de").replaceAll("trr3", "ddr1");
    TRDR trdr_ = new TRDR(imgfile, ddrFile);
    List<Double> wavelength = trdr_.getWavelengths();

    double distance = Math.abs(wavelength.get(0) - wave);
    int idx = 0;
    for (int c = 1; c < wavelength.size(); c++) {
      double cdistance = Math.abs(wavelength.get(c) - wave);
      if (cdistance < distance) {
        idx = c;
        distance = cdistance;
      }
    }
    medianIndex = idx;
    realWave = wavelength.get(idx);

    loaf = array;
    medianSpan = span;
    int h = loaf[0].length;
    int w = loaf[0][0].length;
    float[][][] slice = new float[h][w][medianSpan];
    int knife = (medianSpan - 1) / 2;
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        if (knife == 0) {
          slice[y][x][0] = loaf[medianIndex][y][x];
        } else {
          for (int z = medianIndex - knife; z <= medianIndex + knife; z++) {
            slice[y][x][z - (medianIndex - knife)] = loaf[z][y][x];
          }
        }
      }
    }
    toast = new float[h][w];
    nextLoaf = loaf;
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        Arrays.sort(slice[i][j]);
        int midindex = (slice[i][j].length - 1) / 2;
        toast[i][j] = slice[i][j][midindex];
        nextLoaf[medianIndex][i][j] = toast[i][j];
      }
    }
  }

  public float[][] getToast() {
    return toast;
  }

  public double getWave() {
    return realWave;
  }

}
