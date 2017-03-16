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
source $shell_source_dir/../$detection_dir/src-shell/config.sh
wiki=$country"wiki"
country_long=$(grep "^$country " $shell_source_dir/../$detection_dir/src-shell/countries.txt | cut -d" " -f2-)

dir=$shell_source_dir/../data/$wiki-$version-results

input=$dir/decision-tree-counts.txt
output=$dir/table-geolocation-overview-$country.tex

function make_head() {
  echo "% Created by code-in-progress/wikipedia-reverts/wikipedia-reverts-geolocating/src-shell/$(basename $0)"
  echo "\\begin{table}"
  echo "\\caption{Historic geolocation success for all anonymous editors of the $country_long Wikipedia in terms of edits and unique IP addresses whence they originated. Aside the totals, the subset of edits considered vandalism or damaging as per Section~3 of the paper are given, and their corresponding IP addresses. Numbers are given for each exit node of the decision tree in the Figure above, divided by whether or not the geolocation is trustworthy.}"
  echo "\\label{table-geolocation-overview-$country}"
  echo "\\small"
  echo "\\centering"
  echo "\\setlength{\\tabcolsep}{3pt}"
  echo "\\begin{tabular}{@{}ll@{\\hspace{6pt}}c@{}ccc@{}}"
  echo "\\toprule"
  echo "\\multicolumn{2}{@{}c@{\\hspace{6pt}}}{\\bfseries Decision Tree} & \\multicolumn{2}{c}{\\bfseries Edits} & \\multicolumn{2}{c@{}}{\\bfseries Unique IP addresses} \\\\"
  echo "\\cmidrule(r{1.75\\tabcolsep}){1-2}"
  echo "\\cmidrule(r{\\tabcolsep}){3-4}"
  echo "\\cmidrule(l{\\tabcolsep}){5-6}"
  echo "Trusted & Exit Step & Vandalism as per Sec.\,3 & Total & Vandal IPs & Total \\\\"
  echo "\\midrule"

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

function get_values_by_key() {
  local key=$1

  if [ "$key" == "" ];then
    head -1 $input \
      | awk -F'\t' '{
          printf "%d %d %d %d %d", $2, $2/$1*100, $1, $4, $3
        }'
  else
    cat $input \
      | grep "^[[:blank:]]*$key[[:blank:]]" \
      | awk -F'\t' '{
          printf "%d %d %d %d %d", $3, $3/$2*100, $2, $5, $4
        }'
  fi
}

function make_row() {
  local used=$1
  local print_used=$2
  local last_step=$3

  if [ $print_used -eq 1 ];then
    case $used in
      "Total")
        echo -n "\\multicolumn{2}{@{}l@{}}{\\em Entire Wikipedia}"
        ;;
      "Yes")
        echo -n "$used \\includegraphics[scale=0.5]{../icon-accepted}"
        ;;
      "No")
        echo -n "$used \\includegraphics[scale=0.5]{../icon-rejected}"
        ;;
    esac
  fi
  if [ "$used" != "Total" ];then
    echo -n " & "
    if [ "$last_step" == "Total" ];then
      echo -n "$\\sum$"
    else
      echo -n "Step ($last_step)"
    fi
  fi

  local name=""
  case $used in
    "Yes")
      case $last_step in
        "Total")
          name="Final"
          ;;
        "4")
          name="time zone consistent = true"
          ;;
        "5")
          name="locally time zone consistent = true"
          ;;
        "6")
          name="1 time zone = true"
          ;;
      esac
      ;;
    "No")
      case $last_step in
        "Total")
          name="Rejected"
          ;;
        "1")
          name="RIR = false"
          ;;
        "3")
          name="inconsistent = true"
          ;;
        "5")
          name="locally time zone consistent = false"
          ;;
        "6")
          name="1 time zone = false"
          ;;
      esac
      ;;
    "Total")
      name=""
      ;;
  esac

  local values=($(get_values_by_key "$name"))
  echo -n " & $(latex_pad ${values[0]} 8)"
  echo -n "\\ \\ \\ (${values[1]}\\%)"
  echo -n " & $(latex_pad ${values[2]} 9)"
  echo -n " & $(latex_pad ${values[3]} 8)"
  echo -n " & $(latex_pad ${values[4]} 8)"

  echo " \\\\"
}

function make_rows() {
  make_row Total 1 Total
  echo "\\midrule"
  make_row No 1 1
  make_row No 0 3
  make_row No 0 5
  make_row No 0 6
  echo "\\cmidrule(l{\\tabcolsep}){2-6}"
  make_row No 0 Total
  echo "\\midrule"
  make_row Yes 1 4
  make_row Yes 0 5
  make_row Yes 0 6
  echo "\\cmidrule(l{\\tabcolsep}){2-6}"
  make_row Yes 0 Total
}

function make_foot() {
  echo "\\bottomrule"
  echo "\\end{tabular}"
  echo "\\end{table}"
}

function make_table() {
  make_head
  make_rows
  make_foot
}

make_table > $output


