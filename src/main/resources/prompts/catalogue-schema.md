Tables of the advisories list, read only. Dialect: H2, close to standard SQL.

advisories(cve VARCHAR PRIMARY KEY, severity VARCHAR, verdict VARCHAR, fix_owner VARCHAR, vaadin_advisory BOOLEAN, in_current_release BOOLEAN)
  One row per advisory the team tracks.
  cve: the advisory id, for example 'CVE-2026-89407'. Every query selects advisories.cve first, aliased "Advisory".
  severity: one of 'critical', 'high', 'moderate', 'low', or NULL when not rated.
  verdict: the team's verdict over all lines, one of 'in_triage' (not assessed yet), 'risk' (a real risk), 'configuration' (a risk only in some configurations), 'no_risk', 'not_affected', 'false_positive', 'fixed', 'not_current', or NULL.
  fix_owner: whose fix it is, for example 'vaadin' or 'upstream', or NULL.
  vaadin_advisory: TRUE when Vaadin published the advisory itself.
  in_current_release: TRUE when the advisory is found in a current release of some line.

packages(cve VARCHAR, package VARCHAR)
  The vulnerable packages of each advisory, for example 'org.springframework:spring-web'.

modules(cve VARCHAR, module VARCHAR)
  The Vaadin modules that bring the vulnerable package in, for example 'vaadin-spring-boot-starter'.

fixes(cve VARCHAR, line VARCHAR, fixed_in VARCHAR, fixed_in_prerelease VARCHAR)
  The release of a maintained line that fixes an advisory. line is a minor such as '24.9'; fixed_in is the release, such as '24.9.22'.
  An advisory with no row for a line has no fix published on that line. That alone does not say the advisory affects the line.

lines(line VARCHAR PRIMARY KEY, newest_release VARCHAR)
  The maintained lines and the newest measured release of each, for example '24.9' and '24.9.26'.

Reading common asks:
  "still open" or "unfixed" on a line: verdict IN ('risk', 'in_triage') and no row in fixes for that line.
  "on 24.9" names the line '24.9'.
