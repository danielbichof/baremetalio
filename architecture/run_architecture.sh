#!/usr/bin/env bash

if ! [ -f ./plantuml.jar ]; then
	echo "Making planUml download"
	wget -c https://github.com/plantuml/plantuml/releases/download/v1.2024.5/plantuml-gplv2-1.2024.5.jar -O plantuml.jar
fi

java -jar plantuml.jar *pu

