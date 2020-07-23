package de.uni_hildesheim.sse.exerciseSubmitter.configuration;

/**
 * Constant definitions for plug-in preferences.
 * 
 * @since 2.00
 * @version 2.2
 * @author eichelberger
 * @author El-Sharkawy
 */
public class PreferenceConstants {

    /**
     * Defines the preference constant for the user name.
     * 
     * @since 2.00
     */
    public static final String USERNAME = "net.ssehub.exercisesubmitter.userName";

    /**
     * Defines the preference constant for the password.
     * 
     * @since 2.00
     */
    public static final String PASSWORD = "net.ssehub.exercisesubmitter.password";
    
    /**
     * Defines the preference constant for storing the currently reviews assignment.
     * 
     * @since 2.2
     */
    public static final String ASSIGNMENT = "net.ssehub.exercisesubmitter.assignment";

    /**
     * Prevents this class from being initialized from outside.
     * 
     * @since 2.00
     */
    private PreferenceConstants() {
    }
    
}
