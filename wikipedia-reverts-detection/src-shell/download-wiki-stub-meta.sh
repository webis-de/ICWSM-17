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
wiki=$country"wiki"

output_dir=$shell_source_dir/../data/$wiki-$version
mkdir -p $output_dir

cont=1
part=1
while [ $cont -eq 1 ];do
  output_file=$output_dir/$wiki-$version-stub-meta-history$part.xml.gz
  if [ -e $output_file ];then
    echo "Not downloading already existing file: $output_file"
  else
    echo "Downloading: $output_file"
    curl --output $output_file "https://dumps.wikimedia.org/$wiki/$version/$wiki-$version-stub-meta-history$part.xml.gz"

    if [ $(file $output_file | grep HTML | wc -l) -eq 1 ];then
      echo "No such part: $part. Aborting"
      rm $output_file
      cont=0
    fi
  fi

  let part++
done

