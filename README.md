# aclib
##What is this project?

This project introduces a novel access control framework designed for knowledge graphs, named the Graph-based Authorization Framework (GAF). 
GAF leverages a query rewriting algorithm that, given a SPARQL query, an authorization policy, and a dataset, systematically restructures the input query in accordance with the authorization policy. 
The outcome is a refined query that utilizes the "FILTER NOT EXISTS" mechanism to exclude all unauthorized triples specified in the policy. Subsequently, the revised query is executed against the dataset, and only permitted data are delivered to the user.

# INPUT Structure

The policy presents unauthorised RDF quads following the following structure:
- <S>  <P>  <O>  <G> ; whereas <S> is the subject, <P> is the property, <O> is the object, and <G> is the graph where the triple exists. 

The input query can be any SPARQL query. For now, our framework supports only the following SPARQL queries:
-  Basic Graph Patterns
-  Nested queries
-  FILTER based queries

The dataset can be composed of several RDF graphs following the following structure: 

 <G1>
 {
 <S1>  <P2>  <O3>;
 ...
 <Sn>  <Pn>  <On> .
 }
...
 <Gn>
 {
  <S1>  <P2>  <O3>;
 ...
 <Sn>  <Pn>  <On> .
 }
   
# Example
Below we present a small example that shows the input and output of our framework.
Input Data set:
![image](https://github.com/Ines-Akaichi/aclib/assets/43604498/e9306e4c-a115-4525-bd8b-9a1315b5edc0)

Input Policy:

Input Query
Rewritten Query
Results before query rewriting
Results after query rewriting








##How to use it
  #Architecture of GAF
  config file
  jar
  etc.


