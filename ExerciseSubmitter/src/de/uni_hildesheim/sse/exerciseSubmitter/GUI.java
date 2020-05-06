package de.uni_hildesheim.sse.exerciseSubmitter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ISubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Executable;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    ProgressFinishedListener;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Submission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;
import de.uni_hildesheim.sse.exerciseSubmitter.swingGui.ProgressPane;
import de.uni_hildesheim.sse.exerciseSubmitter.swingGui.ReplayDialog;

/**
 * Klasse, die die grafische Oberflaeche f�r den Benutzer bereitstellt.
 * 
 * @author Maria Horn
 * 
 */

public class GUI extends JPanel implements ActionListener {

    /**
     * Stores the version information for serialization.
     */
    private static final long serialVersionUID = 6639337169527911392L;

    /**
     * Frame zum Authentifizieren des Users.
     */
    private JFrame frame1;

    /**
     * Frame zum Abgeben der Loesungen, zurueckspielen aelterer Versionen und
     * Status der Abgabe.
     */
    private JFrame frame2;

    /**
     * Panel fuer den rechten Reiter des 2. Frames zum Abgeben der Loesungen.
     */
    private JPanel rechts;

    /**
     * Panel fuer den linken Reiter des 2. Frames.
     */
    private JPanel links;

    /**
     * untere Teil des linken Reiters des 2. Frames.
     */
    private JPanel linksunten;

    /**
     * oberer Teil des linken Reiters des 2. Frames.
     */
    private JPanel linksoben;

    /**
     * oberer Teil des rechten Reiters des 2. Frames.
     */
    private JPanel rechtsunten;

    /**
     * untere Teil des rechten Reiters des 2. Frames.
     */
    private JPanel rechtsoben;

    /**
     * stellt im 2. Frame die 2 Reiter "Aufgabe/ Loesung" und "Abgabeergebnisse"
     * bereit.
     */
    private JTabbedPane tabbedPane;

    /**
     * Button fuer den Benutzer um sich mit Hilfe des FileChoosers ein
     * Loesungsverzeichnis auszuwaehlen.
     */
    private JButton auswahl;

    /**
     * Button fuer den Benutzer, um seine Loesungen abzugeben.
     */
    private JButton lsg;

    /**
     * Button fuer den Benutzer, um eine aeltere Version zur�ck zu spielen.
     */
    private JButton version;

    /**
     * Button zum zur&uuml;ckspielen einer Abgabe.
     */
    private JButton replaySubmission;

    /**
     * Authetifizierungs-Button f�r den Benutzer.
     */
    private JButton login;

    /**
     * Eingabefeld fuer den Benutzernamen im Klartext.
     */
    private JTextField userName;

    /**
     * Eingabefeld fuer den Gruppen im Klartext.
     */
    private JTextField groupName;
    
    /**
     * Eingabefeld fuer das Passwort nicht im Klartext.
     */
    private JPasswordField password;

    /**
     * TextArea in der der Pfad des Loesungsverzeichnisses angegeben wird.
     */
    private JTextArea abgabePfad;

    /**
     * TextArea, zeigt nach dem Testdurchlauf die Abgaben mit eventuellen
     * Fehlern.
     */
    private JTextArea abgfeld;

    /**
     * TextArea zeigt an, ob die Loesungabgelehnt wurde, oder nicht.
     */
    private JTextArea statusfeld;

    /**
     * Speichert die initiale Farbe (Status).
     */
    private Color initialColor;

    /**
     * FileChooser, bestimmt den Pfad f�r das Loesungsverzeichnis und zeigt
     * ihn im log an.
     */
    private JFileChooser fc;

    /**
     * JLabel, welches dem Benutzer auffordert seinen Passwort in das
     * Eingabefeld p_eingabe zu schreiben.
     */
    private JLabel passwd;

    /**
     * JLabel, welches dem Benutzer auffordert seinen Benutzernamen in das
     * Eingabefeld n_eingabe zu schreiben.
     */
    private JLabel name;

