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


if test -f ./dbconfig.sh ; then
  . ./dbconfig.sh
  # echo $dbname
fi


do_run java -jar cmdtool/target/chicksnp-cmdtool.jar chickimport toysnp/mocklines.txt 
for i in 1 2 3 4 ; do
  do_run java -jar cmdtool/target/chicksnp-cmdtool.jar vcfimport m${i} toysnp/m${i}.vcf
  # do_run java -jar cmdtool/target/chicksnp-cmdtool.jar vcfjdbcimport m${i} toysnp/m${i}.vcf $dbname $dbuser $dbpassword
done

