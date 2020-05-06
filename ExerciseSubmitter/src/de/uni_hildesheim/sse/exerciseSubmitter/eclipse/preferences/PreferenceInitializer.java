package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.preferences;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import de.uni_hildesheim.sse.exerciseSubmitter.Activator;
import de.uni_hildesheim.sse.exerciseSubmitter.configuration.
    PreferenceConstants;

/**
 * Class used to initialize default preference values.
 * 
 * @author eichelberger
 * @since 2.00
 * @version 2.00
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    /**
     * This method is called by the preference initializer to initialize default
     * preference values. Clients should get the correct node for their bundle
     * and then set the default values on it. For example:
     * 
     * <pre>
     * public void initializeDefaultPreferences() {
     *     Preferences node = new DefaultScope().
     *         getNode(&quot;my.bundle.id&quot;);
     *     node.put(key, value);
     * }
     * </pre>
     * 
     * <p>
     * <em>Note: Clients should only set default preference values for their
     * own bundle.</em>
     * </p>
     * <p>
     * <em>Note:</em> Clients should not call this method. It will be called
     * automatically by the preference initializer when the appropriate default
     * preference node is accessed.
     * </p>
     */
    public void initializeDefaultPreferences() {
        Preferences preferences = 
            Activator.getDefault().getPluginPreferences();
        preferences.setDefault(PreferenceConstants.USERNAME, "");
        preferences.setDefault(PreferenceConstants.PASSWORD, "");
        preferences.setDefault(PreferenceConstants.GROUPNAME, "");
    }

}
