#!/usr/local/perl

#
# Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
# 
# This file is part of JCVI VICS.
# 
# JCVI VICS is free software; you can redistribute it and/or modify it 
# under the terms and conditions of the Artistic License 2.0.  For 
# details, see the full text of the license in the file LICENSE.txt.  
# No other rights are granted.  Any and all third party software rights 
# to remain with the original developer.
# 
# JCVI VICS is distributed in the hope that it will be useful in 
# bioinformatics applications, but it is provided "AS IS" and WITHOUT 
# ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
# warranties of merchantability or fitness for any particular purpose.  
# For details, see the full text of the license in the file LICENSE.txt.
# 
# You should have received a copy of the Artistic License 2.0 along with 
# JCVI VICS.  If not, the license can be obtained from 
# "http://www.perlfoundation.org/artistic_license_2_0."
# 

use strict;
require "getopts.pl";
use Cwd 'realpath';
use File::Basename;
use HTTP::Status;
use HTTP::Response;
use LWP::UserAgent;
use URI::URL;
my $program = realpath($0);
my $myLib = dirname($program);
push @INC, $myLib;
require 'db.pm';
require 'dataset.pm';
our $errorMessage;

my $GUID_BASE_PATH="http:\/\/guid\.jcvi\.org\/guid\/GuidClientServer\?Request=GET\&";
my $max_guid=0;
my $current_guid=0;

my ( $workingDir ) = &initialize;

my $databaseName="sqlPersistAnno.db";
my $databaseFullpath="$workingDir\/$databaseName";

my $btabName="ncbi_blastp_btab.combined.out";
my $btabFullpath="$workingDir\/$btabName";

my $htabName="ldhmmpfam_full.htab.combined.out";
my $htabFullpath="$workingDir\/$htabName";

my $priamName="priam_ec.ectab.combined.out";
my $priamFullpath="$workingDir\/$priamName";

my $tmhmmName="tmhmm.raw.combined.out";
my $tmhmmFullpath="$workingDir\/$tmhmmName";

my $lipoproteinName="lipoprotein_bsml.parsed";
my $lipoproteinFullpath="$workingDir\/$lipoproteinName";

my $annotationName="annotation_rules.combined.out";
my $annotationFullpath="$workingDir\/$annotationName";

my %PROP_HASH;

my $QUERY_FASTA_FILE_NODE_ID_PROP = "QueryInputAnnotationFastaFileNodeId";
my $QUERY_FASTA_FILE_INPUT_PATH_PROP = "QueryInputAnnotationFastaFilePath";
my $QUERY_SEQUENCE_COUNT_PROP = "QuerySequenceCount";
my $QUERY_DATASET_NAME_PROP = "QueryDatasetName";
my $QUERY_DATASET_DESCRIPTION_PROP = "QueryDatasetDescription";

my $BLAST_PANDA_SUBJECT_DB_ID_PROP = "BlastPandaSubjectDatabaseId";
my $BLAST_PANDA_SUBJECT_DB_NAME_PROP = "BlastPandaSubjectDatabaseName";
my $BLAST_PANDA_SUBJECT_DB_DESCRIPTION_PROP = "BlastPandaSubjectDatabaseDescription";
my $BLAST_PANDA_SUBJECT_DB_SEQUENCE_COUNT_PROP = "BlastPandaSubjectDatabaseSequenceCount";

my $HMM_SUBJECT_DB_ID_PROP = "HmmSubjectDatabaseId";
my $HMM_SUBJECT_DB_NAME_PROP = "HmmSubjectDatabaseName";
my $HMM_SUBJECT_DB_DESCRIPTION_PROP = "HmmSubjectDatabaseDescription";
my $HMM_SUBJECT_DB_SEQUENCE_COUNT_PROP = "HmmSubjectDatabaseSequenceCount";

my $JOB_MG_ANNO_PARENT_TASK_ID_PROP = "MgAnnoParentTaskId";
my $JOB_MG_ANNO_PARENT_TASK_NAME_PROP = "MgAnnoParentTaskName";
my $JOB_MG_ANNO_PARENT_TASK_PROJECT_CODE_PROP = "MgAnnoParentTaskProjectCode";
my $JOB_MG_ANNO_PARENT_TASK_OWNER_PROP = "MgAnnoParentTaskOwner";
my $JOB_MG_ANNO_PARENT_TASK_DATE_SUBMITTED_PROP = "MgAnnoParentTaskStartTimeYYYY-MM-DDTHH:MM";
my $JOB_MG_ANNO_PARENT_TASK_DATE_COMPLETED_PROP = "MgAnnoParentTaskCompleteTimeYYYY-MM-DDTHH:MM";

my $JOB_MG_ANNO_BLAST_TASK_ID_PROP = "MgAnnoBlastTaskId";
my $JOB_MG_ANNO_BLAST_TASK_NAME_PROP = "MgAnnoBlastTaskName";
my $JOB_MG_ANNO_BLAST_TASK_PROJECT_CODE_PROP = "MgAnnoBlastTaskProjectCode";
my $JOB_MG_ANNO_BLAST_TASK_OWNER_PROP = "MgAnnoBlastTaskOwner";
my $JOB_MG_ANNO_BLAST_TASK_DATE_SUBMITTED_PROP = "MgAnnoBlastTaskStartTimeYYYY-MM-DDTHH:MM";
my $JOB_MG_ANNO_BLAST_TASK_DATE_COMPLETED_PROP = "MgAnnoBlastTaskCompleteTimeYYYY-MM-DDTHH:MM";

my $JOB_MG_ANNO_HMMPFAM_TASK_ID_PROP = "MgAnnoHmmpfamTaskId";
my $JOB_MG_ANNO_HMMPFAM_TASK_NAME_PROP = "MgAnnoHmmpfamTaskName";
my $JOB_MG_ANNO_HMMPFAM_TASK_PROJECT_CODE_PROP = "MgAnnoHmmpfamTaskProjectCode";
my $JOB_MG_ANNO_HMMPFAM_TASK_OWNER_PROP = "MgAnnoHmmpfamTaskOwner";
my $JOB_MG_ANNO_HMMPFAM_TASK_DATE_SUBMITTED_PROP = "MgAnnoHmmpfamTaskStartTimeYYYY-MM-DDTHH:MM";
my $JOB_MG_ANNO_HMMPFAM_TASK_DATE_COMPLETED_PROP = "MgAnnoHmmpfamTaskCompleteTimeYYYY-MM-DDTHH:MM";

my ($other_process_guid_start) = &getGuidBlock(3);
my $PRIAM_JOB_ID = $other_process_guid_start;
my $LIPOPROTEIN_JOB_ID = $other_process_guid_start + 1;
my $TMHMM_JOB_ID = $other_process_guid_start + 2;
my $PROTEIN_FUNCTION_JOB_ID = $other_process_guid_start + 3;

my %query_acc_to_id_hash;

print `date`;

&create_database;

&load_prop_hash;

&populate_DATASET;

&populate_DATASET_VERSION;

&populate_JOB;

&populate_DATASET_SEQ;

&populate_BTAB;

&populate_HTAB;

&populate_PRIAM;

&populate_TMHMM;

&populate_LIPOPROTEIN;

&populate_PROTEIN_FUNCTION;

print `date`;

&compute_BTAB_rank_qry_vs_subj;

&compute_BTAB_rank_subj_vs_qry;

&compute_BTAB_rank_hsp_vs_hit;

&compute_HTAB_rank_qry_vs_hmm;

&compute_HTAB_rank_hmm_vs_qry;

print `date`;

##################################################################################3

sub initialize {
    use vars qw( $opt_d $opt_h );
    &Getopts('d:h');

    if ($opt_h) {
        &usage;
    }

    if (! -d $opt_d) {
        die "Could not locate targetDirectory=$workingDir\n";
    }

    return ( $opt_d );
}

sub usage {
    print "-d <target directory>\n";
}

sub create_database {
    if (-e $databaseFullpath) {
        print "Cleaning previous database\n";
        `rm $databaseFullpath`;
    }
    system("sqlite3 " . $databaseFullpath . "<" . $myLib . "/dataset.ddl");
    system("sqlite3 " . $databaseFullpath . "<" . $myLib . "/sequence.ddl");
    system("sqlite3 " . $databaseFullpath . "<" . $myLib . "/job_config.ddl");
    system("sqlite3 " . $databaseFullpath . "<" . $myLib . "/compute.ddl");
    if ( ! -e $databaseFullpath ) {
        die "\nCould not initialize sqlite db for database $databaseFullpath.\n"
    }
    chmod(0777, $databaseFullpath);
}

sub getGuidBlock {
    my $size=$_[0];
    my $ua=new LWP::UserAgent;
    $ua->agent("hcat/1.0");
    my $request=new HTTP::Request("GET", $GUID_BASE_PATH . "Size=$size");
    my $response=$ua->request($request);
    my $html_text=$response->content;
    $html_text=~/h2>\s+(\d+)\s+<\/body>/;
    return $1;
}

