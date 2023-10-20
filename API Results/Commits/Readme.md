## Weekly Commit
https://api.github.com/repos/scaleracademy/Scaler-OpenQuest-An-Open-Source-Hackathon/stats/code_frequency


1. Title/ Code frequency stat
2. Number of additions
3. No. of deletions

## Yearly Commit
https://api.github.com/repos/scaleracademy/Scaler-OpenQuest-An-Open-Source-Hackathon/stats/commit_activity

1. Days --> Array of each days and no. of contributions in that.
2. Total --> total commits in that week
3. Week: Week no. based on unix calendar

## Weekly Contributor Commits
https://api.github.com/repos/scaleracademy/Scaler-OpenQuest-An-Open-Source-Hackathon/stats/contributors


Returns the total number of commits authored by the contributor. In addition, the response includes a Weekly Hash (weeks
array) with the following information:

* w - Start of the week, given as a Unix timestamp.
* a - Number of additions
* d - Number of deletions
* c - Number of commits

Apart from this, this also returns:

* Author username
* Author ID
* Node ID
* HTML URL --> we can put this on the dashboard if someone wants to see the contributors github page.

## Repository Clones
https://api.github.com/repos/nisarrg/cufflinks/traffic/clones

## Top 10 Most Popular Contents Over 14 Days
https://api.github.com/repos/nisarrg/Web-Development/traffic/popular/paths

*  PATH and Title
* COUNT --> No.of changes
* Uniques: No. of Unique Changes

## Top 10 Referrers Over 14 Days

https://api.github.com/repos/nisarrg/Web-Development/traffic/popular/referrers

* Referrer
* COUNT 
* Unique

## Views on Repo
https://api.github.com/repos/nisarrg/Web-Development/traffic/views

Get the total number of views and breakdown per day or week for the last 14 days. Timestamps are aligned to UTC midnight of the beginning of the day or week. Week begins on Monday.

* COUNT
* Uniques
* Views:
  * Timestamp
  * Count
  * Uniques

