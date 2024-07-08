package reader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import util.JCATConfig;
import util.JCATLog;

public class ADRVSLibrary {

  private Set<String> vsNames;
  private final String vsPath = "/resources/adr/vs";

  public Set<String> getNames() {
    return vsNames;
  }

  public String getLocalPath() {
    return JCATConfig.getInstance().getLocalArchive() + File.separator + "vs";
  }

  public String getPath() {
    return vsPath;
  }

  private static ADRVSLibrary instance = null;

  public static ADRVSLibrary getInstance() {
    if (instance == null)
      instance = new ADRVSLibrary();
    return instance;
  }

  private ADRVSLibrary() {
    JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating ADRVSLibrary object");
    vsNames = new HashSet<>();
    try {
      readResources();
    } catch (URISyntaxException | IOException e) {
      e.printStackTrace();
    }
  }

  private void readResources() throws URISyntaxException, IOException {
    URI uri = this.getClass().getResource(vsPath).toURI();
    Path myPath;

    JCATConfig config = JCATConfig.getInstance();
    if (config.fromJar()) {
      myPath = config.getJarFileSystem().getPath(vsPath);
    } else {
      myPath = Paths.get(uri);
    }

    try (Stream<Path> walk = Files.walk(myPath, 1)) {
      for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
        Path path = it.next();
        if (path.toString().endsWith("IMG")) {
          vsNames.add(FilenameUtils.getBaseName(path.toString()));
        }
      }
    }
  }

}
