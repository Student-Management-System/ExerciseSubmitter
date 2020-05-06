package de.uni_hildesheim.sse.exerciseSubmitter.submission;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Defines a submission directory entry.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public class SubmissionDirEntry implements Comparable<SubmissionDirEntry> {

    /**
     * Stores the date format to be applied.
     * 
     * @since 2.00
     */
    private static final DateFormat DATE_FORMAT = 
        new SimpleDateFormat("dd.MM.yyyy HH:mm");
    
    /**
     * Stores the one decimal position format to be applied.
     * 
     * @since 2.00
     */
    private static final DecimalFormat ONEDEC_FORMAT = new DecimalFormat("0,0");
    
    /**
     * Stores the path of the entry.
     * 
     * @since 2.00
     */
    private String path;
    
    /**
     * Stores the size of the entry in bytes.
     * 
     * @since 2.00
     */
    private long size;
    
    /**
     * Stores the date of the last change.
     * 
     * @since 2.00
     */
    private Date date;
    
    /**
     * Stoes if this entry is a directory.
     * 
     * @since 2.00
     */
    private boolean isDirectory;
    
    /**
     * Stores the author of the last change.
     * 
     * @since 2.00
     */
    private String author;

    /**
     * Creates a new directory entry.
     * 
     * @param path the name/path of the file/directory
     * @param size the size of the directory
     * @param date the date of the last change
     * @param isDirectory is it a directory or a file
     * @param author the author of the last change
     * 
     * @since 2.00
     */
    public SubmissionDirEntry(String path, long size, Date date, 
        boolean isDirectory, String author) {
        this.path = path;
        this.size = size;
        this.date = date;
        this.isDirectory = isDirectory;
        this.author = author;
    }

    /**
     * Returns the author who changed this entry last.
     * 
     * @return the author of the last change
     * 
     * @since 2.00
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Stores the date of the last change.
     * 
     * @return the date
     * 
     * @since 2.00
     */
    public Date getDate() {
        return date;
    }

    /**
     * Stores if it is a directory.
     * 
     * @return <code>true</code> if it is a 
     * directory, <code>false </code> else
     * 
     * @since 2.00
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    /**
     * Returns the path/name of this entry.
     * 
     * @return the path
     * 
     * @since 2.00
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the size of this entry.
     * 
     * @return the size
     * 
     * @since 2.00
     */
    public long getSize() {
        return size;
    }
    
    /**
     * Returns the formatted date.
     * 
     * @return the formatted date
     * 
     * @since 2.00
     */
    public String getFormattedDate() {
        return DATE_FORMAT.format(date);
    }

    /**
     * Returns the formatted size of this entry.
     * 
     * @return the formatted size
     * 
     * @since 2.00
     */
    public String getFormattedSize() {
        double siz = size;
        int mag = 0;
        while (siz > 1024) {
            siz /= (double) 1024;
            mag++;
        }
        
        String form = ONEDEC_FORMAT.format(siz);
        String result = "";
        switch (mag) {
        case 0:
            result = size + " B";
            break;
        case 1:
            result = form + " kB";
            break;
        case 2:
            result = form + " mB";
            break;
        case 3:
            result = form + " gB";
            break;
        case 4:
            result = form + " tB";
            break;
        default:
            result = "very big";
            break;
        }
        return result;
    }

    /**
     * Returns if this entry is "less than", "equal" to
     * or "greater than" <code>entry</code>.
     * 
     * @param entry the entry to be compared
     * @return <code>-1</code> if "less than", 
     *         <code>0</code> if "equal", <code>1</code>
     *         if "greater than"
     */
    public int compareTo(SubmissionDirEntry entry) {
        return path.compareTo(entry.path);
    }

}
