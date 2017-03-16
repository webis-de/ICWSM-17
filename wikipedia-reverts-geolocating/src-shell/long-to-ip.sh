#!/bin/bash

long=$1

multiplier_third=256
multiplier_second=$(( 256 * $multiplier_third ))
multiplier_first=$(( 256 * $multiplier_second ))

mod=$(( $long % $multiplier_first ))
first=$(( ( $long - $mod ) / $multiplier_first ))
long=$mod

mod=$(( $long % $multiplier_second ))
second=$(( ( $long - $mod ) / $multiplier_second ))
long=$mod

mod=$(( $long % $multiplier_third ))
third=$(( ( $long - $mod ) / $multiplier_third ))
long=$mod

fourth=$long

echo $first.$second.$third.$fourth

