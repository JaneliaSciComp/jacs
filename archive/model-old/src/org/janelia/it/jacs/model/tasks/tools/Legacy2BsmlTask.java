
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 11, 2010
 * Time: 12:16:04 PM
 */
public class Legacy2BsmlTask extends Task {

    /*
    Relevant sections from the Legacy2Bsml man page http://www.cbs.dtu.dk/cgi-bin/nph-runsafe?man=legacy2bsml

	 NAME

     legacy2bsml.pl - Migrates Nt/Prok/Euk legacy databases to BSML documents

     SYNOPSIS

     USAGE:  legacy2bsml.pl [-B] -D database [-F fastadir] -P password -U username [-M mode] -a asmbl_id [--alt_database] [-d debug_level] [--rdbms] [--host] [--exclude_genefinders] [-h] [--input_id_mapping_directories]  [--input_id_mapping_file] [--id_repository] [--idgen_identifier_version] [--include_genefinders] [-l log4perl] [-m] [--model_list_file] [--no_die_null_sequences] [--no_id_generator] [--no-misc-features] [--no-repeat-features] [--no-transposon-features] [-o outdir]  [--output_id_mapping_file] [-q sequence_type] --schema_type [--sourcename] [--tu_list_file] [--alt_species] [--repeat-mapping-file]

     OPTIONS

     <--backup,-B> If specified, will backup existing output .bsml and .fsa files

     <--username,-U> Database username

     <--password,-P> Database password

     <--database,-D> Source legacy organism database name

     <--mode,-M>
     1=Write Gene Model to BSML document  (Default)
     2=Write Gene Model and Computational Evidence to BSML document
     3=Write Computational Evidence to BSML document

     <--asmbl_id,-a> User must specify the appropriate assembly.asmbl_id value

     <--fastadir,-F> Optional  - fasta directory for the organism

     <--rdbms> Optional  - Relational database management system currently supports Sybase for
                           euk, prok and nt_prok schemas Mysql for euk, prok
                           Default: Sybase (if nothing specified)

     <--host>  Optional  - Server housing the database
               Default   - SYBTIGR (if nothing specified)

    <--sequence_type,-q> Sequence type of main <Sequence> e.g. SO:contig

    <--schema_type> Valid alternatives: euk, prok, ntprok

    <--log4perl,-l> Optional - Log4perl log file.  Defaults are:
                    If asmbl_list is defined /tmp/legacy2bsml.pl.database_$database.asmbl_id_$asmbl_id.log
                    Else /tmp/legacy2bsml.pl.database_$database.pid_$$.log

    <--schema,-s> Optional - Performs XML schema validation

    <--dtd,-t> Optional - Performs XML DTD validation

    <--exclude-genefinders> Optional - User can specify which gene finder data types to 
                                       exclude from the migration.  Default is to migrate 
                                       all gene finder data.

    <--include-genefinders> Optional - User can specify which gene finder data types 
                                       to include in the migration.  Default is to migrate 
                                       all gene finder data.

    <--no-misc-features> Optional - User can specify that no miscellaneous feature types
                                    should be extracted from the legacy annotation database.
                                    Default is to migrate all miscellaneous feature types.

    <--no-repeat-features> Optional - User can specify that no repeat feature types should be
                                      extracted from the legacy annotation database.
                                      Default is to migrate all repeat feature types.

    <--no-transposon-features> Optional - User can specify that no transposon feature types
                                          should be extracted from the legacy annotation database.
                                          Default is to migrate all transposon feature types.

    <--alt_database> Optional - User can specify a database prefix which will override the
                                default legacy annotation database name

    <--alt_species> Optional - User can specify an override value for species

    <--no_id_generator> Optional - Do not call IdGenerator services

    <--id_repository> Optional - IdGenerator compliant directory (must contain valid_id_repository
                                 file).  Default is current working directory

    <--idgen_identifier_version> Optional - The user can override the default version value
                                            appended to the feature and sequence identifiers (default is 0)

    <--no_die_null_sequences> Optional - If specified, will force legacy2bsml.pl to
                                         continue execution even if sequences are null for certain feat_types.

    <--sourcename> Optional - User can specify the value to store in the Analysis Attributes for tag name.
                              Default value is the current working directory.
     */

    transient public static final String PARAM_backup = "backup";
    transient public static final String PARAM_db_username = "db_username";
    transient public static final String PARAM_password = "password";
    transient public static final String PARAM_mode = "mode";
    transient public static final String PARAM_fastadir = "fastadir";
    transient public static final String PARAM_rdbms = "rdbms";
    transient public static final String PARAM_host = "host";
    transient public static final String PARAM_schema = "schema";
    transient public static final String PARAM_no_misc_features = "no misc features";
    transient public static final String PARAM_no_repeat_features = "no repeat feature";
    transient public static final String PARAM_no_transposon_features = "no transposon feature";
    transient public static final String PARAM_no_id_generator = "no id generator";
    transient public static final String PARAM_input_id_mapping_files = "input id mapping files";
    transient public static final String PARAM_input_id_mapping_directories = "input id mapping directories";
    transient public static final String PARAM_idgen_identifier_version = "idgen identifier version";
    transient public static final String PARAM_no_die_null_sequences = "no die null sequences";
    transient public static final String PARAM_sourcename = "sourcename";
    transient public static final String PARAM_control_file = "control file";
    transient public static final String PARAM_root_project = "root project";

    public Legacy2BsmlTask() {
        super();
        setTaskName("Legacy2BsmlTask");
    }

    public String getDisplayName() {
        return "Legacy2BsmlTask";
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

    public String generateCommandOptions() throws ParameterException {
        StringBuffer sb = new StringBuffer();

        addCommandParameter(sb, "-B", PARAM_backup);
        addRequiredCommandParameter(sb, "-U", PARAM_db_username);
        addRequiredCommandParameter(sb, "-P", PARAM_password);
        addCommandParameter(sb, "-M", PARAM_mode);
        addCommandParameter(sb, "-f", PARAM_fastadir);
        addCommandParameter(sb, "--rdbms", PARAM_rdbms);
        addCommandParameter(sb, "--host", PARAM_host);
        addCommandParameter(sb, "-s", PARAM_schema);
        addCommandParameter(sb, "--no-misc-features", PARAM_no_misc_features);
        addCommandParameter(sb, "--no-repeat-features", PARAM_no_repeat_features);
        addCommandParameter(sb, "--no-transposon-features", PARAM_no_transposon_features);
        addCommandParameter(sb, "--no_id_generator", PARAM_no_id_generator);
        addCommandParameter(sb, "--input_id_mapping_files", PARAM_input_id_mapping_files);
        addCommandParameter(sb, "--input_id_mapping_directories", PARAM_input_id_mapping_directories);
        addCommandParameter(sb, "--idgen_identifier_version", PARAM_idgen_identifier_version);
        addCommandParameter(sb, "--no_die_null_sequences", PARAM_no_die_null_sequences);
        addCommandParameter(sb, "--sourcename", PARAM_sourcename);
        return sb.toString();
    }

}