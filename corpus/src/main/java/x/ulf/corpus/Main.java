package x.ulf.corpus;

import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.main.AutoProcessor;
import org.osgi.framework.launch.Framework;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: ulf
 * Date: 27.11.2010
 * Time: 10:35:39
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static final FrameworkFactory frameworkFactory = new FrameworkFactory();
    private static Framework framework;

    public static void main(String[] args) throws Exception {
        log.info("Starting Corpus in '" + System.getProperty("user.dir") + "'");

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        Properties config = Main.getConfig();
        try {
            Main.framework = Main.frameworkFactory.newFramework(config);
            Main.framework.init();

            AutoProcessor.process(config, Main.framework.getBundleContext());

            Main.framework.start();
            Main.framework.waitForStop(0);
        } catch (Exception ex) {
            Main.log.severe("Could not create framework: " + ex);
            ex.printStackTrace();
        }

    }

    private static Properties getConfig() throws IOException {
        Properties systemProperties = System.getProperties();
        Properties configProperties = new Properties(systemProperties);
        InputStream config = Main.class.getResourceAsStream("/config.properties");
        if (config != null) {
            log.info("Loading config from /config.properties");
            configProperties.load(config);
        } else {
            log.info("No config.properties file found");
        }

        return configProperties;
    }

    private static class ShutdownHook extends Thread {
        public ShutdownHook() {
            super("Corpus Shutdown Hook Thread");
        }

        public void run() {
            try {
                if (Main.framework != null) {
                    Main.framework.stop();
                    Main.framework.waitForStop(0);
                }
            } catch (Exception ex) {
                Main.log.severe("Error stopping framework: " + ex);
            }
        }
    }
}