sub getNextGuid {
    if ($current_guid == 0 ||
        $current_guid == ($max_guid-1)) {
        # Need to get new block
        $max_guid=&getGuidBlock(10000);
        $current_guid=$max_guid;
        $max_guid=$current_guid+10000;
    } else {
        $current_guid+=1;
    }
    return $current_guid;
}

sub load_prop_hash {
    my $propFile="$workingDir\/persistSqlInfoFile.properties";
    if (! -e $propFile) {
        die "Could not find properties file $propFile\n";
    }
    open(INPUT, "<$propFile") || die "Could not open $propFile\n";
    while(<INPUT>) {
        if (/(\S.*\S)\s*=(.+)/) {
            $PROP_HASH{$1}=$2;
        }
    }
    close(INPUT);
}

sub populate_DATASET {
    print "starting populate_DATASET\n";
    my $dbh=connectSQLite($databaseFullpath);
    if (!&add_DATASET_row($dbh, $PROP_HASH{$QUERY_DATASET_NAME_PROP}, $PROP_HASH{$QUERY_FASTA_FILE_NODE_ID_PROP}, 0) ||
        !&add_DATASET_row($dbh, $PROP_HASH{$BLAST_PANDA_SUBJECT_DB_NAME_PROP}, $PROP_HASH{$BLAST_PANDA_SUBJECT_DB_ID_PROP}, 0) ||
        !&add_DATASET_row($dbh, $PROP_HASH{$HMM_SUBJECT_DB_NAME_PROP}, $PROP_HASH{$HMM_SUBJECT_DB_ID_PROP}, 0)) {
        print $errorMessage;
    } else {
        print "populate_DATASET completed successfully\n";
    }
    $dbh->commit();
    $dbh->disconnect;
}

sub add_DATASET_row {
    my ($dbh, $dataset_name, $current_version, $is_obsolete) = @_;
    my $add=&executeSQL($dbh,
        "insert into DATASET(dataset_name, current_version, is_obsolete) values (?,?,?)",
                        $dataset_name, $current_version, $is_obsolete);
    if (!defined $add) {
        $errorMessage="add_DATASET_row: " . $errorMessage;
        return undef;
    }
    return 1;
}

sub check_table {
    my ($table_name) = @_;
    print "starting check_table $table_name\n";
    my $dbh=connectSQLite($databaseFullpath);
    my $result=&querySQLArrayArray($dbh, "select * from $table_name");
    if (! defined $result) {
        $errorMessage = "check_table $table_name: " . $errorMessage;
        $dbh->disconnect;
        die $errorMessage . "\n";
    }
    foreach my $row ( @$result ) {
        my @values=@$row;
        my $vnum = scalar @values;
        print "Starting new row with $vnum columns\n";
        foreach my $val (@values) {
            print "$val\n";
        }
    }
    $dbh->disconnect;
    print "finished check_table $table_name\n";
}

sub populate_DATASET_VERSION {
    print "starting populate_DATASET_VERSION\n";
    my $dbh=connectSQLite($databaseFullpath);
    my $problem="false";
    if (!&add_DATASET_VERSION_row($dbh,
                                  $PROP_HASH{$QUERY_FASTA_FILE_NODE_ID_PROP},
                                  $PROP_HASH{$QUERY_DATASET_NAME_PROP},
                                  $PROP_HASH{$QUERY_FASTA_FILE_NODE_ID_PROP},
                                  "not specified",
                                  $PROP_HASH{$QUERY_DATASET_DESCRIPTION_PROP},
                                  "seq",
                                  "file",
                                  $PROP_HASH{$QUERY_FASTA_FILE_INPUT_PATH_PROP},
                                  "0",
                                  $PROP_HASH{$QUERY_SEQUENCE_COUNT_PROP},
                                  0)) {
        $problem="true";
        print $errorMessage;
    }
    if (!&add_DATASET_VERSION_row($dbh,
                                  $PROP_HASH{$BLAST_PANDA_SUBJECT_DB_ID_PROP},
                                  $PROP_HASH{$BLAST_PANDA_SUBJECT_DB_NAME_PROP},
                                  $PROP_HASH{$BLAST_PANDA_SUBJECT_DB_ID_PROP},
                                  "not specified",
                                  $PROP_HASH{$BLAST_PANDA_SUBJECT_DB_DESCRIPTION_PROP},
                                  "seq",
                                  "file",
                                  'n/a',
                                  "0",
                                  $PROP_HASH{$BLAST_PANDA_SUBJECT_DB_SEQUENCE_COUNT_PROP},
                                  0)) {
        $problem="true";
        print $errorMessage;
    }
    if (!&add_DATASET_VERSION_row($dbh,
                                  $PROP_HASH{$HMM_SUBJECT_DB_ID_PROP},
                                  $PROP_HASH{$HMM_SUBJECT_DB_NAME_PROP},
                                  $PROP_HASH{$HMM_SUBJECT_DB_ID_PROP},
                                  "not specified",
                                  $PROP_HASH{$HMM_SUBJECT_DB_DESCRIPTION_PROP},
                                  "hmm",
                                  "file",
                                  'n/a',
                                  "0",
                                  $PROP_HASH{$HMM_SUBJECT_DB_SEQUENCE_COUNT_PROP},
                                  0
                                  )) {
        $problem="true";
        print $errorMessage;
    }
    if ($problem eq "true") {
        die "Error loading DATASET_VERSION=$errorMessage\n";
    } else {
        print "populate_DATASET_VERSION completed successfully\n";
    }
    $dbh->commit();
    $dbh->disconnect;
}

sub add_DATASET_VERSION_row {
    my ($dbh,
        $version_id,
        $dataset_name,
        $dataset_version,
        $released_by,
        $description,
        $content_type,
        $source_type,
        $content_path,
        $md5,
        $content_count,
        $content_length) = @_;
    my $add=&executeSQL($dbh,
        "insert into DATASET_VERSION (".
                        "version_id, ".
                        "dataset_name, ".
                        "dataset_version, ".
                        "date_released, ".
                        "released_by, ".
                        "description, ".
                        "content_type, ".
                        "source_type, ".
                        "content_path, ".
                        "md5, ".
                        "content_count, ".
                        "content_length".
                        ") values (?,?,?,date(),?,?,?,?,?,?,?,?)",
                        $version_id,
                        $dataset_name,
                        $dataset_version,
                        $released_by,
                        $description,
                        $content_type,
                        $source_type,
                        $content_path,
                        $md5,
                        $content_count,
                        $content_length);
    if (!defined $add) {
        $errorMessage="add_DATASET_VERSION_row: " . $errorMessage;
        return undef;
    }
    return 1;
}

