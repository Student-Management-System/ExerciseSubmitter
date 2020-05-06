package de.uni_hildesheim.sse.exerciseSubmitter.configuration;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;

/**
 * Implements the basic access to configuration data. Configuration files are
 * stored as plain old java key-value lists. The global configuration may be
 * located in
 * <ul>
 * <li><code>/etc/exerciseSubmission.ini</code> on Unix/Linux systems</li>
 * <li><code>c:\windows\exerciseSubmission.ini</code> on Windows systems</li>
 * <li><code>esPlugin.ini</code> in main installation directory or the jar
 * file (has a lower precedence than the following ones)</li>
 * </ul>
 * Typical format:
 * 
 * <pre>
 *   debug = false
 *   protocol.1 = svn
 *   svn.server = ...
 * </pre>
 * 
 * Multiple protocols may be specified, values like <code>svn.server</code>
 * may represent protocol specific options.
 * 
 * @author Alexander Schmehl
 * @since 1.00
 * @version 2.00
 */
public abstract class IConfiguration {

    /**
     * Stores the configuration instance.
     * 
     * @since 2.00
     */
    public static final IConfiguration INSTANCE;
    
    /**
     * Stores the global configuration properties.
     * 
     * @since 1.00
     */
    private static Properties globalprop = new Properties();

    /**
     * Initializes the configuration instance depending on the executing
     * environment.
     * 
     * @since 2.00
     */
    static {
        if (OsTypes.isEclipse()) {
            INSTANCE = new EclipseConfiguration();
        } else {
            INSTANCE = new DefaultConfiguration();
        }
    }

    /**
     * Creates a new configuration instance and initializes {@link #globalprop}.
     * Calls {@link #handleError(CommunicationException)} in the case of errors.
     * 
     * @since 2.00
     */
    public IConfiguration() {

        try {
            File f = new File(OsTypes.getOSType().getGlobalConfigPrefix()
                + "exerciseSubmission.ini");
            InputStream is = null;
            if (f.exists()) {
                is = new FileInputStream(f);
            } else {
                is = IConfiguration.class
                    .getResourceAsStream("/esPlugin.ini");
            }
            if (null == is) {
                handleError(new CommunicationException(
                    CommunicationException.SubmissionPublicMessage.
                    CONFIGURATION_ERROR, new Throwable()));
            } else {
                globalprop.load(new BufferedInputStream(is));
            }
        } catch (IOException e) {
            handleError(new CommunicationException(
                CommunicationException.SubmissionPublicMessage.
                CONFIGURATION_ERROR, e));
        }
    }

    /**
     * Is called to generically handle errors.
     * 
     * @param exception
     *            the (typed) exception denoting the reason
     * 
     * @since 2.00
     */
    protected abstract void handleError(CommunicationException exception);

    /**
     * Retrieves arbitrary configuration values.
     * 
     * @param key
     *            the identifier of the configuration value
     * 
     * @return the configuration value related to <code>key</code>
     * 
     * @since 1.00
     */
    public String getProperty(String key) {
        return globalprop.getProperty(key);
    }

    /**
     * Returns if debugging mode is enabled.
     * 
     * @return <code>true</code> if debugging is enabled, <code>false</code>
     *         else
     * 
     * @since 2.00
     */
    public boolean isDebuggingEnabled() {
        return globalprop.getProperty("debug", "false")
            .equalsIgnoreCase("true");
    }
    
    /**
     * Returns if the user must specify an explicit group name in the 
     * user settings.
     * 
     * @return <code>true</code> if explicit group name is required, 
     *     <code>false</code> if the group name is equal to the user name
     * 
     * @since 2.10
     */
    public boolean isExplicitGroupNameEnabled() {
        return globalprop.getProperty("explicitGroupName", "true")
            .equalsIgnoreCase("true");
    }
    
    /**
     * Whether we expect an XML response from the server.
     * 
     * @return <code>true</code> for XML response, 
     *     <code>false</code> for text or other response
     */
    public boolean isXmlExpected() {
        return globalprop.getProperty("expectXml", "true")
            .equalsIgnoreCase("true");
    }

    /**
     * Returns the explicit folder name. 
     * Calls {@link #getExplicitFolderName(String)}.
     * 
     * @return the group name if
     *    {@link #isExplicitGroupNameEnabled()} or 
     *    <b>null</b> otherways 
     * 
     * @since 2.10
     */
    public String getExplicitFolderName() {
        return getExplicitFolderName(getGroupName());
    }
    
    /**
     * Returns the explicit folder name [useful if user 
     * data should not be stored in this configuration
     * or validated first].
     * 
     * @param groupName the group name to be used
     * @return the group name if
     *    {@link #isExplicitGroupNameEnabled()} or 
     *    <b>null</b> otherways 
     * 
     * @since 2.10
     */
    public String getExplicitFolderName(String groupName) {
        String result;
        if (isExplicitGroupNameEnabled()) {
            result = groupName;
        } else {
            result = null;
        }
        return result;
    }
    
    /**
     * Retrieves arbitrary configuration values.
     * 
     * @param key
     *            the identifier of the configuration value
     * @param defaultValue
     *            returned in the case that <code>key</code> cannot be found
     * 
     * @return the configuration value related to <code>key</code>
     * 
     * @since 1.00
     */
    public String getProperty(String key, String defaultValue) {
        return globalprop.getProperty(key, defaultValue);
    }

    /**
     * Returns the stored password of the current user (user local
     * configuration).
     * 
     * @return the stored password or an empty string in the case that no
     *         password was stored so far
     * 
     * @since 1.00
     */
    public abstract String getPassword();

    /**
     * Changes the password of the current user (user local configuration). Call
     * {@link #store()} to make this change permanent.
     * 
     * @param password
     *            the password to be stored
     * 
     * @since 1.00
     */
    public abstract void setPassword(String password);

    /**
     * Returns the current user name (user local configuration).
     * 
     * @return the current user name or an empty string if no user name was
     *         stored so far
     * 
     * @since 1.00
     */
    public abstract String getUserName();

    /**
     * Changes the user name of the current user (user local configuration).
     * Call {@link #store()} to make this change permanent.
     * 
     * @param userName
     *            the user name to be stored
     * 
     * @since 1.00
     */
    public abstract void setUserName(String userName);

    /**
     * Returns the current user submission group name (user local 
     * configuration). This value is
     * relevant dependent on {@link #isExplicitGroupNameEnabled()}
     * 
     * @return the current user submission group name or an empty string if no 
     *         name was stored so far. 
     * 
     * @since 2.10
     */
    public abstract String getGroupName();
    
    /**
     * Changes the user submission group name of the (user local configuration).
     * Call {@link #store()} to make this change permanent. This value is
     * relevant dependent on {@link #isExplicitGroupNameEnabled()}.
     * 
     * @param groupName
     *            the user submission group name to be stored
     * 
     * @since 2.10
     */
    public abstract void setGroupName(String groupName);
    
    /**
     * Stores the user name in the user local configuration. A local
     * configuration must not exist.
     * 
     * @since 1.00
     */
    public abstract void store();
    
    /**
     * Adjusts the files after replay.
     * 
     * @param targetDir the entire path to the eclipse directory
     * 
     * @since 2.00
     */
    public abstract void adjustFilesAfterReplay(File targetDir);

}
