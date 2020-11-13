package de.uni_hildesheim.sse.exerciseSubmitter.submission;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Defines common utility methods.
 * 
 * @author Alexander Schmehl
 * @since 1.00
 * @version 2.00
 */
public class CommonStuff {

    /**
     * Prevents from creating instances of this class.
     * 
     * @since 2.00
     */
    private CommonStuff() {
    }
    
    /**
     * Ensures that a path ends with a separator.
     * 
     * @param path the path to be checked
     * @return <code>pfad</code> if <code>path</code> does not
     *         end with {@link java.io.File#separator}, 
     *         <code>path+{@link java.io.File#separator}</code>
     *         else
     *         
     * @since 1.00
     */
    public static String ensureCleanPath(String path) {
        if (!path.endsWith(File.separator)) {
            path = path.concat(File.separator);
        }
        return path;
    }

    /**
     * Creates a temporary directory.
     * 
     * @param prefix the prefix of the name with which the temporary directory 
     *        should start
     * @return the file object of the temporary directory
     * @throws IOException if the temporary directory cannot be created, etc.
     * 
     * @since 1.00
     */
    public static File createTmpDir(String prefix) throws IOException {
        while (prefix.length() < 3) {
            prefix = prefix + "a";
        }
        File myfile = File.createTempFile(prefix, null);
        myfile.delete();
        if (!myfile.mkdirs()) {
            throw new IOException();
        }
        return myfile;
    }

    /**
     * Creates a temporary directory (calls {@link #createTmpDir(String)}).
     * 
     * @return the file object of the temporary directory
     * @throws IOException if the temporary directory cannot be created, etc.
     * 
     * @since 1.00
     */
    public static File createTmpDir() throws IOException {
        return createTmpDir("");
    }

    /**
     * Ensures that the path to the specified filename exists.
     * 
     * @param filename the name the existence of path should be ensured for
     * @throws IOException if errors occur while creating missing directories
     * 
     * @since 1.00
     */
    public static void createPathToFile(String filename) throws IOException {
        File file = new File(filename);
        File directory = new File(file.getAbsolutePath().substring(0,
                filename.length() - file.getName().length()));
        directory.mkdirs();
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException();
        }
    }
 
    /**
     * Deletes a given directory and all contained files and directories.
     * 
     * @param dir the directory to be deleted
     * @param self <code>true</code> if also <code>dir</code> should 
     *        be deleted, or only the contents (<code>false</code>)
     *
     * @since 1.00
     */
    public static void rmdir(File dir, boolean self) {
        File[] filelist = dir.listFiles();
        if (null != filelist) {
            for (File file : filelist) {
                if (file.isFile()) {
                    file.delete();
                }
                if (file.isDirectory()) {
                    rmdir(file.getAbsoluteFile(), true);
                }
            }
        }
        if (self) {
            dir.delete();
        }
    }

    /**
     * Copies all files (or directories) from <code>source</code> to
     * <code>target</code>.
     * 
     * @param source the source directory to be copied
     * @param target the target directory to be copied
     * @param recursive <code>true</code> if all 
     *        sub directories of <code>source</code> 
     *        should also be copied, <code>false</code>
     *        if only the files in <code>source</code> should
     *        be copied
     * @throws IOException if any input/output error occurs
     * 
     * @since 2.00
     */
    public static void copy(File source, File target, boolean recursive)
        throws IOException {
        copy(source, source.getAbsolutePath().length(), target, recursive);
    }

    /**
     * Copies all files (or directories) from <code>source</code> to
     * <code>target</code>.
     * 
     * @param source the source directory to be copied
     * @param skip the number of characters to be skipped as 
     *        prefix from <code>source</code>
     * @param target the target directory to be copied
     * @param recursive <code>true</code> if all 
     *        sub directories of <code>source</code> 
     *        should also be copied, <code>false</code>
     *        if only the files in <code>source</code> should
     *        be copied
     * @throws IOException if any input/output error occurs
     * 
     * @since 2.00
     */
    private static void copy(File source, int skip, File target,
            boolean recursive) throws IOException {
        String sName = source.getAbsolutePath();
        File tFile;
        if (sName.length() == skip) {
            tFile = target;
        } else {
            tFile = new File(target, sName.substring(skip + 1));
        }
        if (recursive && source.isDirectory()) {
            tFile.mkdirs();
            for (File f : source.listFiles()) {
                copy(f, skip, target, recursive);
            }
        } else {
            File parent = tFile.getParentFile();
            if (parent.isDirectory() && !parent.exists()) {
                parent.mkdirs();
            }
            
            try (FileInputStream sourceStream = new FileInputStream(source);
                    FileOutputStream targetStream = new FileOutputStream(target)) {
                FileChannel sourceChannel = sourceStream.getChannel();
                FileChannel targetChannel = targetStream.getChannel();
                
                targetChannel.transferFrom(sourceChannel, 0, source.length());
                targetChannel.close();
                sourceChannel.close();
            }
        }
    }

}