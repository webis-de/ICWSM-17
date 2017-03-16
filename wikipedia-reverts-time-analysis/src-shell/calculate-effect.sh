#!/bin/bash

#day_start=12784 # 2005
day_start=13149 # 2006

shell_source_dir=`dirname $0`
source $shell_source_dir/config.sh
source $shell_source_dir/../$geolocating_dir/src-shell/config.sh
source $shell_source_dir/../$geolocating_dir/$detection_dir/src-shell/config.sh

dir=$shell_source_dir/../data/effect
results=$dir/results.txt
rm -f $results
mkdir -p $dir

marginalize="java -cp $shell_source_dir/../../icwsm17.jar de.aitools.ie.geolocating.RevisionCountMarginalizer"
merge="java -cp $shell_source_dir/../../icwsm17.jar de.aitools.ie.geolocating.RevisionCountMerger"

function effect() {
  local name=$1
  local keya="$2"
  local keyb="$3"

  echo -en "$name\t$keya\t$keyb\t" | tee -a $results
  Rscript $shell_source_dir/../src-r/calculate-effect.r $dir/$name.txt 1 $day_start 2 "$keya" "$keyb" 3 \
    | sed 's/^[^"]*//' \
    | sed 's/"//g' \
    | tee -a $results
}

workday_without_friday="WORKDAY:(MONDAY)|(TUESDAY)|(WEDNESDAY)|(THURSDAY)"
workday_with_friday="$workday_without_friday|(FRIDAY)"
weekend_without_friday="WEEKEND:(SATURDAY)|(SUNDAY)"
weekend_with_friday="$weekend_without_friday|(FRIDAY)"
night="NIGHT:(22)|(23)|(0)|(1)|(2)|(3)|(4)|(5)|(6)|(7)"
morning="MORNING:(8)|(9)|(10)|(11)|(12)|(13)|(14)"
evening="EVENING:(15)|(16)|(17)|(18)|(19)|(20)|(21)"
day="DAY:(8)|(9)|(10)|(11)|(12)|(13)|(14)|(15)|(16)|(17)|(18)|(19)|(20)|(21)"
summer="SUMMER:SUMMER"
not_summer="NOTSUMMER:(WINTER)|(SPRING)|(FALL)"

###########################################################
### WEEKEND MORNING/EVENING
###########################################################

function weekend_morning_evening() {
  local wiki=$1
  local origin=$2

  if [ ! \( -e $dir/"$wiki"wiki-$origin-saturday-morning-evening.txt \) ];then
    echo "MAKE: "$wiki"wiki-$origin-saturday-morning-evening.txt"
    cat $shell_source_dir/../data/"$wiki"wiki-$version-results/day-counts.txt \
      | $marginalize "" "${origin^^}" ".*" "" "SATURDAY" ".*" \
      | awk -F'\t' '{print $2"\t"$4"\t"$5"\t"$6"\t"$7}' \
      | $merge 1 "$morning" "$evening" ":.*" 2 \
      > $dir/"$wiki"wiki-$origin-saturday-morning-evening.txt
  fi

  effect "$wiki"wiki-$origin-saturday-morning-evening "MORNING" "EVENING"

  if [ ! \( -e $dir/"$wiki"wiki-$origin-sunday-morning-evening.txt \) ];then
    echo "MAKE: "$wiki"wiki-$origin-sunday-morning-evening.txt"
    cat $shell_source_dir/../data/"$wiki"wiki-$version-results/day-counts.txt \
      | $marginalize "" "${origin^^}" ".*" "" "SUNDAY" ".*" \
      | awk -F'\t' '{print $2"\t"$4"\t"$5"\t"$6"\t"$7}' \
      | $merge 1 "$morning" "$evening" ":.*" 2 \
      > $dir/"$wiki"wiki-$origin-sunday-morning-evening.txt
  fi

  effect "$wiki"wiki-$origin-sunday-morning-evening "MORNING" "EVENING"

  if [ ! \( -e $dir/"$wiki"wiki-$origin-summer-morning-evening.txt \) ];then
    echo "MAKE: "$wiki"wiki-$origin-summer-morning-evening.txt"
    cat $shell_source_dir/../data/"$wiki"wiki-$version-results/day-counts.txt \
      | $marginalize "" "${origin^^}" ".*" "SUMMER" "" ".*" \
      | awk -F'\t' '{print $2"\t"$4"\t"$5"\t"$6"\t"$7}' \
      | $merge 1 "$morning" "$evening" ":.*" 2 \
      > $dir/"$wiki"wiki-$origin-summer-morning-evening.txt
  fi

  effect "$wiki"wiki-$origin-summer-morning-evening "MORNING" "EVENING"
}

