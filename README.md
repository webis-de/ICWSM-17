ICWSM17 - Spatio-temporal Analysis of Reverted Wikipedia Edits
==============================================================

Supplementary material to and source code for running the analysis from the paper "Spatio-temporal Analysis of Reverted Wikipedia Edits" by Johannes Kiesel, Martin Potthast, Matthias Hagen, and Benno Stein (published at ICWSM17).

The supplementary material shows the analysis of the paper for more variants of Wikipedia and with more countries of origin for the edits.

The code is split in three parts, corresponding to Sections 3 (wikipedia-reverts-detection), 4 (wikipedia-reverts-geolocating), and 5 (wikipedia-reverts-time-analysis) of the paper. The code for each section depends on the code of the previous sections. There is a README in each subfolder that details the necessary steps to reproduce the results. Note that this makes heavy use of shell scripts, so Linux is required. Generally, R is used for plotting. The second step (wikipedia-reverts-geolocating) also depends on our [geolocation library](http://doi.org/10.5281/zenodo.398832). For ease of use, we also provide the [compiled binaries](http://doi.org/10.5281/zenodo.400243) including all (Java) dependencies. See the READMEs for more information.


