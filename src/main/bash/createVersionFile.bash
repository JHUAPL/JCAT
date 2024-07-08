#!/bin/bash

# This script is run from maven.  See the exec-maven-plugin block in the pom.xml file.

cd $(dirname $0)

date=$(date -u +"%Y-%b-%d %H:%M:%S %Z")

rev=$(git rev-parse --verify --short HEAD)
if [ $? -gt 0 ]; then
    rev="UNVERSIONED"
fi

srcFile=../src/main/java/util/AppVersion.java
mkdir -p $(dirname $srcFile)

touch $srcFile

cat <<EOF > $srcFile

package util;

public class AppVersion {
        private static String gitRevision = new String("$rev");
	private static String applicationName = new String("JCAT");
	private static String dateString = new String("$date");

	private AppVersion() {}

	/**
	* version $rev (built $date)
	*/
	public static String getVersionString() {
                return String.format("%s version %s (built %s)", applicationName, gitRevision, dateString);
	}
}
EOF
