
package org.janelia.it.jacs.model.tasks.eukAnnotation;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jul 26, 2010
 * Time: 12:16:04 PM
 */
public class EAPTask extends Task {

    /*
        This service is really the running of several scripts.  Some abridged help follows:

        Create the sqlite db:
         % /export/svn/EAP/trunk/pipeline/createComputeDB.pl -h
        This script creates a sqlite database for use by compute.pl.
        -D <db file> path for sqlite db to be created

        Load the Peptide FASTA into the SQLite Database:
        % /export/svn/EAP/trunk/pipeline/addQueryFasta.pl -h
        This script adds a fasta file to a compute db for use as a QUERY dataset.
        usage: ./addQueryFasta.pl -D /usr/local/projects/jhoover/myResults.db -F /usr/local/projects/jhoover/mySeqs.fasta -N proteins -d "Dataset description." -a "READ_ID,ORF_ID,SAMPLE_NAME"
        -D <compute db file> path to compute db
        -F <fasta file> path to fasta file
        -N <dataset name> short, unique name for dataset
        -d <description> optional, description of dataset
        -a <attribute list> comma separated list of defline tags to be parsed as sequence attributes

        Launch the Computes:
        % /export/svn/EAP/trunk/pipeline/compute.pl -h
        This script checks the status of all required computes, submits grid jobs for new computes or
        to refresh computes based on obsolete data sources or programs, and loads the results into a
        sqlite database.
        usage: ./compute.pl -D ntsm07.db -C eapPrecomputesCfg.db -P 0999
        -D <annotation db file> path to sqlite db contains query sets and compute results
        -C <configuration db file> path to sqlite db defining computes
        -P <project code> project code for grid accounting
        -a <query aliases> aliases from query dataset names that do not match configuration, e.g -a "metagene_mapped_proteins as proteins, reprocessed_reads as reads"
        -r <max retries> optional, maximum number of times to resubmit a failed job (default is 0)
        -c <compute list> optional, run only the computes named in comma separated list
        -v <jacs wsdl url> optional, use an alternate jacs wsdl

        [Create the directory using mkdir, then:]

        Pull out annotations:
        % /export/svn/EAP/trunk/pipeline/get_annotation.pl -h
        NAME
        get_vmap_annotation.pl - Reads evidence from sqlite3 database and assigns annotation
        SYNOPSIS
            USAGE: get_vmap_annotation.pl --database=/database/location/sqlite.db
            --output=/output/directory/ [ --gzip ]
        OPTIONS
            --database,-d REQUIRED. Full location to sqlite3 database

            --output,-o REQUIRED. The directory you would like your output files
            written to.

            --gzip,-g OPTIONAL. Setting gzip flag will write to compressed files. By
            default this is set to 0.

            --help,-h Print this message
        OUTPUT
                prefix.annotation[.gz] - File contains assigned annotation for the ORFs found in the sqlite3 db.
                prefix.evidence[.gz] - File contains all the evidence for each ORF as queried out from the sqlite3 db.
                prefex.annotation_log - Shows progress of what ORF is being annotated as the script is run.

        Summarize results:
         % /export/svn/EAP/trunk/pipeline/summarize_evidence.pl <fasta_file> <$ev_file>


     */

    
    transient public static final String PARAM_input_fasta = "input fasta";
    transient public static final String PARAM_dataset_name = "dataset name";
    transient public static final String PARAM_dataset_description= "dataset description";
    transient public static final String PARAM_attribute_list = "attribute list";
    transient public static final String PARAM_configuration_db = "configuration db";
    transient public static final String PARAM_query_aliases= "query aliases";
    transient public static final String PARAM_max_retries = "max retries";
    transient public static final String PARAM_compute_list= "compute list";
    transient public static final String PARAM_jacs_wsdl_url = "jacs wsdl url";
    transient public static final String PARAM_gzip = "gzip";

    transient public static final String dataset_name_DEFAULT = "proteins";
    transient public static final String configuration_db_DEFAULT = "/usr/local/devel/ANNOTATION/EAP/pipeline/configs/eapPrecomputesCfg.db";
 

    public EAPTask() {
        super();
        setTaskName("EAPTask");
        setParameter(PARAM_dataset_name, dataset_name_DEFAULT);
        setParameter(PARAM_configuration_db, configuration_db_DEFAULT);
    }

    public String getDisplayName() {
        return "EAPTask";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        // no match
        return null;
    }

    public String generateAddQueryFastaOptions() throws ParameterException {
        StringBuffer sb=new StringBuffer();
        addRequiredCommandParameter(sb, "-N", PARAM_dataset_name);
        addCommandParameter(sb, "-d", PARAM_dataset_name);
        addCommandParameter(sb, "-a", PARAM_attribute_list);
        return sb.toString();
    }

    public String generateComputeOptions() throws ParameterException {
        StringBuffer sb=new StringBuffer();
        addRequiredCommandParameter(sb, "-C", PARAM_configuration_db);
        addRequiredCommandParameter(sb, "-P", PARAM_project);
        addCommandParameter(sb, "-a", PARAM_query_aliases);
        addCommandParameter(sb, "-r", PARAM_max_retries);
        addCommandParameter(sb, "-c", PARAM_compute_list);
        addCommandParameter(sb, "-v", PARAM_jacs_wsdl_url);
        return sb.toString();
    }

    public String generateGetAnnotOptions() throws ParameterException {
        StringBuffer sb=new StringBuffer();
        addCommandParameterFlag(sb, "-g", PARAM_gzip);
        return sb.toString();
    }
}