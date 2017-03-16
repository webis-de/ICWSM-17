#!/bin/bash

shell_source_dir=$(dirname $0)
source $shell_source_dir/config.sh
source $shell_source_dir/../$detection_dir/src-shell/config.sh
wikis_file=$shell_source_dir/../$detection_dir/src-shell/countries.txt


table_heads="version\tsize in GB (zipped)\tpages\tedits\tarticles\tarticle edits\tanonymous article edits\treverted article edits\tanonymous reverted article edits\treverted-due-to-vandalism article edits\tanonymous reverted-due-to-vandalism article edits\tanonymous geolocated article edits\tanonymous geolocated reverted-due-to-vandalism article edits"

function get_dump_counts() {
  local wiki_input_dir=$1

  cat $wiki_input_dir/*.xml.gz \
    | gunzip \
    | awk '$1 == "<page>" {
       pages += 1
      } $1 == "<revision>" {
        revisions += 1
      } $1 == "<ns>0</ns>" {
        articles += 1
      } END {
        print pages" "revisions" "articles
      }'

}


function make_for_each_wiki() {
  for wiki in $(cat $wikis_file | cut -d" " -f1);do
    local wiki_name=$(cat $wikis_file | grep "^$wiki" | cut -d" " -f2)
    local wiki_input_dir=$shell_source_dir/../$detection_dir/data/"$wiki"wiki-$version
    local wiki_results_dir=$wiki_input_dir-results
    local wiki_geolocation_file=$shell_source_dir/../data/"$wiki"wiki-$version-results/decision-tree-counts.txt

    local dump_counts=($(get_dump_counts $wiki_input_dir))

    local size=$(du -hs $wiki_input_dir | cut -f1)
    local pages=${dump_counts[0]}
    local edits=${dump_counts[1]}
    local articles=${dump_counts[2]}
    local article_edits=$(cat $wiki_results_dir/revision-statistics.txt | awk -F'\t' '$1 == "All revisions" {print $2}')
    local anonymous_article_edits=$(head -1 $wiki_geolocation_file | cut -f1)
    local reverted_article_edits=$(cat $wiki_results_dir/reverted-revisions-statistics.txt | grep -v "^#" | head -1 | cut -f2)
    local anonymous_reverted_article_edits=$(cat $wiki_results_dir/reverted-revisions-statistics.txt | grep -v "^#" | head -1 | cut -f3)
    local reverted_vandalism_article_edits=$(cat $wiki_results_dir/reverted-revisions-statistics.txt | grep "^And without reverts reverting different users" | cut -f2)
    local anonymous_reverted_vandalism_article_edits=$(cat $wiki_results_dir/reverted-revisions-statistics.txt | grep "^And without reverts reverting different users" | cut -f3)
    local geolocated_edits=$(cat $wiki_geolocation_file | grep "Final" | cut -f2)
    local geolocated_vandalism_edits=$(cat $wiki_geolocation_file | grep "Final" | cut -f3)

    echo -e "$wiki_name\t$size\t$pages\t$edits\t$articles\t$article_edits\t$anonymous_article_edits\t$reverted_article_edits\t$anonymous_reverted_article_edits\t$reverted_vandalism_article_edits\t$anonymous_reverted_vandalism_article_edits\t$geolocated_edits\t$geolocated_vandalism_edits"
  done
}

function make_body() {
  make_for_each_wiki \
    | awk -F'\t' '{
        numf = NF
        printf $1
        for (f = 2; f <= NF; ++f) {
          sums[f] = sums[f] + $f
          printf "\t%'"'"'d", $f
        }
        printf "\n"
      } END {
        print ""
        printf "Sum"
        for (f = 2; f <= numf; ++f) {
          printf "\t%'"'"'d", sums[f]
        }
        printf "\n"
      }'
}

function make_table() {
  echo -e $table_heads
  echo ""
  make_body
}

function make_table_padded() {
  make_table \
    | awk -F'\t' '{
        numf = NF
        numr = NR
        for (f = 1; f <= NF; ++f) {
          values[NR" "f] = $f
          l = length($f)
          if (l > lengths[f]) {
            lengths[f] = l
          }
        }
      } END {
        for (r = 1; r <= numr; ++r) {
          for (f = 1; f <= numf; ++f) {
            value = values[r" "f]
            l = length(value)
            spacing = ""
            while (length(spacing) < lengths[f] - l) {
              spacing = spacing" "
            }
            if (f == 1) {
              printf value""spacing
            } else {
              printf "\t"spacing""value
            }
          }
          printf "\n"
        }
      }'
}

make_table_padded > $shell_source_dir/../data/counts.txt


