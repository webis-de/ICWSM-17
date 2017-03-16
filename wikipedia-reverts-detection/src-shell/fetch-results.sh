#!/bin/bash

user=$1
dir=$2

if [ "$user" != "" ];then
  user="$user@"
fi

if [ -z "$dir" ];then
  dir=webis62.medien.uni-weimar.de:/home/dogu3912/data/wikipedia-reverts
fi

shell_source_dir=$(dirname $0)
source $shell_source_dir/config.sh
countries=$(cat $shell_source_dir/countries.txt | cut -d" " -f1)

for country in $countries;do
  echo "Fetching $country"
  outdir=$shell_source_dir/../data/"$country"wiki-$version-results
  mkdir -p $outdir
  rsync "$user$dir/wikipedia-reverts-detection/data/"$country"wiki-$version-results/*.{txt,tex,pdf}" $outdir
  if [ -e $outdir/.cvsignore ];then
    echo ".cvsignore exists already"
  else
    echo "*.xml.gz" > $outdir/.cvsignore
    echo "enclosing-chains-after-removing-interleaved.txt" >> $outdir/.cvsignore
    echo "enclosing-chains-after-removing-unaccepted.txt" >> $outdir/.cvsignore
    echo "enclosing-chains.txt" >> $outdir/.cvsignore
    echo "interleaved-chains-after-removing-unaccepted.txt" >> $outdir/.cvsignore
    echo "interleaved-chains.txt" >> $outdir/.cvsignore
  fi
done
