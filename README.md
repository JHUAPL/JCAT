# JCAT

Java CRISM Analysis Tool

JCAT is a free analysis tool for Compact Reconnaissance Imaging Spectrometer for Mars (CRISM) data products. It is released under the MIT license.

## Installation
Clone this repository from the command line:
```
> git clone git@github.com:JHUAPL/JCAT.git
```
In the cloned folder ('jcat'), run the mkPackage.bash script:
```
> ./mkPackage.bash
```
This script will create two tar files in the 'dist' directory. Expand that tar file either in a finder window or via command line:
```
> tar -xvzf ./dist/JCAT-XXXX.XX.XX.tar.gz
```
Open runJCAT in your corresponding OS via finder window or command line to start using JCAT:
``` 
> ./JCAT-XXXX.XX.XX/unix/runJCAT 
```

## Tutorial / Documentation

There is a tutorial PDF available when running JCAT. Under the _Help_ tab, click on the _Tutorial_ button to open the document. 

The _Show Log_ button is useful for debugging.


