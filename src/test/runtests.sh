#!/bin/bash

set -o nounset
set -o errexit

rm -rf logs
mkdir logs
mkdir logs/output
cp input/colortest.ods logs/output

java -jar ../../target/AmazonReader-1.2.jar -f scripts/brackets.scr    > logs/output/brackets.txt
java -jar ../../target/AmazonReader-1.2.jar -f scripts/colortest.scr   > logs/output/colortest.txt
java -jar ../../target/AmazonReader-1.2.jar -f scripts/filetest.scr    > logs/output/filetest.txt
java -jar ../../target/AmazonReader-1.2.jar -f scripts/looptest.scr    > logs/output/looptest.txt
java -jar ../../target/AmazonReader-1.2.jar -f scripts/misc.scr        > logs/output/misc.txt
java -jar ../../target/AmazonReader-1.2.jar -f scripts/ocrtest.scr     > logs/output/ocrtest.txt
java -jar ../../target/AmazonReader-1.2.jar -f scripts/spreadsheet.scr > logs/output/spreadsheet.txt
java -jar ../../target/AmazonReader-1.2.jar -f scripts/subroutines.scr > logs/output/subroutines.txt


