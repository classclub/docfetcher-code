#!/bin/sh

scriptdir=$(cd "$(dirname "$0")"; pwd)
cd "$scriptdir"
cd ../../..

classpath=
for f in `ls ./lib/*.jar`
do
	classpath=${classpath}:${f}
done

# Prefer legacy Java 6 runtime from Apple if present
java_exec=/Library/Internet\ Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java
if [ ! -f "$java_exec" ]; then
	java_exec=java
fi

# Note: The java call must not end with '&', otherwise the -Xdock:name property will have no effect.

"$java_exec" \
	-XstartOnFirstThread \
	-Xdock:name="${app_name}" \
	-enableassertions \
	-Xmx1g \
	-Xss2m \
	-cp ".:${classpath}" \
	-Djava.library.path="lib" \
	${main_class} "$@"
