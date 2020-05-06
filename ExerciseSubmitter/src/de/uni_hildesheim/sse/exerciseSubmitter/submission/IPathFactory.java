package de.uni_hildesheim.sse.exerciseSubmitter.submission;

import java.io.File;

/**
 * A wrapping instance to create paths from inside
 * a submission communication instance. Creating paths, 
 * in particular top-level paths in a workspace directory
 * of an IDE, may require special operations.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public interface IPathFactory {

    /**
     * Creates the <code>subdirectory</code> in the specified
     * <code>path</code>.
     * 
     * @param path the target path
     * @param subdirectory the sub directory to be created
     * @return the created directory as File object
     * 
     * @since 2.00
     */
    public File createPath(File path, String subdirectory);

}
