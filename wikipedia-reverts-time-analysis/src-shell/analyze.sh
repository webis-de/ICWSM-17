#!/bin/bash

if [ -z "$1" ];then
  echo "Usage:"
  echo "  $0 <country>"
  echo "With"
  echo "  country"
  echo "    The two-letter country code as used by Wikipedia (en, de, fr, ...) or \"all\""
  exit 1
fi

country=$1

shell_source_dir=`dirname $0`
source $shell_source_dir/config.sh
source $shell_source_dir/../$geolocating_dir/src-shell/config.sh
source $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/config.sh


if [ "$country" == "all" ];then
  countries=$(cat $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f1)
  for country in $countries;do
    ./$0 $country
  done
else
  countries_file=$shell_source_dir/countries.txt
  wikis_file=$shell_source_dir/wikis.txt

  wiki=$country"wiki"
  wiki_name=$(cat $wikis_file | grep "^$country" | cut -d" " -f2-)
  country_long=$(grep "^$country " $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f2-)
  dir=$shell_source_dir/../data/$wiki-$version-results
  input=$dir/counts.txt
  input_timezone=$dir/counts-microsoft-timezone.txt

  # Make tables
  echo "tables"
  if [ -e $dir/table-wiki-country-count-revisions-by-$country.tex ];then
    echo "      Already exists: $dir/table-wiki-country-count-revisions-by-$country.tex"
  else
    $shell_source_dir/make-wiki-country-count-tables.sh $country
  fi

  # Plot maps
  echo "maps"
  if [ -e $dir/world-absolute-$country.pdf ];then
    echo "      Already exists: $dir/world-absolute-$country.pdf"
  else
    Rscript $shell_source_dir/../src-r/plot-world-maps.r $dir/by-country.txt $dir/by-timezone.txt $reverted_threshold $dir/world $country
  fi

  # Plot timelines

  function get() {
    local timezone=$1
    local country=$2
    local season=$3
    local weekday=$4

    local in=$input
    local fields="2-"

    local index=1
    if [ "$timezone" != "" ];then
      let index++
      in=$input_timezone
      fields="1,3-"
    fi
    if [ "$season" != "" ];then let index++;fi
    if [ "$weekday" != "" ];then let index++;fi

    if [ -z "$country" ];then
      cat $in \
        | java -cp $shell_source_dir/../resources:$shell_source_dir/../../icwsm17.jar de.aitools.ie.geolocating.RevisionCountMarginalizer "$timezone" "$country" "$season" "$weekday" ".*" \
        | sort -g -k $index
    else
      cat $in \
        | java -cp $shell_source_dir/../resources:$shell_source_dir/../../icwsm17.jar de.aitools.ie.geolocating.RevisionCountMarginalizer "$timezone" "$country" "$season" "$weekday" ".*" \
        | cut -f $fields \
        | sort -g -k $index
    fi
  }

  function plot() {
    local property=$1
    local country_other=$2

    local timezone=""
    local season=""
    local weekday=""
    case $property in
      "timezone")
        timezone=".*"
        ;;
      "season")
        season=".*"
        ;;
      "weekday")
        weekday=".*"
        ;;
    esac

    local from_long="Worldwide"
    local filename=by-hour
    if [ "$property" != "" ];then
      filename=$filename"-and-$property"
    fi
    filename=$filename"-"$country
    if [ "$country_other" != "" ];then
      filename=$filename-from-$(echo $country_other | awk '{print tolower($1)}')
      from_long=$(cat $countries_file | grep "^$country $country_other" | cut -d" " -f3-)
    fi

    echo "    $filename"
    if [ -e $dir/$filename.txt ];then
      echo "      Already exists: $dir/$filename.txt"
    else
      get "$timezone" "$country_other" "$season" "$weekday" > $dir/$filename.txt
    fi
    if [ -e $dir/plot-$filename.pdf ];then
      echo "      Already exists: $dir/plot-$filename.pdf"
    else
      Rscript $shell_source_dir/../src-r/plot-by-hour.r "" $dir/$filename.txt "$property" "$from_long" "$wiki_name" $reverted_threshold $dir/plot-$filename.pdf
    fi
  }


  plot "" ""
  plot "timezone" ""
  plot "season" ""
  plot "weekday" ""
  for country_other in $(cat $countries_file | grep "^$country" | cut -d" " -f2);do
    echo $country_other
    plot "" $country_other
    plot "season" $country_other
    plot "weekday" $country_other
  done
fi


