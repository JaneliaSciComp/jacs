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

use FindBin qw($Bin);
use lib ("$Bin/lib");

use SOAP::Lite;
use Getopt::Long;
use Cwd 'realpath';
use File::Basename;
use Fasta::SimpleIndexer;
use File::Basename;
use File::Path qw(make_path);
use Data::Dumper;
use DBI;
use Pod::Usage;

=head1 NAME

comparative_computes.pl 

=head1 DESCRIPTION

This program runs the comparative compute pipeline that Sybil is based on. 
It makes VICs service calls to run the necessary computes. The order of the
calls and searches are:

Jaccard - Generates paralog clusters
Clustalw2 - Runs on Jaccard clusters
Jocs - Generates ortholog clusters based on Clustalw2 and Jaccard results
Clustalw2 - Runs on JOCs clusters

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
	
 [optional parameter]
 
 *comparative_computes.pl
 --output_dir	Directory of where to place output, default is /usr/local/scratch/comparative_computes/ 
 [--no_run_joc_clustalw] Flag to determine if final Clustalw2 is run.       
 [--help] 

 *runJaccard
 --input_file_list List of fasta files
 --blast_output	NCBI blast results, no headers
 [--linkscore]		Jaccard coefficient cutoff. Default .6
 [--percent_identity]	Default 80
 [--percent_coverage]	Default 0  
 [--jaccard_p_value]	Cutoff p-value. Default 1e-5
 
 *runJocs
 [--jocs_p_value]			P value cut off for run. Default 1e-5
 [--coverageCutoff]		Default 0

 *runClustalw2
 Runs with all defaults 

=head1  OUTPUT

 	When succesfully submitted, this script returns the following:
 	Comparative Computes completed.
 	Results: [output]/comparative_computes_[date].txt

=head1  CONTACT

    Erin Beck
    ebeck@jcvi.org
    Prokaryotic Affinity Group
	
=cut

my $program = realpath($0);
my $myLib   = dirname($program);

my $port      = 81;
my $server    = 'saffordt-ws1';
my $wsdl_name = 'ComputeWS';
my $wsdl_path = 'compute-compute';
my $wsdl_url  = '';

my $username      = getlogin;
my $project       = '08100';
my $workSessionId = '';
my $jobName       = 'ComparativeComputes';

#comparative_computes.pl
my ( $db_list, $db_user, $db_pass );
my $output_dir       = '';
my $run_joc_clustalw = '';
my $help             = '';

#jaccard
my $input_file_list        = '';
my $blast_output           = '';
my $linkscore              = '.6';
my $percent_identity       = '80';
my $percent_coverage       = '0';
my $jaccard_p_value        = '1e-5';
my $jaccard_maxCogSeqCount = '30';

#jocs
my $bsml_model_list        = '';
my $bsml_jaccard_list      = '';
my $jocs_p_value           = '1e-5';
my $coverage_cutoff        = '0';
my $jaccard_coefficient    = '.6';
my $j_cutoff               = '';
my $jocs_max_cog_seq_count = '100';

