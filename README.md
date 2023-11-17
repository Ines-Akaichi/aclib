# What is aclib?

Aclib introduces a novel access control framework designed for knowledge graphs, named the Graph-based Authorization Framework (GAF). 
GAF leverages a query rewriting algorithm that, given a SPARQL query, an authorization policy, and a dataset, systematically restructures the input query in accordance with the authorization policy. 
The outcome is a refined query that utilizes the "FILTER NOT EXISTS" mechanism to exclude all unauthorized triples specified in the policy. Subsequently, the revised query is executed against the dataset, and only permitted data are delivered to the user.
More details about the algorithm can be found [here](https://penni.wu.ac.at/papers/arXiv%202016%20Query%20Based%20Access%20Control%20for%20Linked%20Data.pdf).

## Input Structure

The policy File can be text file, which presents unauthorised RDF quads following the following structure:
- S  P  O  G ; whereas S is the subject, P is the property, O is the object, and G is the graph where the triple exists. 

The input query can be any SPARQL query. For now, our framework supports only the following SPARQL queries:
-  Basic Graph Patterns
-  Nested queries
-  FILTER based queries

The dataset can be a Trig file, which presents several RDF graphs .
   
## Example
Below we present a small example that shows the input and output of our framework. <br>

### Input Data set: <br>

![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/e9306e4c-a115-4525-bd8b-9a1315b5edc0)

### Input Policy: <br>

![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/a379be44-5513-42f1-9760-5defc7781418)

The quad pattern, entx:MRyan entx:salary ?o ?g, denies access to May Ryan’s salary. Whereas,
entx:MRyan entx:worksFor ?o ?g, restricts access to information pertaining
to the people that May Ryan works for.

### Input Query:  <br>

![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/7da4d8f1-07e0-473d-b5e5-dc79e1ce3cc7) <br>
This query return the salaries of all persons. The results of this query against our dataset is shown below:   <br>

### Results before applying the query rewriting algorithm:   <br>
![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/23ed335b-a7cc-4382-8a97-1383f7b752de)

### Rewritten Query:  <br>
![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/95556449-cdba-44ab-8fa9-68962dbcdb49)  <br>
The rewritten query filters out the salary of Ryan, which appears in the authorised policy. The resul of this query is shown below: <br>

### Results after applying the query rewriting algorithm:   <br>

![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/dcf95347-0bd2-4822-9444-04bb198496d6)

# How to use aclib?
aclib can be utilized as a library by creating a JAR file and adding it to the classpath of your client project. Follow these steps:

## Configuration
Aclib is a maven project built on Java 11, Maven 3.9.1, Jena 4.7.0, and Fuseki 4.7.0.

You can customize the configuration in the config.properties file with the following properties:
- A data File Path
- A policy File Path
- A query Output Path
- queryType
- filters
A policy can be generated automatically from an input data file using the package 'wu.ac.at.generator'.
## Use as a library
Locate the JAR file of aclib in the `target` folder, named `aclib-0.0.1-SNAPSHOT.jar`. To use the library:
1. Create a maven project in Eclipse, for example.
2. Add the JAR to the Java build path.
3. Create an Object Query Engine. Call the functions `LoadSettings` and `ExecuteQuery`. `LoadSettings` loads the settings defined in the configuration file.
The `ExecuteQuery` function calls the rewriting algorithm and executes the rewritten query against a service or an in-memory database.
  
If using Fuseki as a service, take these steps before calling the library functions:

1. Set Jena home: [Jena Documentation](https://jena.apache.org/documentation/tools/).
2. Create a database:
    - Navigate to `<path-to-jena>\bat` (e.g., `apache-jena-4.7.0`).
    - Run ```bash tdb2_tdbloader --loader=parallel --loc <arg1> <arg2> ```, where `arg1` is the path for the database to be created, and `arg2` is the path for the data file to be loaded.
3. Start the Fuseki server:
    - Navigate to the Fuseki folder via the command line.
    - Run the command: `java -jar <path-to-fuseki>/fuseki-server.jar --loc=<path_to_the_database> /<name_of_the_endpoint>`.
    - The `ExecuteQuery` function can take as input the service URL: `http://localhost:3030/<name_of_the_endpoint>/sparql`.

Aclib acts as a query engine with built-in access control, built on top of Jena and SPARQL.

Additionally, the JAR can be invoked from the command line using the following command:

```bash
java -jar aclib-0.0.1-SNAPSHOT.jar arg1 arg2;
```
TWhere `arg1` is the path for the config file, and `arg2` is the input query.


## Update Aclib or Create your Own Jar
To update the library, open the project in a JAVA IDE, make the necessary changes, build the project, and create a new JAR.
