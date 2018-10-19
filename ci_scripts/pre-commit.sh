#!/usr/bin/env bash
#
# Pre-commit hook for running checkstyle on changed Java sources
#
# To use this you need:
# 1. checkstyle's jar file somewhere
# 2. a checkstyle XML check file somewhere
# 3. To configure git:
#   * git config --add checkstyle.jar <location of jar>
#   * git config --add checkstyle.checkfile <location of checkfile>
#   * git config --add java.command <path to java executale> [optional
#     defaults to java assuming it's in your path]
# 4. Put this in your .git/hooks directory as pre-commit
#
# Now, when you commit, you will be disallowed from doing so
# until you pass your checkstyle checks.

changed_files=" "
for file in $(git diff --cached --name-status | grep -E '\.(java)$' | grep -vE '^D' | awk '{print $2}')
do
  changed_files+="$file "
done

printf "Using checkstyle sheet "
check_file_command='git config --get checkstyle.checkfile'
checkstlye_jar_command='git config --get checkstyle.jar'

if ! ($check_file_command || $checkstlye_jar_command)
then
  printf "You must configure checkstyle in your git config"
  exit 1
fi

checkstyle_warnings=$(java -jar $($checkstlye_jar_command) -c $($check_file_command) $changed_files | grep WARN)
if [ $? == 0 ]
then
  printf "\nWarnings found\n\n"
  echo "$checkstyle_warnings"
  printf "\n###############################################################\n\nFix warnings before committing\n\n"
  exit 1
else
  printf "\nCode checkstyle passed.\n"
fi

