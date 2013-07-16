#!/bin/sh

DIR=$(cd "$(dirname "$0")"; pwd)
INSTALL=$HOME/JaneliaWorkstation

echo "Installing Janelia Workstation from $DIR into $INSTALL"

JWVER=`echo $DIR | sed 's/^.*JaneliaWorkstation_linux_//'`

rm -rf $INSTALL
cp -R $DIR $INSTALL

SC=$HOME/Desktop/JaneliaWorkstation.desktop
SC_TMP=/tmp/JaneliaWorkstation.desktop
echo "[Desktop Entry]" > $SC_TMP
echo "Name=JaneliaWorkstation" >> $SC_TMP
echo "Version=$JWVER" >> $SC_TMP
echo "Type=Application" >> $SC_TMP
echo "Terminal=true" >> $SC_TMP
echo "Exec=$INSTALL/run_in_term.sh" >> $SC_TMP
echo "Icon=$INSTALL/workstation_128_icon.png" >> $SC_TMP

chmod +x $SC_TMP
mv $SC_TMP $SC

