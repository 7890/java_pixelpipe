#!/bin/bash

FULLPATH="`pwd`/$0"
DIR=`dirname "$FULLPATH"`

src="$DIR"/src
build="$DIR"/_build

NOW=`date +"%s"`

jsource=1.6
jtarget=1.6

JAVAC="javac -source $jsource -target $jtarget -nowarn"

#========================================================================
checkAvail()
{
	which "$1" >/dev/null 2>&1
	ret=$?
	if [ $ret -ne 0 ]
	then
		echo "tool \"$1\" not found. please install"
		exit 1
	fi
}

#========================================================================
compile()
{
	echo "compiling"
	echo "========="
	$JAVAC -classpath "$build" -sourcepath "$src" -d "$build" "$src"/*.java
}

#========================================================================
build_jar()
{
	echo "creating jar"
	echo "============"

	cur="`pwd`"

	mkdir -p "$build"/jarbuild
	rm -rf "$build"/jarbuild/*
	cp "$build"/*.class "$build"/jarbuild

	cat - > "$build"/jarbuild/Manifest.txt << _EOF_
Manifest-Version: 1.0
Main-Class: PixelPipeGUI
_EOF_

	cd "$build"/jarbuild

	jar cfm PixelPipeGUI_$NOW.jar Manifest.txt *.class

	mv PixelPipeGUI_$NOW.jar "$build"

	echo "build_jar done."

	echo "java -jar "$build"/PixelPipeGUI_$NOW.jar <shm uuid>"

	cd "$cur"
#	java -jar "$build"/PixelPipeGUI_$NOW.jar /dev/shm/`ls -1tr /dev/shm/ |tail -1`
}

for tool in java javac jar javadoc; \
	do checkAvail "$tool"; done

mkdir -p "$build"
rm -rf "$build"/*

compile
build_jar

echo "done."
