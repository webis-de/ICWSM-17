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
country_long=$(grep "^$country " $shell_source_dir/countries.txt | cut -d" " -f2-)

input_reverts=$dir/revert-statistics.txt
input_reverted=$dir/reverted-revisions-statistics.txt
output=$dir/table-reverts-filtering-$country.tex

function make_head() {
  echo "% Created by code-in-progress/wikipedia-reverts/wikipedia-reverts-detection/src-shell/$(basename $0)"
  echo "\\begin{table*}"
  echo "\\caption{Step-by-step filtering of the $country_long Wikipedia as per the revert patterns depicted in Figure~2 in the paper. Counts of full page reverts and counts of reverted edits affected by corresponding full page reverts are given. Full page reverts are analyzed for indications of vandalism in edit comments as per Kittur et al. (2007b), and reverted edits are divided into edits originating from editors who are anonymous, registered, or bots. Note that the approach by Kittur et al.\\ uses specific words to classify comments and is thus likely less effective in finding vandalism for other languages than English.}"
  echo "\\label{table-reverts-filtering-$country}"
  echo "\\scriptsize"
  echo "\\centering"
  echo "\\setlength{\\tabcolsep}{5.6pt}"
  echo "\\begin{tabular}{@{}lrrrrrrrrr@{}}"
  echo "\\toprule"
  echo "\\bfseries Revert filtering step & \\multicolumn{4}{c}{\\bfseries Full page reverts} & \\multicolumn{5}{c}{\\bfseries Reverted edits} \\\\[-0.5ex]"
  echo "\\cmidrule(l{\\tabcolsep}r{\\tabcolsep}){2-5}"
  echo "\\cmidrule(l{\\tabcolsep}){6-10}"
  echo "& \\multicolumn{2}{c}{Vandalism as per Kittur} & \\multicolumn{2}{c}{Total} & \\multicolumn{3}{c}{Editor} & \\multicolumn{2}{c@{}}{Total} \\\\[-0.5ex]"
  echo "\\cmidrule(l{\\tabcolsep}r{\\tabcolsep}){2-3}"
  echo "\\cmidrule(l{\\tabcolsep}r{\\tabcolsep}){4-5}"
  echo "\\cmidrule(l{\\tabcolsep}r{\\tabcolsep}){6-8}"
  echo "\\cmidrule(l{\\tabcolsep}){9-10}"
  echo "& \\multicolumn{1}{@{}c@{}}{No} & \\multicolumn{1}{@{}c@{}}{Yes} & \\multicolumn{1}{@{}c@{}}{Absolute} & \\multicolumn{1}{@{}c@{}}{Relative} & \\multicolumn{1}{@{}c@{}}{Anonymous} & \\multicolumn{1}{@{}c@{}}{Registered} & \\multicolumn{1}{@{}c@{}}{Bot} & \\multicolumn{1}{@{}c@{}}{Absolute} & \\multicolumn{1}{@{}c@{}}{Relative} \\\\[-0.5ex]"
  echo "\\midrule"
}

function get_row_names() {
  cat $input_reverts \
    | grep -v "^#" \
    | cut -f 1
}

