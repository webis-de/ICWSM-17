#!/bin/bash

shell_source_dir=`dirname $0`

classpath=$shell_source_dir/../../icwsm17.jar

input_dir=$shell_source_dir/../data/rir
output_dir=$shell_source_dir/../data/rir-parsed

program="java -Xmx8G -cp $classpath de.aitools.aq.geolocating.rir.RirIpBlocks $input_dir $output_dir"

echo "Running $program"
$program 2>&1 | tee $output_dir.log

