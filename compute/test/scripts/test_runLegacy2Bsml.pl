#!/usr/local/perl -w

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

use warnings;
use strict;
$|++;

use SOAP::Lite;
use Getopt::Long;

=head1 NAME

test_runLegacy2Bsml.pl - test the runLegacy2Bsml web-service call.

=head1 SYNOPSIS

    USAGE: test_runLegacy2Bsml.pl OPTION LIST HERE

=head1 OPTIONS

=head2 connection parameters

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

=head2 VICS parameters

 --username                 User to submit as [Default is current]
 --project                  Grid submmission code [08020]
 --workSessionId            Work session id, if desired
 --jobName                  Name that refers to this job
 --fastaInputNodeId         VICS id for the input fasta
 
=head2 program parameters
 [optional parameters]
 
 --root_project                   Project named found under /usr/local/annotation. ie. acinetobacter
 --db_user                        Database username
 --db_pass                        Database password
 --control_file                   Stores information pertaining to the following parameters:
	                              databases
								  organism_type
								  include_genefinders
								  exclude_genefinders
								  alt_databases
								  alt_species
								  asmbl_id
								  sequence_type
								  tu_list_file
								  model_list_file
								  model_list_file
 --[backup]                       If specified, will backup existing output .bsml and .fsa files	  
 --[mode]                         1=Write Gene Model to BSML document  (Default)
		                          2=Write Gene Model and Computational Evidence to BSML document
                                  3=Write Computational Evidence to BSML document
 --[fastadir]                     Fasta directory for the organism
 --[rdbms]                        Relational database management system currently supports Sybase for  
					              prok and nt_prok schemas Mysql for prok
					              Default: Sybase (if nothing specified)
 --[host]                         Server housing the database
		                          Default   - SYBTIGR (if nothing specified)
 --[schema]                       Performs XML schema validation
 --[no-misc-features]             User can specify that no miscellaneous feature types should 
     						      be extracted from the legacy annotation database.  
							      Default is to migrate all miscellaneous feature types.
 --[no-repeat-features]           User can specify that no repeat feature types should 
							      be extracted from the legacy annotation database. 
							      Default is to migrate all repeat feature types.
 --[no-transposon-features]       User can specify that no transposon feature types 
							      should be extracted from the legacy annotation database. 
							      Default is to migrate all transposon feature types.
 --[no_id_generator]              Do not call IdGenerator services
 --[input_id_mapping_files]       Comma-separated list of files containing old-identifier 
                                  to new-identifier mappings.  The default file will be 
                                  {$outdir}/legacy2bsml.pl.{$database}_{$asmbl_id}_assembly_{$schema_type}.bsml.idmap
 --[input_id_mapping_directories] Comma-separated list of directories that may 
								  contain ID mapping files with file extension .idmap. 
								  Default directories will be is /tmp
 --[idgen_identifier_version]     The user can override the default version value 
							      appended to the feature and sequence identifiers (default is 0)
 --[no_die_null_sequences]        If specified, will force legacy2bsml.pl to continue 
						          execution even if sequences are null for certain feat_types.
 --[sourcename]                   User can specify the value to store in the Analysis Attributes 
                                  for tag name.  Default value is the current working directory.
 

=back

=head1  DESCRIPTION

This script tests the runLegacy2Bsml web service.

=head1  INPUT


=head1  OUTPUT

When succesfully submitted, this web service returns the following:
Job Id: 1436500110895745195

=head1  CONTACT

    Erin Beck
    ebeck@jcvi.org

=cut

my $port      = 81;
my $server    = 'saffordt-ws1';
my $wsdl_name = 'ComputeWS';
my $wsdl_path = 'compute-compute';
my $wsdl_url  = '';

my $username      = getlogin;
my $project       = '08100';
my $workSessionId = '';
my $jobName       = 'TestLegacy2Bsml';

my $backup                       = '';
my $db_username                  = '';
my $password                     = '';
my $mode                         = '';
my $fastadir                     = '';
my $rdbms                        = '';
my $host                         = '';
my $schema                       = '';
my $dtd                          = '';
my $no_misc_features             = '';
my $no_repeat_features           = '';
my $no_transposon_features       = '';
my $no_id_generator              = '';
my $input_id_mapping_files       = '';
my $input_id_mapping_directories = '';
my $idgen_identifier_version     = '';
my $no_die_null_sequences        = '';
my $sourcename                   = '';
my $control_file                 = '';
my $root_project                 = '';

my $result = GetOptions(
	"port|p=i"    => \$port,
	"server|s=s"  => \$server,
	"wsdl_name=s" => \$wsdl_name,
	"wsdl_path=s" => \$wsdl_path,
	"wsdl_url=s"  => \$wsdl_url,

	"username=s"      => \$username,
	"project=s"       => \$project,
	"workSessionId=s" => \$workSessionId,
	"jobName=s"       => \$jobName,

	"backup=s"                       => \$backup,
	"db_username=s"                  => \$db_username,
	"password=s"                     => \$password,
	"mode=s"                         => \$mode,
	"fastadir=s"                     => \$fastadir,
	"rdbms=s"                        => \$rdbms,
	"host=s"                         => \$host,
	"schema=s"                       => \$schema,
	"no_misc_features=s"             => \$no_misc_features,
	"no_repeat_features=s"           => \$no_repeat_features,
	"no_transposon_features=s"       => \$no_transposon_features,
	"no_id_generator=s"              => \$no_id_generator,
	"input_id_mapping_files=s"       => \$input_id_mapping_files,
	"input_id_mapping_directories=s" => \$input_id_mapping_directories,
	"idgen_identifier_version=s"     => \$idgen_identifier_version,
	"no_die_null_sequences=s"        => \$no_die_null_sequences,
	"sourcename=s"                   => \$sourcename,
	"control_file=s"                 => \$control_file,
	"root_project=s"                 => \$root_project
);

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runLegacy2Bsml';
my @options = (
	$username,         # username
	'',                # token
	$project,          # project
	$workSessionId,    # workSessionId
	$jobName,          # jobName

	$backup,                          #backup
	$db_username,                     #username
	$password,                        #password
	$mode,                            #mode
	$fastadir,                        #fastadir
	$rdbms,                           #rdbms
	$host,                            #host
	$schema,                          #schema
	$no_misc_features,                #no_misc_features
	$no_repeat_features,              #no_repeat_features
	$no_transposon_features,          #no_transposon_features
	$no_id_generator,                 #no_id_generator
	$input_id_mapping_files,          #input_id_mapping_files
	$input_id_mapping_directories,    #input_id_mapping_directories
	$idgen_identifier_version,        #idgen_identifier_version
	$no_die_null_sequences,           #no_die_null_sequences
	$sourcename,                      #sourcename
	$control_file,                    #control_file
	$root_project                     #root_project
);

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite->service($wsdl_url)->runLegacy2Bsml(@options);

