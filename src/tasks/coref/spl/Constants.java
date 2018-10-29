package tasks.coref.spl;

import java.util.Arrays;
import java.util.List;

/**
 * This class contains some type definitions used by SPL pipelines and utilities.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Constants {
	public static List<String> EXP_TYPES = 
			Arrays.asList("DefiniteNP", "IndefiniteNP", "ZeroArticleNP", "DemonstrativeNP", "DistributiveNP", 
			"PersonalPronoun", "PossessivePronoun","DemonstrativePronoun", "DistributivePronoun", "ReciprocalPronoun", "RelativePronoun", "IndefinitePronoun");
	public static List<String> ENTITY_TYPES = Arrays.asList("Drug","Drug_Class","Substance","SPAN");
	public static List<String> COREFERENCE_TYPES = Arrays.asList("Anaphora","Cataphora","Appositive","PredicateNominative");

	// UMLS semantic type abbreviations and Reference
	public static List<String> UMLS_SEMTYPES = Arrays.asList("acab","acty","aggp","alga","amas","aapp","amph","anab","anst","anim",
			"antb","arch","bact","bhvr","biof","bacs","bmod","bodm","bird","blor","bpoc","bsoj","bdsu","bdsy","carb",
			"crbs","cell","celc","celf","comd","chem","chvf","chvs","clas","clna","clnd","cnce","cgab","dora","diap",
			"dsyn","drdd","edac","eico","elii","emst","enty","eehu","enzy","evnt","emod","famg","fndg","fish","food","ffas","ftcn","fngs",
			"gngp","gngm","genf","geoa","gora","grup","grpa","hops","hlca","hcro","horm","humn","hcpp","idcn","imft",
			"irda","inbe","inpo","inch","inpr","invt","lbpr","lbtr","lang","lipd","mcha","mamm","mnob","medd","menp","mobd",
			"mbrt","moft","mosq","npop","neop","nsba","nnon","nusq","objt","ocdi","ocac","ortf","orch","orga","orgm","orgf","orgt","opco",
			"patf","podg","phsu","phpr","phob","phsf","plnt","popg","pros","prog","qlco","qnco","rcpt","rnlw","rept","resa",
			"resd","rich","shro","sosy","socb","spco","strd","sbst","tmco","topp","tisu","vtbt","virs","vita","Reference");


}
