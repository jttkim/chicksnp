# SNP Browser for Chicken Lines

_C-DAC / Pirbright Institute joint development_


## Prerequisites

Build:

* Java 8 (7 might work too)
* maven
* make (optional)
* javamisc from https://github.com/jttkim/javamisc

Runtime: wildfly (other JEE containers should also work), installed
with datasource called `ChicksnpPostgresDS`.

Also PostgreSQL for speed-optimised loading of data from VCF files
into the database.


## Architectural sketch

JEE app comprised of modules

* `ejb`: entities and one session bean `SnpSessionBean` that contains
business methods for populating and querying

* `cmdtool`: command line interface, see Instructions below for usage.

* `util`: utility classes and methods that don't depend on the modules
  above


## Instructions

Build by running `mvn package` or simply `make`

Deploy by running
```
/your/path/to/jboss/bin/jboss-cli.sh --connect --command="deploy ear/target/chicksnp.ear --force"
```

Then try populating the system with mock data by running `./mock.sh`


### Command Line Interface

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


## ToDo

* [ ] discuss and compare with C-DAC prototype
* [ ] add web module
* [ ] optimise `SnpSessionBean.findDifferentialSnpLocusList` method
* [ ] fix `pom.xml` so that `mvn wildfly:deploy` will work
* [ ] ...