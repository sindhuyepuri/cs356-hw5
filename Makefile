OUT=SimpleDNS.jar
ROOT=a.root-servers.net
CSV=ec2.csv
JAVA_SRC=${wildcard src/edu/ut/cs/sdn/simpledns/*.java}
JAVA_LIB=${wildcard src/edu/ut/cs/sdn/simpledns/packet/*.java}

JAVA_SRC_CLS=${addsuffix .class,${basename ${JAVA_SRC}}}
JAVA_LIB_CLS=${addsuffix .class,${basename ${JAVA_LIB}}}

BIN=bin

${OUT}: ${JAVA_SRC} ${JAVA_LIB} Makefile
	-ant

clean:
	rm -rf ${BIN} ${OUT}

run: 
	java -jar ${OUT} -r ${ROOT} -e ${CSV} &