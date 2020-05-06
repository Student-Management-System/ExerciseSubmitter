package de.uni_hildesheim.sse.exerciseSubmitter.configuration;

/**
 * Some global constants for exiting this program.
 * 
 * @author Alexander Schmehl
 * @since 1.00
 * @version 2.00
 */
public class ExitConstants {

    /**
     * Exit code in the case of I/O errors when reading a configuration file.
     * 
     * @since 1.00
     */
    public static final int EXIT_CONFIGURATION_ERROR = 3;

    /**
     * Prevents this class from being initialized from outside.
     * 
     * @since 2.00
     */
    private ExitConstants() {
    }
    
}
