CREATE TABLE `user` (id int(10) NOT NULL AUTO_INCREMENT, source_type_id int(10) NOT NULL, source_id int(10) NOT NULL, type varchar(255) NOT NULL, PRIMARY KEY (id));
CREATE TABLE artifact (id int(10) NOT NULL AUTO_INCREMENT, `date` date NOT NULL, source_url varchar(255) NOT NULL, description varchar(255), user_id int(10) NOT NULL, task_id int(10), PRIMARY KEY (id)) comment='If task_id is null, then this artifact is a personal project. Otherwise it is a task solution.';
CREATE TABLE proof (id int(10) NOT NULL AUTO_INCREMENT, level tinyint NOT NULL, description varchar(255), artifact_id int(10) NOT NULL, competence_id int(10) NOT NULL, PRIMARY KEY (id));
CREATE TABLE competence (id int(10) NOT NULL AUTO_INCREMENT, name varchar(255) NOT NULL UNIQUE, name_pl varchar(255) NOT NULL UNIQUE, concept_type varchar(255) NOT NULL, skill_type varchar(255) NOT NULL, PRIMARY KEY (id));
CREATE TABLE competence_relations (source_id int(10) NOT NULL, target_id int(10) NOT NULL, type_id int(10) NOT NULL);
CREATE TABLE relation_type (id int(10) NOT NULL AUTO_INCREMENT, name varchar(255) NOT NULL, PRIMARY KEY (id));
CREATE TABLE subject (id int(10) NOT NULL AUTO_INCREMENT, source_type_id int(10) NOT NULL, source_id int(10) NOT NULL, PRIMARY KEY (id));
CREATE TABLE source_type (id int(10) NOT NULL AUTO_INCREMENT, name varchar(255) NOT NULL, PRIMARY KEY (id)) comment='This entity determines the source of an asset. The database doesn''t store all necessary information and must rely on outside sources. 
For instance user name, semester and various other info will be provided via USOS API. For debug purposes some users will be INTERNAL and stored directly in the database. 
Every entity that references this entity has an additional source_id field which is the asset id used by the source (USOS, INTERNAL). 
Since this is a table and not a fixed ENUM, additional source types can be implemented.';
CREATE TABLE task (id int(10) NOT NULL AUTO_INCREMENT, title varchar(255) NOT NULL, description varchar(255), creation_date date NOT NULL, due_date date NOT NULL, teacher_id int(10) NOT NULL, subject_id int(10) NOT NULL, PRIMARY KEY (id));
CREATE TABLE subject_teacher (user_id int(10) NOT NULL, subject_id int(10) NOT NULL);
ALTER TABLE artifact ADD CONSTRAINT FKartifact330133 FOREIGN KEY (user_id) REFERENCES `user` (id);
ALTER TABLE proof ADD CONSTRAINT FKproof61972 FOREIGN KEY (artifact_id) REFERENCES artifact (id);
ALTER TABLE proof ADD CONSTRAINT FKproof387222 FOREIGN KEY (competence_id) REFERENCES competence (id);
ALTER TABLE competence_relations ADD CONSTRAINT FKcompetence637597 FOREIGN KEY (source_id) REFERENCES competence (id);
ALTER TABLE competence_relations ADD CONSTRAINT FKcompetence197407 FOREIGN KEY (target_id) REFERENCES competence (id);
ALTER TABLE competence_relations ADD CONSTRAINT FKcompetence319715 FOREIGN KEY (type_id) REFERENCES relation_type (id);
ALTER TABLE subject ADD CONSTRAINT FKsubject527698 FOREIGN KEY (source_type_id) REFERENCES source_type (id);
ALTER TABLE `user` ADD CONSTRAINT FKuser41252 FOREIGN KEY (source_type_id) REFERENCES source_type (id);
ALTER TABLE task ADD CONSTRAINT FKtask223007 FOREIGN KEY (teacher_id) REFERENCES `user` (id);
ALTER TABLE task ADD CONSTRAINT FKtask2523 FOREIGN KEY (subject_id) REFERENCES subject (id);
ALTER TABLE artifact ADD CONSTRAINT FKartifact485763 FOREIGN KEY (task_id) REFERENCES task (id);
ALTER TABLE subject_teacher ADD CONSTRAINT FKsubject_te508455 FOREIGN KEY (user_id) REFERENCES `user` (id);
ALTER TABLE subject_teacher ADD CONSTRAINT FKsubject_te471813 FOREIGN KEY (subject_id) REFERENCES subject (id);
