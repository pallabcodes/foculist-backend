-- Widen project_item.key from varchar(10) to varchar(32) to support longer project keys
ALTER TABLE project.project_item ALTER COLUMN key TYPE varchar(32);