sub populate_JOB {
    print "starting populate_JOB\n";
    my $dbh=connectSQLite($databaseFullpath);
    if (
        # MG Annotation Parent Task
        !&add_JOB_row($dbh,
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_ID_PROP},               # job_id
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_NAME_PROP},             # job_name
                      "MG Annotation Parent task",                                # program_name
                      "not specified",                                            # program_version
                      "not specified",                                            # program_options
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_PROJECT_CODE_PROP},     # project_code
                      0,                                                          # subject_db_id
                      $PROP_HASH{$QUERY_FASTA_FILE_NODE_ID_PROP},                 # query_db_id
                      0,                                                          # parent_job_id
                      "not specified",                                            # status
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_DATE_SUBMITTED_PROP},   # date_submitted
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_OWNER_PROP},            # submitted_by
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_DATE_COMPLETED_PROP},   # date_completed
                      "not specified",                                            # result_type
                      "not specified",                                            # result_message
                      0,                                                          # num_results
                      0) ||                                                       # is_obsolete
        # Blast
        !&add_JOB_row($dbh,
                      $PROP_HASH{$JOB_MG_ANNO_BLAST_TASK_ID_PROP},                # job_id
                      $PROP_HASH{$JOB_MG_ANNO_BLAST_TASK_NAME_PROP},              # job_name
                      "MG Annotation Blastp Panda task",                          # program_name
                      "not specified",                                            # program_version
                      "not specified",                                            # program_options
                      $PROP_HASH{$JOB_MG_ANNO_BLAST_TASK_PROJECT_CODE_PROP},      # project_code
                      $PROP_HASH{$BLAST_PANDA_SUBJECT_DB_ID_PROP},                # subject_db_id
                      $PROP_HASH{$QUERY_FASTA_FILE_NODE_ID_PROP},                 # query_db_id
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_ID_PROP},               # parent_job_id
                      "not specified",                                            # status
                      $PROP_HASH{$JOB_MG_ANNO_BLAST_TASK_DATE_SUBMITTED_PROP},    # date_submitted
                      $PROP_HASH{$JOB_MG_ANNO_BLAST_TASK_OWNER_PROP},             # submitted_by
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_DATE_COMPLETED_PROP},   # date_completed
                      "not specified",                                            # result_type
                      "not specified",                                            # result_message
                      0,                                                          # num_results
                      0) ||                                                       # is_obsolete
        # Hmmpfam (Full)
        !&add_JOB_row($dbh,
                      $PROP_HASH{$JOB_MG_ANNO_HMMPFAM_TASK_ID_PROP},              # job_id
                      $PROP_HASH{$JOB_MG_ANNO_HMMPFAM_TASK_NAME_PROP},            # job_name
                      "MG Annotation Hmmpfam (Full) task",                        # program_name
                      "not specified",                                            # program_version
                      "not specified",                                            # program_options
                      $PROP_HASH{$JOB_MG_ANNO_HMMPFAM_TASK_PROJECT_CODE_PROP},    # project_code
                      $PROP_HASH{$HMM_SUBJECT_DB_ID_PROP},                        # subject_db_id
                      $PROP_HASH{$QUERY_FASTA_FILE_NODE_ID_PROP},                 # query_db_id
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_ID_PROP},               # parent_job_id
                      "not specified",                                            # status
                      $PROP_HASH{$JOB_MG_ANNO_HMMPFAM_TASK_DATE_SUBMITTED_PROP},  # date_submitted
                      $PROP_HASH{$JOB_MG_ANNO_HMMPFAM_TASK_OWNER_PROP},           # submitted_by
                      $PROP_HASH{$JOB_MG_ANNO_HMMPFAM_TASK_DATE_COMPLETED_PROP},  # date_completed
                      "not specified",                                            # result_type
                      "not specified",                                            # result_message
                      0,                                                          # num_results
                      0) ||                                                       # is_obsolete
        # Priam
        !&add_JOB_row($dbh,
                      $PRIAM_JOB_ID,                                              # job_id
                      "Priam",                                                    # job_name
                      "MG Annotation Priam service",                              # program_name
                      "not specified",                                            # program_version
                      "not specified",                                            # program_options
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_PROJECT_CODE_PROP},     # project_code
                      0,                                                          # subject_db_id
                      $PROP_HASH{$QUERY_FASTA_FILE_NODE_ID_PROP},                 # query_db_id
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_ID_PROP},               # parent_job_id
                      "not specified",                                            # status
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_DATE_SUBMITTED_PROP},   # date_submitted
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_OWNER_PROP},            # submitted_by
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_DATE_COMPLETED_PROP},   # date_completed
                      "not specified",                                            # result_type
                      "not specified",                                            # result_message
                      0,                                                          # num_results
                      0) ||                                                       # is_obsolete
        # Lipoprotein
        !&add_JOB_row($dbh,
                      $LIPOPROTEIN_JOB_ID,                                        # job_id
                      "Lipoprotein",                                              # job_name
                      "MG Annotation Lipoprotein service",                        # program_name
                      "not specified",                                            # program_version
                      "not specified",                                            # program_options
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_PROJECT_CODE_PROP},     # project_code
                      0,                                                          # subject_db_id
                      $PROP_HASH{$QUERY_FASTA_FILE_NODE_ID_PROP},                 # query_db_id
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_ID_PROP},               # parent_job_id
                      "not specified",                                            # status
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_DATE_SUBMITTED_PROP},   # date_submitted
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_OWNER_PROP},            # submitted_by
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_DATE_COMPLETED_PROP},   # date_completed
                      "not specified",                                            # result_type
                      "not specified",                                            # result_message
                      0,                                                          # num_results
                      0) ||                                                       # is_obsolete
        # Tmhmm
        !&add_JOB_row($dbh,
                      $TMHMM_JOB_ID,                                              # job_id
                      "Tmhmm",                                                    # job_name
                      "MG Annotation Tmhmm service",                              # program_name
                      "not specified",                                            # program_version
                      "not specified",                                            # program_options
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_PROJECT_CODE_PROP},     # project_code
                      0,                                                          # subject_db_id
                      $PROP_HASH{$QUERY_FASTA_FILE_NODE_ID_PROP},                 # query_db_id
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_ID_PROP},               # parent_job_id
                      "not specified",                                            # status
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_DATE_SUBMITTED_PROP},   # date_submitted
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_OWNER_PROP},            # submitted_by
                      $PROP_HASH{$JOB_MG_ANNO_PARENT_TASK_DATE_COMPLETED_PROP},   # date_completed
                      "not specified",                                            # result_type
                      "not specified",                                            # result_message
                      0,                                                          # num_results
                      0)) {                                                       # is_obsolete
        print $errorMessage;
    } else {
        print "populate_JOB completed successfully\n";
    }
    $dbh->commit();
    $dbh->disconnect;
}

sub add_JOB_row {
    my ($dbh,
        $job_id,
        $job_name,
        $program_name,
        $program_version,
        $program_options,
        $project_code,
        $subject_db_id,
        $query_db_id,
        $parent_job_id,
        $status,
        $date_submitted,
        $submitted_by,
        $date_completed,
        $result_type,
        $result_message,
        $num_results,
        $is_obsolete) = @_;
    my $add=&executeSQL($dbh,
        "insert into JOB (".
                        "job_id, ".
                        "job_name, ".
                        "program_name, ".
                        "program_version, ".
                        "program_options, ".
                        "project_code, ".
                        "subject_db_id, ".
                        "query_db_id, ".
                        "parent_job_id, ".
                        "status, ".
                        "date_submitted, ".
                        "submitted_by, ".
                        "date_completed, ".
                        "result_type, ".
                        "result_message, ".
                        "num_results, ".
                        "is_obsolete".
                        ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        $job_id,
                        $job_name,
                        $program_name,
                        $program_version,
                        $program_options,
                        $project_code,
                        $subject_db_id,
                        $query_db_id,
                        $parent_job_id,
                        $status,
                        $date_submitted,
                        $submitted_by,
                        $date_completed,
                        $result_type,
                        $result_message,
                        $num_results,
                        $is_obsolete);
    if (!defined $add) {
        $errorMessage="add_JOB_row: " . $errorMessage;
        return undef;
    }
    return 1;
}

sub populate_DATASET_SEQ {
    print "starting populate_DATASET_SEQ\n";
    my $dbh=connectSQLite($databaseFullpath);
    my $fastaInputFile=$PROP_HASH{$QUERY_FASTA_FILE_INPUT_PATH_PROP};
    my $version_id=$PROP_HASH{$QUERY_FASTA_FILE_NODE_ID_PROP};
    open(FASTA_INPUT, "<$fastaInputFile") || die "Could not open fasta input file $fastaInputFile\n";
    my $sequence_count=0;
    my $current_seq_id;
    my $current_seq_length=0;
    my $current_seq_acc;
    my $current_seq_definition;
    while(<FASTA_INPUT>) {
        my $current_line=$_;
        chomp $current_line;
        if ($current_line=~/^(>.+)/) {
            if ($sequence_count % 10000 == 0) {
                print ".";
            }
            $sequence_count+=1;
            my $defline=$1;
            if (defined $current_seq_acc) {
                $query_acc_to_id_hash{$current_seq_acc}=$current_seq_id;
                if (!&add_DATASET_SEQ_row($dbh,
                                          $version_id,
                                          $current_seq_id,
                                          $current_seq_acc,
                                          $current_seq_length,
                                          $current_seq_definition)) {
                    die $errorMessage;
                }
            }
            # Populate current vars with new data
            $current_line=~/>(\S+)/;
            if (! defined $1) {
                die "Could not parse accession from defline=$current_line\n";
            }
            $current_seq_id=&getNextGuid;
            $current_seq_length=0;
            $current_seq_acc=$1;
            $current_seq_definition=$current_line;
        } else {
            # This is a sequence row
            $current_line=~/\s*(\S+)\s*/;
            if (defined $1) {
                my $seq_chars=$1;
                my $seq_len=length $seq_chars;
                $current_seq_length += $seq_len;
            }
        }
    }
    # Take care of last entry
    if (defined $current_seq_acc) {
        $query_acc_to_id_hash{$current_seq_acc}=$current_seq_id;
        if (!&add_DATASET_SEQ_row($dbh,
                                  $version_id,
                                  $current_seq_id,
                                  $current_seq_acc,
                                  $current_seq_length,
                                  $current_seq_definition)) {
            die $errorMessage;
        }
    }
    close(FASTA_INPUT);
    print "populate_DATASET_SEQ completed successfully, loaded $sequence_count sequences\n";
    $dbh->commit();
    $dbh->disconnect;
}

