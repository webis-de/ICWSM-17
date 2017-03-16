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
input=$dir/reverts.xml.gz
output_base=$dir/revert-lengths

echo -e "#1#length\t#2#count" > $output_base.txt
cat $input \
  | gunzip \
  | grep "<revert " \
  | awk -F "<reverted" '{print NF-1}' \
  | awk '{
      cnt[$1] += 1
    } END {
      for (l in cnt) {
        print l" "cnt[l]
      }
    }' \
  | sort -g \
  | awk '{
      while ($1 > (line + 1)) {
        line += 1
        print line" "0
      }
      print $0
      line += 1
    }' \
  >> $output_base.txt

Rscript $shell_source_dir/../src-r/plot-revert-lengths.r $output_base.txt $output_base-$country.pdf

