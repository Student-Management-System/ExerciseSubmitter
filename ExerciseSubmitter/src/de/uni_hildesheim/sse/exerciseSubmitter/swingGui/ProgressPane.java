package de.uni_hildesheim.sse.exerciseSubmitter.swingGui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Executable;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    ProgressFinishedListener;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ProgressListener;

/**
 * A Swing pane showing the progress of processing a {@link Executable}.
 * The template parameter denotes the result of processing an 
 * {@link Executable}.
 * 
 * @param <F> the final result to be passed to the executable
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
@SuppressWarnings("serial")
public class ProgressPane<F> extends JPanel {

    /**
     * Stores the executable.
     * 
     * @since 2.0
     */
    private Executable<F> executable;

    /**
     * Stores if the executable was initially specified in
     * the constructor.
     * 
     * @since 2.0
     */
    private boolean executableInitiallySpecified = false;

    /**
     * Stores the progress bar.
     * 
     * @since 2.0
     */
    private JProgressBar progressBar;

    /**
     * Stores the cancel button.
     * 
     * @since 2.0
     */
    private JButton cancel;

    /**
     * Stores the text to be displayed next to the {@link #progressBar}.
     * 
     * @since 2.0
     */
    private JLabel progressText;

    /**
     * Stores a progress listener optionally specified from outside
     * this class. (Usually this class provides an internal 
     * implementation).
     * 
     * @since 2.0
     */
    private ProgressListener<F> externalListener;

    /**
     * Stores a finished listener to be informed, when the threaded
     * execution of the {@link #executable} is finished.
     * 
     * @since 2.0
     */
    private ProgressFinishedListener<F> finishedListener;

    /**
     * Creates a new progress pane.
     * 
     * @param executable the functionality to be executed
     * @param showCancel if the cancel button should be displayed
     * 
     * @since 2.0
     */
    public ProgressPane(Executable<F> executable, boolean showCancel) {
        super();
        setLayout(new BorderLayout());
        progressBar = new JProgressBar(0, 10);
        progressText = new JLabel();
        JPanel pPanel = new JPanel();
        pPanel.setLayout(new BorderLayout());
        pPanel.add(progressBar, BorderLayout.NORTH);
        pPanel.add(progressText, BorderLayout.CENTER);
        add(pPanel, BorderLayout.NORTH);
        JPanel temp = new JPanel();
        temp.setLayout(new FlowLayout(FlowLayout.CENTER));
        cancel = new JButton("Abbrechen");
        cancel.setVisible(showCancel);
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (null != ProgressPane.this.executable) {
                    ProgressPane.this.executable.interrupt();
                }
            }

        });
        temp.add(cancel);
        add(temp, BorderLayout.SOUTH);
        executableInitiallySpecified = (null != executable);
        setProgressable(executable);
    }

    /**
     * Defines or changes the executable.
     * 
     * @param executable the functionality to be executed
     * 
     * @since 2.0
     */
    public void setProgressable(Executable<F> executable) {
        if (null == externalListener) {
            this.executable = executable;
            if (null != executable) {
                progressBar.setMaximum(Math.max(1, executable
                    .getNumberOfSteps()));
                progressBar.setValue(0);
                executable.setProgressListener(new DialogProgressListener());
                cancel.setEnabled(executable.isInterruptible());
            } else {
                cancel.setEnabled(false);
            }
        }
    }

    /**
     * Defines the listener to be notified about the ending of the 
     * executed process.
     * 
     * @param listener the listener to be notified
     * 
     * @since 2.0
     */
    public void setProgressFinishedListener(
        ProgressFinishedListener<F> listener) {
        this.finishedListener = listener;
    }

    /**
     * Creates a progress listener matching the type requirements of
     * this progress pane. The returned lister may then be used in
     * other contexts but keeps control over the {@link #progressBar} and 
     * the {@link #executable} of this pane.
     * 
     * @return the newly created progress listener
     * 
     * @since 2.0
     */
    public ProgressListener<F> createExternalProgressListener() {
        if (null == executable) {
            externalListener = new DialogProgressListener();
            progressBar.setValue(0);
        }
        return externalListener;
    }

    /**
     * Starts the {@link #executable} as own thread.
     *
     * @since 2.0
     */
    public void start() {
        Thread thread = new Thread(executable);
        thread.start();
    }

    /**
     * Implements a progress listener for this dialog.
     * 
     * @author Holger Eichelberger
     * @since 2.0
     * @version 2.0
     */
    private class DialogProgressListener implements ProgressListener<F> {

        /**
         * Is called when a step was processed.
         * 
         * @param step the progressed step
         * 
         * @since 2.0
         */
        public void processedStep(int step) {
            progressBar.setValue(step);
        }

        /**
         * Is called when the total number of steps is changed.
         * 
         * @param steps the new total number of steps
         * 
         * @since 2.0
         */
        public void numberofStepsChanged(int steps) {
            assert progressBar.getMinimum() <= steps;
            assert steps <= progressBar.getMaximum();
            progressBar.setMaximum(steps);
        }

        /**
         * Is called when processing is finished.
         * 
         * @param max set the associated progress bar to its
         *        maximum value (or keep the current value)
         * @param finished the result of the processing
         * 
         * @since 2.0
         */
        public void finished(boolean max, F finished) {
            progressBar.setIndeterminate(false);
            progressBar.setValue(max ? progressBar.getMaximum() : progressBar
                .getMinimum());
            notifyNextStep("");
            cancel.setEnabled(false);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            if (!executableInitiallySpecified) {
                executable = null;
            }
            externalListener = null;
            if (null != finishedListener) {
                finishedListener.progressFinished(finished, null);
            }
        }

        /**
         * Notifies the beginning of the next step by announcing
         * the next task description (optional).
         * 
         * @param description the next task description to be displayed
         * 
         * @since 2.0
         */
        public void notifyNextStep(final String description) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressText.setText(description);

                }
            });
        }

        /**
         * Switches from individual step visualization to sweeping
         * infinite visualization.
         * 
         * @param doSweep <code>true</code> if sweeping should be 
         *        activated, <code>false</code> for individual step
         *        display
         *        
         * @since 2.0
         */
        public void sweep(boolean doSweep) {
            progressBar.setIndeterminate(true);
        }

        /**
         * Notifies this listener that an unhandled exception occurred.
         * 
         * @param exception the occurred exception
         * 
         * @since 2.0
         */
        public void notifyExceptionOccurred(Exception exception) {
            if (IConfiguration.INSTANCE.isDebuggingEnabled()) {
                exception.printStackTrace(System.out);
            }
            if (exception instanceof CommunicationException) {
                JOptionPane.showMessageDialog(ProgressPane.this,
                    ((CommunicationException) exception).getMessage());
            } else {
                JOptionPane.showMessageDialog(ProgressPane.this, exception
                    .getMessage());
            }
        }

        /**
         * Returns the current step number.
         * 
         * @return the current processing step number 
         * (1-max, see {@link #numberofStepsChanged(int)})
         * 
         * @since 2.0
         */
        public int getStep() {
            return progressBar.getValue();
        }

    }

}
