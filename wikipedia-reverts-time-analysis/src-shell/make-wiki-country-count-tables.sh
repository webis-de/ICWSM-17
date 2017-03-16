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
source $shell_source_dir/../$geolocating_dir/src-shell/config.sh
source $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/config.sh


if [ "$country" == "all" ];then
  countries=$(cat $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f1)
  for country in $countries;do
    ./$0 $country
  done
else
  country_long=$(grep "^$country " $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d" " -f2-)
  country_index=$(grep -n "^$country " $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt | cut -d":" -f1)

  function get_input_files() {
    cat $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/countries.txt \
      | cut -d" " -f1 \
      | while read country;do
          echo -n $shell_source_dir/../data/"$country"wiki-$version-results/by-country.txt" "
        done
  }

  function make_tabular() {
    local data_column=$1

    local sort_column=$(($country_index + 1))
    local keysep=12pt

    cat $shell_source_dir/countries.txt  \
      | grep "^$country " \
      | cut -d' ' -f 2- \
      | LC_ALL=en_US.UTF-8 awk '{
          if (FILENAME == "/dev/stdin") {
            country_name = $2
            for (f = 3; f <= NF; ++f) {
              country_name = country_name" "$f
            }
            country_names[$1] = country_name
          } else {
            if (filename != FILENAME) {
              num_parts = split(FILENAME, parts, "/")
              dir = parts[num_parts - 1]
              split(dir, dir_parts, "-")
              wiki = dir_parts[1]
              if (wiki_list != "") {
                wiki_list = wiki_list":"
              }
              wiki_list = wiki_list""wiki
              filename = FILENAME
            }

            values[wiki" "$1] = $'$data_column'
          }
        } END {
          num_wikis = split(wiki_list, wikis, ":")

          printf "\\begin{tabular}{@{}l@{\\hspace{'$keysep'}}"
          for (w = 1; w <= num_wikis; ++w) {
            printf "c"
          }
          print "@{}}"
          print "\\toprule"
          print " & \\multicolumn{"num_wikis"}{c}{Wikipedia} \\\\"
          print "\\cmidrule{2-"(num_wikis+1)"}"
          printf "Country "
          for (wiki in wikis) {
            printf "& {"
            if (wiki + 1 == '$sort_column') {
              printf "\\bf "
            }
            printf wikis[wiki]"} "
          }
          print "\\\\"
          print "\\midrule"

          cmd_sort = "sort -r -s -t \"&\" -n -k '$sort_column' | head -'$num_entries_in_country_count_tables' | '"$shell_source_dir"'/latex_pad.sh 8"
          for (country in country_names) {
            country_name = country_names[country]
            printf country_name" " | cmd_sort
            for (wiki in wikis) {
              value = values[wikis[wiki]" "country] * 1
              printf "& "value" " | cmd_sort
            }
            print "\\\\" | cmd_sort
          }
          system("sleep 1")
          close(cmd_sort)

          print "\\bottomrule"
          printf "\\end{tabular}"
        }' /dev/stdin $(get_input_files)
  }

  function make_table() {
    local data_column=$1
    local content=$2

    output=$shell_source_dir/../data/$country"wiki"-$version-results/table-wiki-country-count-$content-by-$country.tex
    echo "\\begin{table}[H]" > $output
    echo "\\caption{Number of $(echo $content | sed 's/revisions/edits/g' | sed 's/-/ /g') by country, sorted for the $country_long Wikipedia.}" >> $output
    echo "\\label{table-wiki-country-count-$content-by-$country}" >> $output
    echo "\\small" >> $output
    echo "\\setlength{\\tabcolsep}{4pt}" >> $output
    echo "\\centering" >> $output
    make_tabular $data_column >> $output
    echo "\\end{table}" >> $output
  }

  function make_tables() {
    make_table 2 "revisions"
    make_table 3 "vandalism-revisions"
    make_table 4 "vandalism-commented-revisions"
  }

  make_tables
fi


