package de.uni_hildesheim.sse.exerciseSubmitter.configuration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;

/**
 * The default configuration class using a separate file for user local
 * configuration (in the user home directory). Use
 * {@link IConfiguration#INSTANCE} to retrieve an instance.
 * 
 * @author Alexander Schmehl
 * @since 1.00
 * @version 2.10
 */
class DefaultConfiguration extends AbstractUserConfiguration {

    /**
     * Stores the user local configuration options.
     * 
     * @since 1.00
     */
    private static Properties userprop = new Properties();

    /**
     * Creates a default configuration objects and tries to load user local
     * configuration.
     * 
     * @since 1.00
     */
    DefaultConfiguration() {
        super();
        openuserprop();
    }

    /**
     * Is called to generically handle errors.
     * 
     * @param exception
     *            the (typed) exception denoting the reason
     * 
     * @since 2.00
     */
    @Override
    protected void handleError(CommunicationException exception) {
        System.err.println(exception.getMessage());
        System.exit(ExitConstants.EXIT_CONFIGURATION_ERROR);
    }

    /**
     * Initializes the user local configuration.
     * 
     * @since 1.00
     */
    private void openuserprop() {
        try {
            // try loading user local configuration
            userprop.load(new BufferedInputStream(new FileInputStream(
                    getUserConfigFile())));
            // load values only if present
            if (userprop.containsKey(PreferenceConstants.USERNAME)
                && userprop.containsKey(PreferenceConstants.PASSWORD)) {
                this.setUserName(userprop
                    .getProperty(PreferenceConstants.USERNAME));
                this.setGroupName(userprop
                    .getProperty(PreferenceConstants.GROUPNAME));
                this.setPassword(CryptoEngine.getInstance().decrypt(
                    userprop.getProperty(PreferenceConstants.PASSWORD)));
            }
        } catch (FileNotFoundException e) {
            // if user local file does not exist, try creating a new one and
            // load information
            File file = new File(getUserConfigFile());
            try {
                if (file.createNewFile()) {
                    openuserprop(); // hmmm???
                }
            } catch (IOException f) {
                System.err.printf(
                    "Cannot create configuration file '%s'. " 
                    + " Please change access privileges\n",
                    getUserConfigFile());
            }
        } catch (IOException e) {
            System.err.printf(
                "Error while reading configuration file '%s'. "
                + " Ignoring contents.\n", getUserConfigFile());
        }
    }

    /**
     * Stores the user name in the user local configuration. A local
     * configuration must not exist.
     * 
     * @since 1.00
     */
    public void store() {
        try {
            userprop.setProperty(PreferenceConstants.USERNAME, userName);
            userprop.setProperty(PreferenceConstants.GROUPNAME, groupName);
            userprop.setProperty(PreferenceConstants.PASSWORD, CryptoEngine
                .getInstance().encrypt(password));
            userprop.store(new BufferedOutputStream(new FileOutputStream(
                getUserConfigFile())),
                "Local configuration of exercise submission");
        } catch (IOException e) {
            // no real error
            System.err.printf("Error while writing '%s'.  Please adjust"
                + " access permissions.\n", getUserConfigFile());
        }
    }

    /**
     * Returns the entire path to the local user configuration.
     * 
     * @return the entire path to the local user configuration
     * 
     * @since 1.00
     */
    private String getUserConfigFile() {
        return OsTypes.getOSType().getUserConfigPrefix() + ".abgabe.cfg";
    }
    
    /**
     * Adjusts the files after replay.
     * 
     * @param targetDir the entire path to the eclipse directory
     * 
     * @since 2.00
     */
    public void adjustFilesAfterReplay(File targetDir) {
    }

}
