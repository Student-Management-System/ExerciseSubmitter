package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.tests;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import de.uni_hildesheim.sse.exerciseSubmitter.Activator;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;

/**
 * A configuration test to inform the user if checkstyle is not
 * enabled on a project.
 * 
 * @since 2.00
 * @version 2.00
 */
public class CheckstyleActivatedTest extends ClientSidedTest {

    /**
     * Defines the plugin name of the checkstyle eclipse plugin.
     * 
     * @since 2.0
     */
    private static final String CHECKSTYLE_PLUGIN_NAME = 
        "net.sf.eclipsecs.core";

    /**
     * Defines the project nature provided by the checkstyle eclipse plugin.
     * 
     * @since 2.0
     */
    private static final String CHECKSTYLE_PROJECT_NATURE = 
        "net.sf.eclipsecs.core.CheckstyleNature";

    /**
     * Defines the build command name provided by the checkstyle eclipse plugin.
     * 
     * @since 2.0
     */
    private static final String CHECKSTYLE_BUILD_COMMAND_NAME = 
        "net.sf.eclipsecs.core.CheckstyleBuilder"; 
    
    /**
     * Creates a new test instance.
     * 
     * @since 2.00
     */
    CheckstyleActivatedTest() {
        super(Type.CONFIGURATION);
    }
    
    /**
     * Returns if this test enables submission on
     * the given project.
     * 
     * @param project the project to be considered
     * @return <b>null</b> if no issue was found, a
     *         message otherways
     * 
     * @since 2.00
     */
    @Override
    public String enableSubmission(ISubmissionProject project) {
        String error = null;
        Bundle checkstyleBundle = Platform.getBundle(CHECKSTYLE_PLUGIN_NAME);
        if (null == checkstyleBundle) {
            error = "Checkstyle not installed!";
        } else {
            if (Bundle.UNINSTALLED == checkstyleBundle.getState()) {
                error = "Checkstyle was disabled in local configuration!";
            } 
        }
        if (null == error && !Activator.inReviewerMode()) {
            boolean enabled = false;
            if (!enabled && null != project) {
                IProject eProject = project.getProject();
                boolean hasNature = false;
                boolean hasBuilder = false;
                try {
                    hasNature = null != eProject.getNature(
                        CHECKSTYLE_PROJECT_NATURE);
                    ICommand[] buildSpecs = 
                        eProject.getDescription().getBuildSpec();
                    for (int b = 0; !hasBuilder && b < buildSpecs.length; b++) {
                        hasBuilder = CHECKSTYLE_BUILD_COMMAND_NAME.
                            equals(buildSpecs[b].getBuilderName());
                    }
                } catch (CoreException ce) {
                }
                if (!(hasNature && hasBuilder)) {
                    error = "Checkstyle disabled on project '" 
                        + project.getName() + "'";
                }
            }
        }
        return error;
    }

}
