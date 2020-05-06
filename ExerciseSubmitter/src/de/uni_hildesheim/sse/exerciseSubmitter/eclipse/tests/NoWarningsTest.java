package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.tests;

import org.eclipse.core.resources.IMarker;

import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;

/**
 * A optional test to inform the user about warnings in
 * a project.
 * 
 * @since 2.00
 * @version 2.00
 */
public class NoWarningsTest extends ClientSidedTest {

    /**
     * Creates a new test instance.
     * 
     * @since 2.00
     */
    NoWarningsTest() {
        super(Type.OPTIONAL);
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
        if (checkForMarkers(project, IMarker.PROBLEM, 
            IMarker.SEVERITY_WARNING)) {
            result = "Warnings were found in project '"
                + project.getName() + "'.";
        }
        return result;
    }
    
    
}
