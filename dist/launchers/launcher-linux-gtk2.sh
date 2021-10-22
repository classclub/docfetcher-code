#!/bin/sh

script=$(readlink -f "$0")
scriptdir=`dirname "$script"`
cd "$scriptdir"

classpath=
for f in `ls ./lib/*.jar`
do
	classpath=${classpath}:${f}
done

export SWT_GTK3=0

java \
	-enableassertions \
	-Xmx1g \
	-Xss2m \
	-cp ".:${classpath}" \
	-Djava.library.path="lib" \
	${main_class} "$@"
