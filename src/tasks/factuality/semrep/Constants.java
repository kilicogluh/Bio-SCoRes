package tasks.factuality.semrep;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Event;
import gov.nih.nlm.ling.sem.Modification;
import gov.nih.nlm.ling.sem.RelationArgument;
import gov.nih.nlm.ling.sem.RelationDefinition;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * A class that encapsulates SemRep-related information, including the UMLS semantic types
 * and abbreviations it uses, the predication types it extracts. 
 * 
 * Currently, it does not address the semantic type or group constraints for the predications.
 * 
 * @author Halil Kilicoglu
 *
 */

public class Constants {
	
	public final static List<String> SEMREP_ENTITY_ABRRVS = Arrays.asList(
			"aapp","acab","aggp","alga","amph","anab","anim","anst","antb","arch",
			"bacs","bact","bdsu","bhvr","biof","bird","blor","bmod","bodm","bpoc","bsoj",
			"carb","celc","celf","cell","cgab","chem","chvs","clas","clnd","comd",
			"diap","drdd","dsyn","eico","elii","emod","emst","enzy","euka",
			"famg","ffas","fish","fndg","fngs","food","genf","gngm","grup",
			"hcro","hlca","hops","horm","humn",
			"imft","inbe","inch","inpo","invt","irda",
			"lbpr","lbtr","lipd",
			"mamm","mbrt","mcha","medd","menp","mobd","moft",
			"neop","nnon","npop","nsba","nusq",
			"ocdi","opco","orch","orga","orgf","orgm","orgt","ortf",
			"patf","phsf","phsu","plnt","podg","popg","prog","pros",
			"rcpt","rept","resd","rich","rnlw",
			"shro","socb","sosy","strd",
			"tisu","tmco","topp",
			"virs","vita","vtbt",
			"acty","amas","bdsy","chvf","clna","cnce","crbs","dora","edac","eehu","enty","evnt",
			"ftcn","geoa","gora","grpa","hcpp","idcn","mnob","mosq","ocac","phob","phpr","qlco",
			"qnco","resa","sbst","spco"
			);
	public final static List<String> SEMREP_ENTITY_TYPES = 
			Arrays.asList("AcquiredAbnormality","Activity","AgeGroup","Alga","AminoAcidSequence",
			"AminoAcidPeptideOrProtein","Amphibian","AnatomicalAbnormality","AnatomicalStructure","Animal",
			"Antibiotic","Archaeon","Bacterium","Behavior","BiologicFunction","BiologicallyActiveSubstance",
			"BiomedicalOccupationOrDiscipline","BiomedicalOrDentalMaterial","Bird","BodyLocationOrRegion",
			"BodyPartOrganOrOrganComponent","BodySpaceOrJunction","BodySubstance","BodySystem","Carbohydrate",
			"CarbohydrateSequence","Cell","CellComponent","CellFunction","CellOrMolecularDysfunction","Chemical",
			"ChemicalViewedFunctionally","ChemicalViewedStructurally","Classification","ClinicalAttribute",
			"ClinicalDrug","ConceptualEntity","CongenitalAbnormality","DailyOrRecreationalActivity","DiagnosticProcedure",
			"DiseaseOrSyndrome","DrugDeliveryDevice","EducationalActivity","Eicosanoid","ElementIonOrIsotope",
			"EmbryonicStructure","Entity","EnvironmentalEffectOfHumans","Enzyme","Event","ExperimentalModelOfDisease",
			"FamilyGroup","Finding","Fish","Food","FullyFormedAnatomicalStructure","FunctionalConcept","Fungus",
			"GeneOrGeneProduct","GeneOrGenome","GeneticFunction","GeographicArea","GovernmentalOrRegulatoryActivity",
			"Group","GroupAttribute","HazardousOrPoisonousSubstance","HealthCareActivity","HealthCareRelatedOrganization",
			"Hormone","Human","Human-causedPhenomenonOrProcess","IdeaOrConcept","ImmunologicFactor",
			"IndicatorReagentOrDiagnosticAid","IndividualBehavior","InjuryOrPoisoning","InorganicChemical",
			"IntellectualProduct","Invertebrate","LaboratoryProcedure","LaboratoryOrTestResult","Language","Lipid",
			"MachineActivity","Mammal","ManufacturedObject","MedicalDevice","MentalProcess","MentalOrBehavioralDysfunction",
			"MolecularBiologyResearchTechnique","MolecularFunction","MolecularSequence","NaturalPhenomenonOrProcess",
			"NeoplasticProcess","NeuroreactiveSubstanceOrBiogenicAmine","NucleicAcidNucleosideOrNucleotide",
			"NucleotideSequence","Object","OccupationOrDiscipline","OccupationalActivity","OrganOrTissueFunction",
			"OrganicChemical","Organism","OrganismAttribute","OrganismFunction","Organization","OrganophosphorusCompound",
			"PathologicFunction","PatientOrDisabledGroup","PharmacologicSubstance","PhenomenonOrProcess","PhysicalObject",
			"PhysiologicFunction","Plant","PopulationGroup","ProfessionalSociety","ProfessionalOrOccupationalGroup",
			"QualitativeConcept","QuantitativeConcept","Receptor","RegulationOrLaw","Reptile","ResearchActivity",
			"ResearchDevice","RickettsiaOrChlamydia","Self-helpOrReliefOrganization","SignOrSymptom","SocialBehavior",
			"SpatialConcept","Steroid","Substance","TemporalConcept","TherapeuticOrPreventiveProcedure","Tissue",
			"Vertebrate","Virus","Vitamin");
	public final static List<String> SEMREP_RELATION_TYPES = Arrays.asList(
						"ADMINISTERED_TO","AFFECTS","ASSOCIATED_WITH","AUGMENTS",
						"CAUSES","COEXISTS_WITH","COMPLICATES","CONVERTS_TO","DIAGNOSES","DISRUPTS",
						"INHIBITS","INTERACTS_WITH","LOCATION_OF","MANIFESTATION_OF","METHOD_OF","OCCURS_IN",
						"PART_OF","PREDISPOSES","PREVENTS","PROCESS_OF","PRODUCES","STIMULATES","TREATS","USES",
						"ISA","PRECEDES","compared_with","same_as","higher_than","lower_than","different_from");
	public final static List<String> SEMREP_NEG_RELATION_TYPES = Arrays.asList(
			"NEG_ADMINISTERED_TO","NEG_AFFECTS","NEG_ASSOCIATED_WITH","NEG_AUGMENTS",
			"NEG_CAUSES","NEG_COEXISTS_WITH","NEG_COMPLICATES","NEG_CONVERTS_TO","NEG_DIAGNOSES","NEG_DISRUPTS",
			"NEG_INHIBITS","NEG_INTERACTS_WITH","NEG_LOCATION_OF","NEG_MANIFESTATION_OF","NEG_METHOD_OF","NEG_OCCURS_IN",
			"NEG_PART_OF","NEG_PREDISPOSES","NEG_PREVENTS","NEG_PROCESS_OF","NEG_PRODUCES","NEG_STIMULATES","NEG_TREATS","NEG_USES",
			"NEG_ISA","NEG_PRECEDES","NEG_compared_with","NEG_same_as","NEG_higher_than","NEG_lower_than","NEG_different_from");
	
