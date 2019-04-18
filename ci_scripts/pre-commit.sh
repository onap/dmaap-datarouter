#!/usr/bin/env bash
#
# Pre-commit hook for running checkstyle on changed Java sources
#
# To use this you need:
# 1. Checkstyle's jar file downloaded *version is important checkstyle-8.13-all.jar
# 2. To configure git:
#   * git config --add checkstyle.jar <location_of_jar>
# 3. Copy this file to your .git/hooks directory as pre-commit
#
# Now, when you commit, you will be disallowed from doing so
# until you pass your checkstyle checks.

changed_files=" "
for file in $(git diff --cached --name-status | grep -E '\.(java)$' | grep -vE '^D' | awk '{print $2}')
do
  changed_files+="$file "
done

printf "Using checkstyle sheet "
checkstlye_jar_command='git config --get checkstyle.jar'

if ! ($checkstlye_jar_command)
then
  printf "You must configure checkstyle in your git config"
  exit 1
fi

checkstyle_warnings=$(java -jar $($checkstlye_jar_command) -c ci_scripts/onap-style-java.xml $changed_files | grep WARN)
if [ $? == 0 ]
then
  printf "\nWarnings found\n\n"
  echo "$checkstyle_warnings"
  printf "\n###############################################################\n\nFix warnings before committing\n\n"
  exit 1
else
  printf "\nCode checkstyle passed.\n"
fi
