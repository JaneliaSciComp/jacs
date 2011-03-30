#!/usr/local/bin/perl

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

$file = @ARGV[0];

open (FILE, $file) || die ("Can't open");

while (<FILE>){
	chomp($_);
	@details = split /\t/, $_;
	$pep = $details[0];
	$cname_cat = $details[1];
	$cname_val = $details[2];
	$cname_evd = $details[3];
	$gsymbol_cat = $details[4];
	$gsymbol_val = $details[5];
	$gsymbol_evd = $details[6];
	$go_cat = $details[7];
	$go_val = $details[8];
	$go_evd = $details[9];
	$ec_cat = $details[10];
	$ec_val = $details[11];
	$ec_evd = $details[12];
	$trole_cat = $details[13];
	$trole_val = $details[14];
	$trole_evd = $details[15];
	
	@go_val_split = split('\|\\|', $go_val);
	@go_evd_split = split('\|\|', $go_evd);
	
	@cname_val_split = split('\|\|', $cname_val);
        @cname_evd_split = split('\|\|', $cname_evd);
	
	@ec_val_split = split('\|\|', $ec_val);
        @ec_evd_split = split('\|\|',$ec_evd);
	
	@gsymbol_val_split = split('\|\|', $gsymbol_val);
        @gsymbol_evd_split = split('\|\|',$gsymbol_evd);
	
	@trole_val_split = split('\|\|', $trole_val);
        @trole_evd_split = split('\|\|', $trole_evd);	
	
 	#print $go_val,"***",@go_val_split,"~~~", scalar @go_val_split,"\n";	

	foreach ($i=0; $i < scalar @cname_val_split; $i++){
		$val = @cname_val_split[$i];
		$evd = @cname_evd_split[$i];
		$val=~s/^\s+//;
		$evd=~s/^\s+//;
		$val=~s/\s+$//;
		$evd=~s/\s+$//;
		chomp ($val); chomp($evd);
		print "$pep\t$cname_cat\t$val\t$evd\n";
	}
	foreach ($i=0; $i < scalar @gsymbol_val_split; $i++){
                $val = @gsymbol_val_split[$i];
                $evd = @gsymbol_evd_split[$i];
                $val=~s/^\s+//;
                $evd=~s/^\s+//;
                $val=~s/\s+$//;
                $evd=~s/\s+$//;
                chomp ($val); chomp($evd);
                print "$pep\t$gsymbol_cat\t$val\t$evd\n";
        }
	foreach ($i=0; $i < scalar @go_val_split; $i++){
                $val = @go_val_split[$i];
                $evd = @go_evd_split[$i];
                $val=~s/^\s+//;
                $evd=~s/^\s+//;
                $val=~s/\s+$//;
                $evd=~s/\s+$//;
                chomp ($val); chomp($evd);
                print "$pep\t$go_cat\t$val\t$evd\n";
        }
	foreach ($i=0; $i < scalar @ec_val_split; $i++){
                $val = @ec_val_split[$i];
                $evd = @ec_evd_split[$i];
                $val=~s/^\s+//;
                $evd=~s/^\s+//;
                $val=~s/\s+$//;
                $evd=~s/\s+$//;
                chomp ($val); chomp($evd);
                print "$pep\t$ec_cat\t$val\t$evd\n";
        }
	foreach ($i=0; $i < scalar @trole_val_split; $i++){
                $val = @trole_val_split[$i];
                $evd = @trole_evd_split[$i];
                $val=~s/^\s+//;
                $evd=~s/^\s+//;
                $val=~s/\s+$//;
                $evd=~s/\s+$//;
                chomp ($val); chomp($evd);
                print "$pep\t$trole_cat\t$val\t$evd\n";
        }
}
