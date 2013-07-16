
package org.janelia.it.jacs.shared.blast;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2006
 * Time: 1:43:11 PM
 *
 * @version $Id: FormatDBTool.java 1 2011-02-16 21:07:19Z tprindle $
 */
public class FormatDBTool {
    public static final String FORMATDB_PATH_PROP = "FormatDBTool.Path";
    public static final String DEFAULT_FORMATDB_PATH = "formatdb";

    protected String formatDBPath = null;
    protected Properties properties = null;
    protected String _partitionPrefix;
    private Logger logger;

    public static void main(String[] args) {
        Properties prop = new Properties();
        prop.setProperty(FormatDBTool.FORMATDB_PATH_PROP,
                SystemConfigurationProperties.getString("Executables.ModuleBase")+
                    SystemConfigurationProperties.getString(FormatDBTool.FORMATDB_PATH_PROP));
        prop.setProperty(SystemCall.SCRATCH_DIR_PROP,
                SystemConfigurationProperties.getString(SystemCall.SCRATCH_DIR_PROP));
        prop.setProperty(SystemCall.SHELL_PATH_PROP,
                SystemConfigurationProperties.getString(SystemCall.SHELL_PATH_PROP));
        prop.setProperty(SystemCall.STREAM_DIRECTOR_PROP,
                SystemConfigurationProperties.getString(SystemCall.STREAM_DIRECTOR_PROP));
        // NOTE: Might need to make the partition prefix a formal switch
        // I'm not aware of anyone who runs this from main.
        FormatDBTool fdb = new FormatDBTool(prop, Logger.getLogger(FormatDBTool.class), "p_");
        boolean formatProteins = false;
        String input = null;
        for (int i = 0; i < args.length;) {
            if (args[i].equals("-p")) {
                formatProteins = true;
                i++;
            }
            else if (args[i].equals("-i")) {
                input = args[i + 1];
                i += 2;
            }
            else {
                i++;
            }
        }
        if (input == null) {
            usage();
        }
        try {
            File inputFile = new File(input);
            if (inputFile.isDirectory()) {
                if (formatProteins) {
                    fdb.formatProteinDir(inputFile);
                }
                else {
                    fdb.formatNucleotideDir(inputFile);
                }
            }
            else if (inputFile.isFile()) {
                if (formatProteins) {
                    fdb.formatProteinFile(inputFile);
                }
                else {
                    fdb.formatNucleotideFile(inputFile);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void usage() {
        System.err.println("usage: [-p] -i <source file/dir> ");
        System.exit(1);
    }

    public FormatDBTool(Properties props, Logger logger, String partitionPrefix) {
        if (props != null) {
            properties = props;
            String formatDBPathString = SystemConfigurationProperties.getString("Executables.ModuleBase")+props.getProperty(FORMATDB_PATH_PROP);
            if (formatDBPathString != null) formatDBPath = formatDBPathString;
        }
        this.logger = logger;
        this._partitionPrefix = partitionPrefix;
        if (formatDBPath == null) formatDBPath = DEFAULT_FORMATDB_PATH;
    }

    public void formatNucleotideFile(File nuclFile) {
        SystemCall call = new SystemCall(properties, null, logger);
        String command = "cd " + nuclFile.getParentFile().getAbsolutePath() +
                "; " + formatDBPath + " -p F -i " + nuclFile.getName();
        try {
            int exitValue = call.execute(command, true);
            if (exitValue != 0) {
                throw new RuntimeException("Error with system command: " + command);
            }
            else {
                call.cleanup();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void formatProteinFile(File proFile) {
        SystemCall call = new SystemCall(properties, null, logger);
        String command = "cd " + proFile.getParentFile().getAbsolutePath() +
                "; " + formatDBPath + " -p T -i " + proFile.getName();
        try {
            int exitValue = call.execute(command, true);
            if (exitValue != 0) {
                throw new RuntimeException("Error with system command: " + command);
            }
            else {
                call.cleanup();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void formatNucleotideDir(File nuclDir) {
        for (File f : nuclDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(_partitionPrefix);
            }
        })) {
            formatNucleotideFile(f);
        }
    }

    public void formatProteinDir(File proDir) {
        for (File f : proDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(_partitionPrefix);
            }
        })) {
            formatProteinFile(f);
        }
    }

}
