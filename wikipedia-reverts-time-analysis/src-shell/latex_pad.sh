#!/bin/bash

numbers=$1

cat /dev/stdin \
  | awk -F'&' '{
      printf $1
      for (f = 2; f <= NF; ++f) {
        value = $f
        value = sprintf("%0'$numbers'd", value)

        pos = 4
        while (pos < '$numbers') {
          p = '$numbers' - pos
          value = substr(value, 1, p+1)","substr(value, p+2 ,length(value))
          pos += 3
        }

        sub(/^[0,]+/, "\\phantom{&}", value)

        printf " & "value
      }
      print " \\\\"
    }'

