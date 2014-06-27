package org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.validatable;

import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.FinderException;

import java.io.File;

/**
 * Implement this in whatever way is most efficient, to find files that are below the parent and have right
 * kind of filename.
 * Created by fosterl on 6/18/14.
 */
public interface FileFinder {
    File[] getFiles(File parentPath, String filenameKey) throws FinderException;
}
