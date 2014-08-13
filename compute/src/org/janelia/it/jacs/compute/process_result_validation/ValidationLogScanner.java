package org.janelia.it.jacs.compute.process_result_validation;

import org.janelia.it.jacs.compute.process_result_validation.content_checker.engine.ValidationEngine;

import java.io.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Given a directory hierarchy containing zero or more failure logs, this scanner will find and enumerate all
 * the different types of errors contained with all of them combined.
 *
 * Created by fosterl on 8/5/14.
 */
public class ValidationLogScanner {

    public static final String FAILURE_REPORT_EXT = ".Failure.report.tsv";
    public static final String STD_DATE_FORMAT = "MMMM dd, yyyy";
    private File startingPoint = null;
    private Map<String,StatInfo> statsMap;
    private FileFilter fileAcceptor;
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(STD_DATE_FORMAT, Locale.US );

    enum SectionReturnVal { EOF, RevertedLine, NO_SECTION;
        private String revertedLine;
        public String getRevertedLine() { return revertedLine; }
        public void setRevertedLine( String revertedLine ) { this.revertedLine = revertedLine; }
    }

    /** The incoming file may be a directory containing logs, or just a log. */
    public ValidationLogScanner( File startingPoint ) {
        this.startingPoint = startingPoint;
    }

