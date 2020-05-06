package de.uni_hildesheim.sse.exerciseSubmitter.swingGui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;

import de.uni_hildesheim.sse.exerciseSubmitter.submission.Executable;

/**
 * A dialog showing the progress of processing a {@link Executable}. 
 * This dialog uses a {@link ProgressPane} to display the progress.
 * 
 * @param <F> the final result to be passed to the executable
 * 
 * @author Holger Eichelberger
 * @since 2.0
 * @version 2.0
 */
@SuppressWarnings("serial")
public class ProgressDialog<F> extends JDialog {

    /**
     * Stores the executable.
     * 
     * @since 2.0
     */
    private Executable<F> executable;

    /**
     * Stores the progress pane containing the progress bar.
     * 
     * @since 2.0
     */
    private ProgressPane<F> progressPane;

    /**
     * Creates a new progress dialog.
     * 
     * @param parent the parent Swing component
     * @param executable the functionality to be 
     *        executed as a thread. This instance is 
     *        controlled to visualize the progress.
     * 
     * @since 2.0
     */
    public ProgressDialog(JFrame parent, Executable<F> executable) {
        super(parent, executable.getDescription(), false);
        this.executable = executable;

        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        progressPane = new ProgressPane<F>(executable, true);
        pane.add(progressPane);

        if (executable.isInterruptible()) {
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent event) {
                    ProgressDialog.this.executable.interrupt();
                }
            });
        }

        pack();
        setVisible(true);

        progressPane.start();
    }

}