    /**
     * JLabel, welches dem Benutzer dan Status seiner Abgaben andeutet.
     */
    private JLabel status;

    /**
     * Checkbox, um das Passwort zu einem Benutzernamen speichern.
     */
    private JCheckBox ch;

    /**
     * Auswahlbox, welche die zur Verfuegung stehenden Abgabemoeglichkeiten
     * bereit stellt.
     */
    private JComboBox abgabewahl;

    /**
     * Speichert das Kommunikationsobjekt.
     */
    private SubmissionCommunication kommlink;

    /**
     * Speichert das Panel zu Darstellung des Abgabeprozesses.
     */
    private ProgressPane<ISubmission> progressPanel;

    /**
     * Erstellt eine grafische Oberfl�che.
     * 
     */
    public GUI() {
        // Erzeugen eines Fensters fuer die Authentifizierung
        frame1 = new JFrame("Authentifizierung");
        Container pane1 = frame1.getContentPane();
        pane1.setLayout(new BorderLayout());
        // Erzeugen eines Fensters f�r die Projektabgabe
        frame2 = new JFrame("Projektabgabe");
        Container pane2 = frame2.getContentPane();
        pane2.setLayout(new BorderLayout());

        // Erstellen eines FileChoosers, der nur Verzeichnisse ausw�hlt
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // 1. Fenster fuer Benutzerdaten zur Authentifizierung
        JPanel auth = new JPanel();
        int rows = 4;
        if (IConfiguration.INSTANCE.isExplicitGroupNameEnabled()) {
            rows++;
        }
        auth.setLayout(new GridLayout(rows, 2));
        if (IConfiguration.INSTANCE.isExplicitGroupNameEnabled()) {
            name = new JLabel("Gruppen/Teamname:");
            auth.add(name);
            groupName = new JTextField(
                IConfiguration.INSTANCE.getGroupName(), 20);
            auth.add(groupName);
        }
        name = new JLabel("Benutzername:");
        auth.add(name);
        userName = new JTextField(IConfiguration.INSTANCE.getUserName(), 20);
        auth.add(userName);
        passwd = new JLabel("Passwort:");
        auth.add(passwd);
        password = new JPasswordField(IConfiguration.INSTANCE.getPassword()
            , 20);
        auth.add(password);
        ch = new JCheckBox("Passwort speichern");
        ch.setSelected(IConfiguration.INSTANCE.getUserName().length() > 0
            && IConfiguration.INSTANCE.getPassword().length() > 0);
        auth.add(ch);
        ch.addActionListener(this);
        login = new JButton("Authentifizieren");
        auth.add(login);
        login.addActionListener(this);
        pane1.add(auth, BorderLayout.NORTH);

        Border blackline;
        blackline = BorderFactory.createLineBorder(Color.black);
        TitledBorder title;
        title = BorderFactory.createTitledBorder(blackline, "Benutzerdaten");
        auth.setBorder(title);

        // 2. Fenster fuer die Abgaben mit 2 Reitern
        // 1. Reiter von Frame2
        links = new JPanel();
        linksunten = new JPanel();
        linksoben = new JPanel();
        tabbedPane = new JTabbedPane();
        linksoben.setLayout(new BorderLayout());
        linksunten.setLayout(new BorderLayout());
        abgabewahl = new JComboBox();
        linksoben.add(abgabewahl, BorderLayout.NORTH);
        pane2.add(linksoben, BorderLayout.NORTH);

        title = BorderFactory.createTitledBorder(blackline, "Abgeben als:");
        linksoben.setBorder(title);

        abgabePfad = new JTextArea(1, 20);
        abgabePfad.setMargin(new Insets(5, 5, 5, 5));
        abgabePfad.setEditable(false);
        linksunten.add(abgabePfad, BorderLayout.NORTH);
        auswahl = new JButton("Auswaehlen");
        auswahl.addActionListener(this);

        JPanel lbuttons = new JPanel();
        lbuttons.setLayout(new BorderLayout());
        JPanel lbuttonsNorth = new JPanel();
        lbuttonsNorth.add(auswahl);
        lbuttons.add(lbuttonsNorth, BorderLayout.NORTH);
        JPanel lbuttonsSouth = new JPanel();
        lbuttonsSouth.setLayout(new FlowLayout());
        lbuttons.add(lbuttonsSouth, BorderLayout.CENTER);
        version = new JButton("Korrektur zurueckladen");
        version.addActionListener(this);
        lbuttonsSouth.add(version);
        replaySubmission = new JButton("Version zurueckladen");
        replaySubmission.addActionListener(this);
        lbuttonsSouth.add(replaySubmission);

        linksunten.add(lbuttons, BorderLayout.CENTER);
        pane2.add(linksunten, BorderLayout.SOUTH);

        title = BorderFactory.createTitledBorder(blackline,
            "Loesungsverzeichnis:");
        linksunten.setBorder(title);
        links.setLayout(new GridLayout(2, 2));
        links.add(linksoben);
        links.add(linksunten);
        tabbedPane.addTab("Aufgabe/ Loesung", links);

        // 2. Reiter von Frame2
        rechts = new JPanel();
        rechtsunten = new JPanel();
        rechtsoben = new JPanel();
        rechtsoben.setLayout(new GridLayout(4, 1));
        abgfeld = new JTextArea(4, 20);
        rechtsoben.add(new JScrollPane(abgfeld,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        abgfeld.setMargin(new Insets(5, 5, 5, 5));
        abgfeld.setEditable(false);
        status = new JLabel("Status:");
        rechtsoben.add(status);
        statusfeld = new JTextArea(1, 10);
        statusfeld.setMargin(new Insets(5, 5, 5, 5));
        statusfeld.setEditable(false);
        initialColor = statusfeld.getBackground();
        rechtsoben.add(statusfeld);

        title = BorderFactory.createTitledBorder(blackline, "Abgabeergebnisse");
        rechtsoben.setBorder(title);
        pane2.add(rechtsoben);
        progressPanel = new ProgressPane<ISubmission>(null, false);
        rechtsoben.add(progressPanel);

        rechtsunten.setLayout(new FlowLayout());
        lsg = new JButton("Loesung abgeben");
        rechtsunten.add(lsg);
        lsg.addActionListener(this);
        pane2.add(rechtsunten);

        rechts.setLayout(new GridLayout(2, 2));
        rechts.add(rechtsoben);
        rechts.add(rechtsunten);
        tabbedPane.addTab("Abgabe", rechts);
        // Frame1 packen, Groesse und Position setzen und sichtbar machen
        frame1.pack();
        frame1.setSize(350, 170);
        frame1.setLocation(300, 250);
        frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame1.setVisible(true);
        // Frame2 packen, Groesse und Position setzen und sichtbar machen
        frame2.pack();
        frame2.setSize(400, 350);
        frame2.setLocation(300, 250);
        frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame2.setVisible(false);
        frame2.getContentPane().add(tabbedPane);
    }

    /**
     * Erzeugt neue grafische Oberflaeche.
     * 
     * @param args
     *            Kommandozeilenparameter (ignoriert)
     */
    public static void main(String[] args) {
        // IConfiguration.setDefaultInstance();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new GUI();
            }
        });
    }

    /**
     * Verwaltet die Benutzeraktionen, wenn die Knoepfe "Authentifizieren",
     * "Auswaehlen", "Loesungen abgeben" und "Aeltere Version zurueckspielen"
     * gedrueckt werden.
     * 
     * @param event das zu verarbeitende Ereignis
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == auswahl) {
            int returnVal = fc.showOpenDialog(GUI.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String pfad = fc.getSelectedFile().getAbsolutePath();
                abgabePfad.setText(pfad);
            }
            abgabePfad.setCaretPosition(abgabePfad.getDocument().getLength());
        }
        // Wird "Loesungen abgeben" gedrueckt, werden alle Dateien unter dem
        // best Pfad als Zip zum Server geschickt.
        if (event.getSource() == lsg) {
            ISubmission abgabe = new Submission();
            abgabe.setPath(new File(abgabePfad.getText()));
            // sende das Objekt an den Server
            try {
                statusfeld.setBackground(initialColor);
                statusfeld.setText("");
                Executable<ISubmission> exec = kommlink.submit(abgabe,
                    (String) abgabewahl.getSelectedItem());
                progressPanel.setProgressable(exec);
                progressPanel.setProgressFinishedListener(
                    new ProgressFinishedListener<ISubmission>() {

                        public void progressFinished(ISubmission finished,
                            Exception exception) {
                            if (finished instanceof ISubmission) {
                                ISubmission ab = (ISubmission) finished;
                                abgfeld.setText(ab.getUnparsedMessage(null));
                                switch (ab.getResult()) {
                                case SUCCESSFUL:
                                    statusfeld.setText("Erfolgreiche Abgabe");
                                    statusfeld.setBackground(Color.GREEN);
                                    break;
                                case FAILED:
                                    statusfeld.setText("Abgabe fehlgeschlagen");
                                    statusfeld.setBackground(Color.RED);
                                    break;
                                case EMPTY:
                                    statusfeld.setText(
                                        "Abgabe leer - nichts auszufuehren");
                                    statusfeld.setBackground(Color.GREEN);
                                    break;
                                case POST_SUCCESS:
                                    statusfeld.setText("Erfolgreiche Abgabe");
                                    statusfeld.setBackground(Color.GREEN);
                                    break;
                                case POST_FAILED:
                                    statusfeld.setText("Erfolgreiche Abgabe. "
                                        + "Es wurden jedoch Fehler im Programm "
                                        + "gefunden.");
                                    statusfeld.setBackground(Color.YELLOW);
                                    break;
                                default:
                                    break;
                                }
                            }
                        }

                    });
                progressPanel.start();
            } catch (Exception e2) {
                System.err
                    .println("Bei der Abgabe trat leider folgender Fehler auf: "
                    + e2);
            }
        }
        // Wird "Aeltere Version zurueckspielen" gedrueckt, werden die unter
        // Choice gewaehlten Aufgaben vom Server geholt.
        if (event.getSource() == version
            || event.getSource() == replaySubmission) {
            if (null == abgabePfad) {
                JOptionPane.showMessageDialog(frame2,
                    "Bitte erst ein Verzeichnis w�hlen!");
            } else {
                new ReplayDialog(frame2, kommlink, (String) abgabewahl
                    .getSelectedItem(), event.getSource() == version, new File(
                        abgabePfad.getText()));
            }
        }
        // Wird "Authentifizieren" gedrueckt und der Checkbox-Haken ist nicht
        // gesetzt, werden die Daten aus den Eingabefeldern geholt.
        if (event.getSource() == login) {
            List<SubmissionCommunication> links;
            try {

                links = SubmissionCommunication.getInstances(
                    userName.getText(), new String(password.getPassword()),
                    false, null, null, IConfiguration.INSTANCE.
                    getExplicitFolderName(groupName.getText()));
                kommlink = links.get(0);
                frame2.setVisible(true);
                frame1.setVisible(false);
                if (ch.isSelected()) {
                    IConfiguration.INSTANCE.setUserName(userName.getText());
                    IConfiguration.INSTANCE.setGroupName(groupName.getText());
                    IConfiguration.INSTANCE.setPassword(new String(password
                        .getPassword()));
                } else {
                    IConfiguration.INSTANCE.setUserName("");
                    IConfiguration.INSTANCE.setPassword("");
                }
                IConfiguration.INSTANCE.store();
                for (String s : kommlink.getAvailableForSubmission()) {
                    abgabewahl.addItem(s);
                }
            } catch (CommunicationException ke) {
                if (IConfiguration.INSTANCE.isDebuggingEnabled()) {
                    ke.printStackTrace(System.out);
                }
                JOptionPane.showMessageDialog(this, ke.getMessage(), 
                    "Authentifizierung fehlgeschlagen",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}
