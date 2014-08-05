package org.janelia.it.jacs.compute.process_result_validation;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by fosterl on 8/5/14.
 */
public class ValidationLogScanner {

    public static final String FAILURE_REPORT_EXT = ".Failure.report.tsv";
    private File startingPoint = null;
    private Map<String,Long> statsMap;
    private FileFilter fileAcceptor;

    /** The incoming file may be a directory containing logs, or just a log. */
    public ValidationLogScanner( File startingPoint ) {
        this.startingPoint = startingPoint;
    }

    public void writeStatisticSummary( PrintWriter writer ) throws IOException {
        statsMap = new HashMap<>();
        fileAcceptor = new ErrorReportAcceptor();
        collectStatistics(startingPoint);

        // Note: caller creates the writer, and is expected to close it.
        writer.println("Total error counts for all categories follow.");
        for ( String category: statsMap.keySet() ) {
            writer.println(String.format("Category '%s' has %d occurrences.", category, statsMap.get( category )));
        }
    }

    private void collectStatistics(File currentFile) throws IOException {
        if ( currentFile.isDirectory() ) {
            for ( File subFile: currentFile.listFiles( fileAcceptor ) ) {
                collectStatistics(subFile);
            }
        }
        else if ( fileAcceptor.accept( currentFile ) ) {
            getStatsFrom(currentFile);
        }
    }

    /** Read the file, look for its statistics, and write them as needed. */
    private void getStatsFrom(File file) throws IOException {
        try (
            BufferedReader bufferedReader = new BufferedReader( new FileReader( file ) )
        ) {
            String inline = null;
            boolean endOfFile = false;
            while ( (!endOfFile)  &&  null != ( inline = bufferedReader.readLine() )) {
                inline = inline.trim();
                if ( inline.length() == 0 )
                    continue;

                if ( inline.equals(ValidationLogger.COUNT_BY_CATEGORY_HEADER) ) {
                    endOfFile = accumulateStats( bufferedReader );
                }
            }
        }
    }

    /** @return T=end-of-file encountered. */
    private boolean accumulateStats( BufferedReader br ) throws IOException {
        String inline = null;
        boolean inCounts = true;
        while ( inCounts  &&  null != ( inline = br.readLine() ) ) {
            inline = inline.trim();
            if ( inline.length() == 0 )
                continue;

            if ( inline.startsWith( ValidationLogger.SECTION_BREAK_DELIM )) {
                inCounts = false;
            }
            else {
                String[] countVCat = inline.split("\t");
                Long count = Long.parseLong( countVCat[ 0 ].trim() );
                String category = countVCat[ 1 ].trim();
                Long oldStat = statsMap.get( category );
                if ( oldStat == null ) {
                    oldStat = 0L;
                }
                count += oldStat;
                statsMap.put( category, count );
            }

        }
        // If still in the counts, file ended before any other section found.
        return ! inCounts;
    }

    /** This filter only takes things relevant to our file scan. */
    private static class ErrorReportAcceptor implements FileFilter {

        @Override
        public boolean accept(File file) {
            return file.getName().endsWith( FAILURE_REPORT_EXT );
        }
    }

}
