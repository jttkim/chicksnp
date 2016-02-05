JBOSS_HOME=$(HOME)/hacking/java/jboss/jboss-current


default : package

package :
	mvn clean
	mvn package

clean :
	mvn clean

deploy : package
	$(JBOSS_HOME)/bin/jboss-cli.sh --connect --command="deploy ear/target/chicksnp.ear --force"

redeploy :
	$(JBOSS_HOME)/bin/jboss-cli.sh --connect --command="deploy ear/target/chicksnp.ear --force"

undeploy :
	$(JBOSS_HOME)/bin/jboss-cli.sh --connect --command="undeploy chicksnp.ear"

dbdump :
	pg_dump -b -c -O chicksnp_test > chicksnp_dump.sql

dbrestore :
	psql chicksnp_test < chicksnp_dump.sql

.PHONY : default package clean deploy undeploy dbdump dbrestore
