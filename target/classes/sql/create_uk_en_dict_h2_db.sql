CREATE TABLE IF NOT EXISTS en_inf (
  id int(11) NOT NULL DEFAULT '0' PRIMARY KEY,
  type int(3) DEFAULT NULL,
  inf varchar(100) NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS en_uk (
  en_id int(11) NOT NULL DEFAULT '0',
  uk_id int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (en_id,uk_id)
);

CREATE TABLE IF NOT EXISTS en_wf (
  id int(11) NOT NULL DEFAULT '0' PRIMARY KEY,
  fk_inf int(11) NOT NULL DEFAULT '0',
  wf varchar(100) NOT NULL DEFAULT '',
  fid varchar(20) DEFAULT ''
);

CREATE TABLE IF NOT EXISTS uk_inf (
  id int(11) NOT NULL DEFAULT '0' PRIMARY KEY,
  type int(3) DEFAULT NULL,
  inf varchar(100) NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS uk_wf (
  id int(11) NOT NULL DEFAULT '0' PRIMARY KEY,
  fk_inf int(11) NOT NULL DEFAULT '0',
  wf varchar(100) NOT NULL DEFAULT '',
  fid varchar(20) DEFAULT ''
);

CREATE INDEX ind_en_inf_type ON en_inf (inf, type);

CREATE INDEX ind_en_wf_fk ON en_wf (fk_inf);
CREATE INDEX ind_en_wf ON en_wf (wf);

CREATE INDEX ind_uk_inf_type ON uk_inf (inf, type);

CREATE INDEX ind_uk_wf_fk ON uk_wf (fk_inf);
CREATE INDEX ind_uk_wf ON uk_wf (wf);


