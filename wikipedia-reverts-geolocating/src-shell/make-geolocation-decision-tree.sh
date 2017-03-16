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
working_dir=$dir/tmp-$$
mkdir -p $working_dir
cp $shell_source_dir/decision-tree-counts/* $working_dir

input=$dir/decision-tree-counts.txt
coordinate_file=$working_dir/geolocation-decision-tree-input.tex
frame_file_name=geolocation-decision-tree.tex
compiled_file=$working_dir/geolocation-decision-tree.pdf
output=$dir/geolocation-decision-tree-$country.pdf

function latex_number() {
  local value=$1
  local length=${#value}

  local pos=3
  while [ $pos -lt $length ];do
    local p=$(($length - $pos))
    value=${value:0:$p}","${value:$p}

    pos=$((pos + 3))
  done

  echo $value
}

function replace_values() {
  local total=$(latex_number $1)
  local reverted=$(latex_number $2)
  local placeholder_letter=$3

  sed -i "s/TEXT"$placeholder_letter"1/$total/" $coordinate_file
  sed -i "s/TEXT"$placeholder_letter"2/$reverted/" $coordinate_file
}

function get_values_by_key() {
  local key=$1

  if [ "$key" == "" ];then
    head -1 $input
  else
    cat $input \
      | grep "^[[:blank:]]*$key[[:blank:]]" \
      | cut -f2-3
  fi
}

function replace_values_by_key() {
  local key=$1
  local placeholder_letter=$2

  local value=($(get_values_by_key "$key"))
  local total=${value[0]}
  local reverted=${value[1]}

  replace_values $total $reverted $placeholder_letter
}

function get_values_by_class() {
  local class=$1

  cat $input \
    | awk -F"\t" '{
        if ($NF == "'"$class"'") {
          sum_total += $2
          sum_reverted += $3
        }
      } END {
        print sum_total"\t"sum_reverted
      }'
}

function replace_values_by_class() {
  local class=$1
  local placeholder_letter=$2

  local value=($(get_values_by_class "$class"))
  local total=${value[0]}
  local reverted=${value[1]}

  replace_values $total $reverted $placeholder_letter
}

function replace_all() {
  replace_values_by_key "" A
  replace_values_by_key "RIR = true" B
  replace_values_by_key "RIR = false" C
  replace_values_by_key "IPlocation = true" D
  replace_values_by_key "IPlocation = false" E
  replace_values_by_key "inconsistent = false" F
  replace_values_by_key "inconsistent = true" G
  replace_values_by_key "1 time zone = true" N
  replace_values_by_key "1 time zone = false" H
  replace_values_by_key "time zone consistent = true" I
  replace_values_by_key "time zone consistent = false" J
  replace_values_by_key "locally time zone consistent = true" K
  replace_values_by_key "locally time zone consistent = false" L
  replace_values_by_class "true" M
  replace_values_by_class "false" O
}

replace_all
pushd $working_dir
pdflatex $frame_file_name
pdflatex $frame_file_name
popd
cp $compiled_file $output
rm -rf $working_dir


