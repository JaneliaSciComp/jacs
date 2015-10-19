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

my @anno = <STDIN>;
foreach my $line ( @anno) {
	$line =~ s/[\r\n]//g;
	my $hmm_len = undef;
	my ( $hmm_acc, $db, $noise, $trusted, $type, $length, undef, $description, $protein ) = split( /\t/, $line );
	my $definition = "description=\"" . $description . "\" protein=\"" . $protein . "\"";
	print join( "\t", ( $hmm_acc, $length, $noise, $trusted, $definition ) ) . "\n";
}
exit(0);
