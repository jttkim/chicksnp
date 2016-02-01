#!/bin/sh

java -jar cmdtool/target/chicksnp-cmdtool.jar chickimport ${PWD}/toysnp/mocklines.txt 
for i in 1 2 3 4 ; do
  java -jar cmdtool/target/chicksnp-cmdtool.jar vcfimport m${i} ${PWD}/toysnp/m${i}.vcf 
done