sub add_DATASET_SEQ_row {
    my ($dbh,
        $version_id,
        $seq_id,
        $seq_acc,
        $seq_definition,
        $seq_length) = @_;
    my $add=&executeSQL($dbh,
        "insert into DATASET_SEQ (".
                        "version_id, ".
                        "seq_id, ".
                        "seq_acc, ".
                        "seq_definition, ".
                        "seq_length ".
                        ") values (?,?,?,?,?)",
                        $version_id,
                        $seq_id,
                        $seq_acc,
                        $seq_definition,
                        $seq_length);
    if (!defined $add) {
        $errorMessage="add_DATASET_SEQ_row: " . $errorMessage;
        return undef;
    }
    return 1;
}

sub populate_BTAB {
    print "starting populate_BTAB\n";
    my $btab_count=0;
    my $dbh=connectSQLite($databaseFullpath);
    open(BTAB, "<$btabFullpath") || die "Could not open file $btabFullpath to read\n";
    while(<BTAB>) {
        my $line=$_;
        $line=~/\s*(\S.+\S)\s*/;
        if (defined $1) {
            my $trimmed_line=$1;
            my @btabarr=split /\t/, $trimmed_line;
            my $btablen = scalar @btabarr;
            if ($btablen<20) {
                print "Could not parse btab line $trimmed_line, found $btablen entries but expected 21:\n";
                my $c=0;
                foreach my $mem (@btabarr) {
                    print "$c\t$mem\n";
                    $c+=1;
                }
                die "\n";
            }
            if ($btablen==20) {
                # Assume missing the pvalue
                push @btabarr, 'n/a';
            }
            my $query_name=$btabarr[0];
            my $date=$btabarr[1];
            my $query_length=$btabarr[2];
            my $algorithm=$btabarr[3];
            my $database_name=$btabarr[4];
            my $hit_name=$btabarr[5];
            my $qry_start=$btabarr[6];
            my $qry_end=$btabarr[7];
            my $hit_start=$btabarr[8];
            my $hit_end=$btabarr[9];
            my $percent_identity=$btabarr[10];
            my $percent_similarity=$btabarr[11];
            my $raw_score=$btabarr[12];
            my $bit_score=$btabarr[13];
            # NOTE: btabarr[14] is skipped intentionally
            my $hit_description=$btabarr[15];
            my $blast_frame=$btabarr[16];
            my $qry_strand=$btabarr[17];
            my $hit_length=$btabarr[18];
            my $e_value=$btabarr[19];
            my $p_value=$btabarr[20];
            if ($btab_count % 10000==0) {
                print ".";
            }
            if (!&add_BTAB_row($dbh,
                               &getNextGuid,                                # $btab_id,
                               $PROP_HASH{$JOB_MG_ANNO_BLAST_TASK_ID_PROP}, # $job_id,
                               0, # TBD $rank_qry_vs_subj,
                               0, # TBD $rank_subj_vs_qry,
                               0, # TBD $rank_hsp_vs_hit,
                               $hit_name,                                   # $subject_seq_id,
                               $hit_start,                                  # $subject_left,
                               $hit_end,                                    # $subject_right,
                               'n/a',                                       # $subject_frame,
                               $qry_strand,                                 # $orientation,
                               $query_name,                                 # $query_seq_id,
                               $qry_start,                                  # $query_end5,
                               $qry_end,                                    # $query_end3,
                               $blast_frame,                                # $query_frame,
                               'n/a',                                       # $num_identical,
                               'n/a',                                       # $num_similar,
                               'n/a',                                       # $num_gaps,
                               'n/a',                                       # $alignment_length,
                               $percent_identity,                           # $pct_identity,
                               $percent_similarity,                         # $pct_similarity,
                               0.0, # TBD $pct_length,
                               $raw_score,                                  # $hsp_score,
                               $bit_score,                                  # $bit_score,
                               $e_value,                                    # $evalue,
                               $p_value,                                    # $pvalue
                               )) {
                die $errorMessage;
            } else {
                $btab_count++;
            }
        }
    }
    close(BTAB);
    print "finished populate_BTAB, added $btab_count entries\n";
    $dbh->commit();
    $dbh->disconnect;
}

sub add_BTAB_row {
    my ($dbh,
        $btab_id,
        $job_id,
        $rank_qry_vs_subj,
        $rank_subj_vs_qry,
        $rank_hsp_vs_hit,
        $subject_seq_id,
        $subject_left,
        $subject_right,
        $subject_frame,
        $orientation,
        $query_seq_id,
        $query_end5,
        $query_end3,
        $query_frame,
        $num_identical,
        $num_similar,
        $num_gaps,
        $alignment_length,
        $pct_identity,
        $pct_similarity,
        $pct_length,
        $hsp_score,
        $bit_score,
        $evalue,
        $pvalue
        ) = @_;
    my $add=&executeSQL($dbh,
        "insert into BTAB (".
                        "btab_id, ".
                        "job_id, ".
                        "rank_qry_vs_subj, ".
                        "rank_subj_vs_qry, ".
                        "rank_hsp_vs_hit, ".
                        "subject_seq_id, ".
                        "subject_left, ".
                        "subject_right, ".
                        "subject_frame, ".
                        "orientation, ".
                        "query_seq_id, ".
                        "query_end5, ".
                        "query_end3, ".
                        "query_frame, ".
                        "num_identical, ".
                        "num_similar, ".
                        "num_gaps, ".
                        "alignment_length, ".
                        "pct_identity, ".
                        "pct_similarity, ".
                        "pct_length, ".
                        "hsp_score, ".
                        "bit_score, ".
                        "evalue, ".
                        "pvalue ".
                        ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        $btab_id,
                        $job_id,
                        $rank_qry_vs_subj,
                        $rank_subj_vs_qry,
                        $rank_hsp_vs_hit,
                        $subject_seq_id,
                        $subject_left,
                        $subject_right,
                        $subject_frame,
                        $orientation,
                        $query_seq_id,
                        $query_end5,
                        $query_end3,
                        $query_frame,
                        $num_identical,
                        $num_similar,
                        $num_gaps,
                        $alignment_length,
                        $pct_identity,
                        $pct_similarity,
                        $pct_length,
                        $hsp_score,
                        $bit_score,
                        $evalue,
                        $pvalue);
    if (!defined $add) {
        $errorMessage="add_BTAB_row: " . $errorMessage;
        return undef;
    }
    return 1;
}


sub populate_HTAB {
    print "starting populate_HTAB\n";
    my $htab_count=0;
    my $dbh=connectSQLite($databaseFullpath);
    ### FIRST PASS - to populate DATASET_HMM
    my %hmm_acc_to_id_hash;
    open(HTAB, "<$htabFullpath") || die "Could not open file $htabFullpath to read\n";
    while(<HTAB>) {
        my $line=$_;
        $line=~/\s*(\S.+\S)\s*/;
        if (defined $1 &&
            (! ($line=~/\s*No hits\s.+/))) {
            my ($hmm_acc,
                $date,
                $hmm_length,
                $search_method,
                $database_name,
                $query_acc,
                $hmm_begin,
                $hmm_end,
                $query_begin,
                $query_end,
                $null_value_1,
                $domain_score,
                $total_score,
                $domain_index,
                $domain_count,
                $hmm_definition,
                $query_definition,
                $hmm_cutoff,
                $noise_cutoff,
                $total_evalue,
                $domain_evalue) = &parse_HTAB_line($line);
            if (undef $hmm_definition ||
                $hmm_definition=~/\s*/) {
                $hmm_definition="Hmm definition undefined for hmm accession $hmm_acc\n";
            }
            if (undef $hmm_length ||
                $hmm_length=~/\s*/) {
                $hmm_length=0;
            }
            my $hmm_id=$hmm_acc_to_id_hash{$hmm_acc};
            if (! defined $hmm_id) {
                my $hmm_id=&getNextGuid;
                $hmm_acc_to_id_hash{$hmm_acc}=$hmm_id;
                if (!add_DATASET_HMM_row($dbh,
                                         $PROP_HASH{$HMM_SUBJECT_DB_ID_PROP},
                                         $hmm_id,
                                         $hmm_acc,
                                         $hmm_definition,
                                         $hmm_length)) {
                    die "Error adding row to DATASET_HMM: $errorMessage\n";
                }
            } else {
                # since already defined, assume an entry is already in DATASET_HMM
            }
        }
    }
    close(HTAB);

    ### SECOND PASS - to populate HTAB
    print "Beginning second pass for HTAB\n";
    open(HTAB, "<$htabFullpath") || die "Could not open file $htabFullpath to read\n";
    while(<HTAB>) {
        my $line=$_;
        $line=~/\s*(\S.+\S)\s*/;
        if (defined $1 &&
            (! ($line=~/\s*No hits\s.+/))) {
            my ($hmm_acc,
                $date,
                $hmm_length,
                $search_method,
                $database_name,
                $query_acc,
                $hmm_begin,
                $hmm_end,
                $query_begin,
                $query_end,
                $null_value_1,
                $domain_score,
                $total_score,
                $domain_index,
                $domain_count,
                $hmm_definition,
                $query_definition,
                $hmm_cutoff,
                $noise_cutoff,
                $total_evalue,
                $domain_evalue) = &parse_HTAB_line($line);
            # Get hmm definition info

            my $hmm_id=$hmm_acc_to_id_hash{$hmm_acc};
            if (! defined $hmm_id) {
                die "Unexpectedly could not find hmm_id definition for hmm_acc=$hmm_acc\n";
            }
            my $query_seq_id=$query_acc_to_id_hash{$query_acc};
            if (! (defined $query_seq_id)) {
                die "Unexpectedly could not find query_seq_id corresponding to query_acc=$query_acc\n";
            }
            if (!&add_HTAB_row($dbh,
                               &getNextGuid,                                     # htab_id
                               $PROP_HASH{$JOB_MG_ANNO_HMMPFAM_TASK_ID_PROP},    # job_id
                               0, # rank_qry_vs_hmm TBD
                               0, # rank_hmm_vs_qry TBD
                               0, # rank_dom_vs_hit TBD
                               $hmm_id,
                               $query_seq_id,
                               $hmm_begin,
                               $hmm_end,
                               $query_begin,
                               $query_end,
                               $domain_evalue,
                               $domain_score,
                               $domain_index,
                               0, # domain_matches TBD
                               $domain_count,
                               $total_evalue,
                               $total_score
                               )) {
                die $errorMessage;
            } else {
                $htab_count++;
            }
        }
    }
    close(HTAB);
    print "finished populate_HTAB, added $htab_count entries\n";
    $dbh->commit();
    $dbh->disconnect;
}