weekend_morning_evening en us

weekend_morning_evening en gb
weekend_morning_evening en ca
weekend_morning_evening en in
weekend_morning_evening en au

weekend_morning_evening de de
weekend_morning_evening en de
weekend_morning_evening fr fr
weekend_morning_evening en fr

weekend_morning_evening es es
weekend_morning_evening ru ru
weekend_morning_evening it it
weekend_morning_evening ja jp


###########################################################
### DAY/NIGHT
###########################################################

function day_night() {
  local wiki=$1
  local origin=$2

  if [ ! \( -e $dir/"$wiki"wiki-$origin-day-night.txt \) ];then
    echo "MAKE: "$wiki"wiki-$origin-day-night.txt"
    cat $shell_source_dir/../data/"$wiki"wiki-$version-results/day-counts.txt \
      | $marginalize "" "${origin^^}" ".*" "" "" ".*" \
      | awk -F'\t' '{print $2"\t"$3"\t"$4"\t"$5"\t"$6}' \
      | $merge 1 "$day" "$night" ":.*" 2 \
      > $dir/"$wiki"wiki-$origin-day-night.txt
  fi

  effect "$wiki"wiki-$origin-day-night "NIGHT" "DAY"
}

day_night en us

day_night en gb
day_night en ca
day_night en in
day_night en au

day_night de de
day_night en de
day_night fr fr
day_night en fr

day_night es es
day_night ru ru
day_night it it
day_night ja jp


###########################################################
### US TIMEZONE
###########################################################

if [ ! \( -e $dir/enwiki-us-timezone.txt \) ];then
  echo "MAKE: enwiki-us-timezone.txt"
  cat $shell_source_dir/../data/enwiki-$version-results/day-counts-microsoft-timezone.txt \
    | $marginalize ".*" "" ".*" "" "" ".*" \
    | $merge 2 "$morning" 3 \
    | awk -F'\t' '{print $2"\t"$1"\t"$4"\t"$5"\t"$6}' \
    > $dir/enwiki-us-timezone.txt
fi
effect enwiki-us-timezone "Pacific" "Central"
effect enwiki-us-timezone "Pacific" "Eastern"
effect enwiki-us-timezone "Pacific" "Mountain"
effect enwiki-us-timezone "Eastern" "Central"
effect enwiki-us-timezone "Eastern" "Mountain"
effect enwiki-us-timezone "Central" "Mountain"

###########################################################
### SEASONS
###########################################################

function seasons() {
  local wiki=$1
  local origin=$2

  if [ ! \( -e $dir/"$wiki"wiki-$origin-seasons.txt \) ];then
    echo "MAKE: "$wiki"wiki-$origin-seasons.txt"
    cat $shell_source_dir/../data/"$wiki"wiki-$version-results/day-counts.txt \
      | $marginalize "" "${origin^^}" ".*" ".*" "" ".*" \
      | awk -F'\t' '{print $2"\t"$3"\t"$4"\t"$5"\t"$6"\t"$7}' \
      | $merge 2 "$day" 3 \
      | awk -F'\t' '{print $1"\t"$2"\t"$4"\t"$5"\t"$6}' \
      | $merge 1 "$summer" "$not_summer" 2 \
      > $dir/"$wiki"wiki-$origin-seasons.txt
  fi

  effect $wiki"wiki-$origin-seasons" "SUMMER" "NOTSUMMER"
}

seasons en us
seasons de de
seasons es es
seasons fr fr
seasons ru ru
seasons it it
seasons ja jp

