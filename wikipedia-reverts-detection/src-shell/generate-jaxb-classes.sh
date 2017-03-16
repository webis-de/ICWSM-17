#!/bin/bash

shell_source_dir=`dirname $0`
source $shell_source_dir/config.sh

java_source_dir=$shell_source_dir/../src

pushd $java_source_dir
LANG="en_US.UTF-8" xjc -p de.aitools.aq.wikipedia.xml ../data/$schema
popd

