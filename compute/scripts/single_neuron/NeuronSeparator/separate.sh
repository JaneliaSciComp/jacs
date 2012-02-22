#/bin/sh
#
# Run the two-stage neuron separation
#
# Usage:
#
# sh separate.sh <output dir> <name> <input file> 

DIR=$(cd "$(dirname "$0")"; pwd)

NSDIR="$DIR/../../../neusep-redhat"
OUTDIR=$1
NAME=$2
INPUTFILE=$3
FRAGFILE="$OUTDIR/$2.chk4"

#Usage: setup4  [-c<double(5.0)>] [-e<double(3.5)>] [-s<int(300)>] 
#                <output:FILE>  <inputs:FILE> ...
#Usage: finish4  [-3BM] [-nr] [-cl]
#                 <folder:FOLDER>  <core:STRING>  <input:FILE>

# Decrease "c" to get more neurons
# Decrease "s" to get more (smaller) neurons
# Decrease "e" to get more neurons

SETUP="$NSDIR/setup4 -c6.0 -e4.5 -s800 -r0 $OUTDIR $FRAGFILE $INPUTFILE"
echo "$SETUP"
$SETUP

FINISH="$NSDIR/finish4 -nr $OUTDIR $NAME $FRAGFILE"
echo "$FINISH"
$FINISH

