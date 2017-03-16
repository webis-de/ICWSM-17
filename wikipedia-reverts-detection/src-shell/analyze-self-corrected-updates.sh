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

shell_source_dir=$(dirname $0)
source $shell_source_dir/config.sh
wiki=$country"wiki"
dir=$shell_source_dir/../data/$wiki-$version-results/
input=$dir/reverts-self-corrected-updates.xml.gz
output=$dir/reverts-self-corrected-updates-statistics.txt

function read_all() {
  cat $input | gunzip | grep -v "^<"
}

function to_lengths() {
  cat /dev/stdin | awk '{if(NF == 0) {print ""} else {printf gsub(/<\/reverted>/,"")" "}}END{print ""}'
}

read_all \
  | to_lengths \
  | grep -v "^$" \
  | awk '{
      num_sequences += 1
      diff = $NF - $1
      if (diff > 0) {
        num_sequences_revert_got_larger += 1
      } else if (diff < 0) {
        num_sequences_revert_got_smaller += 1
      }
      num_sequences_by_length[NF - 1] += 1
    } END {
      print "num sequences\t"num_sequences
      print "num sequences revert got larger\t"num_sequences_revert_got_larger
      print "num sequences revert got smaller\t"num_sequences_revert_got_smaller
      for (l in num_sequences_by_length) {
        print "num sequences length "l"\t"num_sequences_by_length[l]
      }
    }' \
  > $output


