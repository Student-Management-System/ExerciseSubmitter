package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions;

import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Submission;

/**
 * Replays some information as if a submission would have been executed.
 * 
 * @author Holger Eichelberger
 */
public class TestMarkerAction extends AbstractSubmissionAction {
    
    private static final String LEADIN = "<submitResults>";
    private static final String MESSAGE = 
        "  <message tool=\"checkstyle\" type=\"error\" file=\"MyMain.java\" "
        + "line=\"4\" message=\"Missing a Javadoc comment.\"/>"
        + "<message tool=\"junit\" type=\"error\" message=\"Angabe der Autoren "
        + "fehlt.\" file=\"MyMain.java\" line=\"1\"></message>";
    private static final String LEADOUT = "</submitResults>";

    @Override
    public void run(IAction action) {
        Iterator<ISubmissionProject> iter = getSelectedProjects();
        while (iter.hasNext()) {
            ISubmissionProject prj = iter.next();
            MessageListener messageListener = new MessageListener();
            messageListener.setProject(prj);
            System.out.println("INJECTING MESSAGE");
            String test = Submission.getUnparsedMessage(
                LEADIN + MESSAGE + LEADOUT, messageListener);
            System.out.println(test);
            break;
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

}
