package de.uni_hildesheim.sse.exerciseSubmitter.submission;

import java.util.List;

import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;

/**
 * Defines an optional handler for server authentications.
 * {@link #authenticate} is called when
 * a server operation is being carried out and an authentication
 * might be required. {@link #mapProjects} may be implemented
 * to modify internal project wrapper instances to meet the 
 * authentication. This class implements an empty 
 * authentication. Individual implementations may refine this
 * behavior and change the default instance.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public class ServerAuthentication {
    
    /**
     * Stores the default instance.
     * 
     * @since 2.00
     */
    private static ServerAuthentication instance = new ServerAuthentication();

    /**
     * Prevents this class from being instantiated from outside.
     * 
     * @since 1.00
     */
    protected ServerAuthentication() {
    }
    
    /**
     * Returns the default instance.
     * 
     * @return the default instance
     * 
     * @since 2.00
     */
    public static ServerAuthentication getInstance() {
        return instance;   
    }
    
    /**
     * Changes the default instance.
     * 
     * @param newInstance the new default instance
     * 
     * @since 2.00
     */
    protected static void setInstance(ServerAuthentication newInstance) {
        assert newInstance != null;
        instance = newInstance;
    }
    
    /**
     * Is called in the case that a submission authentication might
     * be required.
     * 
     * @param comm the communication instance to be considered
     * @param submission <code>true</code> if user names
     *        for submissions should be returned, <code>false</code>
     *        if user names for replay should be returned
     * @return <code>true</code> if the authentication was successful, 
     *         <code>false</code> else
     * 
     * @since 2.00
     */
    public boolean authenticate(SubmissionCommunication comm, 
        boolean submission) {
        return true;
    }
    
    /**
     * Maps the specified projects according to the authentication represented
     * by this class. The authentication may change the concrete paths/names to 
     * be considered while submission/replay.
     * 
     * @param projects the projects to be mapped
     * @param comm the communication instance to be considered while doing the 
     *        mapping
     * @return the mapped projects
     * 
     * @since 2.00
     */
    public List<ISubmissionProject> mapProjects(List<ISubmissionProject> projects, SubmissionCommunication comm) {
        return projects;
    }

}