	public final static List<String> SEMREP_MODIFICATION_TYPES = Arrays.asList("Factuality");

	/**
	 * Returns a set of predication definitions, all of which consist of two argument (Subject, Object) and a predicate. 
	 * 
	 * @return	a set of SemRep predication definitions
	 */
	public static Set<RelationDefinition> loadSemRepDefinitions() {
		Set<RelationDefinition> relDefs = new HashSet<RelationDefinition>();
		Map<Class<? extends SemanticItem>,Set<String>> entityMap= new HashMap<Class<? extends SemanticItem>,Set<String>>();
		Map<Class<? extends SemanticItem>,Set<String>> eventMap = new HashMap<Class<? extends SemanticItem>,Set<String>>();		
		entityMap.put(Entity.class, new HashSet<String>(SEMREP_ENTITY_ABRRVS));
		eventMap.put(Event.class, new HashSet<String>(SEMREP_RELATION_TYPES));
		// This clearly does not take into account actual semantic type restrictions
		for (String relType: SEMREP_RELATION_TYPES) {
			relDefs.add(new RelationDefinition(relType,relType,Event.class,
					Arrays.asList(new RelationArgument("Subject",entityMap,false),new RelationArgument("Object",entityMap,false)),
					null));
		}
		for (String relType: SEMREP_NEG_RELATION_TYPES) {
			relDefs.add(new RelationDefinition(relType,relType,Event.class,
					Arrays.asList(new RelationArgument("Subject",entityMap,false),new RelationArgument("Object",entityMap,false)),
					null));
		}
		for (String modType: SEMREP_MODIFICATION_TYPES) {
			relDefs.add(new RelationDefinition(modType,modType,Modification.class,
					Arrays.asList(new RelationArgument("Relation",eventMap,false)),
					null));
		}
		return relDefs;
	}
}