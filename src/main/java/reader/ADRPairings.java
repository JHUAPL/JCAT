package reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.Level;

import util.JCATLog;

public class ADRPairings {

  private HashMap<String, String> adrPairs;

  public String getADRPair(String obsID) {
    String adrID = adrPairs.get(obsID);
    return adrID;
  }

  public ADRPairings() {
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating ADRPairings object");
    adrPairs = new HashMap<>();
    try {
      readVSLookup();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  public void readVSLookup() throws URISyntaxException {
    String swPath = "/resources/vs/lookup";
    URI uri = this.getClass().getResource(swPath).toURI();
    Path myPath = Paths.get(uri);
    File f = new File(myPath + "/crism_obs_info.txt");
    if (f.exists()) {
      try {
        BufferedReader in = new BufferedReader(new FileReader(f));
        String line = null;
        while ((line = in.readLine()) != null) {
          String[] parts = line.split("\\s+");
          if (!parts[1].equals("65.535")) {
            adrPairs.put(parts[1].toLowerCase(), parts[6]); // gets the pair using McGuire
                                                            // wavelengths
          }
        }
        in.close();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