sub parse_HTAB_line {
    my $trimmed_line=$_;
    my @htabarr=split /\t/, $trimmed_line;
    my $htablen = scalar @htabarr;
    if ($htablen<21) {
        print "Could not parse htab line $trimmed_line, found $htablen entries but expected 21:\n";
        my $c=0;
        foreach my $mem (@htabarr) {
            print "$c\t$mem\n";
            $c+=1;
        }
        die "\n";
    }
    return @htabarr;
}

sub add_HTAB_row {
    my ($dbh,
        $htab_id,
        $job_id,
        $rank_qry_vs_hmm,
        $rank_hmm_vs_qry,
        $rank_dom_vs_hit,
        $subject_hmm_id,
        $query_seq_id,
        $hmm_begin,
        $hmm_end,
        $query_begin,
        $query_end,
        $domain_evalue,
        $domain_score,
        $domain_index,
        $domain_matches,
        $domain_count,
        $total_evalue,
        $total_score) = @_;
    my $add=&executeSQL($dbh,
        "insert into HTAB (".
                        "htab_id, ".
                        "job_id, ".
                        "rank_qry_vs_hmm, ".
                        "rank_hmm_vs_qry, ".
                        "rank_dom_vs_hit, ".
                        "subject_hmm_id, ".
                        "query_seq_id, ".
                        "hmm_begin, ".
                        "hmm_end, ".
                        "query_begin, ".
                        "query_end, ".
                        "domain_evalue, ".
                        "domain_score, ".
                        "domain_index, ".
                        "domain_matches, ".
                        "domain_count, ".
                        "total_evalue, ".
                        "total_score ".
                        ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        $htab_id,
                        $job_id,
                        $rank_qry_vs_hmm,
                        $rank_hmm_vs_qry,
                        $rank_dom_vs_hit,
                        $subject_hmm_id,
                        $query_seq_id,
                        $hmm_begin,
                        $hmm_end,
                        $query_begin,
                        $query_end,
                        $domain_evalue,
                        $domain_score,
                        $domain_index,
                        $domain_matches,
                        $domain_count,
                        $total_evalue,
                        $total_score);
    if (!defined $add) {
        $errorMessage="add_HTAB_row: " . $errorMessage;
        return undef;
    }
    return 1;
}

sub add_DATASET_HMM_row {
    my ($dbh,
        $version_id,
        $hmm_id,
        $hmm_acc,
        $hmm_definition,
        $hmm_length) = @_;
    my $add=&executeSQL($dbh,
        "insert into DATASET_HMM (".
                        "version_id, ".
                        "hmm_id, ".
                        "hmm_acc, ".
                        "hmm_definition, ".
                        "hmm_length ".
                        ") values (?,?,?,?,?)",
                        $version_id,
                        $hmm_id,
                        $hmm_acc,
                        $hmm_definition,
                        $hmm_length);
    if (!defined $add) {
        $errorMessage="add_DATASET_HMM_row: " . $errorMessage;
        return undef;
    }
    return 1;
}

sub populate_PRIAM {
    print "starting populate_PRIAM\n";
    my $priam_count=0;
    my $dbh=connectSQLite($databaseFullpath);
    open(PRIAM, "<$priamFullpath") || die "Could not open file $priamFullpath to read\n";
    while(<PRIAM>) {
        my $line=$_;
        $line=~/\s*(\S.+\S)\s*/;
        if (defined $1) {
            my $trimmed_line=$1;
            my @priamarr=split /\t/, $trimmed_line;
            my $priamlen = scalar @priamarr;
            if ($priamlen<7) {
                print "Could not parse priam line $trimmed_line, found $priamlen entries but expected 7:\n";
                my $c=0;
                foreach my $mem (@priamarr) {
                    print "$c\t$mem\n";
                    $c+=1;
                }
                die "\n";
            }
            my $query_seq_id=$priamarr[0];
            my $ec_num=$priamarr[1];
            my $ec_definition=$priamarr[2];
            my $evalue=$priamarr[3];
            my $bit_score=$priamarr[4];
            my $query_begin=$priamarr[5];
            my $query_end=$priamarr[6];

            if ($priam_count % 10000==0) {
                print ".";
            }

            if (!&add_PRIAM_row($dbh,
                                &getNextGuid,
                                $PRIAM_JOB_ID,
                                $query_seq_id,
                                $ec_num,
                                $ec_definition,
                                $evalue,
                                $bit_score,
                                $query_begin,
                                $query_end)) {
                die $errorMessage;
            } else {
                $priam_count++;
            }
        }
    }
    close(PRIAM);
    print "finished populate_PRIAM, added $priam_count entries\n";
    $dbh->commit();
    $dbh->disconnect;
}

sub add_PRIAM_row {
    my ($dbh,
        $priam_id,
        $job_id,
        $query_seq_id,
        $ec_num,
        $ec_definition,
        $evalue,
        $bit_score,
        $query_begin,
        $query_end) = @_;
    my $add=&executeSQL($dbh,
        "insert into PRIAM (".
                        "priam_id, ".
                        "job_id, ".
                        "query_seq_id, ".
                        "ec_num, ".
                        "ec_definition, ".
                        "evalue, ".
                        "bit_score, ".
                        "query_begin, ".
                        "query_end ".
                        ") values (?,?,?,?,?,?,?,?,?)",
                        $priam_id,
                        $job_id,
                        $query_seq_id,
                        $ec_num,
                        $ec_definition,
                        $evalue,
                        $bit_score,
                        $query_begin,
                        $query_end);
    if (!defined $add) {
        $errorMessage="add_PRIAM_row: " . $errorMessage;
        return undef;
    }
    return 1;
}

sub populate_TMHMM {
    print "starting populate_TMHMM\n";
    my $tmhmm_count=0;
    my $dbh=connectSQLite($databaseFullpath);
    open(TMHMM, "<$tmhmmFullpath") || die "Could not open file $tmhmmFullpath to read\n";
    my $current_query_seq_id;
    my $current_exp_aa;
    my $current_exp_first60;
    my $current_num_predicted_helixes;
    my $current_topology;
    my $prev_top_line="true"; # start this way
    while(<TMHMM>) {
        my $line=$_;
        $line=~/\s*(\S.+\S)\s*/;
        if (defined $1) {
            my $trimmed_line=$1;
            if ($trimmed_line=~/\#\s+(\S+)/) {
                if ($prev_top_line eq "true") {
                    if (defined $current_query_seq_id) {
                        if ($tmhmm_count % 10000==0) {
                            print ".";
                        }
                        if (!&add_TMHMM_row($dbh,
                                            $TMHMM_JOB_ID,
                                            $current_query_seq_id,
                                            $current_exp_aa,
                                            $current_exp_first60,
                                            $current_num_predicted_helixes,
                                            $current_topology)) {
                            die $errorMessage;
                        } else {
                            $tmhmm_count++;
                        }
                        undef $current_query_seq_id;
                        undef $current_exp_aa;
                        undef $current_exp_first60;
                        undef $current_num_predicted_helixes;
                        undef $current_topology;
                    }
                    $current_query_seq_id=$1;
                }
                if (! (defined $current_query_seq_id)) {
                    die "current_query_seq_id is unexpectedly undefined. Current line=$trimmed_line\n";
                }
                if ($trimmed_line=~/.+Exp number of AAs in TMHs:\s+(\S+)\s*/) {
                    $current_exp_aa=$1;
                } elsif ($trimmed_line=~/.+Exp number, first 60 AAs:\s+(\S+)\s*/) {
                    $current_exp_first60=$1;
                } elsif ($trimmed_line=~/.+Number of predicted TMHs:\s+(\S+)\s*/) {
                    $current_num_predicted_helixes=$1;
                }
                $prev_top_line="false";
            } else {
                # Assume topology row
                my @toparr=split /\s+/, $trimmed_line;
                my $topcount=scalar @toparr;
                if ($topcount < 5) {
                    die "Could not parse tmhmm line=$trimmed_line\n";
                }
                my $info=$toparr[2]." ".$toparr[3]. " ".$toparr[4]." ";
                if (!(defined $current_topology)) {
                    $current_topology=$info;
                } else {
                    $current_topology.=", $info";
                }
                # Track state info
                $prev_top_line="true";
            }
        }
    }
    # Handle last case
    if ($prev_top_line eq "true") {
        if (!&add_TMHMM_row($dbh,
                            $TMHMM_JOB_ID,
                            $current_query_seq_id,
                            $current_exp_aa,
                            $current_exp_first60,
                            $current_num_predicted_helixes,
                            $current_topology)) {
            die $errorMessage;
        }
    }
    close(TMHMM);
    print "finished populate_TMHMM, added $tmhmm_count entries\n";
    $dbh->commit();
    $dbh->disconnect;
}