#clustalw2
my $fastaInputNodeId   = '';
my $fastaInputFileList = '';
my $quiet              = '';
my $align              = '';
my $newtree            = '';
my $usetree            = '';
my $newtree1           = '';
my $usetree1           = '';
my $newtree2           = '';
my $usetree2           = '';
my $bootstrap          = '';
my $tree               = '';
my $quicktree          = '';
my $convert            = '';
my $batch              = '';
my $iteration          = '';
my $type               = '';
my $profile            = '';
my $sequences          = '';
my $matrix             = '';
my $dnamatrix          = '';
my $negative           = '';
my $noweights          = '';
my $gapopen            = '';
my $gapext             = '';
my $endgaps            = '';
my $nopgap             = '';
my $nohgap             = '';
my $novgap             = '';
my $hgapresidues       = '';
my $maxdiv             = '';
my $gapdist            = '';
my $pwmatrix           = '';
my $pwdnamatrix        = '';
my $pwgapopen          = '';
my $pwgapext           = '';
my $ktuple             = '';
my $window             = '';
my $pairgap            = '';
my $topdiags           = '';
my $score              = '';
my $transweight        = '';
my $seed               = '';
my $kimura             = '';
my $tossgaps           = '';
my $bootlabelsode      = '';
my $output             = 'gcg';
my $outputtree         = '';
my $outorder           = '';
my $cluster_case       = '';
my $seqnos             = '';
my $seqno_range        = '';
my $range              = '';
my $nosecstr1          = '';
my $nosecstr2          = '';
my $secstrout          = '';
my $helixgap           = '';
my $strandgap          = '';
my $loopgap            = '';
my $terminalgap        = '';
my $helixendin         = '';
my $helixendout        = '';
my $strandendin        = '';
my $strandendout       = '';
my $numiter            = '';
my $clustering         = '';
my $maxseqlen          = '';
my $stats              = '';

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

	#comparative_computes.pl
	"output_dir=s"     => \$output_dir,
	"run_joc_clustalw" => \$run_joc_clustalw,
	"help"             => \$help,

	#runJaccard
	"input_file_list=s"        => \$input_file_list,
	"blast_output=s"           => \$blast_output,
	"linkscore=s"              => \$linkscore,
	"percent_identity=s"       => \$percent_identity,
	"percent_coverage=s"       => \$percent_coverage,
	"jaccard_p_value=s"        => \$jaccard_p_value,
	"jaccard_maxCogSeqCount=s" => \$jaccard_maxCogSeqCount,

	#runJocs
	"bsml_model_list=s"        => \$bsml_model_list,
	"bsml_jaccard_list=s"      => \$bsml_jaccard_list,
	"jocs_p_value=s"           => \$jocs_p_value,
	"coverage_cutoff=s"        => \$coverage_cutoff,
	"jaccard_coefficient=s"    => \$jaccard_coefficient,
	"j_cutoff=s"               => \$j_cutoff,
	"jocs_max_cog_seq_count=s" => \$jocs_max_cog_seq_count,

	#runClustalw2
	"fastaInputNodeId=s"   => \$fastaInputNodeId,
	"fastaInputFileList=s" => \$fastaInputFileList,
	"quiet=s"              => \$quiet,
	"align=s"              => \$align,
	"newtree=s"            => \$newtree,
	"usetree=s"            => \$usetree,
	"newtree1=s"           => \$newtree1,
	"usetree1=s"           => \$usetree1,
	"newtree2=s"           => \$newtree2,
	"usetree2=s"           => \$usetree2,
	"bootstrap=s"          => \$bootstrap,
	"tree=s"               => \$tree,
	"quicktree=s"          => \$quicktree,
	"convert=s"            => \$convert,
	"batch=s"              => \$batch,
	"iteration=s"          => \$iteration,
	"type=s"               => \$type,
	"profile=s"            => \$profile,
	"sequences=s"          => \$sequences,
	"matrix=s"             => \$matrix,
	"dnamatrix=s"          => \$dnamatrix,
	"negative=s"           => \$negative,
	"noweights=s"          => \$noweights,
	"gapopen=s"            => \$gapopen,
	"gapext=s"             => \$gapext,
	"endgaps=s"            => \$endgaps,
	"nopgap=s"             => \$nopgap,
	"nohgap=s"             => \$nohgap,
	"novgap=s"             => \$novgap,
	"hgapresidues=s"       => \$hgapresidues,
	"maxdiv=s"             => \$maxdiv,
	"gapdist=s"            => \$gapdist,
	"pwmatrix=s"           => \$pwmatrix,
	"pwdnamatrix=s"        => \$pwdnamatrix,
	"pwgapopen=s"          => \$pwgapopen,
	"pwgapext=s"           => \$pwgapext,
	"ktuple=s"             => \$ktuple,
	"window=s"             => \$window,
	"pairgap=s"            => \$pairgap,
	"topdiags=s"           => \$topdiags,
	"score=s"              => \$score,
	"transweight=s"        => \$transweight,
	"seed=s"               => \$seed,
	"kimura=s"             => \$kimura,
	"tossgaps=s"           => \$tossgaps,
	"bootlabelsode=s"      => \$bootlabelsode,
	"output=s"             => \$output,
	"outputtree=s"         => \$outputtree,
	"outorder=s"           => \$outorder,
	"cluster_case=s"       => \$cluster_case,
	"seqnos=s"             => \$seqnos,
	"seqno_range=s"        => \$seqno_range,
	"range=s"              => \$range,
	"nosecstr1=s"          => \$nosecstr1,
	"nosecstr2=s"          => \$nosecstr2,
	"secstrout=s"          => \$secstrout,
	"helixgap=s"           => \$helixgap,
	"strandgap=s"          => \$strandgap,
	"loopgap=s"            => \$loopgap,
	"terminalgap=s"        => \$terminalgap,
	"helixendin=s"         => \$helixendin,
	"helixendout=s"        => \$helixendout,
	"strandendin=s"        => \$strandendin,
	"strandendout=s"       => \$strandendout,
	"numiter=s"            => \$numiter,
	"clustering=s"         => \$clustering,
	"maxseqlen=s"          => \$maxseqlen,
	"stats=s"              => \$stats
);

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

