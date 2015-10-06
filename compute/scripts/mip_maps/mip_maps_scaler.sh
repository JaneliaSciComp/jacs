#!/bin/sh

SCRIPT_DIR=$(cd "$(dirname "$0")"; pwd)
JAVA_MEMORY="6G"

# Prepare the scaler parameters
SCALER_JAR_FILE="$SCRIPT_DIR/ScaleCATMAID-jar-with-dependencies.jar"

SCALER_PARAMS="-DtileFormat=${ROOT_URL}/${TILE_STACK_FORMAT}"

if [ "$IMAGE_WIDTH" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DsourceWidth=${IMAGE_WIDTH}"
fi

if [ "$IMAGE_HEIGHT" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DsourceHeight=$IMAGE_HEIGHT"
fi

if [ "$IMAGE_DEPTH" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DsourceDepth=$IMAGE_DEPTH"
fi    

if [ "$SOURCE_MIN_X" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DminX=$SOURCE_MIN_X"
fi

if [ "$SOURCE_MIN_Y" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DminY=$SOURCE_MIN_Y"
fi

if [ "$SOURCE_MIN_Z" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DminZ=$SOURCE_MIN_Z"
fi							

if [ "$SOURCE_WIDTH" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -Dwidth=$SOURCE_WIDTH"
fi

if [ "$SOURCE_HEIGHT" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -Dheight=$SOURCE_HEIGHT"
fi

if [ "$SOURCE_MAX_Z" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DmaxZ=$SOURCE_MAX_Z"
fi							

if [ "$TARGET_TILE_WIDTH" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DtileWidth=$TARGET_TILE_WIDTH"
fi								    

if [ "$TARGET_TILE_HEIGHT" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DtileHeight=$TARGET_TILE_HEIGHT"
fi									

if [ "$TARGET_QUALITY" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -Dquality=$TARGET_QUALITY"
fi										    

if [ "$TARGET_TYPE" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -Dtype=$TARGET_TYPE"
fi										    

if [ "$TARGET_MEDIA_FORMAT" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -Dformat=$TARGET_MEDIA_FORMAT"
fi										    

if [ "$TARGET_SKIP_EMPTY_TILES" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DignoreEmptyTiles=$TARGET_SKIP_EMPTY_TILES"
fi				    


scaler_cmd="java -Xms${JAVA_MEMORY} -Xmx${JAVA_MEMORY} ${SCALER_PARAMS} -jar ${SCALER_JAR_FILE}"
echo "Executing $scaler_cmd"

java -Xms${JAVA_MEMORY} -Xmx${JAVA_MEMORY} ${SCALER_PARAMS} -jar ${SCALER_JAR_FILE}
