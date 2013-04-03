#!/bin/bash
#
# Common functions 
#

ensureRawFile()
{
    local _Vaa3D="$1"
    local _WORKING_DIR="$2"
    local _FILE="$3"
    local _RESULTVAR="$4"
    local _EXT=${_FILE#*.}
    if [ "$_EXT" == "v3dpbd" ]; then
        local _PBD_FILE=$_FILE
        local _FILE_STUB=`basename $_PBD_FILE`
        _FILE="$_WORKING_DIR/${_FILE_STUB%.*}.v3draw"
        echo "Converting PBD to RAW format"
        $_Vaa3D -cmd image-loader -convert "$_PBD_FILE" "$_FILE"
    fi
    eval $_RESULTVAR="'$_FILE'"
}


