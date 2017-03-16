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
wiki=$country"wiki"

if [ "$country" == "all" ];then
  countries=$(cat $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f1)
  for country in $countries;do
    ./$0 $country
  done
else
  input_dir=$shell_source_dir/../$geolocating_dir/data/$wiki-$version-results

  classpath=$shell_source_dir/../resources:$shell_source_dir/../../icwsm17.jar

  input=$input_dir/$wiki-$version-stub-meta-history*.xml.gz
  output_dir=$shell_source_dir/../data/$wiki-$version-results

  mkdir -p $output_dir

  program="java -Xmx8G -cp $classpath de.aitools.ie.geolocating.RevisionCounter $num_threads $input $output_dir/day-counts.txt"

  if [ ! \( -e $output_dir/day-counts.txt \) ];then
    echo "Running $program"
    $program 2>&1 | tee $output_dir.log
  fi

  if [ ! \( -e $output_dir/counts.txt \) ];then
    echo "create $output_dir/counts.txt"
    cat $output_dir/day-counts.txt \
      | java -cp $classpath de.aitools.ie.geolocating.RevisionCountMarginalizer ".*" ".*" "" ".*" ".*" ".*" \
      > $output_dir/counts.txt
  fi

  if [ ! \( -e $output_dir/by-country.txt \) ];then
    echo "create $output_dir/by-country.txt"
    cat $output_dir/counts.txt \
      | java -cp $shell_source_dir/../resources:$shell_source_dir/../../icwsm17.jar de.aitools.ie.geolocating.RevisionCountMarginalizer "" ".*" "" "" "" \
      > $output_dir/by-country.txt
  fi

  if [ ! \( -e $output_dir/by-timezone.txt \) ];then
    echo "create $output_dir/by-timezone.txt"
    cat $output_dir/counts.txt \
      | java -cp $shell_source_dir/../resources:$shell_source_dir/../../icwsm17.jar de.aitools.ie.geolocating.RevisionCountMarginalizer ".*" "" "" "" "" \
      > $output_dir/by-timezone.txt
  fi

  if [ ! \( -e $output_dir/day-counts-microsoft-timezone.txt \) ];then
    echo "create $output_dir/day-counts-microsoft-timezone.txt"
    cat $output_dir/day-counts.txt \
      | java -cp $shell_source_dir/../resources:$shell_source_dir/../../icwsm17.jar de.aitools.ie.geolocating.RevisionCountMerger 0 \
        "Pacific:America/Los_Angeles" \
        "Mountain:(America/Boise)|(America/Denver)|(America/Phoenix)" \
        "Central:(America/Chicago)|(America/Menominee)|(America/North_Dakota/Beulah)|(America/North_Dakota/Center)|(America/North_Dakota/New_Salem)" \
        "Eastern:(America/Detroit)|(America/Indiana/Indianapolis)|(America/Indiana/Knox)|(America/Indiana/Marengo)|(America/Indiana/Petersburg)|(America/Indiana/Tell_City)|(America/Indiana/Vevay)|(America/Indiana/Vincennes)|(America/Indiana/Winamac)|(America/Kentucky/Louisville)|(America/New_York)|(America/Kentucky/Monticello)" \
        ":.*" \
        6 \
      > $output_dir/day-counts-microsoft-timezone.txt
  fi

  if [ ! \( -e $output_dir/counts-microsoft-timezone.txt \) ];then
    echo "create $output_dir/counts-microsoft-timezone.txt"
    cat $output_dir/day-counts-microsoft-timezone.txt \
      | java -cp $classpath de.aitools.ie.geolocating.RevisionCountMarginalizer ".*" ".*" "" ".*" ".*" ".*" \
      > $output_dir/counts-microsoft-timezone.txt
  fi
fi

