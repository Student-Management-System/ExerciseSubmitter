package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;

/**
 * Defines the interface for (pluggable) client side pre-submission tests.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public abstract class ClientSidedTest {

    /**
     * Stores the registered tests.
     * 
     * @since 2.00
     */
    private static final List<ClientSidedTest> TESTS = 
        new ArrayList<ClientSidedTest>();

    /**
     * Stores the type of the test.
     * 
     * @since 2.00
     */
    private Type type;
    
    /**
     * Defines the test types.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    public enum Type {

        /**
         * Defines a type for configuration checks
         * usually to be executed before any other
         * tests.
         * 
         * @since 2.00
         */
        CONFIGURATION,
        
        /**
         * Defines the required test type.
         * 
         * @since 2.00
         */
        REQUIRED,

        /**
         * Defines the optional test type.
         * 
         * @since 2.00
         */
        OPTIONAL;
    }
    
    /**
     * Creates a new test plugin.
     * 
     * @param type the type of the test
     * 
     * @since 2.00
     */
    protected ClientSidedTest(Type type) {
        this.type = type;
    }
    
    /**
     * Returns the type of the test.
     * 
     * @return the type of the test
     * 
     * @since 2.00
     */
    public Type getType() {
        return type;
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
    public abstract String enableSubmission(ISubmissionProject project);

    /**
     * Checks the specified <code>project</code> for a
     * certain type of markers.
     * 
     * @param project the project to be considered
     * @param type the type of the marker to be searched 
     *        (recursively)
     * @param severity the severity of the marker to be
     *        searched for
     * @return <code>true</code> if the specified type of
     *         markers was found, <code>false</code> else
     * 
     * @since 2.00
     */
    protected boolean checkForMarkers(ISubmissionProject project, 
        String type, int severity) {
        boolean found = false;
        if (null != project) {
            IMarker[] marker = project.getMarker(type, 
                true, IResource.DEPTH_INFINITE);
            if (null != marker) {
                for (int i = marker.length - 1; !found && i >= 0; i--) {
                    try {
                        Object sev = marker[i].getAttribute(IMarker.SEVERITY);
                        found = null != sev && Integer.parseInt(sev.toString()) 
                            == severity;
                    } catch (CoreException e) {
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        return found;
    }
    
    /**
     * Returns the currently registered tests.
     * 
     * @return the currently registered tests
     * 
     * @since 2.00
     */
    public static final Iterator<ClientSidedTest> getRegisteredTests() {
        return TESTS.iterator();
    }
    
    /**
     * Registers a specified <code>test</code>.
     * 
     * @param test the test to be registered
     * @throws NullPointerException if <code>test==<b>null</b></code>
     * 
     * @since 2.00
     */
    public static final void registerTest(ClientSidedTest test) {
        if (null == test) {
            throw new NullPointerException();
        }
        TESTS.add(test);
    }
    
    /**
     * Registers the default tests (no dynamic class loading
     * so far).
     * 
     * @since 2.00
     */
    static {
        registerTest(new CheckstyleActivatedTest());
        registerTest(new NoErrorsTest());
        registerTest(new NoWarningsTest());
    }
    
    /**
     * Returns if all registered tests of the given 
     * <code>type</code> enable submission of the 
     * specified <code>project</code>.
     * 
     * @param type the type of tests to be considered 
     *        (<b>null</b> selects all registered tests)
     * @param project the project to be considered
     * @return the first message delivered by 
     *         {@link #enableSubmission(ISubmissionProject)}
     * 
     * @since 2.00
     */
    public static final String allTestsEnableSubmission(
        Type type, ISubmissionProject project) {
        String error = null;
        Iterator<ClientSidedTest> iter = TESTS.iterator();
        while (null == error && iter.hasNext()) {
            ClientSidedTest test = iter.next();
            if (null == type || test.getType() == type) {
                error = test.enableSubmission(project);
            }
        }
        return error;
    }
    
}
