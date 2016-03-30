package org.janelia.it.jacs.compute.largevolume.auto_discovery;

import org.janelia.it.jacs.compute.largevolume.TileBaseReader;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.EnumSet;
import org.apache.log4j.Logger;

/**
 * A NIO file visitor to help discover likely directories.
 * Created by fosterl on 10/23/15.
 */
public class SampleDiscoveryVisitor extends SimpleFileVisitor<Path> {
    private Path basePath;
    private Logger logger;
    
    private Set<Path> tabooList;

    private static final String TIF0 = "default.0.tif";
    private static final int MAX_DESCENT_DEPTH = 4;

    private Set<File> validatedFolders = new HashSet<>();

    private boolean visitationComplete = false;

    public SampleDiscoveryVisitor(String basePathStr) {
        logger = Logger.getLogger(SampleDiscoveryVisitor.class);
        basePath = Paths.get(basePathStr);
        logger.info("Examining " + basePath.toString());
        tabooList = new HashSet<Path>();
        tabooList.add(FileSystems.getDefault().getPath("/nobackup2/mouselight/cluster"));
    }

    /** Launches the visit process. */
    public void exec() throws IOException {
        Files.walkFileTree(basePath, EnumSet.noneOf(FileVisitOption.class), MAX_DESCENT_DEPTH, this);
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
    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
        FileVisitResult result = FileVisitResult.CONTINUE;
        if (validatedFolders.contains(directory.getParent().toString())) {
            logger.debug("Skipping " + directory + " because parent is already marked.");
            result = FileVisitResult.SKIP_SUBTREE;
        }
        else if (tabooList.contains(directory)) {
            logger.debug("Skipping " + directory + " because it is on the taboo list.");
            result = FileVisitResult.SKIP_SUBTREE;
        }
        else if (isDigitDirectory(directory, directory.getFileName().toString())) {
            result = FileVisitResult.SKIP_SUBTREE;
        }
        else {
            result = checkDirectoryContents(directory, attributes);
        }
        return result;
    }

    /** Avoid file exceptions' killing the process. */
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException io) {
        logger.warn("Error in " + file + " due to " + io);
        return FileVisitResult.SKIP_SUBTREE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException {
        if (dir.equals(basePath)) {
            visitationComplete = true;
        }
        return FileVisitResult.CONTINUE;
    }

    private FileVisitResult checkDirectoryContents(Path file, BasicFileAttributes attributes) {
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
                    else if (isDigitDirectory(subPath, subPathFileName)) {
                        digitSubDirCount ++;
                    }
                }

                if ( hasTileBaseYml  &&  hasTransformTxt  &&  hasTif0  &&  digitSubDirCount > 0 ) {
                    // Candidate directory has passed.
                    validatedFolders.add(file.toFile());
                    logger.info("Adding folder " + file.toString());
                }
                else if (logger.isDebugEnabled()  &&  (hasTileBaseYml || hasTransformTxt || hasTif0)) {
                    logger.debug("Rejecting folder " + file.toString() + " " + hasTileBaseYml + " " + hasTransformTxt + " " + hasTif0 + " "  + digitSubDirCount);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                result = FileVisitResult.TERMINATE;
            }
        }
        return result;
    }

    private  boolean isDigitDirectory(Path subPath, String subPathFileName) {
        return Files.isDirectory(subPath)  &&
                subPathFileName.length() == 1  &&
                Character.isDigit(subPathFileName.getBytes()[0]);
    }
}
