#!/bin/bash
# 

ROOT_DIR=".."

while read cur_file ; do
        echo $cur_file
        java -cp vlad-aligner-1.1-jar-with-dependencies.jar vlad.aligner.cli.Aligner \
           -db jdbc:h2:./aligner.h2 \
           -fmt SPL \
           -f "${ROOT_DIR}/${cur_file}_en.spl" -lf en \
           -t "${ROOT_DIR}/${cur_file}_uk.spl" -lt uk \
           -o "${ROOT_DIR}/${cur_file}_en_uk.html"
done < align_list.txt



