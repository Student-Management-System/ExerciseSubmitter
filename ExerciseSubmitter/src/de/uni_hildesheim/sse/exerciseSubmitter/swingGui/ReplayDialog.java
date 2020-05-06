package de.uni_hildesheim.sse.exerciseSubmitter.swingGui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ISubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ProgressListener;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IVersionedSubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Submission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;

/**
 * Realizes the Swing dialog for replaying a submission.
 * 
 * @author Holger Eichelberger
 * @since 2.0
 * @version 2.0
 */
public class ReplayDialog extends JDialog {

    /**
     * Defines the unique identifier for serialization.
     * 
     * @since 2.0
     */
    private static final long serialVersionUID = 289883385729441615L;

    /**
     * Stores the list of exercises or version to be replayed.
     * 
     * @since 2.0
     */
    private JList list;

    /**
     * Stores the data in {@link #list}. Since {@link IVersionedSubmission} as
     * well as (legacy) String instances may occur, the most common type was
     * chosen.
     * 
     * @since 2.0
     */
    private Vector<Object> data;

    /**
     * Stores the button to start playing back a submission to the 
     * local machine.
     * 
     * @since 2.0
     */
    private JButton playBack;

    /**
     * The server communication proxy.
     * 
     * @since 2.0
     */
    private SubmissionCommunication comm;

    /**
     * Stores the progress visualization pane.
     * 
     * @since 2.0
     */
    private ProgressPane<ISubmission> progress;

    /**
     * Stores the target directory where to write the
     * replayed submission to.
     * 
     * @since 2.0
     */
    private File targetDir;

    /**
     * Creates a new replay dialog.
     * 
     * @param frame the parent frame
     * @param comm the communication proxy for communication
     *        with the submission server
     * @param task the concrete description of the task to be
     *        executed (e.g. replay corrected submission)
     * @param corrected <code>true</code> if corrected submissions
     *        should be replayed, <code>false</code> if a version of
     *        the current active submission should be replayed
     * @param targetDir the directory where to write the sumission to
     * 
     * @since 2.0
     */
    public ReplayDialog(JFrame frame, SubmissionCommunication comm,
            String task, boolean corrected, File targetDir) {
        super(frame, "Version wiederherstellen", true);
        this.comm = comm;
        this.targetDir = targetDir;
        Container pane = getContentPane();

        pane.setLayout(new BorderLayout());
        if (!corrected) {
            try {
                data = new Vector<Object>(comm.getSubmissionsForReplay(task));
                Collections.reverse(data);
            } catch (CommunicationException e) {
                data = new Vector<Object>();
            }
        } else {
            data = new Vector<Object>();
            for (String s : comm.getSubmissionsForReplay()) {
                data.add(s);
            }
        }
        list = new JList(data);
        list.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent event) {
                ReplayDialog.this.playBack.setEnabled(!ReplayDialog.this.list
                        .isSelectionEmpty());
            }

        });

        pane.add(new JScrollPane(list,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new BorderLayout());
        progress = new ProgressPane<ISubmission>(null, false);
        south.add(progress, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        south.add(buttonPanel, BorderLayout.SOUTH);

        playBack = new JButton("Rückspielen");
        playBack.setEnabled(false);
        playBack.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (!list.isSelectionEmpty()) {
                    if (JOptionPane.OK_OPTION == JOptionPane
                            .showConfirmDialog(
                                    ReplayDialog.this,
                                    "Das Rückspielen einer Abgabe " 
                                    + "überschreibt alle Dateien im "
                                    + "aktuellen Abgabeverzeichnis! "
                                    + "Überschreiben?")) {
                        SwingUtilities.invokeLater(new ReplaySubmission(
                                ReplayDialog.this, ReplayDialog.this.comm, list
                                        .getSelectedValue(),
                                ReplayDialog.this.progress
                                        .createExternalProgressListener()));
                    }
                }
            }

        });
        buttonPanel.add(playBack);
        JButton button = new JButton("Schließen");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                ReplayDialog.this.setVisible(false);
            }

        });
        buttonPanel.add(button);
        pane.add(south, BorderLayout.SOUTH);

        setDefaultCloseOperation(HIDE_ON_CLOSE);

        pack();
        setVisible(true);
    }

    /**
     * Realizes the runnable to execute the replay of a
     * submission.
     * 
     * @author Holger Eichelberger
     * @since 2.0
     * @version 2.0
     */
    private class ReplaySubmission implements Runnable {

        /**
         * Stores the object describing the submission. This may
         * be a {@link IVersionedSubmission} or a (legacy) string.
         * 
         * @since 2.0
         */
        private Object selectedSubmission;

        /**
         * The server communication proxy.
         * 
         * @since 2.0
         */
        private SubmissionCommunication comm;

        /**
         * Stores the parent window component.
         * 
         * @since 2.0
         */
        private Component parent;

        /**
         * Stores the associated progress listener.
         * 
         * @since 2.0
         */
        private ProgressListener<ISubmission> listener;

        /**
         * Creates a new progressing object.
         * 
         * @param parent the parent GUI component
         * @param comm the server communication proxy
         * @param selectedSubmission the object describing the
         *        selected submission
         * @param listener the progress listener to be passed
         *        to <code>comm</code>
         */
        ReplaySubmission(Component parent, SubmissionCommunication comm,
                Object selectedSubmission, 
                ProgressListener<ISubmission> listener) {
            this.selectedSubmission = selectedSubmission;
            this.parent = parent;
            this.comm = comm;
            this.listener = listener;
        }

        /**
         * Executes the replay of the selected submission.
         * 
         * @since 2.0
         */
        public void run() {
            try {
                ISubmission submission = new Submission();
                submission.setPath(ReplayDialog.this.targetDir);
                if (selectedSubmission instanceof IVersionedSubmission) {
                    submission = comm.replaySubmission(submission,
                            (IVersionedSubmission) selectedSubmission, 
                            listener);
                } else {
                    submission = comm.replaySubmission(submission, 
                            selectedSubmission.toString(),
                            listener);
                }
            } catch (CommunicationException ex) {
                if (IConfiguration.INSTANCE.isDebuggingEnabled()) {
                    ex.printStackTrace(System.out);
                }
                JOptionPane.showMessageDialog(parent, ex.getMessage(), 
                    "Zurückladen fehlgeschlagen",
                    JOptionPane.ERROR_MESSAGE);
            } 
        }

    }
}
