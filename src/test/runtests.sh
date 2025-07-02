#!/bin/bash

set -o nounset
set -o errexit

rm -rf logs
mkdir logs
mkdir logs/output
cp input/colortest.ods logs/output

declare -a testList=("brackets" "looptest" "subroutines" "filetest" "spreadsheet" "colortest" "misc" "ocrtest")

for test in "${testList[@]}"
do
    echo "running ${test}..."
    java -jar ../../target/AmazonReader-1.2.jar -script scripts/${test}.scr > logs/output/${test}.txt
done


