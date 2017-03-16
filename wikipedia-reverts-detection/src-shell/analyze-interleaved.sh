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
output=$dir/interleaved-chains-after-removing-unaccepted-statistics.txt

function read_all() {
  cat $dir/interleaved-chains-after-removing-unaccepted.txt
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


echo -e "#1#count\t#2#reverts\t#3#chains" > $output

read_all | count_as "all"

tmp=/tmp/analyze-$$
read_all | filter_two_texts | strip_non_revert_revisions > $tmp
cat $tmp | count_as "2 texts"
cat $tmp | filter_by_num_reverts "== 2"   | count_as "2 texts  2 reverts"
cat $tmp | filter_by_num_reverts "== 3"   | count_as "2 texts  3 reverts"
cat $tmp | filter_by_num_reverts "== 4"   | count_as "2 texts  4 reverts"
cat $tmp | filter_by_num_reverts "== 5"   | count_as "2 texts  5 reverts"
cat $tmp | filter_by_num_reverts "== 6"   | count_as "2 texts  6 reverts"
cat $tmp | filter_by_num_reverts "== 7"   | count_as "2 texts  7 reverts"
cat $tmp | filter_by_num_reverts "== 8"   | count_as "2 texts  8 reverts"
cat $tmp | filter_by_num_reverts "== 9"   | count_as "2 texts  9 reverts"
cat $tmp | filter_by_num_reverts "== 10"  | count_as "2 texts 10 reverts"
rm $tmp

