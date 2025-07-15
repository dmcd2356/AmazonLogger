#!/bin/bash

set -o nounset
set -o errexit

rm -rf logs
mkdir logs
mkdir logs/output

declare -a testList=("brackets" "looptest" "subroutines" "filetest" "spreadsheet" "colortest" "misc" "ocrtest")

for test in "${testList[@]}"
do
    echo "running ${test}..."
    java -jar ../../target/AmazonReader-1.3.jar -script scripts/${test}.scr > logs/output/${test}_out.txt
done


