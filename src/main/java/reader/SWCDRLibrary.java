package reader;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;

import util.FileUtils;
import util.JCATConfig;
import util.JCATLog;
import util.JCATMessageWindow;

public class SWCDRLibrary {

  // private NavigableMap<Integer, NavigableMap<Integer, String>> swMapS;
  // private NavigableMap<Integer, NavigableMap<Integer, String>> swMapL;
  private NavigableMap<Integer, String> swMapS;
  private NavigableMap<Integer, String> swMapL;

  private final String swPath = "/resources/sw";

  private static SWCDRLibrary instance = null;

  public static SWCDRLibrary getInstance() {
    if (instance == null)
      instance = new SWCDRLibrary();
    return instance;
  }

  private SWCDRLibrary() {

    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating SWCDRLibrary object");
    try {
      readResources();
    } catch (URISyntaxException | IOException e) {
      JCATMessageWindow.show(e);
      e.printStackTrace();
    }
  }

  public LinkedHashMap<Integer, Double> getBandMap(String detector, int partition, double sclk) {
    NavigableMap<Integer, String> swMap = null;

    if (detector.trim().equalsIgnoreCase("s"))
      swMap = swMapS;
    else if (detector.trim().equalsIgnoreCase("l"))
      swMap = swMapL;

    // Map.Entry<Integer, NavigableMap<Integer, String>> swEntry = swMap.floorEntry(partition);
    // NavigableMap<Integer, String> partitionMap = swEntry.getValue();
    // if (partitionMap == null)
    //   return null;

    Map.Entry<Integer, String> swEntry = swMap.floorEntry((int) sclk);

    String cdrFile = swEntry.getValue();
    if (cdrFile == null)
      return null;

    // System.out.printf("Reading %s\n", FilenameUtils.getBaseName(cdrFile));

    LinkedHashMap<Integer, Double> bandMap = new LinkedHashMap<>();
    try (Reader in = FileUtils.getInputStream(cdrFile)) {
      Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
      int counter = (detector.trim().equalsIgnoreCase("l")) ? 0 : 1;
      for (CSVRecord record : records) {
        int band = Integer.parseInt(record.get(0).trim());
        double wavelength = Double.valueOf(record.get(1).trim());
        if (wavelength < 65535 || counter == 0) { // First line from SWCDR tab file is FillValue,
                                                  // but important
                                                  // to keep in, will be taken out later for gbBands
          bandMap.put(band, wavelength);
        }
        counter++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return bandMap;
  }

  private void readResources() throws URISyntaxException, IOException {
    swMapS = new TreeMap<>();
    swMapL = new TreeMap<>();

    URI uri = this.getClass().getResource(swPath).toURI();
    Path myPath;

    JCATConfig config = JCATConfig.getInstance();
    if (config.fromJar()) {
      myPath = config.getJarFileSystem().getPath(swPath);
    } else {
      myPath = Paths.get(uri);
    }

    try (Stream<Path> walk = Files.walk(myPath, 1)) {
      for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
        Path path = it.next();
        if (path.toString().endsWith("tab")) {
          String basename = FilenameUtils.getBaseName(path.toString());
          String[] parts = basename.split("_");
          // int partition = Integer.parseInt(parts[1]); # not needed since sclk does not reset on partition change 
          int sclk = Integer.parseInt(parts[2]);
          String detector = parts[4];

          NavigableMap<Integer, String> swMap;
          if (detector.equalsIgnoreCase("s")) {
            swMap = swMapS;
          } else if (detector.equalsIgnoreCase("l")) {
            swMap = swMapL;
          } else
            continue;

          // NavigableMap<Integer, String> partitionMap = swMap.get(partition);
          // if (partitionMap == null) {
          //   partitionMap = new TreeMap<>();
          //   swMap.put(partition, partitionMap);
          // }

          String swEntry = swMap.get(sclk);
          if (swEntry == null){
              swMap.put(sclk, path.toString());
          }

          // partitionMap.put(sclk, path.toString());
        }
      }
    }
  }
}