function latex_fmt() {
  local value=$1
  local target_size=${#value}

  local pos=3
  while [ $pos -lt $target_size ];do
    local p=$(($target_size - $pos))
    value=${value:0:$p}","${value:$p}

    pos=$((pos + 3))
  done

  echo $value
}

function latex_pad() {
  local value=$1
  local target_size=$2
  local use_phantom=0

  while [ ${#value} -lt $target_size ];do
    value="0"$value
    use_phantom=1
  done

  local pos=3
  while [ $pos -lt $target_size ];do
    local p=$(($target_size - $pos))
    value=${value:0:$p}","${value:$p}

    pos=$((pos + 3))
  done

  if [ $use_phantom -eq 1 ];then
    echo $value | sed 's/^\([0,]*\)\(.\)/\\phantom{\1}\2/'
  else
    echo $value
  fi
}

function parse_values() {
  local input=$1

  if [ "$input" == "$input_reverts" ];then
    cat /dev/stdin \
      | awk -F"\t" '{print ($2-$4)" "$4" "$2}'
  else
    cat /dev/stdin \
      | awk -F"\t" '{print $3" "$4" "$5" "$2}'
  fi
}

function get_percentage() {
  local input=$1
  local value=$2

  all=$(head -2 $input | tail -1 | cut -f2)
  echo $value $all | awk '{printf "%.1f\\\%", $1/$2*100}'
}

function to_cells() {
  local input=$1
  shift
  local name=$1
  shift
  local print_diff=$1
  shift

  local values=$(cat $input | grep "^$name" | parse_values $input)
  if [ $print_diff -gt 0 ];then
    local line=$(cat $input | grep -n "^$name" | cut -d: -f1)
    local prev_line=$(($line - $print_diff))
    local prev_vals=($(head -$prev_line $input | tail -1 | parse_values $input))
    local vals=($values)
    i=0;
    while [ $i -lt ${#vals[*]} ];do
      vals[$i]=$((${prev_vals[$i]} - ${vals[$i]}))
      i=$((i+1))
    done
    values="${vals[*]}"
  fi

  local value=""
  local last_value=""
  for value in $values;do
    local padded=$(latex_fmt $value)
    last_value=$value
    if [ $print_diff -gt 0 ];then
      if [ "${value}" != "0" ];then
        echo -n " & -$padded"
      else
        echo -n " & $padded"
      fi
    else
      echo -n " & $padded"
    fi
  done

  value=$(get_percentage $input $last_value)
  if [ $print_diff -gt 0 ];then
    if [ "${value}" != "0.0\\%" ];then
      echo -n " & -$value"
    else
      echo -n " & $value"
    fi
  else
    echo -n " & $value"
  fi
}

function make_row() {
  local name=$1
  local print_name=$2
  local print_diff=$3

  echo -n "{$print_name}"

  local cells=$(to_cells $input_reverts "$name" "$print_diff")
  echo -n "$cells"
  cells=$(to_cells $input_reverted "$name" "$print_diff")
  echo "$cells \\\\"
}

function make_rows() {
  local name="All full page reverts"
  local print_name="Results of naive SHA-1 matching"
  make_row "$name" "$print_name" 0

  echo "\\midrule"

  local name="Without reverts to page blank or deleted"
  local print_name="(a) reverts to page blank"
  make_row "$name" "$print_name" 1
  local name="And without reverts without reverted"
  local print_name="(b) empty reverts due to renaming/removal/error"
  make_row "$name" "$print_name" 3

  echo "\\midrule"

  local name="And without reverts without reverted"
  local print_name="Results after filtering pseudo-reverts\\hfill \$\\Sigma\$"
  make_row "$name" "$print_name" 0

  echo "\\midrule"

  local name="And without self reverts"
  local print_name="(c) self reverts"
  make_row "$name" "$print_name" 1
  local name="Changing immediate self reverted reverts"
  local print_name="(d) revert corrections"
  make_row "$name" "$print_name" 1
  local name="And without unaccepted reverts"
  local print_name="(e) reverted reverts"
  make_row "$name" "$print_name" 1

  echo "\\midrule"

  local name="And without unaccepted reverts"
  local print_name="Results after filtering error-corrections\\hfill \$\\Sigma\$"
  make_row "$name" "$print_name" 0

  echo "\\midrule"

  local name="And without interleaved reverts"
  local print_name="(f) interleaved reverts"
  make_row "$name" "$print_name" 1
  local name="And without reverts reverting different users"
  local print_name="(g) reverts reverting more than one editor"
  make_row "$name" "$print_name" 1

  echo "\\midrule"

  local name="And without reverts reverting different users"
  local print_name="Results after filtering ambiguous reverts\\hfill \$\\Sigma\$"
  make_row "$name" "$print_name" 0 

  echo "\\midrule"
  local name="And without reverts reverting non-anonymous users"
  local print_name="(h1) reverts reverting registered editors or bots"
  make_row "$name" "$print_name" 1
  local name="And without reverts reverting ipv6 users"
  local print_name="(h2) reverts reverting editors with IPv6 addresses"
  make_row "$name" "$print_name" 1


  echo "\\midrule"

  local name="And without reverts reverting ipv6 users"
  local print_name="\\bfseries Results after all filtering steps\\hfill \$\\Sigma\$"
  make_row "$name" "$print_name" 0
}

function make_foot() {
  echo "\\bottomrule"
  echo "\\end{tabular}%"
  echo "\\vspace{2ex}%"
  echo "\\end{table*}"
}

function make_table() {
  make_head
  make_rows
  make_foot
}

make_table > $output