&checkOptions();

my $bsml_search_list = &convert_blast_to_bsml( $input_file_list, $blast_output );

my $jaccardList = &run_jaccard( $input_file_list, $bsml_search_list );
my $clustalwJaccardBsml = &run_clustalw($jaccardList);
my ( $jocsResultsList, $jocsResultsCog ) = &run_jocs( $input_file_list, $clustalwJaccardBsml );
my $clustalwResultsJocs = &run_clustalw($jocsResultsList) if ($run_joc_clustalw);

#make sub to parse fasta files to get hash of the loci to file
my $loci_genome_lookup = &parse_fasta_files($input_file_list);
my $final_results
	= &print_clusters( $jocsResultsCog, $input_file_list, $output_dir, $loci_genome_lookup );

print "\nComparative Computes completed.\n";
print "Results: " . $final_results . "\n";
exit(0);

################## SUBS ##########################
sub convert_blast_to_bsml {
	my ( $input_file_list, $blast_output ) = @_;

	#First cat fasta files into combined file
	my $ffh;
	open( $ffh, "<$input_file_list" ) || die("Can't open $input_file_list\n");

	my $cmd = "cat ";

	while (<$ffh>) {
		chomp($_);
		my $file = $_;
		$cmd .= $file . " ";
	}

	close $ffh;
	$cmd .= "> $output_dir/combined_fasta_file.txt";
	system($cmd) == 0 or die("Could not run $cmd");

	#Second run blast to bsml converter
	print "Creating bsml file\n";
	my $bsml_cmd
		= "./createBsmlFromBlastForJaccard -o $output_dir -f $blast_output -q $output_dir/combined_fasta_file.txt";
	system($bsml_cmd) == 0 or die("Could not run $bsml_cmd");

	return ("$output_dir/bsml.list");
}

sub checkOptions {
	if ($help) {
		&_pod;
		exit(0);
	}

	print "\n--input_file_list required" unless ($input_file_list);
	print "\n--blast_output required"    unless ($blast_output);

	if (    !$input_file_list
		 || !$blast_output )
	{

		print "\n\n";
		&_pod;
		exit(0);
	}

	if ( !$output_dir ) {

		#TODO: Switch to plan scratch area
		$output_dir = "/usr/local/scratch/comparative_computes/$$";
		&make_path($output_dir) || die "Could not make output directory $output_dir:$!";
	}
}

sub _pod {
	pod2usage( { -exitval => 0, -verbose => 2, -output => \*STDERR } );
}

sub parse_fasta_files {
	my $input_file_list = shift;

	my $loci_genome_lookup = {};

	open( IN, $input_file_list );
	my @files = <IN>;
	close IN;

	foreach my $file (@files) {
		my @deflines = split( "\n", `grep "^>" $file` );

		foreach my $line (@deflines) {
			if ( $line =~ /^>(\S*)\s/ ) {
				my $id = $1;

				my ( $filename, $directory, $suffix ) = &fileparse($file);
				$filename = &trim($filename);

				$loci_genome_lookup->{$id} = $filename;
			}
		}
	}

	return $loci_genome_lookup;
}