sub add_TMHMM_row {
    my ($dbh,
        $job_id,
        $query_seq_id,
        $exp_aa,
        $exp_first60,
        $num_predicted_helixes,
        $topology) = @_;
    my $add=&executeSQL($dbh,
        "insert into TMHMM (".
                        "job_id, ".
                        "query_seq_id, ".
                        "exp_aa, ".
                        "exp_first60, ".
                        "num_predicted_helixes, ".
                        "topology ".
                        ") values (?,?,?,?,?,?)",
                        $job_id,
                        $query_seq_id,
                        $exp_aa,
                        $exp_first60,
                        $num_predicted_helixes,
                        $topology);
    if (!defined $add) {
        $errorMessage="add_TMHMM_row: " . $errorMessage;
        return undef;
    }
    return 1;
}


sub populate_LIPOPROTEIN {
    print "starting populate_LIPOPROTEIN\n";
    my $lipoprotein_count=0;
    my $dbh=connectSQLite($databaseFullpath);
    open(LIPOPROTEIN, "<$lipoproteinFullpath") || die "Could not open file $lipoproteinFullpath to read\n";
    while(<LIPOPROTEIN>) {
        my $line=$_;
        $line=~/\s*(\S.+\S)\s*/;
        if (defined $1) {
            my $trimmed_line=$1;
            my @lipoarr=split /\s+/, $trimmed_line;
            my $lipo_size=scalar @lipoarr;
            if ($lipo_size < 10) {
                die "Could not parse lipoprotein line=$trimmed_line\n";
            }
            my $query_seq_id=$lipoarr[0];
            if (!add_LIPOPROTEIN_row($dbh,
                                     $LIPOPROTEIN_JOB_ID,
                                     $query_seq_id,
                                     1)) {
                die $errorMessage;
            } else {
                $lipoprotein_count++;
            }
        }
    }
    close(LIPOPROTEIN);
    print "finished populate_LIPOPROTEIN, added $lipoprotein_count entries\n";
    $dbh->commit();
    $dbh->disconnect;
}


sub add_LIPOPROTEIN_row {
    my ($dbh,
        $job_id,
        $query_seq_id,
        $is_lipoprotein) = @_;
    my $add=&executeSQL($dbh,
        "insert into LIPOPROTEIN (".
                        "job_id, ".
                        "query_seq_id, ".
                        "is_lipoprotein".
                        ") values (?,?,?)",
                        $job_id,
                        $query_seq_id,
                        $is_lipoprotein);
    if (!defined $add) {
        $errorMessage="add_LIPOPROTEIN_row: " . $errorMessage;
        return undef;
    }
    return 1;
}

sub populate_PROTEIN_FUNCTION {
    print "starting populate_PROTEIN_FUNCTION\n";
    my $annotation_count=0;
    my $dbh=connectSQLite($databaseFullpath);
    open(ANNOTATION, "<$annotationFullpath") || die "Could not open file $annotationFullpath to read\n";
    while(<ANNOTATION>) {
        my $line=$_;
        $line=~/\s*(\S.+\S)\s*/;
        if (defined $1) {
            my $trimmed_line=$1;
            my @annoarr=split /\t/, $trimmed_line;
            my $anno_size=scalar @annoarr;
            if ($anno_size < 14) {
                print "Expected at least 14 tab fields, found $anno_size, could not parse annotation line=$trimmed_line\n";
                my $c=0;
                foreach my $mem (@annoarr) {
                    print "$c\t$mem\n";
                    $c++;
                }
                die;
            }
            my $query_acc=$annoarr[0];
            my $query_seq_id=$query_acc_to_id_hash{$query_acc};
            if (! (defined $query_seq_id)) {
                die "Could not find query_seq_is match to query_acc=$query_acc\n";
            }
            my $field=1;
            while ($field<16) {
                my $class=$annoarr[$field];
                my $value;
                my $source;
                if ($anno_size>14) {
                    $value=$annoarr[$field+1];
                } else {
                    $value="";
                }
                if ($anno_size>15) {
                    $source=$annoarr[$field+2];
                } else {
                    $source="";
                }
                if ($value=~/\s*(\S.+\S)\s*/) {
                    my $trimmed_value=$1;
                    if (!add_PROTEIN_FUNCTION_row($dbh,
                                                  &getNextGuid,
                                                  $PROTEIN_FUNCTION_JOB_ID,
                                                  $query_seq_id,
                                                  $class,
                                                  $trimmed_value,
                                                  $source)) {
                        die $errorMessage;
                    } else {
                        $annotation_count++;
                    }
                }
                $field+=3;
            }
        }
    }
    close(ANNOTATION);
    print "finished populate_PROTEIN_FUNCTION, added $annotation_count entries\n";
    $dbh->commit();
    $dbh->disconnect;
}

sub add_PROTEIN_FUNCTION_row {
    my ($dbh,
        $function_id,
        $job_id,
        $query_seq_id,
        $function_class,
        $function_value,
        $evidence_source) = @_;
    my $add=&executeSQL($dbh,
                        "insert into PROTEIN_FUNCTION (".
                        "function_id, ".
                        "job_id, ".
                        "query_seq_id, ".
                        "function_class, ".
                        "function_value, ".
                        "evidence_source".
                        ") values (?,?,?,?,?,?)",
                        $function_id,
                        $job_id,
                        $query_seq_id,
                        $function_class,
                        $function_value,
                        $evidence_source);
    if (!defined $add) {
        $errorMessage="add_PROTEIN_FUNCTION_row: " . $errorMessage;
        return undef;
    }
    return 1;
}

sub compute_BTAB_rank_qry_vs_subj {
    print "starting compute_BTAB_rank_qry_vs_subj\n";
    my $dbh=connectSQLite($databaseFullpath);

    ###
    ### rank_qry_vs_subj
    ###
    # First, obtain the list of subject ids in the table
    my $add=&executeSQL($dbh, "create table tmp_rank_qry_vs_subj ( btab_id integer, rank_qry_vs_subj integer not null, primary key(btab_id) )");
    if (!defined $add) {
        $errorMessage="compute_BTAB_rank_qry_vs_subj: " . $errorMessage;
        die $errorMessage;
    }
    my $result=&querySQLArrayArray($dbh, "select distinct subject_seq_id from BTAB");
    foreach my $resultRow (@$result) {
        my $subject_seq_id=$$resultRow[0];
#       print "Computing rank_qry_vs_subj for subject_seq_id=$subject_seq_id------------------------------------------------------\n";
        # For each subject, get the list of hits
        my $result2=&querySQLArrayArray($dbh, "select query_seq_id, btab_id, subject_seq_id, evalue, bit_score from BTAB where subject_seq_id=? order by query_seq_id asc, evalue asc, bit_score desc", $subject_seq_id);
        my %query_rank_hash;
        my $rank=1;
        foreach my $resultRow2 (@$result2) {
            my $query_seq_id=$$resultRow2[0];
            my $btab_id=$$resultRow2[1];
            my $current_rank=$query_rank_hash{$query_seq_id};
            if (! (defined $current_rank)) {
                $query_rank_hash{$query_seq_id}=$rank;
                $current_rank=$rank;
                $rank++;
            }
            my $evalue=$$resultRow2[3];
            my $bit_score=$$resultRow2[4];
#           print "updating BTAB btab_id=$btab_id query_seq_id=$query_seq_id evalue=$evalue bit_score=$bit_score rank_qry_vs_subj=$current_rank\n";
#           print "Adding btab_id=$btab_id  qry_vs_subj_rank=$current_rank\n";
            $add=&executeSQL($dbh, "insert into tmp_rank_qry_vs_subj(btab_id, rank_qry_vs_subj) values (?,?)",
                             $btab_id, $current_rank);
            if (!defined $add) {
                $errorMessage="compute_BTAB_rank_qry_vs_subj: " . $errorMessage;
                die $errorMessage;
            }
#           my $add=&executeSQL($dbh, "update BTAB set rank_qry_vs_subj=? where btab_id=?", $current_rank, $btab_id);
        }
    }
    $add=&executeSQL($dbh, "update btab set rank_qry_vs_subj = (select tmp_rank_qry_vs_subj.rank_qry_vs_subj from tmp_rank_qry_vs_subj where tmp_rank_qry_vs_subj.btab_id = btab.btab_id) where exists (select tmp_rank_qry_vs_subj.rank_qry_vs_subj from tmp_rank_qry_vs_subj where tmp_rank_qry_vs_subj.btab_id = btab.btab_id)");
    if (!defined $add) {
        $errorMessage="compute_BTAB_rank_qry_vs_subj: " . $errorMessage;
        die $errorMessage;
    }
    $add=&executeSQL($dbh, "drop table tmp_rank_qry_vs_subj");
    if (!defined $add) {
        $errorMessage="compute_BTAB_rank_qry_vs_subj: " . $errorMessage;
        die $errorMessage;
    }
    print "finished compute_BTAB_rank_qry_vs_subj\n";
    $dbh->commit();
    $dbh->disconnect;
}

