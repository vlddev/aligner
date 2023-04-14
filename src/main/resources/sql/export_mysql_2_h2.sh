# export data from MySQL to H2

#DB_host=$1
#DB_name=$2
#DB_user=$3
#DB_pass=$4

DB_host=localhost
DB_name=aligner
DB_user=root
DB_pass=pwd

COMMON_CMD="-h $DB_host -u $DB_user -p$DB_pass --database=$DB_name --batch --raw --skip-column-names -e"

echo "DUMPING TABLE: en_inf"
mysql $COMMON_CMD "SELECT id, type, inf FROM en_inf order by id" > en_inf.csv

echo "DUMPING TABLE: en_wf"
mysql $COMMON_CMD "SELECT id, fk_inf, wf FROM en_wf ORDER BY 1 " > en_wf.csv

echo "DUMPING TABLE: uk_inf"
mysql $COMMON_CMD "SELECT id, type, inf FROM uk_inf order by id" > uk_inf.csv

echo "DUMPING TABLE: uk_wf"
mysql $COMMON_CMD "SELECT id, fk_inf, wf FROM uk_wf ORDER BY 1 " > uk_wf.csv

echo "DUMPING TABLE: en_uk"
mysql $COMMON_CMD "SELECT en_id, uk_id FROM en_uk order by en_id, uk_id" > en_uk.csv

echo "Import to H2"
java -cp h2-2.1.214.jar org.h2.tools.RunScript -url "jdbc:h2:./aligner.h2;MODE=MYSQL;DATABASE_TO_LOWER=TRUE" -script import_csv_h2.sql

echo "clean up"
rm *.csv
