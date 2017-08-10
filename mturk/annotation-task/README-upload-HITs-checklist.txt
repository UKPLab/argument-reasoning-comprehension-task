-- make sure sandbox is enabled/disabled in HITCreator

-- run HITCreator

-- make sure the generated *.input task contains all hits (edit folder in run fill-task.input.sh
and run it; set the path to generated hits properly)

-- check the path on the UKP server in *.question.xml

-- copy HITs to the UKP public server (using samba or sshfs: /srv/www/)

-- make sure sandbox is enabled/disabled in aws-mturk-clt-1.3.1/bin/mturk.properties

-- make sure US and 9X% acceptance are enabled/disabled in *task.properties

-- [if qualification required] make sure your qualification is updated in *task.properties
