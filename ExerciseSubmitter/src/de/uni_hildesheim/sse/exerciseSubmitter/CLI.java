package de.uni_hildesheim.sse.exerciseSubmitter;

import java.io.Console;
import java.io.File;
import java.util.List;
import java.util.Scanner;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ISubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    Submission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;

/**
 * Das Hauptprogramm... eigentlich lediglich eine Art "proof of concept"
 * 
 * @author Alexander Schmehl
 * 
 */
public class CLI {

    /**
     * Das Kommunikationsinterface.
     */
    private static SubmissionCommunication kommlink;

    /**
     * Der Konfigurations-Manager zur einfachen Verwaltung der
     * Konfigurationsdaten.
     */
    private static IConfiguration konfig;

    /**
     * Prevents this class from being instantiated from outside.
     * 
     * @since 2.00
     */
    private CLI() {
    }
    
    /**
     * Diese Methode initialisiert {@link #kommlink} und {@link #konfig}.
     */
    private static void initSystem() {
        // instanzieren des Konfigurationsmanagers
        // versuche username und passwort auszulesen
        String username = IConfiguration.INSTANCE.getUserName();
        String groupname = IConfiguration.INSTANCE.getGroupName();
        String password = IConfiguration.INSTANCE.getPassword();
        // falls der user keinen Usernamen gespeichert hatte, wird er danach
        // gefragt
        if (0 == username.length()) {
            username = inputString(
                "Bitte geben Sie ihren Usernamen ein: ", false);
            if (IConfiguration.INSTANCE.isExplicitGroupNameEnabled() 
                && 0 == groupname.length()) {
                groupname = inputString(
                    "Bitte geben Sie ihren Gruppen/Teamnamen ein: ", false);
            }
        }
        // falls der user kein Passwort gespeichert hatte, wird er danach
        // gefragt
        if (0 == password.length()) {
            password = inputString("Bitte geben Sie ihr Passwort ein: ", true);
        }
        // falls sowohl username als auch passwort leer waren, fragen wir, ob es
        // gespeichert werden soll
        if (0 == konfig.getUserName().length() 
            && 0 == konfig.getPassword().length()) {
            // In der Konfigurationsdatei gab es nichts dazu
            if (inputString("Wollen Sie diese Daten speichern? ", false)
                .toLowerCase().trim().equals("ja")) {
                konfig.setUserName(username);
                konfig.setPassword(password);
                konfig.store();
            }
        }
        // instanzieren des Kommunikationsinterfaces
        System.out
                .println("Nehme Kontakt zum Server auf... dies kann "
                    + "einen Augenblick dauern...");
        try {
            List<SubmissionCommunication> links = SubmissionCommunication
                .getInstances(username, password, false, null, null, 
                IConfiguration.INSTANCE.getExplicitFolderName(groupname));
            if (links.isEmpty()) {
                System.err.println("Konfigurationsfehler... kein "
                    + "Kommunikationsprotokoll konfiguriert!");
                System.exit(0);
            } else {
                kommlink = links.get(0);
                System.out.println("Sehr gut, sie konnten am Server "
                    + "authentifiziert werden!");
            }
        } catch (CommunicationException ke) {
            System.err
                    .println("Leider konnten Sie nicht am Server "
                        + "identifiziert werden!");
            System.exit(1);
        }

    }

    /**
     * Baut ein einfaches menu auf, warten auf Benutzereingabe und ruft dann
     * entsprechende Funktionen auf.
     */
    private static void menu() {
        int eingabe = -1;
        while (!(eingabe == 0 || eingabe == 1 || eingabe == 2)) {
            System.out.println("\n\nWas möchten Sie nun machen?");
            System.out.println("\t1 - Eine Lösung zu einer Aufgabe abgeben");
            System.out.println("\t2 - Eine alte, kommentierte Lösung"
                + "herunterladen");
            System.out.println("\n\t0 - Das Programm beenden");
            eingabe = inputInt("");
            switch (eingabe) {
            case 0:
                System.out.println("Vielen Dank.  Sie haben ein einfaches "
                    + "Abgabe-Programm sehr gluecklich gemacht");
                System.exit(0);
                break;
            case 1:
                gebeab();
                break;
            case 2:
                laderunter();
                break;
            default:
            }
        }
    }

