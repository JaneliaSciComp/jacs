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

package TestComputeWs;

use strict;
use warnings;

use Carp;
use Config::IniFiles;
use Data::Dumper;
use SOAP::Lite;

=head1 NAME

testComputeWs.pm - methods for using .ini based testing of ComputeWS methods.

=head1 SYNOPSIS

    use TestComputeWs;
    my $tcw = new TestComputeWs;
    $tcw->config('/path/to/config/file');
    $tcw->submit('compute_call')
    $tcw->submit('compute_call',\%options);

=head1 DESCRIPTION

TestComputeWs provides an object-oriented interface to a config-file driven 
system of submitting calls to a webservice.  Both the url to the wsdl as well as
the commands and options are provided in the .ini style config file.  It is up to
a calling script to determine the order and overide provided config values. (For
example, providing a newly created Session Id to calls that take it.)

=head1 CONFIG FILE FORMAT

Config files follow the typical .ini rules, with the following assumptions made:

1.  There is a WSDL section, and in the WSDL section there exists a wsdl_url parameeter
containing the url to the webservice url.

2.  Commands to be run are present as sections, with the name of the command as the name
of a section.  All parameters are present in the section, with 'default' values in place.
These can be overridden during invocation of the 'submit' method.

=head1 USAGE

Create a new TestComputeWs object with the I<new> method:

    $tcw = TestComputeWs->new();
    $tcw = new TestComputeWs;

Currently, there are no parameters required by the constructor.  However, it is necessary
to pass in a config file:

    $tcw->config('/path/to/config/file');

Or specify one when calling the constructer:

    $tcw = new TestComputeWs('/path/to/config/file');

At this point, any commands present in the config file can be sent to the service by
running:

    $tcw->submit('commandName');

or to override some parameter values:

    $tcw->submit('commandName',\%newValues);


=head1 METHODS

=cut

sub new {

    my ($class,$config) = @_;
    my $self = {};
    $self->{CONFIG} = ($config) ? $self->config($config) : undef;
    bless($self,$class);
    return $self;

}

=head2 config ([ $config_file ])

Returns the current path to the config file in use.  If used with an argument,
attempts to set the passed in filename as the config file, and returns the config
file path as with the no-argument version.

Attempts to set the WSDL as found in the config file if passed in.

=cut

sub config {

    my $self = shift;
    if (@_) {
        $self->{CONFIG} = shift;
        $self->{CFG} = new Config::IniFiles ( -file => $self->{CONFIG} );
        $self->{WSDL}   = $self->_get_wsdl_from_config($self->{CFG})
    }
    return $self->{CONFIG};

}

=head2 submit ($command_name, [ \%option_hash ])

Attempts to submit the given command.  Looks within the config to determine the
default options, then replaces any values with those given in the optional hash_ref
argument containing key/value pairs for any parameters for which overriding is desired.

Commands are issued to the wsdl url using SOAP::Lite, and the results are passed directly
by return().

=cut

sub submit {

    my $self = shift;
    my ($command,$user_options) = @_;

    # get options from the config
    my ($options,$order) = $self->get_config_options($command);

    # override with any options passed directly in
    foreach my $param (keys %$user_options) {

        if ( exists($options->{$param}) ) {
            $options->{$param} = '' unless (defined $options->{$param});
            $options->{$param} = $user_options->{$param};
        }

    }

    # convert the option hash to an array of the values
    my @param_values;
    foreach my $opt ( sort {$order->{$a} <=> $order->{$b}} keys %{$options} ) {
        push @param_values,$options->{$opt};
    }

    # send it off.  Return the output
    return( SOAP::Lite-> service($self->{WSDL})->$command(@param_values) );

}

=head2 get_config_options ( $command )

    my ($options, $order) = $tcw->get_config_options('myCommand');

Used to retrieve the values found in a config file for a given command.

Two hash references are returned.  The first is simply the key-value pairs for
the parameters.  This allows simple and easy replacement of the values, or easy
printing of the same.

The second hash reference contains the same keys, but the value is an integer
dictating the order in which it should appear in the submission command.  This
is determined solely by the order in which it was read from the configuration.
    
=cut

sub get_config_options {

    my ($self, $command) = @_;

    # Get a list of the params in the command's section
    my @param_list = $self->{CFG}->Parameters($command);
    my %config_param_values = ();

    my %order = ();
    my $order = 0;

    # Pull each value into the hash
    foreach my $param (@param_list) {

        $config_param_values{$param} = $self->{CFG}->val($command,$param);
        $order{$param} = $order;
        $order++;

    }
#print map {"$_\t$order{$_}\n"} sort {$order{$a} <=> $order{$b}} keys %order;
    # return refernces to the hashes
    return \%config_param_values,\%order;

}

=head2 get_job_status ( $taskId, [$command, $sleep] )

    my $result          = $tcw->get_job_status( $taskId );
    my $blastResult     = $tcw->get_job_status( $taskId, 'Blast');
    my $hmmResult       = $tcw->get_job_status( $taskId, 'Hmmpfam', $sleep );
    my $metageneResult  = $tcw->get_job_status( $taskId, 'Metagene', $sleep );

Checks on the status of a given job id and returns the status string.

Works for any status finding web service named 'getFooStatus' where the command
of the given taskId is 'Foo'.  When no command is passed in, launches the
plain old 'getTaskStatus' service.

So in the above example, the first form runs getTaskStatus, the second calls
getBlastStatus, and the third calls getHmmpfamStatus.  More services are also
called by this method.  In fact, any command name passed is used to construct
the name of the anticipated web service in the form described.

This function will sleep for 1 second between attempts to retrieve the status
unless $sleep is also passed in, in which case it will sleep for that long.

Returned status flags are 'error' and 'complete'.  Any other states are
considered ignorable.

=cut

sub get_job_status {

    my $self = shift;

    my ($taskId, $cmd, $sleep) = @_;
    $cmd = 'Task' unless $cmd;
    my $command = "get$cmd" . 'Status';
    $sleep = 1 unless $sleep;
    croak "No task id found\n" unless $taskId;

    my $status = '';
    my $statusText = '';
    while ($status !~ /error|completed/) {
;
        sleep ($sleep);

        $statusText = SOAP::Lite-> service($self->{WSDL})->$command(getlogin,'',$taskId);
        if ($statusText =~ /Status Type: (\S+)/s) {
            $status = $1;
            print '.';
        } else {
            croak "$statusText\n";
        }
    }

    return $statusText;

}

###############################################################################
#
# _get_wsdl_from_config ()
#
#   Used to retrieve the wsdl from the WSDL section of the config, and store it
#   in $self->{WSDL}
#
#######################################
sub _get_wsdl_from_config {

    my $self = shift;

    # Store the wsdl url; return it, too.
    $self->{WSDL} = $self->{CFG}->val('WSDL','wsdl_url');
    return $self->{WSDL};

}



# One is the loneliest number...
1;