###########################################################
### WEEKEND TO WORKDAY AT MORNING AND EVENING
###########################################################

function weekend_workday() {
  local wiki=$1
  local origin=$2

  if [ ! \( -e $dir/"$wiki"wiki-$origin-buckets.txt \) ];then
    echo "MAKE: "$wiki"wiki-$origin-buckets.txt"
    cat $shell_source_dir/../data/"$wiki"wiki-$version-results/day-counts.txt \
      | $marginalize "" "${origin^^}" ".*" "" ".*" ".*" \
      | awk -F'\t' '{print $2"\t"$3"\t"$4"\t"$5"\t"$6"\t"$7}' \
      | $merge 2 "$morning" "$evening" "$night" ":.*" 3 \
      > $dir/"$wiki"wiki-$origin-buckets.txt
  fi

  if [ ! \( -e $dir/"$wiki"wiki-$origin-morning-weekend-workday.txt \) ];then
    echo "MAKE: "$wiki"wiki-$origin-morning-weekend-workday.txt"
    cat $dir/"$wiki"wiki-$origin-buckets.txt \
      | $marginalize ".*" ".*" "MORNING" \
      | awk -F'\t' '{print $1"\t"$2"\t"$4"\t"$5"\t"$6}' \
      | $merge 1 "$workday_with_friday" "$weekend_without_friday" ":.*" 2 \
      > $dir/"$wiki"wiki-$origin-morning-weekend-workday.txt
  fi

  if [ ! \( -e $dir/"$wiki"wiki-$origin-evening-weekend-workday.txt \) ];then
    echo "MAKE: "$wiki"wiki-$origin-evening-weekend-workday.txt"
    cat $dir/"$wiki"wiki-$origin-buckets.txt \
      | $marginalize ".*" ".*" "EVENING" \
      | awk -F'\t' '{print $1"\t"$2"\t"$4"\t"$5"\t"$6}' \
      | $merge 1 "$workday_without_friday" "$weekend_with_friday" ":.*" 2 \
      > $dir/"$wiki"wiki-$origin-evening-weekend-workday.txt
  fi

  effect $wiki"wiki-$origin-morning-weekend-workday" "WEEKEND" "WORKDAY"
  effect $wiki"wiki-$origin-evening-weekend-workday" "WEEKEND" "WORKDAY"
}
weekend_workday en us

weekend_workday en gb
weekend_workday en ca
weekend_workday en in
weekend_workday en au

weekend_workday de de
weekend_workday en de
weekend_workday fr fr
weekend_workday en fr

weekend_workday es es
weekend_workday ru ru
weekend_workday it it
weekend_workday ja jp


###########################################################
### French Wednesday
###########################################################

if [ ! \( -e $dir/frwiki-french-wednesday-to-workday.txt \) ];then
  echo "MAKE: frwiki-french-wednesday-to-workday.txt"
  cat $shell_source_dir/../data/frwiki-$version-results/day-counts.txt \
    | $marginalize "" "FR" ".*" "" ".*" ".*" \
    | $merge 3 "$evening" ":.*" 4 \
    | awk -F'\t' '{print $2"\t"$3"\t"$5"\t"$6"\t"$7}' \
    > $dir/frwiki-french-wednesday-to-workday.txt
fi
effect "frwiki-french-wednesday-to-workday" "WEDNESDAY" "(MONDAY)|(TUESDAY)|(THURSDAY)"

if [ ! \( -e $dir/enwiki-french-wednesday-to-workday.txt \) ];then
  echo "MAKE: enwiki-french-wednesday-to-workday.txt"
  cat $shell_source_dir/../data/enwiki-$version-results/day-counts.txt \
    | $marginalize "" "FR" ".*" "" ".*" ".*" \
    | $merge 3 "$evening" ":.*" 4 \
    | awk -F'\t' '{print $2"\t"$3"\t"$5"\t"$6"\t"$7}' \
    > $dir/enwiki-french-wednesday-to-workday.txt
fi
effect "enwiki-french-wednesday-to-workday" "WEDNESDAY" "(MONDAY)|(TUESDAY)|(THURSDAY)"

