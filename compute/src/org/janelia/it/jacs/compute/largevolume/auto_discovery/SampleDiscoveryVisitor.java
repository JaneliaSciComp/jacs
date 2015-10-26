package org.janelia.it.jacs.compute.largevolume.auto_discovery;

import org.janelia.it.jacs.compute.largevolume.TileBaseReader;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

/**
 * A NIO file visitor to help discover likely directories.
 * Created by fosterl on 10/23/15.
 */
public class SampleDiscoveryVisitor extends SimpleFileVisitor<Path> {
    private Path basePath;

    private static final String TIF0 = "default.0.tif";

    private Set<File> validatedFolders = new HashSet<>();

    private boolean visitationComplete = false;

    public SampleDiscoveryVisitor(String basePathStr) {
        basePath = Paths.get(basePathStr);
    }

    /** Launches the visit process. */
    public void exec() throws IOException {
        Files.walkFileTree(basePath, this);
    }

    /** After completion, can get the full set of stuff that was found. */
    public Set<File> getValidatedFolders() {
        if (! visitationComplete) {
            throw new IllegalStateException("Process incomplete: please await termination of visitation.");
        }
        return validatedFolders;
    }

    /** Avoid diving into things that cannot possibly be samples.  Samples are not recursive. */
    @Override
    public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attributes) {
        FileVisitResult result = FileVisitResult.CONTINUE;
        if (validatedFolders.contains(file.getParent().toString())) {
            result = FileVisitResult.SKIP_SUBTREE;
        }

        return result;
    }

    /** Descend the base path, looking for the stuff of interest. */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
        FileVisitResult result = FileVisitResult.CONTINUE;
        // Do not look directly at files, at this level.
        if (attributes.isDirectory()) {
            // Need the directory listing.
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(file)) {
                boolean hasTileBaseYml = false;
                boolean hasTif0 = false;
                boolean hasTransformTxt = false;
                int digitSubDirCount = 0;
                for (Path subPath: directoryStream) {
                    String subPathFileName = subPath.getFileName().toString();
                    if (subPathFileName.equals(TileBaseReader.STD_TILE_BASE_FILE_NAME)) {
                        hasTileBaseYml = true;
                    }
                    if (subPathFileName.equals(TIF0)) {
                        hasTif0 = true;
                    }
                    if (subPathFileName.equals(CoordinateToRawTransform.TRANSFORM_FILE)) {
                        hasTransformTxt = true;
                    }
                    else if (Files.isDirectory(subPath)  &&
                             subPathFileName.length() == 1  &&
                             Character.isDigit(subPathFileName.getBytes()[0])) {
                        digitSubDirCount ++;
                    }
                }

                if ( hasTileBaseYml  &&  hasTransformTxt  &&  hasTif0  &&  digitSubDirCount > 0 ) {
                    // Candidate directory has passed.
                    validatedFolders.add(file.toFile());
                    result = FileVisitResult.CONTINUE;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                result = FileVisitResult.TERMINATE;
            }
        }
        return result;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException {
        if (dir.equals(basePath)) {
            visitationComplete = true;
        }
        return FileVisitResult.CONTINUE;
    }
}
