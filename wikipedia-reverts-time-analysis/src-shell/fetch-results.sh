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
source $shell_source_dir/../$geolocating_dir/src-shell/config.sh
source $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/config.sh
countries=$(cat $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f1)

for country in $countries;do
  echo "Fetching $country"
  outdir=$shell_source_dir/../data/"$country"wiki-$version-results
  mkdir -p $outdir
  rsync "$user$dir/wikipedia-reverts-time-analysis/data/"$country"wiki-$version-results/*.{txt,tex,pdf}" $outdir
  if [ -e $outdir/.cvsignore ];then
    echo ".cvsignore exists already"
  else
    echo "*.xml.gz" > $outdir/.cvsignore
  fi
done
