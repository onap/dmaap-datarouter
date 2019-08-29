#!/bin/bash

cp build-tools/src/main/resources/commit-hooks/pre-commit .git/hooks/
cp build-tools/src/main/resources/commit-hooks/pre-push .git/hooks/
chmod ug+x .git/hooks/pre-commit
chmod ug+x .git/hooks/pre-push
