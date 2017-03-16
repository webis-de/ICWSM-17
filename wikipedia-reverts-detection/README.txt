MOP   WIKIPEDIA REVERTS DETECTION README.
DATE. March 15th, 2017.


REQUIREMENTS.

  - You need the following directory structure:
    src-shell/
      <files from src-shell next to this readme>
    src-r/
      <files from src-r next to this readme>
    ../icwsm17.jar
      See wikipedia-reverts-time-analysis/README.txt
  - Rscript

QUICKSTART.

  - Edit src-shell/config.sh
      - Change the version (Wikipedia history dump version) to the one you want to work with.
      - Make sure that the dumps in https://dumps.wikimedia.org/enwiki/<version> still use the same xml schema version (attribute xsi:schemaLocation) that is specified as schema in the config.sh . If not, update the config.sh, download the schema (check xsi:schemaLocation) and put it into data/. Then re-generate the Java classes for the schema: (1) delete src/de/aitools/aq/wikipedia/xml and (2) run src-shell/generate-jaxb-classes.sh
  - Make sure the <country> you want to use is specified in src-shell/countries.txt . 
      - <country> is a Wikipedia country code like "en", "de", "fr", "es", ...
      - The publication uses all Wikipedias with more than 50,000,000 edits according to https://en.wikipedia.org/wiki/List_of_Wikipedias#Notes . As of September 21st, 2016, those were: en, de, fr, es, ru, it, and ja.
  - From the command line, run either
      - ./src-shell/extract-reverts.sh <country>
    or
      - ./src-shell/extract-reverts.sh all
  - Find results in data/<country>wiki-<version>-results


DOCUMENTATION.

  - You can update the bot-list in resources/de/aitools/aq/wikipedia/bots.txt by using src-shell/download-bot-names.sh .

  - Run src-shell/extract-reverts.sh all . This will take some hours.
  - It will first download the history stub dumps if they are not yet present.
  - If you already gathered the history stub dumps, place them under  data/<country>wiki-<version>, e.g., data/enwiki-20160501/enwiki-20160501-stub-meta-history1.xml.gz .
  - It will then run src-shell/analyze-interleaved.sh ,  src-shell/analyze-enclosing.sh , src-shell/analyze-self-corrected-updates.sh , src-shell/analyze-revert-lengths.sh , src-r/plot-double-submissions.r , and src-shell/make-filtering-table.sh .
  - All output will be in data/<country>wiki-<version>-results .
    - revision-statistics.txt
      Statistics on all revisions in the dump.
    - revert-statistics.txt
      Statistics on reverts (count after several cleaning steps):
      <step-name>TAB<num-reverts>TAB<num-reverts-with-revert-comment>TAB<num-reverts-with-vandalism-comment>
    - reverted-revisions-statistics.txt
      Statistics on reverted revisions (count after several cleaning steps):
      <step-name>TAB<num-reverted-revisions>TAB<num-reverted-revisions-by-anonymous-user>TAB<num-reverted-revisions-by-registered-and-not-deleted-users>TAB<num-reverted-revisions-by-deleted-users>
    - double-submission-time-spans-in-seconds.txt
      For each "revert" that occurred due to submitting twice, this file contains one line that contains the number of seconds between the submits.
    - double-submission-time-spans-in-seconds-<country>.pdf
      Cumulative distribution of double sumission time spans for the first minutes.
    - interleaved-chains*.txt
      A representation of all interleaved reverts (e.g., edit wars; not part of the final reverts).
      Each revision is depicted as <user>:<hash>:<revert-start> where
        <user> Represents the user that did the revision. It is either A<number>, R<number>, or D for an anonymous, a registered, or a deleted user. Same numbers <=> same user. 
        <hash> Representing the sha1 hash of the revision text. It is either a number (same numbers <=> same hash) or "_" if the revision was neither a revert nor target of a revert.
        <revert-start> Is the index of the revision in the line to which this revision reverts to (starting with index 0).
      - interleaved-chains.txt
        After removing self-reverts.
      - interleaved-chains-after-removing-unaccepted.txt
        After removing unaccepted reverts.
    - interleaved-chains-after-removing-unaccepted-statistics.txt
      Statistics on the interleaved reverts and chains, calculated from interleaved-chains-after-removing-unaccepted.txt .
      "2 texts" means that the chain is a back-and-forth between two texts (with possibly some other texts in between, but the article is never reverted to them)
    - enclosing-chains*.txt
      A representation of all reverts that contain other reverts (and are not part of the final reverts).
      The format is the same as interleaved-chains*.txt .
      - enclosing-chains.txt
        After removing self-reverts.
      - enclosing-chains-after-removing-unaccepted.txt
        After removing unaccepted reverts.
      - enclosing-chains-after-removing-interleaved.txt
        After removing interleaved reverts.
    - enclosing-chains-after-removing-interleaved-statistics.txt
      Statistics on the enclosing reverts and chains, calculated from enclosing-chains-after-removing-interleaved.txt .
      "2 texts" means that all reverts of a chain target one of two texts
      "2 reverts" means that the chain contains exactly two reverts
    - <country>wiki-<version>-stub-meta-history<part-number>.xml.gz
      The revisions of the history dump for further processing. You can parse them again using de.aitools.aq.wikipedia.reverts.RevertPageUnmarshaller . The revisions are annotated on whether they are reverted and, if so, whether they are the first revision reverted by the revert.
    - reverts.xml.gz
      The final reverts. You can parse them again using de.aitools.aq.wikipedia.reverts.RevertUnmarshaller .
    - reverts-*.xml.gz
      Reverts filtered out at the specific step denoted by the * part of the filename.
    - reverts-self-corrected-updates.xml.gz
      Reverts removed and added when changing self corrections.
      Format:
        <revert-removed-1>
        <revert-removed-2>
        ...
        <revert-removed-n>
        <revert-added>
        <empty-line>
    - reverts-self-corrected-updates-statistics.txt
      Calculated by analyze-self-corrected-updates.sh from reverts-self-corrected-updates.xml.gz .
      <value-name>[TAB]<value>
    - revert-lengths.txt
      Lengths of the final reverts with format:
        <length> <number-of-reverts-with-this-length>
      The length is the number of edits that are reverted by the revert.
    - revert-lengths-<country>.pdf
      A plot of revert-lengths.txt .
    - table-reverts-filtering-<country>.tex
      Latex table listing how many reverts and reverted revisions are filtered in each step.

  - You can look up the article name and URL for a pageId using https://<country>.wikipedia.org/w/api.php?action=query&prop=info&inprop=url&pageids=<pageId>


[end]

