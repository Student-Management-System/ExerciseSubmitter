package de.uni_hildesheim.sse.exerciseSubmitter.configuration;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;

/**
 * Implements the basic access to configuration data. Configuration files are
 * stored as plain old java key-value lists. The global configuration may be
 * located in <code>esPlugin.ini</code> in main installation directory or the jar
 * file.
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
 * @author El-Sharkawy
 * @since 1.00
 * @version 2.10
 */
public abstract class IConfiguration {

    /**
     * Stores the configuration instance.
     * 
     * @since 2.00
     * @version 2.10
     */
    public static final IConfiguration INSTANCE = new EclipseConfiguration();
    
    /**
     * Stores the global configuration properties.
     * 
     * @since 1.00
     */
    private static Properties globalprop;

    /**
     * Creates a new configuration instance and initializes {@link #globalprop}.
     * Calls {@link #handleError(CommunicationException)} in the case of errors.
     * 
     * @since 2.00
     * @version 2.10
     */
    public IConfiguration() {

        try (InputStream is = IConfiguration.class.getResourceAsStream("/esPlugin.ini")) {
            if (null == is) {
                handleError(new CommunicationException(
                    CommunicationException.SubmissionPublicMessage.CONFIGURATION_ERROR, new Throwable()));
            } else {
                if (null == globalprop) {
                    globalprop = new Properties();
                }
                globalprop.load(new BufferedInputStream(is));
            }
        } catch (IOException e) {
            handleError(new CommunicationException(
                CommunicationException.SubmissionPublicMessage.CONFIGURATION_ERROR, e));
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
        return globalprop.getProperty("debug", "false").equalsIgnoreCase("true");
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
    
    /**
     * Sets the currently reviewed assignment.
     * This is only used by the ExerciseReviewer.
     * @param assignment Sets the assignment, which is reviewed at the current workspace.
     */
    public abstract void setAsssignment(Assignment assignment);
    
    /**
     * Returns the currently reviews assignment.
     * This is only used by the ExerciseReviewer and, thus, will return <tt>null</tt> at the ExerciseSubmitter.
     * @return The currently reviewed {@link Assignment} or <tt>null</tt> if not specified.
     */
    public abstract Assignment getAssignment();

}