sub parse_fasta_lookup {
	my ( $file, $id_lookup_hsh ) = @_;

	open( IN, $file );
	my @loci = <IN>;
	close IN;

	foreach my $locus (@loci) {
		my ( $original, $new ) = split( /\t/, $locus );
		$new =~ s/\s+$//;
		$id_lookup_hsh->{$new} = $original;
	}

	return $id_lookup_hsh;
}

sub print_clusters {
	my ( $cog_file, $input_file_list, $output_dir, $loci_genome_lookup ) = @_;
	my $cog_hsh = {};
	my @db_array;
	my $original_loci_lookup = {};

	open( IN, $input_file_list );
	my @files = <IN>;
	close IN;

	foreach my $file (@files) {
		my ( $filename, $directory, $suffix ) = &fileparse($file);
		$filename = &trim($filename);
		push( @db_array, $filename );

		my $lookup_file = $directory . "/" . $filename . "_id_lookup.txt";
		if ( -s $lookup_file ) {
			$original_loci_lookup = &parse_fasta_lookup( $lookup_file, $original_loci_lookup );
		}
	}

	open( COG, $cog_file );
	my @cog_clusters = <COG>;
	close COG;

	my ( $sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst ) = localtime();

	$mon  += 1;
	$year += 1900;

	my $date = $mon . $mday . $year . $hour . $min . $sec;

	$output_dir .= "/" unless ( $output_dir =~ /\/$/ );

	my $file = $output_dir . "comparative_computes_" . $date . ".txt";

	open( OUT, ">" . $file );
	my $cluster_id;
	my $cluster_size;

	print OUT "CLUSTER\tNUM IN CLUSTER";

	foreach my $db (@db_array) {
		print OUT "\t" . uc($db);
	}

	print OUT "\n";

	my $column_max = scalar @db_array;
	my $cluster_loci_hsh;
	my $db;
	my @loci;

	my %cluster_print_hsh = ();

	foreach my $cluster (@cog_clusters) {
		if ( $cluster =~ /COG = (\d+), size (\d+)/ ) {    #marks beginning of cluster

			if (%cluster_print_hsh) {
				&print_cluster( \%cluster_print_hsh, $column_max, $cluster_id, \@db_array );

				%cluster_print_hsh = ();
			}

			$cluster_id = $1;
		}
		else {
			$cluster = &trim($cluster);

			#If lookup file exists grab original loci found in fasta
			#Loci in cog output do not have the | as it causes clustal to fail
			if ( exists $original_loci_lookup->{$cluster} ) {
				$cluster = $original_loci_lookup->{$cluster};
			}

			my $file_location = $loci_genome_lookup->{$cluster};
			push( @{ $cluster_print_hsh{$file_location} }, $cluster );
		}
	}

	&print_cluster( \%cluster_print_hsh, $column_max, $cluster_id, \@db_array );

	close OUT;

	return $file;
}

sub print_cluster {
	my ( $cluster_print_hsh_ref, $column_max, $cluster_id, $db_array ) = @_;

	my %cluster_print_hsh = %$cluster_print_hsh_ref;

	my ( $max_rows, $cluster_size ) = &find_max_rows( \%cluster_print_hsh );

	for ( my $i = 0; $i < $max_rows; $i++ ) {

		for ( my $j = 0; $j < $column_max; $j++ ) {
			if ( $i == 0 ) {
				if ( $j == 0 ) {
					print OUT "$cluster_id\t$cluster_size\t";
				}
			}
			else {
				if ( $j == 0 ) {
					print OUT "\t\t";
				}
			}

			if ( exists $cluster_print_hsh{ $db_array->[$j] } ) {
				if ( $i <= $#{ $cluster_print_hsh{ $db_array->[$j] } } ) {
					my $locus = $cluster_print_hsh{ $db_array->[$j] }->[$i];
					print OUT "$locus";
				}
			}

			print OUT "\t";
		}

		print OUT "\n";
	}

}

