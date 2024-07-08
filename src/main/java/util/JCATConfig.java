package util;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.logging.Level;

public class JCATConfig {

    private static JCATConfig instance = null;
    private final File configFile;
    private PropertiesConfiguration config;
    private boolean fromJar;
    private FileSystem jarFileSystem;

    public static JCATConfig getInstance() {
        if (instance == null) instance = new JCATConfig();
        return instance;
    }

    public boolean fromJar() {
        return fromJar;
    }

    public FileSystem getJarFileSystem() {
        return jarFileSystem;
    }

    public File getLastWorkingDirectory() {
        String lastDir = config.getString("lastDirectory");
        if (lastDir == null) setLastDirectory(System.getProperty("user.home"));
        return new File(config.getString("lastDirectory"));
    }

    public void setLastDirectory(String path) {
        config.setProperty("lastDirectory", path);
    }

    private PropertiesConfiguration defaultConfiguration(File rootDir) {
        PropertiesConfiguration config = new PropertiesConfiguration();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        config.setLayout(layout);
        config.setProperty("blue", 41);
        layout.setComment("blue", "Index of blue band");
        config.setProperty("green", 122);
        layout.setComment("green", "Index of green band");
        config.setProperty("red", 304);
        layout.setComment("red", "Index of red band");
        config.setProperty("localArchive", rootDir.toString());
        layout.setComment("localArchive", "Path to local JCAT files");
        return config;
    }

    private JCATConfig() {
        JCATLog.getInstance().getLogger().log(Level.FINER, "Instantiating JCATConfig object");
        Configurations configs = new Configurations();
        File rootDir = new File(System.getProperty("user.home"), "JCAT");
        if (!rootDir.isDirectory()) {
            if (!rootDir.mkdirs()) {
                System.err.println("Cannot create directory " + rootDir);
                System.exit(0);
            }
        }
        configFile = new File(rootDir, "config.txt");
        if (configFile.exists()) {
            try {
                config = configs.properties(configFile);
            } catch (ConfigurationException e) {
                JCATMessageWindow.show(e);
                e.printStackTrace();
            }
        } else {
            config = defaultConfiguration(rootDir);
            save();
        }

        try {
            URI uri = getClass().getResource("/resources").toURI();
            fromJar = uri.getScheme().equals("jar");
            if (fromJar) {
                jarFileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            }
        } catch (URISyntaxException | IOException e) {
            JCATMessageWindow.show(e);
            e.printStackTrace();
        }
    }

    public int getRed() {
        return config.getInt("red");
    }

    public int getGreen() {
        return config.getInt("green");
    }

    public int getBlue() {
        return config.getInt("blue");
    }

    public String getLocalArchive() {
        return config.getString("localArchive");
    }

    public void save() {
        try {
            config.write(new PrintWriter(configFile));
        } catch (ConfigurationException | IOException e) {
            JCATMessageWindow.show(e);
            e.printStackTrace();
        }
    }
}
