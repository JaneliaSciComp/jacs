#!/bin/bash
HOST=prd-db
DB=flyportal
USER=flyportalAdmin
PASS=flyp0rt@lAdm1n

mysqlcheck -a -h ${HOST} -u ${USER} -p${PASS} ${DB}

