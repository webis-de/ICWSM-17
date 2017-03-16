#!/bin/bash

shell_source_dir=`dirname $0`
output=$shell_source_dir/../resources/de/aitools/aq/wikipedia/bots.txt

# Downloads list of bots from Wikipedia.

function curl_table_files() {
  # 1) Curated (but outdated) list of bot accounts
  # 2-3) Curated (but outdated) list of inactive bot accounts
  curl \
    "https://en.wikipedia.org/wiki/Wikipedia:Bots/Status" \
    "https://en.wikipedia.org/wiki/Wikipedia:Bots/Status/inactive_bots_1" \
    "https://en.wikipedia.org/wiki/Wikipedia:Bots/Status/inactive_bots_2"
}

function curl_list_files() {
  # 1) List of users currently in group "bot" over all wikidata wikis
  # 2) List of users currently in group "bot" in the English Wikipedia
  wget -O - "https://meta.wikimedia.org/w/index.php?title=Special:GlobalUsers&offset=&limit=500&username=&group=global-bot" 2> /dev/zero
  wget -O - "https://en.wikipedia.org/w/index.php?title=Special%3AListUsers&username=&group=bot&limit=1000" 2> /dev/zero
}

function extract_table() {
  cat /dev/stdin \
    | awk '{
        if ($0 == "<tr>") {
          take = 1
        } else if (take == 1) {
          print $0
          take = 0
        }
      }' \
    | grep -o "title=\"User:[^\"]*" \
    | sed 's/ (page does not exist)//' \
    | cut -d: -f 2-
}

function extract_list() {
  cat /dev/stdin \
    | grep -o "title=\"User:[^\"]*" \
    | sed 's/ (page does not exist)//' \
    | cut -d: -f 2-
}

function get_all() {
  curl_table_files | extract_table
  curl_list_files  | extract_list
}

function post_process() {
  cat /dev/stdin \
    | perl -MURI::Escape -e 'print uri_unescape(<>)' \
    | sort \
    | uniq
}

get_all \
  | post_process \
  > $output

