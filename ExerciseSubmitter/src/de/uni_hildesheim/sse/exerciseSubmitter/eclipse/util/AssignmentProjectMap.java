package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;

/**
 * Maps available {@link Assignment}s to local Eclipse projects.
 * @author El-Sharkawy
 *
 */
public class AssignmentProjectMap implements Iterable<AssignmentProjectMap.Entry> {
    
    /**
     * a 2-tuple storing an assignment and a submitable project wiht the same name.
     * @author El-Sharkawy
     *
     */
    public static class Entry {
        private Assignment assignment;
        private ISubmissionProject project;
        
        /**
         * Sole constructor.
         * @param assignment The assignment.
         * @param project A submitable project with the same name as the assignment.
         */
        private Entry(Assignment assignment, ISubmissionProject project) {
            this.assignment = assignment;
            this.project = project;
        }
        
        /**
         * Returns the assignment.
         * @return The assignment.
         */
        public Assignment getAssignment() {
            return assignment;
        }
        
        /**
         * Returns the submitable project, which has the same name as the assignment.
         * @return The project candidate to submit.
         */
        public ISubmissionProject getProject() {
            return project;
        }
    }
    
    private Map<String, Entry> map = new HashMap<>();
    
    /**
     * Puts a new map entry into this map.
     * @param assignment An assignment, must not be <tt>null</tt> as this is also used as key.
     * @param project The project which belongs the assignment (same name), may be temporarily <tt>null</tt>.
     */
    public void put(Assignment assignment, ISubmissionProject project) {
        map.put(assignment.getName(), new Entry(assignment, project));
    }
    
    /**
     * Replaces the project for an already exiting entry.
     * {@link #containsKey(String)} must not be <tt>false</tt>.
     * @param name The name of the {@link Assignment} <b>and</b> the project.
     * @param project The project to set.
     */
    public void set(String name, ISubmissionProject project) {
        Entry value = map.get(name);
        if (null != value) {
            value.project = project;
        }
    }
    
    /**
     * Checks if an {@link Assignment} with the specified name was added to the map.
     * @param name A name of an {@link Assignment}, which should already be part of this map.
     * @return <tt>true</tt> if an {@link Entry} with the given name was already added to this map, <tt>false</tt>
     *     otherwise.
     */
    public boolean containsKey(String name) {
        return map.containsKey(name);
    }
    
    /**
     * Removes all {@link Entry}s from this map for which no project was stored.
     */
    public void prune() {
        Iterator<Entry> itr = map.values().iterator();
        while (itr.hasNext()) {
            Entry element = itr.next();
            if (null == element.project) {
                itr.remove();
            }
        }
    }
    
    /**
     * Checks if the map is empty.
     * @return <tt>true</tt> if size is 0.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Iterator<Entry> iterator() {
        return map.values().iterator();
    }
}
