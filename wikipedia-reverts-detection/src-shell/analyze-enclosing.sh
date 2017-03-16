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
output=$dir/enclosing-chains-after-removing-interleaved-statistics.txt

function read_all() {
  cat $dir/enclosing-chains-after-removing-interleaved.txt
}

function count_as() {
  as="$1"
  echo "Counting: $as"

  echo -ne "$as\t" >> $output
  cat /dev/stdin \
    | sed 's/[^ ]*://g' \
    | sed 's/_ //g' \
    | awk 'BEGIN {
         sum = 0
       } {
         sum += NF
       } END {
         print sum"\t"NR
       }' \
    >> $output
}

function strip_non_revert_revisions() {
  cat /dev/stdin | sed 's/[^ ]*:_:[^ ]* //g'
}

function filter_two_texts() {
  cat /dev/stdin | grep -v ":2:"
}

function filter_by_num_reverts() {
  filter="$1"

  cat /dev/stdin \
    | awk '{
         c = 0
         for (f = 1; f <= NF; ++f) {
           if (!($f ~ /_$/)) {
             ++c
           }
         }
         if (c '"$filter"') {
           print $0
         }
       }'
}

function filter_self_correction() {
  no_revert="[AR][^ :]*:[^ :]*:_"
  first_revert="[AR]([^ :]*):[^ :]*:[^_ ]*"
  revert_by_same="[AR]\3:[^ :]*:[^_ ]*"
  cat /dev/stdin | grep -P "^($no_revert )*($first_revert)( (($no_revert)|($revert_by_same)))*$" 
}


echo -e "#1#count\t#2#reverts\t#3#chains" > $output

read_all | count_as "all"
read_all | filter_two_texts | strip_non_revert_revisions | count_as "2 texts"
read_all | filter_by_num_reverts "== 2" | count_as "2 reverts"