    /**
     * Gibt die angegebene Abgabe ab.
     * 
     * @since 1.00
     */
    private static void gebeab() {
        // hole eine Liste der Abgaben
        String[] abgaben = kommlink.getAvailableForSubmission();
        System.out.println("Folgende Aufgaben sind zur Abgabe freigegeben:");
        for (String s : abgaben) {
            System.out.println("\t" + s);
        }
        String welcheAbgabe = inputString("Zu welcher Aufgabe moechten "
            + "sie eine Loesung abgeben? ", false);
        String pfad = inputString("Welches Verzeichnis soll abgebenen "
            + "werden? ", false);
        // instanziere ein neues abgabe Objekt
        ISubmission abgabe = new Submission();
        abgabe.setPath(new File(pfad));
        // sende das Objekt an den Server
        try {
            kommlink.submit(abgabe, welcheAbgabe).executeAllSteps();
        } catch (Exception e) {
            System.err
                    .println("Bei der Abgabe trat leider folgender Fehler auf: "
                            + e);
        }
    }

    /**
     * L&auml;dt eine Abgabe vom Server herunter.
     * 
     * @since 1.00
     */
    private static void laderunter() {
        // hole eine Liste der Abgaben
        String[] abgaben = kommlink.getSubmissionsForReplay();
        System.out
                .println("Der Server kennt folgende alte Abgaben von ihnen: ");
        for (String s : abgaben) {
            System.out.println("\t" + s);
        }
        String welcheAbgabe = inputString("Welche Abgabe moechten sie "
            + "herunterladen? ", false);
        String pfad = inputString("In welches Verezichnis soll diese "
            + "gespeichert werden? ", false);
        ISubmission abgabe = new Submission();
        // herunterladend der Abgabe
        try {
            abgabe.setPath(new File(pfad));
            abgabe = kommlink.replaySubmission(abgabe, welcheAbgabe, null);
        } catch (Exception e) {
            System.err
                    .println("Bei Herunterladen der Abgabe trat leider "
                        + "folgender Fehler auf: " + e);
        }
        // speichern der Abgabe
        // try {
        // abgabe.transferToPath(pfad);
        // }
        // catch (IOException e) {
        // System.err.println("Beim Speichern der Abgabe trat leider folgender
        // Fehler auf: "+e);
        // }
    }

    /**
     * Gibt einen String aus, und liest einen Integer Wert ein.
     * 
     * @param meldung
     *            String der ausgegeben werden soll, bevor der Integer-Wert
     *            eingelesen wird
     * @return Der eingelesene Integerwert
     */
    private static int inputInt(String meldung) {
        System.out.print(meldung);
        Scanner sc = new Scanner(System.in);
        return sc.nextInt();
    }

    /**
     * Gibt einen String aus, und liest einen String ein.
     * 
     * @param meldung
     *            String der ausgegeben werden soll, bevor der String eingelesen
     *            wird
     * @param hidden soll die Eingabe angezeigt werden (fall m&ouml;glich)
     * @return Der eingelesene String
     */
    private static String inputString(String meldung, boolean hidden) {
        System.out.print(meldung);
        String result;
        Console console = System.console();
        if (hidden && null != console) {
            result = String.valueOf(console.readPassword());
        } else {
            Scanner sc = new Scanner(System.in);
            result = sc.next();
        }
        return result;
    }

    /**
     * Das Hauptprogramm.
     * 
     * @param args
     *            keine
     */
    public static void main(String[] args) {
        initSystem();
        menu();
    }

}