sub compute_BTAB_rank_subj_vs_qry {
    print "starting compute_BTAB_rank_subj_vs_qry\n";
    my $dbh=connectSQLite($databaseFullpath);

    ###
    ### rank_subj_vs_qry
    ###
    # First, obtain the list of query ids in the table
    my $add=&executeSQL($dbh, "create table tmp_rank_subj_vs_qry ( btab_id integer, rank_subj_vs_qry integer not null, primary key(btab_id) )");
    if (!defined $add) {
        $errorMessage="compute_BTAB_rank_subj_vs_qry: " . $errorMessage;
        die $errorMessage;
    }
    my $result=&querySQLArrayArray($dbh, "select distinct query_seq_id from BTAB");
    foreach my $resultRow (@$result) {
        my $query_seq_id=$$resultRow[0];
#       print "Computing rank_subj_vs_qry for query_seq_id=$query_seq_id------------------------------------------------------\n";
        # For each query, get the list of hits
        my $result2=&querySQLArrayArray($dbh, "select subject_seq_id, btab_id, query_seq_id, evalue, bit_score from BTAB where query_seq_id=? order by subject_seq_id asc, evalue asc, bit_score desc", $query_seq_id);
        my %subject_rank_hash;
        my $rank=1;
        foreach my $resultRow2 (@$result2) {
            my $subject_seq_id=$$resultRow2[0];
            my $btab_id=$$resultRow2[1];
            my $current_rank=$subject_rank_hash{$subject_seq_id};
            if (! (defined $current_rank)) {
                $subject_rank_hash{$subject_seq_id}=$rank;
                $current_rank=$rank;
                $rank++;
            }
            my $evalue=$$resultRow2[3];
            my $bit_score=$$resultRow2[4];
#           print "updating BTAB btab_id=$btab_id subject_seq_id=$subject_seq_id evalue=$evalue bit_score=$bit_score rank_subj_vs_qry=$current_rank\n";
#           print "Adding btab_id=$btab_id  subj_vs_qry_rank=$current_rank\n";
            $add=&executeSQL($dbh, "insert into tmp_rank_subj_vs_qry(btab_id, rank_subj_vs_qry) values (?,?)",
                             $btab_id, $current_rank);
            if (!defined $add) {
                $errorMessage="compute_BTAB_rank_subj_vs_qry: " . $errorMessage;
                die $errorMessage;
            }
#           my $add=&executeSQL($dbh, "update BTAB set rank_subj_vs_qry=? where btab_id=?", $current_rank, $btab_id);
        }
    }
    $add=&executeSQL($dbh, "update btab set rank_subj_vs_qry = (select tmp_rank_subj_vs_qry.rank_subj_vs_qry from tmp_rank_subj_vs_qry where tmp_rank_subj_vs_qry.btab_id = btab.btab_id) where exists (select tmp_rank_subj_vs_qry.rank_subj_vs_qry from tmp_rank_subj_vs_qry where tmp_rank_subj_vs_qry.btab_id = btab.btab_id)");
    if (!defined $add) {
        $errorMessage="compute_BTAB_rank_subj_vs_qry: " . $errorMessage;
        die $errorMessage;
    }
    $add=&executeSQL($dbh, "drop table tmp_rank_subj_vs_qry");
    if (!defined $add) {
        $errorMessage="compute_BTAB_rank_subj_vs_qry: " . $errorMessage;
        die $errorMessage;
    }
    print "finished compute_BTAB_rank_subj_vs_qry\n";
    $dbh->commit();
    $dbh->disconnect;
}

sub compute_BTAB_rank_hsp_vs_hit {
    print "starting compute_BTAB_rank_hsp_vs_hit\n";
    my $dbh=connectSQLite($databaseFullpath);
    ###
    ### rank_hsp_vs_hit
    ###
    my $add=&executeSQL($dbh, "create table tmp_rank_hsp_vs_hit ( btab_id integer, rank_hsp_vs_hit integer not null, primary key(btab_id) )");
    if (!defined $add) {
        $errorMessage="compute_BTAB_rank_hsp_vs_hit: " . $errorMessage;
        die $errorMessage;
    }
    # First, obtain the list of query ids from the table
    my $result=&querySQLArrayArray($dbh, "select distinct query_seq_id from BTAB");
    foreach my $resultRow (@$result) {
        my $query_seq_id=$$resultRow[0];
        # For each query, get the list of hits
        my $result2=&querySQLArrayArray($dbh, "select subject_seq_id, btab_id, query_seq_id, evalue, bit_score from BTAB where query_seq_id=? order by subject_seq_id asc, evalue asc, bit_score desc", $query_seq_id);
        my %subject_rank_hash;
        foreach my $resultRow2 (@$result2) {
            my $subject_seq_id=$$resultRow2[0];
            my $btab_id=$$resultRow2[1];
            my $hitRef=$subject_rank_hash{$subject_seq_id};
            if (! (defined $hitRef)) {
                my @hitArr=();
                $hitRef=\@hitArr;
                $subject_rank_hash{$subject_seq_id}=$hitRef;
            }
            push @$hitRef, $btab_id;
        }
        # We have all the data for the current query, so now add to tmp table
        foreach my $subject_key (keys %subject_rank_hash) {
            my $hitRef=$subject_rank_hash{$subject_key};
            my $rank=1;
            foreach my $btab_id (@$hitRef) {
                $add=&executeSQL($dbh, "insert into tmp_rank_hsp_vs_hit(btab_id, rank_hsp_vs_hit) values (?,?)",
                                 $btab_id, $rank);
                if (!defined $add) {
                    $errorMessage="compute_BTAB_rank_hsp_vs_hit: " . $errorMessage;
                    die $errorMessage;
                }
                $rank++;
            }
        }
    }
    # The tmp table is now fully populated, so it is time to update the btab table
    $add=&executeSQL($dbh, "update btab set rank_hsp_vs_hit = (select tmp_rank_hsp_vs_hit.rank_hsp_vs_hit from tmp_rank_hsp_vs_hit where tmp_rank_hsp_vs_hit.btab_id = btab.btab_id) where exists (select tmp_rank_hsp_vs_hit.rank_hsp_vs_hit from tmp_rank_hsp_vs_hit where tmp_rank_hsp_vs_hit.btab_id = btab.btab_id)");
    if (!defined $add) {
        $errorMessage="compute_BTAB_rank_hsp_vs_hit: " . $errorMessage;
        die $errorMessage;
    }
    $add=&executeSQL($dbh, "drop table tmp_rank_hsp_vs_hit");
    if (!defined $add) {
        $errorMessage="compute_BTAB_rank_hsp_vs_hit: " . $errorMessage;
        die $errorMessage;
    }
    print "finished compute_BTAB_rank_hsp_vs_hit\n";
    $dbh->commit();
    $dbh->disconnect;
}

###
### HTAB
###