sub find_max_rows {
	my $clusters_hsh = shift;
	my $max_rows     = 0;
	my $cluster_size = 0;

	foreach my $db ( keys %$clusters_hsh ) {
		my $rows = scalar @{ $clusters_hsh->{$db} };

		$cluster_size += $rows;
		$max_rows = ( $max_rows > $rows ) ? $max_rows : $rows;
	}

	return ( $max_rows, $cluster_size );
}

sub trim {
	my $string = shift;

	$string =~ s/^\s+//;
	$string =~ s/\s+$//;

	return $string;
}

sub run_jaccard {
	my ( $input_file_list, $bsml_search_list ) = @_;
	my $results;

	my $prog = 'runJaccard';
	my @options = (
		$username,         # username
		'',                # token
		$project,          # project
		$workSessionId,    # workSessionId
		$jobName,          # jobName

		$input_file_list,          #input_file_list
		$bsml_search_list,         #bsml_search_list
		$linkscore,                #linkscore
		$percent_identity,         #percent_identity
		$percent_coverage,         #percent_coverage
		$jaccard_p_value,          #p_value
		$jaccard_maxCogSeqCount    #maxCogSeqCount
	);

	print "\nRunning: $prog\n@options\n";
	my $status;

	my $message = SOAP::Lite->service($wsdl_url)->runJaccard(@options);
	my $taskId = $1 if ( $message =~ /Job Id: (\d+)/ );

	if ($taskId) {
		$status = &get_job_status($taskId);
		$results = $1 if ( $status =~ /Result\(s\) location:\s(.*)/s );
	}
	else {
		print "No task id found: $message\n";
	}

	my $clustalw_jaccard_input_file_list = $results . "/jaccard.list";

	if ( -s $clustalw_jaccard_input_file_list ) {
		print "\nFinished $prog\n";
		return $clustalw_jaccard_input_file_list;
	}
	else {
		print "$clustalw_jaccard_input_file_list does not exists or is size zero";
		exit;
	}
}

sub run_jocs {
	my ( $input_file_list, $jaccardBsml ) = @_;
	my $results;

	my $prog = 'runJocs';
	my @options = (
		$username,         # username
		'',                # token
		$project,          # project
		$workSessionId,    # workSessionId
		$jobName,          # jobName

		$bsml_search_list,         #bsmlSearchList
		$input_file_list,          #bsmlModelList
		$jaccardBsml,              #bsmlJaccardlLis
		$jocs_p_value,             #pvalcut
		$coverage_cutoff,          #coverageCutoff
		$jaccard_coefficient,      #j
		$j_cutoff,                 #c
		$jocs_max_cog_seq_count    #maxCogSeqCount
	);

	print "\nRunning: $prog\n@options\n";
	my $status;

	my $message = SOAP::Lite->service($wsdl_url)->runJocs(@options);
	my $taskId = $1 if ( $message =~ /Job Id: (\d+)/ );

	if ($taskId) {
		$status = &get_job_status($taskId);
		$results = $1 if ( $status =~ /Result\(s\) location:\s(.*)/s );
	}
	else {
		print "No task id found: $message\n";
	}

	my $clustalw_jocs_input_file_list = $results . "/jocs.list";
	my $jocs_cog_list                 = $results . "/jocs.cog";

	if ( -s $clustalw_jocs_input_file_list && -s $jocs_cog_list ) {
		print "\nFinished $prog\n";
		return ( $clustalw_jocs_input_file_list, $jocs_cog_list );
	}
	else {
		die "$clustalw_jocs_input_file_list  or $jocs_cog_list does not exists or is size zero";
	}
}

