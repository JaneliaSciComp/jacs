#!/bin/sh

DIR=$(cd "$(dirname "$0")"; pwd)
INSTALL=$HOME/FlySuite

echo "Installing FlySuite from $DIR into $INSTALL"

FWVER=`echo $DIR | sed 's/^.*FlySuite_linux_//'`

rm -rf $INSTALL
cp -R $DIR $INSTALL

SC=$HOME/Desktop/FlySuite.desktop
SC_TMP=/tmp/FlySuite.desktop
echo "[Desktop Entry]" > $SC_TMP
echo "Name=FlySuite" >> $SC_TMP
echo "Version=$FWVER" >> $SC_TMP
echo "Type=Application" >> $SC_TMP
echo "Terminal=true" >> $SC_TMP
echo "Exec=$INSTALL/run_in_term.sh" >> $SC_TMP
echo "Icon=$INSTALL/fly.png" >> $SC_TMP

chmod +x $SC_TMP
mv $SC_TMP $SC

