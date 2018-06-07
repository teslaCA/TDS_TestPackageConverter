# TDS_TestPackageConverter
The TDS Test Package Converter supports conversion between the existing test pacakge formats and the updated formats.  

## Required Tools
This project uses the following tools

* Java 1.8
* Maven 3

## Building and Running the project
Once you have Java 1.8 and Maven 3 install you can build the project using the following command (in the projects root directory).

`mvn clean install`

This will build the project creating a `target` directory.  Once the project is built you will see something like:

`target/tds-test-package-converter-0.1.0.jar`

Once you see that jar you can execute it with by:

`java -jar target/tds-test-package-converter-0.1.0.jar`

This will run and present an error message that you need to provide more information.  The sections below cover the required commands an the optional flags that can be provided.

## Converting To New Pacakge Format

To convert a legacy format to the new package format one needs to provide the `convert-to-new` command.  By default this works with an xml file.

`java -jar tds-test-package-converter-0.0.1-SNAPSHOT.jar convert-to-new handScoreTestLegacy.xml handScoreTest.xml`

The `handScoreTestLegacy.xml` represents the legacy XML file contained in the same directory for which you run the package converter.  The `handScoreTest.xml` is the name of the file that will be outputed by the tool.

### Converting legacy format zip file to new format
The tool has the ability to convert zip archive files containing test packages to the new format.  The command to do that is below.  Note the `-z` argument.

`java -jar tds-test-package-converter-0.0.1-SNAPSHOT.jar convert-to-new -z HandScoreTest.zip handScoreTest.xml`

The `-z` tells the tool you want to convert an ZIP archive of test pacakges.  Note the `HandScoreTest.zip` in the command is a zip file within the same directory as you run the project.  