sub run_clustalw {
	my $input_file_list = shift;
	my $results;

	my $prog = 'runClustalw2';
	my @options = (
		$username,            # username
		'',                   # token
		$project,             # project
		$workSessionId,       # workSessionId
		$jobName,             # jobName
		$fastaInputNodeId,    # fastaInputNodeId
		$input_file_list,     # fastaInputFileList

		'',                   # align
		'',                   # tree
		'',                   # bootstrap
		'',                   # convert
		'',                   # quicktree
		'',                   # type
		'',                   # negative
		$output,              # output
		'',                   # outorder
		'',                   # case
		'',                   # seqnos
		'',                   # seqno_range
		'',                   # range
		'',                   # maxseqlen
		'',                   # quiet
		'',                   # stats
		'',                   # ktuple
		'',                   # topdiags
		'',                   # window
		'',                   # pairgap
		'',                   # score
		'',                   # pwmatrix
		'',                   # pwdnamatrix
		'',                   # pwgapopen
		'',                   # pwgapext
		'',                   # newtree
		'',                   # usetree
		'',                   # matrix
		'',                   # dnamatrix
		'',                   # gapopen
		'',                   # gapext
		'',                   # endgaps
		'',                   # gapdist
		'',                   # nopgap
		'',                   # nohgap
		'',                   # hgapresidues
		'',                   # maxdiv
		'',                   # transweight
		'',                   # iteration
		'',                   # numiter
		'',                   # noweights
		'',                   # profile
		'',                   # newtree1
		'',                   # usetree1
		'',                   # newtree2
		'',                   # usetree2
		'',                   # sequences
		'',                   # nosecstr1
		'',                   # nosecstr2
		'',                   # secstrout
		'',                   # helixgap
		'',                   # strandgap
		'',                   # loopgap
		'',                   # terminalgap
		'',                   # helixendin
		'',                   # helixendout
		'',                   # strandendin
		'',                   # strandendout
		'',                   # outputtree
		'',                   # seed
		'',                   # kimura
		'',                   # tossgaps
		'',                   # bootlabelsode
		'',                   # clustering
		''                    # batch
	);

	print "\nRunning: $prog\n@options\n";
	my $status;

	my $message = SOAP::Lite->service($wsdl_url)->runClustalw2(@options);
	my $taskId = $1 if ( $message =~ /Job Id: (\d+)/ );

	if ($taskId) {
		$status = &get_job_status($taskId);
		$results = $1 if ( $status =~ /Result\(s\) location:\s(.*)/s );
	}
	else {
		print "No task id found: $message\n";
	}

	my $clustalw_output = $results . "/clustalw2.list";

	if ( -s $clustalw_output ) {
		print "\nFinished $prog\n";
		return $clustalw_output;
	}
	else {
		die "$clustalw_output does not exists or is size zero";
	}
}

sub get_job_status {
	my $taskId  = shift;
	my $command = "getTaskStatus";

	my $sleep = 1;

	my $status     = '';
	my $statusText = '';
	while ( $status !~ /error|completed/ ) {

		sleep($sleep);

		$statusText = SOAP::Lite->service($wsdl_url)->$command( $username, '', $taskId );
		if ( $statusText =~ /Status Type: (\S+)/s ) {
			$status = $1;
			print '.';
		}
		else {
			print "$statusText\n";
			exit;
		}
	}

	return $statusText;
}

sub do_sql {
	my ( $dbproc, $query ) = @_;

	my ( $statementHandle, @x, @result );
	my ( $i, $row );

	my $delimeter = "\t";

	$statementHandle = $dbproc->prepare($query);

	if ( !defined $statementHandle ) {
		die "Cannot prepare statement: $DBI::errstr\n";
	}
	elsif ( !defined $statementHandle ) {
		return;
	}
	$statementHandle->execute() || die "failed query: $query\n";

	if ( $statementHandle->{syb_more_results} ne "" ) {
		while ( my @row = $statementHandle->fetchrow() ) {

			#Handles NULL values returned so that join can work properly
			map { $_ = '' unless $_ } @row;

			push( @result, join( $delimeter, @row ) );
		}
	}

	#release the statement handle resources
	$statementHandle->finish;
	return ( \@result );
}

sub create_db_handle {
	my ( $db, $user, $pswd ) = @_;

	my $dbh;

	$ENV{"SYBASE"} ||= "/usr/local/packages/sybase";

	$dbh = DBI->connect( "dbi:Sybase:server=SYBPROD", $user, $pswd );

	if ( !defined $dbh ) {
		die "Cannot connect to Sybase server: $DBI::errstr\n";
	}

	return ($dbh);
}
