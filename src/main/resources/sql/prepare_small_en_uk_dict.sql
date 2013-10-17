-- prepare small version of en-uk dictionary
-- extract referenced data from dict tables

create table en_inf_h2 as
select distinct inf.* from en_inf inf, en_uk tr where inf.id = tr.en_id

create table en_wf_h2 as
select distinct wf.* from en_wf wf, en_uk tr where wf.fk_inf = tr.en_id

create table uk_inf_h2 as
select distinct inf.* from uk_inf inf, en_uk tr where inf.id = tr.uk_id

create table uk_wf_h2 as
select distinct wf.* from uk_wf wf, en_uk tr where wf.fk_inf = tr.uk_id