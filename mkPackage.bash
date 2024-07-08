#!/bin/bash

function build_jar() {
    cwd=$(pwd)
    cd $(dirname $0)

    date=$(date +"%Y-%b-%d %H:%M:%S %Z")

    if [ -d .git ]; then
	rev=$(git rev-parse --verify --short HEAD)
    else
	rev="UNVERSIONED"
    fi

    mvn clean install
    
    if [ ! -f target/${package}.jar ]; then
	echo "compilation failed"
	exit 0
    fi
    
    cd $cwd
}

function make_scripts() {
    cwd=$(pwd)

    binDir=${root}/unix
    batchDir=${root}/windows
    libDir=${root}/lib

    mkdir -p ${binDir}
	mkdir -p ${batchDir}
    mkdir -p ${libDir}
    rsync -a target/${package}.jar ${libDir}
    rsync -a target/${package}_lib ${libDir}

    for class in $(jar tf target/${package}.jar  | grep $appSrcDir | grep -v '\$' | grep class); do
	base=$(basename $class ".class")
	tool=${binDir}/${base}
	path=$(dirname $class | sed 's,/,.,g').${base}
	echo "#!/bin/bash" > ${tool}
	echo 'root=$(dirname $0)' >> ${tool}
	echo 'MEMSIZE=""' >> ${tool}
	echo 'if [ "$(uname)" == "Darwin" ]; then' >> ${tool}
	echo '    MEMSIZE=$(sysctl hw.memsize | awk '\''{print int($2/1024)}'\'')'  >> ${tool}
	echo 'elif [ "$(uname)" == "Linux" ]; then' >> ${tool}
	echo '    MEMSIZE=$(grep MemTotal /proc/meminfo | awk '\''{print $2}'\'')' >> ${tool}
	echo 'fi' >> ${tool}
	echo 'java=$(which java)' >> ${tool}
	echo 'if [ -z $java ]; then' >> ${tool}
	echo '    echo "Java executable not found in your PATH"' >> ${tool}
	echo '    exit 1' >> ${tool}
	echo 'fi' >> ${tool}
	echo 'fullVersion=$(java -version 2>&1 | head -1 |awk -F\" '\''{print $2}'\'')' >> ${tool}
	echo 'version=$(echo $fullVersion | awk -F\. '\''{print $1}'\'')' >> ${tool}
	echo 'if [ "$version" -lt "17" ];then' >> ${tool}
	echo '    echo "minimum Java version required is 17.  Version found is $fullVersion."' >> ${tool}
	echo '    exit 1' >> ${tool}
	echo 'fi' >> ${tool}
	echo "java -Xmx\${MEMSIZE}K -cp \${root}/../lib/*:\${root}/../lib/${package}_lib/* $path \$@" >> ${tool}

	chmod +x ${tool}

	tool=${batchDir}/${base}.bat
	echo '@ECHO OFF' > ${tool}
	echo 'set root=%~dp0\..' >> ${tool}
	echo 'set CLASSPATH=%root%\lib\JCAT.jar;%root%\lib\JCAT_lib\*' >> ${tool}
	echo '' >> ${tool}
	echo ':: https://stackoverflow.com/questions/11343190/how-to-check-available-memory-ram-via-batch-script' >> ${tool}
	echo 'for /f "skip=1" %%p in ('\''wmic os get freephysicalmemory'\'') do ( ' >> ${tool}
	echo '  set memsize=%%p' >> ${tool}
	echo '  goto :done' >> ${tool}
	echo ')' >> ${tool}
	echo ':done' >> ${tool}
	echo '' >> ${tool}
	echo ':: https://stackoverflow.com/questions/17714681/get-java-version-from-batch-file' >> ${tool}
	echo 'for /f tokens^=2-5^ delims^=.-_^" %%j in ('\''java -fullversion 2^>^&1'\'') do set "version=%%j"' >> ${tool}
	echo '' >> ${tool}
	echo 'if %version% LSS 17 (' >> ${tool}
	echo 'ECHO minimum Java version required is 17.  Version found is %version%.' >> ${tool}
	echo ') else (' >> ${tool}
	echo 'java -Xmx%memsize%K app.runJCAT' >> ${tool}
	echo ')' >> ${tool}
	echo ':: Keep the CMD window open in case of errors' >> ${tool}
	echo 'PAUSE' >> ${tool}

    done
    cd $cwd
}

### Don't need to modify anything below this line

package=JCAT
root=${package}-$(date "+%Y.%m.%d")

java=$(which java)
if [ -z $java ]; then
    echo "Java executable not found in your PATH"
    exit 1
fi

cwd=$(pwd)
cd $(dirname $0)

srcFile="./src/main/java/util/AppVersion.java"
appSrcDir='app'

java=$(which java)
if [ -z $java ]; then
    echo "Java executable not found in your PATH"
    exit 1
fi
fullVersion=$(java -version 2>&1 | head -1 |awk -F\" '{print $2}')
version=$(echo $fullVersion | awk -F\. '{print $1}')
if [ "$version" -lt "17" ];then
    echo "minimum Java version required is 17.  Version found is $fullVersion."
    exit 1
fi

build_jar
make_scripts

if [ -d .git ]; then
    git restore $srcFile
fi

cd $cwd

mkdir -p dist
tar cfz ./dist/${root}.tar.gz ./${root}
rsync -a mkPackage.bash pom.xml src ${root}-src/
tar cfz ./dist/${root}-src.tar.gz ./${root}-src

echo -e "\nCreated ./dist/${root}.tar.gz ./dist/${root}-src.tar.gz"

/bin/rm -fr ${root} ./${root}-src
