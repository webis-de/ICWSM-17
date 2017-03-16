#!/bin/bash

shell_source_dir=$(dirname $0)

output_dir_name=rir
output_dir=$shell_source_dir/../data/$output_dir_name

mkdir -p $output_dir

pushd $output_dir

wget_log=../../$output_dir_name-download.log
rm -f $wget_log

function get_rir_years() {
  local dir=$1

  echo "=================== GET YEARS FROM $dir ===================" >> $wget_log
  wget -O- "$dir" 2>> $wget_log \
    | grep "<tr>" \
    | grep -o "href=\"20..\/\"" \
    | grep -o "20.."
}

function get_rir_files() {
  local dir=$1

  local tmp=/tmp/$$-download

  echo "=================== GET FILES FROM $dir ===================" >> $wget_log
  wget -O- "$dir" 2>> $wget_log \
    | grep "<tr>" \
    | grep -oE "href=\"delegated-[^-]*-(extended-)?20......(.gz)?(.bz2)?\"" \
    | grep -oE "delegated-[^-]*-(extended-)?20......(.gz)?(.bz2)?" \
    | uniq \
    > $tmp

  echo "" > $tmp-extended
  cat $tmp | grep "extended" >> $tmp-extended
  cat $tmp \
    | grep -v "extended" \
    | awk 'FILENAME == "'$tmp-extended'"{
        split($0,parts,"-")
        extended[parts[4]] = 1

        print $0
      } FILENAME == "/dev/stdin" { 
        split($0,parts,"-")
        if (parts[3] in extended) {
        } else {
          print $0
        }
      }' $tmp-extended /dev/stdin

  rm $tmp
  rm $tmp-extended
}

function get_rir_files_by_years() {
  local dir=$1

  for year in $(get_rir_years $dir);do
    get_rir_files $dir/$year | sed "s/^/$year\//"
  done
}

function download_if_needed() {
  local dir=$1

  cat /dev/stdin \
    | grep delegated \
    | while read line;do
        file=$(basename $line)
        if [ -e $(echo $file | sed 's/.gz$//' | sed 's/.bz2$//') ];then
          echo "Skipping existing $file"
        else
          echo "DOWNLOADING $file"
          echo "------------------- GET FILE $dir/$line -------------------" >> $wget_log
          wget -O $file $dir/$line 2>> $wget_log

          if [ "$file" == *.gz ];then
            echo "UNCOMPRESSING $file"
            gunzip $file
          fi
          if [ "$file" == *.bz2 ];then
            echo "UNCOMPRESSING $file"
            bunzip2 $file
          fi
        fi
      done
}

function download() {
  local server=http://ftp.$1.net/pub/stats
  local registry=$2

  mkdir -p $registry
  pushd $registry
  get_rir_files_by_years $server/$registry/archive | download_if_needed $server/$registry/archive
  get_rir_files_by_years $server/$registry | download_if_needed $server/$registry
  get_rir_files $server/$registry | download_if_needed $server/$registry
  popd
}

function download_from_server() {
  local server=$1

  download $server afrinic
  download $server apnic
  download $server arin
  download $server lacnic
  download $server ripe-ncc
}

download_from_server arin
download_from_server apnic

popd

