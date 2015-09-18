package org.janelia.it.jacs.compute.util;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Some methods for dealing with files and directories.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileUtils {

	private static final Logger logger = Logger.getLogger(FileUtils.class);

	/**
	 * Write the given string to a file, overwriting the current content.
	 * @param file
	 * @param s
	 */
    public static void writeStringToFile(File file, String s) {
    	writeStringToFile(file, s, false);
    }
    
	/**
	 * Write the given string to a file.
	 * @param file
	 * @param s
	 */
    public static void writeStringToFile(File file, String s, boolean append) {
    	BufferedWriter out = null;
    	try {
    		out = new BufferedWriter(new FileWriter(file, append));
            out.write(s);
    	} 
    	catch (Exception e) {
    		logger.error("Could not write to file: "+file.getAbsolutePath(),e);
    	}
    	finally {
    		try {
    			out.close();
    		}
            catch (Exception e) {
        		logger.warn("Could not close file: "+file.getAbsolutePath(),e);
            }
    	}
    }
    
    /**
     * Deletes a directory recursively. 
     *
     * @param directory  directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        if (!isSymlink(directory)) {
            cleanDirectory(directory);
        }

        if (!directory.delete()) {
            String message =
                "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (File file : files) {
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }
    
    //-----------------------------------------------------------------------
    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     *      (java.io.File methods returns a boolean)</li>
     * </ul>
     *
     * @param file  file or directory to delete, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws FileNotFoundException if the file was not found
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent){
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                String message =
                    "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }
    
    /**
     * Determines whether the specified file is a Symbolic Link rather than an actual file.
     * <p>
     * Will not return true if there is a Symbolic Link anywhere in the path,
     * only if the specific file is.
     *
     * @param file the file to check
     * @return true if the file is a Symbolic Link
     * @throws IOException if an IO error occurs while checking the file
     * @since Commons IO 2.0
     */
    public static boolean isSymlink(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        File fileInCanonicalDir;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        return !fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }


    /**
     * @param  dir  the directory to examine.
     *
     * @return the child files of the given directory, sorted by name.
     *         An empty list is returned if the directory does not exist or is not a directory.
     */
    public static List<File> getOrderedFilesInDir(File dir) {

        List<File> orderedFiles = null;

        if ((dir != null) && dir.isDirectory()) {
            final File[] files = dir.listFiles();
            if (files != null) {
                orderedFiles = Arrays.asList(files);
            }
        }

        if (orderedFiles == null) {
            orderedFiles = new ArrayList<File>();
        } else {
            sortFilesByName(orderedFiles);
        }

        return orderedFiles;
    }
    
    /**
     * Sort the given list of files in place, by name.
     * @param files
     */
    public static void sortFilesByName(List<File> files) {
        Collections.sort(files, new Comparator<File>() {
        	@Override
        	public int compare(File file1, File file2) {
        		return file1.getName().compareTo(file2.getName());
        	}
		});
    }

    public static String getFilePrefix(String filepath) {
        File file = new File(filepath);
        String name = file.getName();
        int index = name.lastIndexOf('.');
        if (index<0) return name;
        return name.substring(0, index);
    }
}
