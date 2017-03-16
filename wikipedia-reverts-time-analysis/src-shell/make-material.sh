#!/bin/bash


shell_source_dir=`dirname $0`
source $shell_source_dir/config.sh
source $shell_source_dir/../$geolocating_dir/src-shell/config.sh
source $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/config.sh

dir=$shell_source_dir/material/make-$$
mkdir -p $dir

main_file_name=material
cat $shell_source_dir/material/head.tex > $dir/$main_file_name.tex

$shell_source_dir/analyze.sh all

countries=$(cat $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f1)
for country in $countries;do
  country_long=$(grep "^$country " $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f2-)

  cp $shell_source_dir/../$geolocating_dir/$detection_dir/data/$country"wiki"-$version-results/revert-lengths-$country.pdf $dir
  cp $shell_source_dir/../$geolocating_dir/$detection_dir/data/$country"wiki"-$version-results/table-reverts-filtering-$country.tex $dir
  sed -i 's/\\cite{kittur:2007a}/Kittur et.\\ al, 2007/g' $dir/table-reverts-filtering-$country.tex
  sed -i 's/begin{table*}/begin{table}[H]/g' $dir/table-reverts-filtering-$country.tex
  sed -i 's/end{table*}/end{table}[H]/g' $dir/table-reverts-filtering-$country.tex
  cp $shell_source_dir/../$geolocating_dir/data/$country"wiki"-$version-results/full-agreement-num-iplocations-$country.pdf $dir
  cp $shell_source_dir/../$geolocating_dir/data/$country"wiki"-$version-results/geolocation-decision-tree-$country.pdf $dir
  cp $shell_source_dir/../$geolocating_dir/data/$country"wiki"-$version-results/table-geolocation-overview-$country.tex $dir
  sed -i 's/ ([^)]*})//g' $dir/table-geolocation-overview-$country.tex
  sed -i 's/begin{table}/begin{table}[H]/g' $dir/table-geolocation-overview-$country.tex
  cp $shell_source_dir/../data/$country"wiki"-$version-results/world-ratio-$country.pdf $dir
  cp $shell_source_dir/../data/$country"wiki"-$version-results/world-absolute-$country.pdf $dir
  cp $shell_source_dir/../data/$country"wiki"-$version-results/table-*.tex $dir
  cp $shell_source_dir/../data/$country"wiki"-$version-results/plot-by*.pdf $dir

  cp $shell_source_dir/material/aaai17.sty $dir

  cat $shell_source_dir/material/country-template-head.tex \
    | sed "s/COUNTRY_CODE/$country/g" \
    | sed "s/COUNTRY_NAME/$country_long/g" \
    > $dir/part-$country.tex

  countries_file=$shell_source_dir/countries.txt
  for country_other in $(cat $countries_file | grep "^$country" | cut -d" " -f2);do
    country_other_lowercase=$(echo $country_other | awk '{print tolower($1)}')
    country_other_long=$(cat $countries_file | grep "^$country $country_other" | cut -d" " -f3-)
    cat $shell_source_dir/material/country-template-from.tex \
      | sed "s/COUNTRY_CODE/$country/g" \
      | sed "s/COUNTRY_NAME/$country_long/g" \
      | sed "s/COUNTRY_FROM_CODE/$country_other_lowercase/g" \
      | sed "s/COUNTRY_FROM_NAME/$country_other_long/g" \
      >> $dir/part-$country.tex
  done

  cat $shell_source_dir/material/country-template-tail.tex \
    | sed "s/COUNTRY_CODE/$country/g" \
    | sed "s/COUNTRY_NAME/$country_long/g" \
    >> $dir/part-$country.tex
  echo "\\input{part-$country}" >> $dir/$main_file_name.tex
done

cat $shell_source_dir/material/tail.tex >> $dir/$main_file_name.tex

pushd $dir
pdflatex $main_file_name
pdflatex $main_file_name
popd

cp $dir/$main_file_name.pdf $shell_source_dir/../data/supplementary-material.pdf

rm -rf $dir

