MOP   WIKIPEDIA REVERTS TIME ANALYSIS README.
DATE. March 16th, 2017.


REQUIREMENTS.

  - You need the following directory structure:
    src-shell/
      <files from src-shell next to this readme>
    src-r/
      <files from src-r next to this readme>
    resources/
      <files from resources next to this readme>
    ../icwsm17.jar
      Java JAR file containing compiled binaries from wikipedia-reverts-detection, wikipedia-reverts-geolocating, and wikipedia-reverts-time-analysis, as well as aitools4-aq-geolocation with depending libraries [https://github.com/webis-de/aitools4-aq-geolocation].

  - Rscript
    - library maptools

GETTING THE WIKIPEDIA DATA.

  - See wikipedia-reverts-geolocation

ANALYSIS.

   - Run
     src-shell/bucket-wikipedia-revisions.sh all
     src-shell/analyze.sh all
   - Output: data/<country>wiki-<version>-results

   - Run: src-shell/calculate-effect.sh
   - Output: data/effect/results.txt


[end]
