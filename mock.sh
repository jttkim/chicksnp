#!/bin/sh

do_run ()
{
  echo $* >& 2
  if $* ; then
    true
  else
    echo '*** error ***'
    exit 1
  fi
}


. ./dbconfig.sh
echo $dbname

do_run java -jar cmdtool/target/chicksnp-cmdtool.jar chickimport ${PWD}/toysnp/mocklines.txt 
for i in 1 2 3 4 ; do
  do_run java -jar cmdtool/target/chicksnp-cmdtool.jar vcfjdbcimport m${i} ${PWD}/toysnp/m${i}.vcf $dbname $dbuser $dbpassword
done

