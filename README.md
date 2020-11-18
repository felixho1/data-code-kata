# data-code-kata

Convert Fixed width to CSV Comma Delimited

## Build
To build the docker image, run the command `./build.sh`
<br/><br/>
Docker Image generated: <b>data-code-kata:latest</b>
<br/><br/>
To build the docker image, you need the following installed in your workstation:
* sbt
* java
* scala 

<br/><br/>
If you are unable to build the docker image, I have uploaded to public repository and ready to use: <b>felixho/data-code-kata:1.0</b>
## Instructions
Main program needs 3 arguments
* location of spec.json
* location of input file (fixed width)
* location of output file
### Command To Run By Docker
Be mindful to map the location of your spec.json file and location of your data files to docker container, so that it's accessible by the program. 
<br/><br/>
An example to run the program by mapping your directories, and passing the location of spec.json file (/myfolder/config/spec.json)
, input file (/myfolder/data/Kata_FW.txt), output file (/myfolder/data/Kata.csv)
```
docker run --rm \
-v /tmp/config:/myfolder/config \
-v /tmp/data:/myfolder/data \
data-code-kata:latest \
data-code-kata /myfolder/config/spec.json /myfolder/data/Kata_FW.txt /myfolder/data/Kata.csv
```

Replace <b>data-code-kata:latest</b> with <b>felixho/data-code-kata:1.0</b> if you don't have the image in your local workstation
### Command To Run By SBT 
Sample command to run by SBT with sample locations of the required arguments
```
sbt "run /mylocation/spec.json /mydata/Kata_FW.txt /mydata/Kata.csv"
```
### Sample spec.json
Below is sample spec.json file
```
{
    "ColumnNames": [
        "f1",
        "f2",
        "f3",
        "f4",
        "f5"
    ],
    "Offsets": [
        "40",
        "15",
        "10",
        "25",
        "15"
    ],
    "FixedWidthEncoding": "windows-1252",
    "IncludeHeader": "True",
    "DelimitedEncoding": "utf-8"
}
```