---------------------------------------------------------------------------
|*|  PUBLIC DOMAIN NOTICE                                                      
|*|                                                                             
|*| This work is a "United States Government Work" under the terms of the
|*| United States Copyright Act. It was written as part of the authors'
|*| official duties as a United States Government employee and thus cannot
|*| be copyrighted within the United States. The data is freely available
|*| to the public for use. The National Library of Medicine and the U.S.
|*| Government have not placed any restriction on its use or reproduction.
|*|                                                                             
|*| Although all reasonable efforts have been taken to ensure the accuracy
|*| and reliability of the data and its source code, the NLM and the
|*| U.S. Government do not and cannot warrant the performance or results
|*| that may be obtained by using it. The NLM and the U.S. Government
|*| disclaim all warranties, express or implied, including warranties of
|*| performance, merchantability or fitness for any particular purpose.
|*| 
|*| Please cite the authors in any work or product based on this material:
|*| 
|*| Bio-SCoRes: A Smorgasbord Architecture for Coreference Resolution in
|*| Biomedical Text
|*| Halil Kilicoglu and Dina Demner-Fushman, PLOS ONE, 2016.
---------------------------------------------------------------------------

This directory contains datasets that we used for coreference resolution 
experiments. At the top level, there are 3 directories:  SPL directory 
contains Structured Drug Product Labels. BIONLP contains PubMed abstracts.
Evaluation contains evaluation results for some of our experiments.

The SPL dataset is split into TRAIN and TEST directories, which reflects 
the split that was used in the experiments for the article. The
directories under these two are organized as follows (see the Note on 
MetaMap below):

Annotations:	    	This directory contains the raw text files and the
		    	corresponding brat standoff annotation files for
			the corpus.
XML:			The gold standard dataset in internal XML
			representation used for coreference resolution
			experiments. The XML files include lexical information
			and syntactic parses of the documents in addition to
			the annotations, to avoid reparsing the corpus.
OUT_GOLD_ENTITY:	The system output generated using the gold standard
			term annotations as input (bin/splGoldEntities script
			runs with this input).
OUT_GOLD_EXP:		The system output generated using the gold standard
			term and coreferential mention annotations
			(splGoldMentions).

The BIONLP directory contains files PubMed abstracts and related annotations,
which forms the development portion of BioNLP 2011 Protein Coreference Dataset.
The raw text and standoff annotation files have been downloaded from 
http://weaver.nlplab.org/~bionlp-st/BioNLP-ST/downloads/downloads.shtml. 
The directories under BIONLP directory are organized as follows:

GOLD_DEV:	Contains gold standoff annotation files.
XML_DEV:	The gold standard annotations in internal XML representation.
OUT_DEV:	The system output generated using the gold standard term 
		(Protein) annotations (bin/bionlp script).


The Evaluation directory contains the evaluation results obtained for the 
system runs.

--------------------------------
Note on the i2b2/VA corpus
--------------------------------
Due to usage restrictions, we do not include data derived from the i2b2/VA 
corpus and experiment results based on this corpus. The corpus can 
be obtained from http://www.i2b2.org/NLP/Coreference and the XML input 
expected by the i2b2 pipeline can be generated using the tasks.coref.i2b2.
I2B2ToXMLWriter class included. I2B2CoreferencePipeline in the same package
can be used to perform coreference resolution on this input. The i2b2 pipeline
reported in the paper was evaluated using the i2b2 coreference scorer, which 
can be obtained from the same URL.

--------------------------------
Note on MetaMap
--------------------------------
Results of experiments on SPL dataset that involve MetaMap annotations
rather than the gold annotations were reported in the article. To use MetaMap,
a UMLS license is required. MetaMap-related files are not included in this
distribution, but are available upon request.






			


