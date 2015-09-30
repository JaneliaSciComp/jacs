#!/usr/bin/env perl

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
use Getopt::Long qw(:config no_ignore_case no_auto_abbrev pass_through);
use Pod::Usage;
use File::Basename;
use DB_File;

#######
## ubiquitous options parsing and logger creation
my %options = ();
my $results = GetOptions( \%options, 'output|o=s', 'fasta_file|f=s', 'help|h' )
	|| pod2usage();

## display documentation
if ( $options{'help'} ) {
	pod2usage( { -exitval => 0, -verbose => 2, -output => \*STDERR } );
}

&check_parameters( \%options );

#  filenames, a directory of files, or a list of directories of files (lists are
#  comma-separated.  it can even be a mixed list of file and dir names.  fancy!
#  a file containing a list of file/dir names should be passed with --bsml_list.
my @files;
my @list_elements = ();

my $file = $options{'fasta_file'};

print STDERR "1:$file\n";

#######
## parse out sequences from each file

my %lookup;

$DB_BTREE->{'cachesize'} = 100000000;
unlink $options{'output'};
my $dbtie = tie %lookup, 'DB_File', $options{'output'}, O_RDWR | O_CREAT, 0660, $DB_BTREE or
		die "Can't tie $options{'output'}";

# open input file
open (IN, $options{fasta_file});
my @file = <IN>;
close IN;

my ($filename, $directory, $suffix) = &fileparse($options{fasta_file});
open(LOOKUP,">" . $directory . "/" . $filename . "_id_lookup.txt");

foreach my $sequence (@file){
	if($sequence =~ /^>(\S*)\s/){
		my $id = $1;
		
		if($id =~ /\|/){
			print LOOKUP "$id\t";
			
			$id =~ s/\|/_/g;
			
			print LOOKUP "$id\n";		
		}
			
		$lookup{$id} = $filename;
	}
}

close LOOKUP;

sub check_parameters {
	my ($options) = @_;

	## they have to pass some form of input
	unless ($options{fasta_file})
	{
		die "You must specify input with --fasta_file";
	}

	## output is required
	unless ( $options{output} ) {
		die "You must specify an output directory or file with --output";
	}

	
	if (0) {
		pod2usage(
			   { -exitval => 2, -message => "error message", -verbose => 1, -output => \*STDERR } );
	}
}
