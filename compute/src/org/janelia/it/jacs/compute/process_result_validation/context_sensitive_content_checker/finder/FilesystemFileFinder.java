package org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.finder;

import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.FinderException;
import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.validatable.FileFinder;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by fosterl on 6/19/14.
 */
public class FilesystemFileFinder implements FileFinder {
    @Override
    public File[] getFiles(File parentPath, String filenameKey) throws FinderException {
        try {
            return parentPath.listFiles( new ExtensionFilter(filenameKey) );
        } catch ( Exception ex ) {
            throw new FinderException( "Failed to find filename under " + parentPath + " for " + filenameKey, ex );
        }
    }

    private static class ExtensionFilter implements FileFilter {
        private String extension;
        public ExtensionFilter( String extension ) {
            this.extension = extension;
        }

        @Override
        public boolean accept(File pathname) {
            return ( pathname.getName().endsWith( extension ) );
        }
    }

}
