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

shell_source_dir=$(dirname $0)
source $shell_source_dir/config.sh

if [ "$country" == "all" ];then
  countries=$(cat $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f1)
  for country in $countries;do
    ./$0 $country
  done
else
  wiki=$country"wiki"

  # Download if necessary

  ./$shell_source_dir/download-wiki-stub-meta.sh $country

  # Extracting

  data_dir=$shell_source_dir/../data
  wiki_data=$data_dir/$wiki-$version/$wiki-$version-stub-meta-history*.xml.gz
  classpath=$shell_source_dir/../resources:$shell_source_dir/../../icwsm17.jar

  program="java -cp $classpath de.aitools.aq.wikipedia.reverts.ExtractReverts"
  output_dir=$shell_source_dir/../data/$wiki-$version-results/

  echo "Running $program $num_threads $wiki_data $output_dir"
  $program $num_threads $wiki_data $output_dir

  tmp_cvsignore=.tmp_cvsignore
  echo "enclosing-chains-after-removing-interleaved.txt" >> $tmp_cvsignore
  echo "enclosing-chains-after-removing-unaccepted.txt" >> $tmp_cvsignore
  echo "enclosing-chains.txt" >> $tmp_cvsignore
  echo "interleaved-chains-after-removing-unaccepted.txt" >> $tmp_cvsignore
  echo "interleaved-chains.txt" >> $tmp_cvsignore
  echo "*.xml.gz" >> $tmp_cvsignore
  if [ -e $output_dir/.cvsignore ];then
    cat $output_dir/.cvsignore >> $tmp_cvsignore
  fi
  cat $tmp_cvsignore | sort | uniq > $output_dir/.cvsignore
  rm $tmp_cvsignore


  # Analysis

  ./$shell_source_dir/analyze-interleaved.sh $country
  ./$shell_source_dir/analyze-enclosing.sh $country
  ./$shell_source_dir/analyze-self-corrected-updates.sh $country
  ./$shell_source_dir/analyze-revert-lengths.sh $country

  Rscript $shell_source_dir/../src-r/plot-double-submissions.r $output_dir/double-submission-time-spans-in-seconds.txt $output_dir/double-submission-time-spans-in-seconds-$country.pdf

  ./$shell_source_dir/make-filtering-table.sh $country
fi
