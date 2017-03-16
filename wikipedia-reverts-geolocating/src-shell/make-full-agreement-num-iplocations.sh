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
wiki=$country"wiki"
country_long=$(grep "^$country " $shell_source_dir/../$detection_dir/src-shell/countries.txt | cut -d" " -f2-)

dir=$shell_source_dir/../data/$wiki-$version-results

input=$dir/decision-tree-counts.txt
output_data=$dir/full-agreement-num-iplocations-$country.txt
output=$dir/full-agreement-num-iplocations-$country.pdf

name="time zone consistent = true"

cat $input \
  | grep "^[[:blank:]]*$name[[:blank:]]" \
  | awk -F '\t' 'function read_lengths(text, lengths, target,           num_entries,entry,entries) {
      num_entries = split(text,entries,",")
      for (i = 1; i <= num_entries; ++i) {
        split(entries[i],entry,":")
        target[entry[1]] = entry[2]
        lengths[entry[1]] = 1
      }
    }{
      read_lengths($6, lengths, total)
      read_lengths($7, lengths, reverted)

      for (l in lengths) {
        print l" "total[l]" "reverted[l]
      }
    }' \
  > $output_data

Rscript $shell_source_dir/../src-r/plot-num-iplocations.r $output_data $country_long $output

rm $output_data

