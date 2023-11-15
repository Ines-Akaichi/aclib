# What is aclib?

Aclib introduces a novel access control framework designed for knowledge graphs, named the Graph-based Authorization Framework (GAF). 
GAF leverages a query rewriting algorithm that, given a SPARQL query, an authorization policy, and a dataset, systematically restructures the input query in accordance with the authorization policy. 
The outcome is a refined query that utilizes the "FILTER NOT EXISTS" mechanism to exclude all unauthorized triples specified in the policy. Subsequently, the revised query is executed against the dataset, and only permitted data are delivered to the user.

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

Input Data set: <br>

![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/e9306e4c-a115-4525-bd8b-9a1315b5edc0)

Input Policy: <br>

![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/a379be44-5513-42f1-9760-5defc7781418)

The quad pattern, entx:MRyan entx:salary ?o ?g, denies access to May Ryan’s salary. Whereas,
entx:MRyan entx:worksFor ?o ?g, restricts access to information pertaining
to the people that May Ryan works for.

Input Query:  <br>

![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/7da4d8f1-07e0-473d-b5e5-dc79e1ce3cc7) <br>
This query return the salaries of all persons. The results of this query against our dataset is shown below:   <br>

Results before applying the query rewriting algorithm:   <br>
![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/23ed335b-a7cc-4382-8a97-1383f7b752de)

Rewritten Query:  <br>
![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/95556449-cdba-44ab-8fa9-68962dbcdb49)  <br>
The rewritten query filters out the salary of Ryan, which appears in the authorised policy. The resul of this query is shown below: <br>

Results after applying the query rewriting algorithm:   <br>

![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/dcf95347-0bd2-4822-9444-04bb198496d6)

# How to use aclib?
  
## Configuration

## Use as a library


