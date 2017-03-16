#!/bin/bash

if [ -z "$1" ];then
  echo "Usage:"
  echo "  $0 <wiki>"
  echo "With"
  echo "  wiki"
  echo "    The two-letter country code as used by Wikipedia (en, de, fr, ...) or \"all\""
  exit 1
fi

wiki=$1

shell_source_dir=`dirname $0`
source $shell_source_dir/config.sh
source $shell_source_dir/../$geolocating_dir/src-shell/config.sh
source $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/config.sh

marginalize="java -cp $shell_source_dir/../resources:$shell_source_dir/../../icwsm17.jar de.aitools.ie.geolocating.RevisionCountMarginalizer"


if [ "$wiki" == "all" ];then
  wikis=$(cat $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f1)
  for wiki in $wikis;do
    ./$0 $wiki
  done
else
  dir=$shell_source_dir/../data/"$wiki"wiki-$version-results

  function timeline() {
    local origin=$1

    if [ ! \( -e $dir/by-day-$wiki-from-$origin.txt \) ];then
      echo "MAKE: by-day-$wiki-from-$origin.txt"
      cat $dir/day-counts.txt \
        | $marginalize "" "${origin^^}" ".*" ".*" ".*" "" \
        | awk -F'\t' '{print $2"\t"$3"\t"$4"\t"$5"\t"$6"\t"$7}' \
        > $dir/by-day-$wiki-from-$origin.txt
    fi

  }

  timeline us
fi
