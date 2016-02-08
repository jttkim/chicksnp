# SNP Browser for Chicken Lines

_C-DAC / Pirbright Institute joint development_


## Prerequisites

Build: Java 8 (7 might work too), maven, make

Runtime: wildfly (other JEE containers should also work), datasource
called `ChicksnpPostgresDS`.

Also PostgreSQL for speed-optimised loading of data from VCF files
into the database.


## Architectural sketch

JEE app comprised of modules

* `ejb`: entities and one session bean `SnpSessionBean` that contains
business methods for populating and querying

* `cmdtool`: command line interface, usage:
```
java -jar cmdtool/target/chicksnp-cmdtool.jar <cmd> <args>
```
where `cmd` is one of
  * `vcfimport <chickenLine> <vcfFile>` import from VCF file
  * `vcfjdbcimport <chickenLine> <vcfFile> <dbname> <dbuser> <dbpassword>` import from VCF file using JDBC and bypassing business tier, much faster
  * `chickimport <file>` import chicken line names
  * `testsplit <split>` get SNP loci supporting split, specified as
    two comma-separated lists separated by `|`, e.g. `Line6|Line7` or
    `m1,m2|m3,m4`

* `util`: utility classes and methods that don't depend on the modules
  above


## ToDo

* [ ] discuss and compare with C-DAC prototype
* [ ] add web module
* [ ] optimise `SnpSessionBean.findDifferentialSnpLocusList` method
* [ ] ...