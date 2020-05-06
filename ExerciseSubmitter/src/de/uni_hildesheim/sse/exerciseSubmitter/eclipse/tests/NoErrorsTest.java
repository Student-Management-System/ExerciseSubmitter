package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.tests;

import org.eclipse.core.resources.IMarker;

import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;

/**
 * A mandatory test to inform the user about errors in
 * a project.
 * 
 * @since 2.00
 * @version 2.00
 */
public class NoErrorsTest extends ClientSidedTest {

    /**
     * Creates a new test instance.
     * 
     * @since 2.00
     */
    NoErrorsTest() {
        super(Type.REQUIRED);
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
        String result = null;
        if (checkForMarkers(project, IMarker.PROBLEM, IMarker.SEVERITY_ERROR)) {
            result = "Errors were found in project '"
                + project.getName() + "'.";
        }
        return result;
    }
    
}
