package org.janelia.it.jacs.compute.process_result_validation.octree;

import java.io.BufferedReader;
import java.io.FileFilter;
import java.io.File;
import java.io.FileReader;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Given log output from a renderer run, this class will scan, trying to find relevant ERROR, seg-fault and
 * other data.
 *
 * Created by fosterl on 4/2/15.
 */
public class RenderLogScanner {
    private File baseDirectory;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("USAGE: java " + RenderLogScanner.class + " <directory>");
        }
        RenderLogScanner scanner = new RenderLogScanner(new File(args[0]));
        scanner.scan();
    }

    public RenderLogScanner(File baseDirectory) throws Exception {
        this.baseDirectory = baseDirectory;
    }

    public void scan() {
        File[] logs = baseDirectory.listFiles( new LogFileFilter() );
        Map<File,StringBuilder> logToErrors = new HashMap<>();
        Map<Long,File> modDateToFile = new TreeMap<>(new InverseLongComparator());
        for (File log: logs) {
            try (BufferedReader br = new BufferedReader( new FileReader( log ) )) {
                boolean inErrorBlock = false;

                StringBuilder errorBuilder = null;
                StringBuilder wholeLogBuilder = new StringBuilder();

                logToErrors.put(log, wholeLogBuilder);
                if (log == null) {
                    throw new IllegalStateException();
                }
                modDateToFile.put(new Long(log.lastModified()), log);

                String latestInfo = null;
                String inline = null;
                while (null != (inline = br.readLine())) {
                    if (inline.startsWith("ERROR: ")) {
                        inErrorBlock = true;
                        errorBuilder = new StringBuilder(inline);
                    }
                    else if (inline.startsWith("INFO:")) {
                        if (inErrorBlock) {
                            wholeLogBuilder
                                    .append(latestInfo)
                                    .append("\n")
                                    .append(errorBuilder);
                            inErrorBlock = false;
                            errorBuilder = null;
                        }
                        latestInfo = inline;
                    }
                    else if (inErrorBlock) {
                        errorBuilder.append("\n")
                                    .append(inline);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        System.out.println("Reporting on " + baseDirectory + " Errors Shown in Most-Recent-Mod-First Order");
        for (Long modDate: modDateToFile.keySet()) {
            File log = modDateToFile.get( modDate );
            if (log == null) {
                System.out.println(modDate + "/" + log);
                continue;
            }

            StringBuilder errors = logToErrors.get( log );

            System.out.println(log.getName() + "========================");
            System.out.println(errors);
        }
    }

    private static class LogFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return (pathname.isFile()  &&  pathname.getName().endsWith(".log"));
        }
    }

    private static class InverseLongComparator implements Comparator<Long> {

        @Override
        public int compare(Long o1, Long o2) {
            return -o1.compareTo(o2);
        }
    }
}
