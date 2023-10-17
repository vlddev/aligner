Aligner
================
Parallel texts aligner

Features:

Installation
============


Installation (on Ubuntu Linux)
------------

### Build jar

1. Run `mvn clean install -DskipTests`

### Show entry points usage

vlad.aligner.cli.Aligner
- `java -jar vlad-aligner-1.0-SNAPSHOT-jar-with-dependencies.jar vlad.aligner.cli.Aligner -h`

vlad.aligner.cli.AlignerBatch
- `java -jar vlad-aligner-1.0-SNAPSHOT-jar-with-dependencies.jar vlad.aligner.cli.AlignerBatch -h`

vlad.aligner.cli.TextToSentencesConverter
- `java -jar vlad-aligner-1.0-SNAPSHOT-jar-with-dependencies.jar vlad.aligner.cli.TextToSentencesConverter -h`

### Simple text alignment with vlad.aligner.cli.Aligner

Run aligner using internal dictionary (in H2-DB), 
align english text and its ukrainian translation.

    java -Xmx4024M -jar vlad-aligner-1.0-SNAPSHOT-jar-with-dependencies.jar \
    "../c/text_en.txt" en \
    "../c/translation_uk.txt" uk

Run using external dictionary (Mysql/MariaDb)

    java -Xmx4024M -Djdbc.driver=com.mysql.jdbc.Driver -Djdbc.url="jdbc:mysql://localhost:3306/aligner?useUnicode=true&characterEncoding=UTF-8" -Djdbc.user=<user> -Djdbc.password=<pwd> \
    -jar vlad-aligner-1.0-SNAPSHOT-jar-with-dependencies.jar \
    "../c/text_en.txt" en \
    "../c/translation_uk.txt" uk

Align english text and its german translation using external dictionary (Mysql/MariaDb).

    java -Xmx4024M -Djdbc.driver=com.mysql.jdbc.Driver -Djdbc.url="jdbc:mysql://localhost:3306/aligner?useUnicode=true&characterEncoding=UTF-8" -Djdbc.user=<user> -Djdbc.password=<pwd> \
    -jar vlad-aligner-1.0-SNAPSHOT-jar-with-dependencies.jar \
    "../c/text_en.txt" en \
    "../c/translation_de.txt" de


### Convert unformatted text to SPL-format (sentence per line)

Convert unformatted english text to SPL using standford parser to recognize eon of sentence. 

java -jar vlad-aligner-1.0-SNAPSHOT-jar-with-dependencies.jar \
vlad.aligner.cli.TextToSentencesConverter \
-m stanford -i "en.txt" -l en -o "en.spl"

Convert unformatted ukrainian text to SPL using standford parser to recognize eon of sentence.

java -jar vlad-aligner-1.0-SNAPSHOT-jar-with-dependencies.jar \
vlad.aligner.cli.TextToSentencesConverter \
-i "uk.txt" -l uk -o "uk.spl"

### Batch processing english and ukrainian pairs only

- `java -cp vlad-aligner-1.1-jar-with-dependencies.jar vlad.aligner.cli.AlignerBatch -l "/library/root/folder/" -i file_list.txt > alignerBatch.log`

__AlignerBatch uses dictionary in internal H2-DB__

__file_list.txt__ content looks like:

    folder1/file-prefix1
    folder1/file-prefix2
    folder2/file-prefix1

AlignerBatch expected in this case that there are files (see -l param)

    /library/root/folder/folder1/file-prefix1_uk.txt
    /library/root/folder/folder1/file-prefix1_en.txt

which wil be converted to SPL

    /library/root/folder/folder1/file-prefix1_uk.spl
    /library/root/folder/folder1/file-prefix1_en.spl

than SPL-files will be aligned and result stored in 

    /library/root/folder/folder1/file-prefix1_en_uk.spl.par.html
    /library/root/folder/folder1/file-prefix1_en_uk.spl.par.json


### E-Books and text conversion tools

* pandoc - converting e-Books to plain text 
> docker run --rm -v "$(pwd):/data" -u $(id -u):$(id -g) pandoc/core "file.fb2" -t plain --wrap=none -o 1.txt

* iconv - change file encoding (convert fb2 from windows-1251 to utf-8)
> iconv -f windows-1251 -o out.fb2 in.fb2