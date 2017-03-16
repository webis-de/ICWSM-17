MOP   WIKIPEDIA REVERTS GEOLOCATING README.
DATE. March 15th, 2017.


REQUIREMENTS.

  - You need the following directory structure:
    src-shell/
      <files from src-shell next to this readme>
    src-r/
      <files from src-r next to this readme>
    ../icwsm17.jar
      See wikipedia-reverts-time-analysis/README.txt

USED RESSOURCES.

  - See aitools4-aq-geolocation [https://github.com/webis-de/aitools4-aq-geolocation]

GETTING THE WIKIPEDIA DATA.

  - See wikipedia-reverts-detection


GETTING THE IPLOCATION DATA.

  - Current storage of data (Webis group only. Please ask johannes.kiesel@uni-weimar.de for access): webis62:/media/storage5/dogu3912/wikipedia-reverts/wikipedia-reverts-geolocating/data/iplocation
  - Put the iplocation data into data/iplocation. Files should be named (YYYYMM is four-digit year, two-digit month): [^-]*-YYYYMM-[^-]*.csv
     - ipligence
        Data we bought.
        Format:
        One IP-range block per line. They seem to be consecutive.
        "<ip-range-start>","<ip-range-end-inclusive>","<country-short>","<country-long>","<continent-short>","<continent-long>","<timezone>","<region-code>","<region-name>","<owner>","<owner-city-name>","<owner-country-name>","<latitude>","<longitude>"
        Where:
          - IPs are encoded IPv4 by AAA.BBB.CCC.DDD -> AAA*256^3 + BBB*256^2 + CCC*256 + DDD with leading zeroes.
          - <country-short> is the 2-letter country code of the country where the IPs are allocated.
          - <timezone> is in GMT+XX.
          - <owner> might be "NOT ALLOCATED", in which case the line contains no information other than the start and end.

      - ip2location
        Data we downloaded for free from https://lite.ip2location.com/database-ip-country-region-city-latitude-longitude-zipcode-timezone (said to have lower accuracy)
        Format:
        One IP-range block per line. They seem to be consecutive.
        "<ip-range-start>","<ip-range-end-inclusive>","<country-short>","<country-long>","<region-name>","<city-name>","<latitude>","<longitude>","<zip-code>","<timezone>"
  - Run src-shell/parse-iplocation.sh
      - Output in data/iplocation-parsed
      - Output can be deserialized with de.aitools.aq.geolocating.iplocations.IplocationIpBlocks#deserializeAll(new File("data/iplocation-parsed"))

  - This uses the longitude/latitude for finding the time zone and checks with the given country.
      - For IPligence, some latitude have the wrong sign, but the country is right (cross-checked with ip2location data). The code uses hard-coded bug-fixes.
      - If the right country is wihin 1 degree of the longitude/latitude, the code prefers time zones from the right country.
      - IPligence uses the longitude/latitude 0/0 if it has no idea... and for example puts the "TKS TELEPOST KABEL-SERVICE KAISERSLAUTERN GMBH CO. KG" into the US (PST-2)


GETTING THE RIR DATA.

  - Current storage of data (Webis group only. Please ask johannes.kiesel@uni-weimar.de for access): webis62:/media/storage5/dogu3912/wikipedia-reverts/wikipedia-reverts-geolocating/data/rir
  - Put the data (directories afrinic, apnic, arin, lacnin, ripe-ncc) into data/rir
    Format:
      - Comment lines start with #
      - 1 Header line: last field is offset of the RIR from UTC (in usual timezone offset notation)
      - 3 Summary lines
      - One IP-range block per further line. The seem to be consecutive. All information is only on the *last* allocation or assignment to that IP block. We thus need all files and not only the last one.
        <registry>|<country-code>|<protocol>|<ip-range-start>|<ip-range-size>|<date-of-allocation-or-assignment>|<status>[|extension]
        Where:
          - <date-of-allocation-or-assignment> is the date of the allocation or assignment in YYYYMMDD
      - The same allocation/assignment (i.e., same IP block at the <same date-of-allocation-or-assignment>) can have different <country-code> values in different files. Currently unsure why. The current implementation saves all different country codes.
      - RIR data contains special country codes:
          - EU: Europe
          - AP: Asia/Pacific (rare)
          - UK: United Kingodm (frequently)
          - ZZ: Unknown
          - ...?
  - Update with new data (if available): src-shell/download-rir.sh .
  - Run src-shell/parse-rir.sh .
      - Output in data/rir-parsed/rir.txt .
      - Output can be deserialized with de.aitools.aq.geolocating.rir.RirIpBlocks#deserialize(new File("data/rir-parsed/rir.txt")) .


GEOLOCATE WIKIPEDIA EDITS.

   - Requirements: WIKIPEDIA DATA, IPLOCATION DATA, RIR DATA.
   - Run src-shell/geolocate-wikipedia-pages.sh all
   - Output: data/
   - XML can be deserialized using de.aitools.aq.geolocating.wikipedia.GeolocatedPageUnmarshaller .


[end]

