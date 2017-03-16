#!/bin/bash

shell_source_dir=`dirname $0`
source $shell_source_dir/config.sh
source $shell_source_dir/../$geolocating_dir/src-shell/config.sh
source $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/config.sh
countries_file=$shell_source_dir/countries.txt
wikis_file=$shell_source_dir/wikis.txt

dir=paperplots
mkdir -p $dir

function get_filename() {
  local wiki=$1
  local property=$2
  local origin=$3
  local vandalism_with_comment=$4

  local filename=by-hour
  if [ "$property" != "" ];then
    filename=$filename"-and-$property"
  fi
  filename=$filename"-"$wiki
  if [ "$origin" != "" ];then
    filename=$filename-from-$(echo "$origin" | awk '{print tolower($1)}')
  fi
  echo $filename
}

function get_table_file() {
  local wiki=$1
  local property=$2
  local origin=$3

  local filename=$(get_filename $wiki "$property" "$origin")
  filename=$(echo $filename | sed 's/timezone-en-from-us/timezone-en/')
  echo $shell_source_dir/../data/$wiki"wiki"-$version-results/$filename.txt
}

function get_wiki_name() {
  local wiki=$1
  cat $wikis_file | grep "^$wiki " | cut -d" " -f2-
}

function get_origin_name() {
  local origin=$1
  if [ "$origin" == "" ];then
    echo "Worldwide"
  else
    echo $(cat $countries_file | grep "^.. $origin " | head -1 | cut -d" " -f3-)
  fi
}

function single_plot() {
  local label=$1
  local wiki=$2
  local property=$3
  local origin=$4

  local filename=$(get_filename $wiki "$property" "$origin")
  local wiki_name=$(get_wiki_name "$wiki")
  local origin_name=$(get_origin_name "$origin")
  local table=$(get_table_file $wiki "$property" "$origin")

  Rscript $shell_source_dir/../src-r/plot-by-hour.r "$label" $table "$property" "$origin_name" "$wiki_name" $reverted_threshold $dir/$filename.pdf
}

function quadruple_plot() {
  local output=$1
  local label_offset=$2
  local property=$3
  shift 3

  local args="$label_offset $property $reverted_threshold $dir/$output.pdf"
  while [ $# -gt 0 ];do
    local wiki=$1
    local origin=$2
    shift 2

    local filename=$(get_filename $wiki "$property" "$origin")
    local wiki_name=$(get_wiki_name "$wiki")
    local origin_name=$(get_origin_name "$origin")
    local table=$(get_table_file $wiki "$property" "$origin")

    args="$args $table \"$origin_name\" \"$wiki_name\""
  done

  eval "Rscript $shell_source_dir/../src-r/quadruple-plot-by-hour.r $args"
}

function ten_plot() {
  local output=$1
  local label_offset=$2
  local property=$3
  shift 3

  local args="$label_offset $property $reverted_threshold $dir/$output.pdf"
  while [ $# -gt 0 ];do
    local wiki=$1
    local origin=$2
    shift 2

    local filename=$(get_filename $wiki "$property" "$origin")
    local wiki_name=$(get_wiki_name "$wiki")
    local origin_name=$(get_origin_name "$origin")
    local table=$(get_table_file $wiki "$property" "$origin")

    args="$args $table \"$origin_name\" \"$wiki_name\""
  done

  eval "Rscript $shell_source_dir/../src-r/ten-plot-by-hour.r $args"
}


#######################################################################################
#######################################################################################

$shell_source_dir/analyze.sh all


# World map
cp $shell_source_dir/../data/enwiki-$version-results/world-ratio-en.pdf $dir

single_plot "(a)" en "" US
single_plot "(b)" en "timezone" US
single_plot "(c)" en "weekday" US
single_plot "(d)" en "season" US

quadruple_plot "quad-by-hour-and-weekday-en-from-others" 0 "weekday"   en GB   en CA   en IN   en AU
quadruple_plot "quad-by-hour-and-weekday-home-and-en" 4 "weekday"   de DE   en DE   fr FR   en FR
quadruple_plot "quad-by-hour-and-weekday-home-others" 0 "weekday"   es ES   ru RU   it IT   ja JP

ten_plot "by-hour-and-weekday-others" 0 "weekday"   en GB   en CA   en IN   en AU   de DE   en DE   fr FR   en FR   es ES   ja JP