    public void writeStatisticSummary( PrintWriter writer ) throws IOException {
        statsMap = new TreeMap<>();
        fileAcceptor = new ErrorReportAcceptor();
        collectStatistics(startingPoint);

        // Note: caller creates the writer, and is expected to close it.
        writer.println("Total error counts for all categories follow.");
        for ( String category: statsMap.keySet() ) {
            StatInfo info = statsMap.get(category);
            if ( info.getCount() > 0 ) {
                writer.println(
                        String.format(
                                "Category '%s' has %d occurrences.  Latest occurrence was %s.  Earliest occurence was %s.",
                                category,
                                info.getCount(),
                                DATE_FORMATTER.format( info.getLatestDate() ),
                                DATE_FORMATTER.format( info.getEarliestDate() )
                        )
                );
            }
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

    /** Read the file, break up into sample-specific readers, handoff to stats collector. */
    private void getStatsFrom(File file) throws IOException {
        try (
            BufferedReader bufferedReader = new BufferedReader( new FileReader( file ) )
        ) {
            String inline;
            StringBuilder sampleBuilder = new StringBuilder( ValidationEngine.PUTATIVE_MAX_LOG_SIZE );
            while ( null != ( inline = bufferedReader.readLine() ) ) {
                if ( inline.contains( ValidationLogger.SAMPLE_BREAK_DELIM ) ) {
                    getStatsFrom(sampleBuilder);
                }
                else {
                    sampleBuilder.append( inline ).append( "\n" );
                }
            }

            getStatsFrom(sampleBuilder);
        }
    }

    /** process statistics from a buffer of log information. */
    private void getStatsFrom(StringBuilder sampleBuilder) throws IOException {
        if ( sampleBuilder.length() > 0 ) {
            try ( BufferedReader sampleReader = packageBuilderIntoReader( sampleBuilder ) ) {
                getStatsFrom( sampleReader );
            }
        }
        sampleBuilder.setLength( 0 );
    }

    /** Look for its statistics, and write them as needed. */
    private void getStatsFrom(BufferedReader bufferedReader) throws IOException {
        String inline = null;
        inline = bufferedReader.readLine();
        while ( null != inline ) {
            inline = inline.trim();
            if ( inline.length() == 0 ) {
                inline = bufferedReader.readLine();
                continue;
            }

            SectionReturnVal returnVal = SectionReturnVal.NO_SECTION;
            if ( inline.equals(ValidationLogger.COUNT_BY_CATEGORY_HEADER) ) {
                returnVal = accumulateCountStats(bufferedReader);
            }

            if ( inline.startsWith(ValidationLogger.ERROR_ENUM_DELIM) ) {
                returnVal = accumulateDateStats(bufferedReader);
            }

            // May need to back up by one line and use a stashed value, to avoid overlooking sections.
            if ( returnVal == SectionReturnVal.RevertedLine ) {
                inline = returnVal.getRevertedLine();
            }
            else if ( returnVal != SectionReturnVal.EOF ) {
                inline = bufferedReader.readLine();
            }
            else {
                inline = null;
            }
        }
    }

    /** @return T=end-of-file encountered. */
    private SectionReturnVal accumulateCountStats(BufferedReader br) throws IOException {
        String inline = null;
        SectionReturnVal rtnVal = SectionReturnVal.EOF;
        boolean inSection = true;
        while ( inSection  &&  null != ( inline = br.readLine() ) ) {
            inline = inline.trim();
            if ( inline.length() == 0 )
                continue;

            if ( inline.startsWith( ValidationLogger.SECTION_BREAK_DELIM )) {
                //  Return Control.
                inSection = false;
                rtnVal = SectionReturnVal.RevertedLine;
                rtnVal.setRevertedLine( inline );
            }
            else {
                String[] fields = inline.split("\t");
                String category = fields[ fields.length - 1 ];
                Long count = Long.parseLong(fields[0].trim());
                StatInfo oldStat = getStatInfo(category);
                oldStat.setCount( oldStat.getCount() + count );
                statsMap.put( category, oldStat );
            }

        }

        return rtnVal;
    }

    private BufferedReader packageBuilderIntoReader( StringBuilder sampleBuilder ) throws IOException {
        BufferedReader br = new BufferedReader(
                new CharArrayReader(
                        sampleBuilder.toString().toCharArray()
                )
        );
        return br;
    }

    private SectionReturnVal accumulateDateStats(BufferedReader br) throws IOException {
        String inline = null;
        SectionReturnVal rtnVal = SectionReturnVal.EOF;
        boolean inSection = true;
        while ( inSection  &&  null != ( inline = br.readLine() ) ) {
            inline = inline.trim();
            if ( inline.length() == 0 )
                continue;

            if ( inline.startsWith( ValidationLogger.SECTION_BREAK_DELIM )) {
                //  Return Control.
                inSection = false;
                rtnVal = SectionReturnVal.RevertedLine;
                rtnVal.setRevertedLine( inline );
            }
            else {
                String[] fields = inline.split("\t");
                String category = fields[ fields.length - 1 ];
                String dateStr = fields[ 3 ].trim();
                StatInfo oldStat = getStatInfo(category);
                Date date = DATE_FORMATTER.parse( dateStr, new ParsePosition(0) );
                if ( date == null ) {
                    System.err.println("Error: date " + dateStr + " not parsed.");
                }
                if ( date.after(oldStat.getLatestDate()) ) {
                    oldStat.setLatestDate( date );
                }
                if ( date.before( oldStat.getEarliestDate() )) {
                    oldStat.setEarliestDate( date );
                }
                statsMap.put( category, oldStat );
            }

        }

        return rtnVal;
    }

    private StatInfo getStatInfo(String category) {
        StatInfo oldStat = statsMap.get(category);
        if ( oldStat == null ) {
            oldStat = new StatInfo();
            oldStat.setCount( 0L );
            Date newDate = new Date();
            newDate.setTime(0L);   // The default is the oldest possible date stamp.  Bound to be superceded.
            oldStat.setLatestDate(newDate);

            newDate = new Date();
            oldStat.setEarliestDate(newDate);
        }
        return oldStat;
    }

    /** This filter only takes things relevant to our file scan. */
    private static class ErrorReportAcceptor implements FileFilter {

        @Override
        public boolean accept(File file) {
            return file.getName().endsWith( FAILURE_REPORT_EXT ) || file.isDirectory();
        }
    }

    private static class StatInfo {
        private Long count;
        private Date latestDate;
        private Date earliestDate;

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public Date getLatestDate() {
            return latestDate;
        }

        public void setLatestDate(Date latestDate) {
            this.latestDate = latestDate;
        }

        public Date getEarliestDate() {
            return earliestDate;
        }

        public void setEarliestDate(Date earliestDate) {
            this.earliestDate = earliestDate;
        }
    }

}
