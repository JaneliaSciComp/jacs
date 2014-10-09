package org.janelia.it.jacs.model.lsm;

import java.io.File;
import java.io.IOException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A pattern for identifying the base and relative path portions of a full file path.
 *
 * For example, the pattern:
 *   '/fs/lab/confocalStacks/{yyyy}/{MM}/{dd}'
 *
 * identifies a base path of:
 *   '/fs/lab/confocalStacks'
 *
 * and a relative path of:
 *   '{yyyy}/{MM}/{dd}'.
 *
 * @author Eric Trautman
 */
public class FileStoragePathPattern {

    private String patternString;
    private String basePath;
    private int numberOfParentDirectoriesToIncludeInRelativePath;

    public FileStoragePathPattern(String patternString) throws IllegalArgumentException {

        if (patternString == null) {
            throw new IllegalArgumentException("No storage pattern was specified." + PATTERN_EXAMPLE);
        }

        if (patternString.contains("..")) {
            throw new IllegalArgumentException("Storage pattern '" + patternString +
                                               "' may not contain '..'."  + PATTERN_EXAMPLE);
        }

        final Matcher m = VALIDATION_PATTERN.matcher(patternString);
        if (! m.matches()) {
            throw new IllegalArgumentException("An invalid storage pattern '" + patternString +
                                               "' was specified."  + PATTERN_EXAMPLE);
        }

        this.patternString = patternString;
        this.basePath = m.group(1);
        this.numberOfParentDirectoriesToIncludeInRelativePath = 0;

        for (int i = patternString.indexOf('{'); i > -1; i = patternString.indexOf('{', (i+1))) {
            this.numberOfParentDirectoriesToIncludeInRelativePath++;
        }
    }

    public String getBasePath() {
        return basePath;
    }

    public int getNumberOfParentDirectoriesToIncludeInRelativePath() {
        return numberOfParentDirectoriesToIncludeInRelativePath;
    }

    @Override
    public String toString() {
        return patternString;
    }

    /**
     * @param  sourceFile  file whose path is to be parsed.
     *
     * @return the relative path portion (including the file name) of the specified file
     *         based upon this pattern.
     *
     * @throws IllegalArgumentException
     *   if a canonical path cannot be derived for the specified file.
     */
    public String getRelativePath(File sourceFile) throws IllegalArgumentException {
        String relativePath = null;

        if (sourceFile != null) {

            final File canonicalLsmFile;
            try {
                canonicalLsmFile = sourceFile.getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException("cannot derive canonical path for " + sourceFile.getAbsolutePath(),
                                                   e);
            }

            Stack<String> nameStack = new Stack<>();
            nameStack.push(sourceFile.getName());

            File parent = canonicalLsmFile.getParentFile();
            String name;
            while ((parent != null) && (nameStack.size() <= numberOfParentDirectoriesToIncludeInRelativePath)) {
                name = parent.getName();
                if (name.length() > 0) {
                    nameStack.push(name);
                    parent = parent.getParentFile();
                } else {
                    break;
                }
            }

            StringBuilder sb = new StringBuilder(128);
            sb.append(nameStack.pop());
            for (int i = nameStack.size(); i > 0; i--) {
                sb.append('/');
                sb.append(nameStack.pop());
            }

            relativePath = sb.toString();
        }

        return relativePath;
    }

    /**
     * @param  sourceFile  source file for storage file derivation.
     *
     * @return a file whose path is composed of this pattern's base path
     *         followed by the specified source file's relative path.
     *
     * @throws IllegalArgumentException
     *   if a canonical path cannot be derived for the specified file.
     */
    public File getStorageFile(File sourceFile) throws IllegalArgumentException {

        File storageFile = null;

        final String relativePath = getRelativePath(sourceFile);
        if (relativePath != null) {
            final File rawStorageFile = new File(basePath + '/' + relativePath);
            try {
                storageFile = rawStorageFile.getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "cannot derive canonical path for " + rawStorageFile.getAbsolutePath(), e);
            }
        }

        return storageFile;
    }

    /**
     * @param  sourceFile  source file to check.
     *
     * @return true if the specified source file is in a different location than its derived storage path;
     *         otherwise false.
     *
     * @throws IllegalArgumentException
     *   if a canonical path cannot be derived for the specified file.
     */
    public boolean isLocationDifferent(File sourceFile) throws IllegalArgumentException {
        final String canonicalSourceFilePath;
        try {
            canonicalSourceFilePath = sourceFile.getCanonicalPath();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "cannot derive canonical path for " + sourceFile.getAbsolutePath(), e);
        }

        final File storageFile = getStorageFile(sourceFile);
        final String storageFilePath = storageFile.getPath();

        return (! canonicalSourceFilePath.equals(storageFilePath));
    }

    private static final Pattern VALIDATION_PATTERN = Pattern.compile("((/[^\\{}/]+)+)(/\\{[\\w]*})*");

    private static final String PATTERN_EXAMPLE =
            "  The pattern should look something like '/fs/lab/confocalStacks/{yyyy}/{MM}/{dd}'.";
}
