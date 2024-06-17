#!/usr/bin/env bash
set -eu

MAIN_BRANCH=origin/main

COMMIT_RANGE=${COMMIT_RANGE:-$(git merge-base ${MAIN_BRANCH} HEAD)".."}

# Go to the root of the repo
cd "$(git rev-parse --show-toplevel)"


# Bazel has a nonzero return code if the file queried isn't tracked by any BUILD files.
# Get a list of the current files in package form by querying Bazel.
files=()
for file in $(git diff --name-only "${COMMIT_RANGE}" ); do
  echo $file
  if [[ -f $file ]] && [[ -e $(bazel query "$file" 2> /dev/null) ]]; then
      files+=("$(bazel query "$file" 2> /dev/null )")
  fi
done

if [ -z ${files+x} ]; then
    exit
fi

# Query for the associated buildables
buildables=$(bazel query \
    --keep_going \
    --noshow_progress \
    "kind(.*_binary, rdeps(//..., set(${files[*]})))")
# Run the tests if there were results
if [[ ! -z $buildables ]]; then
  echo "Building binaries"
  bazel build "$buildables"
fi

tests=$(bazel query \
    --keep_going \
    --noshow_progress \
    "kind(test, rdeps(//..., set(${files[*]}))) except attr('tags', 'manual', //...)")
# Run the tests if there were results
if [[ ! -z $tests ]]; then
  echo "Running tests"
  bazel test "$tests"
fi

./gradlew build
