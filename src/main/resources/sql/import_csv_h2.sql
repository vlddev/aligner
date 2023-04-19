CREATE TABLE IF NOT EXISTS en_inf (
  id int(11) AUTO_INCREMENT PRIMARY KEY,
  type int(3) DEFAULT NULL,
  inf varchar(100) NOT NULL DEFAULT ''
) AS SELECT * from CSVREAD( 'en_inf.csv', null,  'charset=UTF-8 fieldSeparator=' || CHAR(9));

CREATE TABLE IF NOT EXISTS en_uk (
  en_id int(11) NOT NULL DEFAULT '0',
  uk_id int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (en_id,uk_id)
) AS SELECT * from CSVREAD( 'en_uk.csv', null,  'charset=UTF-8 fieldSeparator=' || CHAR(9));

CREATE TABLE IF NOT EXISTS en_wf (
  id int(11) AUTO_INCREMENT PRIMARY KEY,
  fk_inf int(11) NOT NULL DEFAULT '0',
  wf varchar(100) NOT NULL DEFAULT ''
  -- , fid varchar(20) DEFAULT ''
) AS SELECT * from CSVREAD( 'en_wf.csv', null,  'charset=UTF-8 fieldSeparator=' || CHAR(9));

CREATE TABLE IF NOT EXISTS uk_inf (
  id int(11) AUTO_INCREMENT PRIMARY KEY,
  type int(3) DEFAULT NULL,
  inf varchar(100) NOT NULL DEFAULT ''
) AS SELECT * from CSVREAD( 'uk_inf.csv', null,  'charset=UTF-8 fieldSeparator=' || CHAR(9));

CREATE TABLE IF NOT EXISTS uk_wf (
  id int(11) AUTO_INCREMENT PRIMARY KEY,
  fk_inf int(11) NOT NULL DEFAULT '0',
  wf varchar(100) NOT NULL DEFAULT ''
  --, fid varchar(20) DEFAULT ''
) AS SELECT * from CSVREAD( 'uk_wf.csv', null,  'charset=UTF-8 fieldSeparator=' || CHAR(9));

CREATE INDEX ind_en_inf_type ON en_inf (inf, type);

CREATE INDEX ind_en_wf_fk ON en_wf (fk_inf);
CREATE INDEX ind_en_wf ON en_wf (wf);

CREATE INDEX ind_uk_inf_type ON uk_inf (inf, type);

CREATE INDEX ind_uk_wf_fk ON uk_wf (fk_inf);
CREATE INDEX ind_uk_wf ON uk_wf (wf);


