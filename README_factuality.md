# Bio-SCoRes-Factuality
Assessing factuality of semantic relations

Bio-SCoRes now incorporates a component for factuality prediction of semantic
relations. It has so far used semantic predications extracted by SemRep 
(Rindflesch and Fiszman, 2003) as the basis, but it is capable of using relations
extracted by other systems, as well, provided that the relations and their
arguments are associated with textual mentions. See Kilicoglu et al. (2017) for
more details. 

------------------------
Prerequisites
------------------------
- Java (jar files have been generated with 1.8, though it is possible to
recompile with 1.7)
- Ant (needed for recompilation, version 1.8 was used for compilation)

------------------------
Directory Structure
------------------------
dist directory contains libraries relevant to Bio-SCoRes-Factuality. These are
the following:

- ling.jar: Contains the core linguistic components used by Bio-SCoRes-Factuality.
- factualitytasks.jar: Contains the factuality-related tasks.

To use Bio-SCoRes-Factuality from your application, ensure that ling.jar
is included in your classpath. factualitytasks.jar is
required if you plan to use/adapt the example factuality assessment pipeline
described in Kilicoglu et al. (2017).

Data used for experiments in Kilicoglu et al. (2017) can be downloaded from 
https://skr3.nlm.nih.gov/Factuality. A UMLS license is required.

lib directory contains third-party libraries required by the system (see Note
regarding Stanford Core NLP below.)

resources directory contains WordNet dictionary files that are required by 
the system as well as the factuality trigger file (factuality_dist.xml).

The top level directory contains ant build file as well as properties files
used by the pipelines.

- build.xml:			Ant build file for all components.
- factuality_semrep.properties:	Properties file for SemRep factuality pipeline

------------------------
Usage
------------------------
If you're interested in incorporating Bio-SCoRes-Factuality into your NLP pipeline, 
a  good starting point is the source code for the SemRep factuality pipeline 
(tasks.factuality.semrep.SemRepFactualityPipeline). 

--------------------------------
Note on Named Entity Recognition
--------------------------------			
Bio-SCoRes does not provide a named entity recognition module, but it can extract 
relations using a compositional interpretation method (Kilicoglu et al., 2015; 2017), 
if provided with named entities and relation triggers. However, for the experiments in
Kilicoglu et al. (2017), it uses named entities and relations provided by SemRep and 
only attempts to address factuality of these semantic relations.

--------------------------------
Note on Stanford CoreNLP package
--------------------------------
Stanford CoreNLP model jar file that is needed for processing raw text
for lexical and syntactic information (stanford-corenlp-3.3.1-models.jar) is 
not included with the distribution due to its size. It can be downloaded from 
http://stanfordnlp.github.io/CoreNLP/ and copied to lib directory.


------------------------
DEVELOPER
------------------------

Halil Kilicoglu


---------
CONTACT
---------

- Halil Kilicoglu:      kilicogluh@mail.nih.gov


---------
WEBPAGE
---------

A Bio-SCoRes webpage is available with all up-to-date instructions, code, 
and pipelines.

https://github.com/kilicogluh/Bio-SCoRes

---------------------------------------------------------------------------