sub compute_HTAB_rank_qry_vs_hmm {
    print "starting compute_HTAB_rank_qry_vs_hmm\n";
    my $dbh=connectSQLite($databaseFullpath);

    ###
    ### rank_qry_vs_hmm
    ###
    # First, obtain the list of subject ids in the table
    my $add=&executeSQL($dbh, "create table tmp_rank_qry_vs_hmm ( htab_id integer, rank_qry_vs_hmm integer not null, primary key(htab_id) )");
    if (!defined $add) {
        $errorMessage="compute_HTAB_rank_qry_vs_hmm: " . $errorMessage;
        die $errorMessage;
    }
    my $result=&querySQLArrayArray($dbh, "select distinct subject_hmm_id from HTAB");
    foreach my $resultRow (@$result) {
        my $subject_hmm_id=$$resultRow[0];
        # For each subject, get the list of hits
        my $result2=&querySQLArrayArray($dbh, "select query_seq_id, htab_id, subject_hmm_id, total_evalue, total_score, domain_evalue, domain_score from HTAB where subject_hmm_id=? order by query_seq_id asc, total_evalue asc, total_score desc, domain_evalue asc, domain_score desc", $subject_hmm_id);
        my %query_rank_hash;
        my $rank=1;
        foreach my $resultRow2 (@$result2) {
            my $query_seq_id=$$resultRow2[0];
            my $htab_id=$$resultRow2[1];
            my $current_rank=$query_rank_hash{$query_seq_id};
            if (! (defined $current_rank)) {
                $query_rank_hash{$query_seq_id}=$rank;
                $current_rank=$rank;
                $rank++;
            }
            $add=&executeSQL($dbh, "insert into tmp_rank_qry_vs_hmm(htab_id, rank_qry_vs_hmm) values (?,?)",
                             $htab_id, $current_rank);
            if (!defined $add) {
                $errorMessage="compute_HTAB_rank_qry_vs_hmm: " . $errorMessage;
                die $errorMessage;
            }
        }
    }
    $add=&executeSQL($dbh, "update htab set rank_qry_vs_hmm = (select tmp_rank_qry_vs_hmm.rank_qry_vs_hmm from tmp_rank_qry_vs_hmm where tmp_rank_qry_vs_hmm.htab_id = htab.htab_id) where exists (select tmp_rank_qry_vs_hmm.rank_qry_vs_hmm from tmp_rank_qry_vs_hmm where tmp_rank_qry_vs_hmm.htab_id = htab.htab_id)");
    if (!defined $add) {
        $errorMessage="compute_HTAB_rank_qry_vs_hmm: " . $errorMessage;
        die $errorMessage;
    }
    $add=&executeSQL($dbh, "drop table tmp_rank_qry_vs_hmm");
    if (!defined $add) {
        $errorMessage="compute_HTAB_rank_qry_vs_hmm: " . $errorMessage;
        die $errorMessage;
    }
    print "finished compute_HTAB_rank_qry_vs_hmm\n";
    $dbh->commit();
    $dbh->disconnect;
}

sub compute_HTAB_rank_hmm_vs_qry {
    print "starting compute_HTAB_rank_hmm_vs_qry\n";
    my $dbh=connectSQLite($databaseFullpath);
    ###
    ### rank_hmm_vs_qry
    ###
    # First, obtain the list of query ids in the table
    my $add=&executeSQL($dbh, "create table tmp_rank_hmm_vs_qry ( htab_id integer, rank_hmm_vs_qry integer not null, primary key(htab_id) )");
    if (!defined $add) {
        $errorMessage="compute_HTAB_rank_hmm_vs_qry: " . $errorMessage;
        die $errorMessage;
    }
    my $result=&querySQLArrayArray($dbh, "select distinct query_seq_id from HTAB");
    foreach my $resultRow (@$result) {
        my $query_seq_id=$$resultRow[0];
        # For each query, get the list of hits
        my $result2=&querySQLArrayArray($dbh, "select subject_hmm_id, htab_id, query_seq_id, total_evalue, total_score, domain_evalue, domain_score from HTAB where query_seq_id=? order by subject_hmm_id asc, total_evalue asc, total_score desc, domain_evalue asc, domain_score desc", $query_seq_id);
        my %subject_rank_hash;
        my $rank=1;
        foreach my $resultRow2 (@$result2) {
            my $subject_hmm_id=$$resultRow2[0];
            my $htab_id=$$resultRow2[1];
            my $current_rank=$subject_rank_hash{$subject_hmm_id};
            if (! (defined $current_rank)) {
                $subject_rank_hash{$subject_hmm_id}=$rank;
                $current_rank=$rank;
                $rank++;
            }
            $add=&executeSQL($dbh, "insert into tmp_rank_hmm_vs_qry(htab_id, rank_hmm_vs_qry) values (?,?)",
                             $htab_id, $current_rank);
            if (!defined $add) {
                $errorMessage="compute_HTAB_rank_hmm_vs_qry: " . $errorMessage;
                die $errorMessage;
            }
        }
    }
    $add=&executeSQL($dbh, "update htab set rank_hmm_vs_qry = (select tmp_rank_hmm_vs_qry.rank_hmm_vs_qry from tmp_rank_hmm_vs_qry where tmp_rank_hmm_vs_qry.htab_id = htab.htab_id) where exists (select tmp_rank_hmm_vs_qry.rank_hmm_vs_qry from tmp_rank_hmm_vs_qry where tmp_rank_hmm_vs_qry.htab_id = htab.htab_id)");
    if (!defined $add) {
        $errorMessage="compute_HTAB_rank_hmm_vs_qry: " . $errorMessage;
        die $errorMessage;
    }
    $add=&executeSQL($dbh, "drop table tmp_rank_hmm_vs_qry");
    if (!defined $add) {
        $errorMessage="compute_HTAB_rank_hmm_vs_qry: " . $errorMessage;
        die $errorMessage;
    }
    print "finished compute_HTAB_rank_hmm_vs_qry\n";
    $dbh->commit();
    $dbh->disconnect;
}


sub compute_HTAB_rank_hsp_vs_hit {
    print "starting compute_HTAB_rank_hsp_vs_hit\n";
    my $dbh=connectSQLite($databaseFullpath);
    ###
    ### rank_hsp_vs_hit
    ###
    my $add=&executeSQL($dbh, "create table tmp_rank_hsp_vs_hit ( htab_id integer, rank_hsp_vs_hit integer not null, primary key(htab_id) )");
    if (!defined $add) {
        $errorMessage="compute_HTAB_rank_hsp_vs_hit: " . $errorMessage;
        die $errorMessage;
    }
    # First, obtain the list of query ids from the table
    my $result=&querySQLArrayArray($dbh, "select distinct query_seq_id from HTAB");
    foreach my $resultRow (@$result) {
        my $query_seq_id=$$resultRow[0];
        # For each query, get the list of hits
        my $result2=&querySQLArrayArray($dbh, "select subject_hmm_id, htab_id, query_seq_id, total_evalue, total_score, domain_evalue, domain_score from HTAB where query_seq_id=? order by subject_hmm_id asc, total_evalue asc, total_score desc, domain_evalue asc, domain_evalue desc", $query_seq_id);
        my %subject_rank_hash;
        foreach my $resultRow2 (@$result2) {
            my $subject_hmm_id=$$resultRow2[0];
            my $htab_id=$$resultRow2[1];
            my $hitRef=$subject_rank_hash{$subject_hmm_id};
            if (! (defined $hitRef)) {
                my @hitArr=();
                $hitRef=\@hitArr;
                $subject_rank_hash{$subject_hmm_id}=$hitRef;
            }
            push @$hitRef, $htab_id;
        }
        # We have all the data for the current query, so now add to tmp table
        foreach my $subject_key (keys %subject_rank_hash) {
            my $hitRef=$subject_rank_hash{$subject_key};
            my $rank=1;
            foreach my $htab_id (@$hitRef) {
                $add=&executeSQL($dbh, "insert into tmp_rank_hsp_vs_hit(htab_id, rank_hsp_vs_hit) values (?,?)",
                                 $htab_id, $rank);
                if (!defined $add) {
                    $errorMessage="compute_HTAB_rank_hsp_vs_hit: " . $errorMessage;
                    die $errorMessage;
                }
                $rank++;
            }
        }
    }
    # The tmp table is now fully populated, so it is time to update the htab table
    $add=&executeSQL($dbh, "update htab set rank_hsp_vs_hit = (select tmp_rank_hsp_vs_hit.rank_hsp_vs_hit from tmp_rank_hsp_vs_hit where tmp_rank_hsp_vs_hit.htab_id = htab.htab_id) where exists (select tmp_rank_hsp_vs_hit.rank_hsp_vs_hit from tmp_rank_hsp_vs_hit where tmp_rank_hsp_vs_hit.htab_id = htab.htab_id)");
    if (!defined $add) {
        $errorMessage="compute_HTAB_rank_hsp_vs_hit: " . $errorMessage;
        die $errorMessage;
    }
    $add=&executeSQL($dbh, "drop table tmp_rank_hsp_vs_hit");
    if (!defined $add) {
        $errorMessage="compute_HTAB_rank_hsp_vs_hit: " . $errorMessage;
        die $errorMessage;
    }
    print "finished compute_HTAB_rank_hsp_vs_hit\n";
    $dbh->commit();
    $dbh->disconnect;
}


1;
