package de.uni_hildesheim.sse.exerciseSubmitter.submission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.uni_hildesheim.sse.exerciseSubmitter.submission.plugins.
    SvnSubmissionCommunication;

/**
 * Defines the basic class for submission plugins. This version
 * does not provide dynamic plugin loading, but this version is
 * prepared.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public abstract class SubmissionPlugin {

    /**
     * Stores all registered plugin instances.
     * 
     * @since 2.00
     */
    private static final List<SubmissionPlugin> PLUGINS = 
        new ArrayList<SubmissionPlugin>();

    /**
     * Register the default plugins.
     * 
     * @since 2.00
     */
    static {
        register(SvnSubmissionCommunication.PLUGIN);
    }
    
    /**
     * Returns the name of the protocol implemented by this class. 
     * The string will be considered when reading the communication 
     * based submission configuration.
     * <i>Note:</i> Currently no mechanism for avoiding duplicate 
     * protocol names is realized.
     * 
     * @return the name of the implemented protocol
     * 
     * @since 2.00
     */
    public abstract String getProtocol();

    /**
     * Creates an instance of the described submission communication
     * class.
     * 
     * @param userName the name of the user which will communicate with a
     *        concrete communication server
     * @param password the password of <code>username</code>
     * @param asReviewer initialize this instance in reviewer mode - 
     *        this may show additional exercises to be submitted but
     *        finally the access permissions on the server should 
     *        prevent from misuse
     * @return the created instance
     * 
     * @since 2.10
     */
    public abstract SubmissionCommunication createInstance(String userName, String password, boolean asReviewer);

    /**
     * Registers a given plugin instance. An instance will not be registered
     * if one of the previously registered instances is of the same class.
     * 
     * @param plugin the plugin instance to be registered
     * @return <code>true</code> if the plugin was registered, 
     *         <code>false</code> else
     * 
     * @since 2.00
     */
    private static boolean register(SubmissionPlugin plugin) {
        boolean found = false;
        for (SubmissionPlugin pl : PLUGINS) {
            if (pl.getClass() == plugin.getClass()) {
                found = true;
                break;
            }
        }
        boolean result;
        if (found) {
            result = false;
        } else {
            PLUGINS.add(plugin);
            result = true;
        }
        return result;
    }

    /**
     * Returns all plugins as iterator.
     * 
     * @return all plugins as (unmodifiable) iterator
     * 
     * @since 2.00
     */
    public static Iterable<SubmissionPlugin> getPlugins() {
        return Collections.unmodifiableList(PLUGINS);
    }

}
