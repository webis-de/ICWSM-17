#!/bin/bash

if [ -z "$1" ];then
  echo "Usage:"
  echo "  $0 <country>"
  echo "With"
  echo "  country"
  echo "    The two-letter country code as used by Wikipedia (en, de, fr, ...)"
  exit 1
fi

country=$1

shell_source_dir=`dirname $0`
source $shell_source_dir/config.sh
source $shell_source_dir/../$detection_dir/src-shell/config.sh

if [ "$country" == "all" ];then
  countries=$(cat $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f1)
  for country in $countries;do
    ./$0 $country
  done
else
  wiki=$country"wiki"

  input_dir=$shell_source_dir/../$detection_dir/data/$wiki-$version-results

  classpath=$shell_source_dir/../../icwsm17.jar

  iplocation_dir=$shell_source_dir/../data/iplocation-parsed
  rir_dir=$shell_source_dir/../data/rir-parsed
  input=$input_dir/$wiki-$version-stub-meta-history*.xml.gz
  output_dir=$shell_source_dir/../data/$wiki-$version-results

  rm -rf $output_dir
  mkdir -p $output_dir

  program="java -Xmx32G -cp $classpath de.aitools.aq.geolocating.wikipedia.WikiGeolocator $iplocation_dir $rir_dir $num_threads $input $output_dir"

  echo "Running $program"
  $program 2>&1 | tee $output_dir.log


  # Analysis

  ./$shell_source_dir/make-overview-table.sh $country
  ./$shell_source_dir/make-geolocation-decision-tree.sh $country
  ./$shell_source_dir/make-full-agreement-num-iplocations.sh $country
fi

