package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.util.StringUtils;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.sem.ConjunctionDetection;

/**
 * A <code>DependencyTransformation</code> implementation that extends coordination detection, 
 * by recognizing that terms separated by comma are likely to be coordinated. <p>
 * This is attempt to take care of cases where coordinated items are taken as appositives.
 * It assumes that {@link ModifierCoordinationCorrection} has already been applied.
 * 
 * @author Halil Kilicoglu
 *
 */
public class TermCoordinationCorrection implements DependencyTransformation {
	private static Logger log = Logger.getLogger(TermCoordinationCorrection.class.getName());

	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		List<Class<? extends DependencyTransformation>> prereqs = 
				new ArrayList<Class<? extends DependencyTransformation>>();
		prereqs.add(ModifierCoordinationCorrection.class);
		return prereqs;
	}

	/**
	 * 
	 * 
	 * @param  sent  the sentence to transform
	 */
	public synchronized void transform(Sentence sent) {
		if (sent.getSurfaceElements() == null || sent.getEmbeddings() == null) {
			log.log(Level.WARNING,"No textual units or embeddings can be found for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<Class<? extends DependencyTransformation>> applied  = sent.getTransformations();
		if (applied.containsAll(getPrerequisites()) == false) {
			log.log(Level.WARNING,"The prerequisites for TermCoordinationCorrection are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		if (sent.getTransformations().contains(CoordinationTransformation.class)) {
			log.warning("Term coordination correction should be applied before coordination transformation.");
			return;
		}
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		List<SynDependency> embeddings = sent.getEmbeddings();
		// It's common to have coordinated items to be mislabeled as one of NP dependencies.
		List<SynDependency> maybeMislabeled = 
				SynDependency.dependenciesWithTypes(sent.getEmbeddings(),Arrays.asList("appos","dep","nn","dobj"),true);
		if (maybeMislabeled.size() == 0) return;
		List<SynDependency> add = new ArrayList<>();
		List<SynDependency> remove = new ArrayList<>();
		// TODO Consider splitting as well, "drugs/substances" -> "drugs" "/" "substances", conj_and(drugs,substances)
		for (int i=0; i < surfs.size();i++) {
			SurfaceElement surf = surfs.get(i);	
			if (surf.hasSemantics() == false) continue;
			if (StringUtils.isPunct(surf.getText())) continue;
			List<SurfaceElement> addedAsConjunct = new ArrayList<>();
			List<SynDependency> removeConjDep = new ArrayList<>();
			List<SynDependency> addedConjDep = new ArrayList<>();
			for (int j=i+1; j < surfs.size(); j++) {
				SurfaceElement surfOther = surfs.get(j);
				if (surfOther.hasSemantics() == false) continue;
				if (StringUtils.isPunct(surfOther.getText())) continue;
				if (surfOther.isCoordConj()) continue;
				if (ConjunctionDetection.conjunctsByDependencyPath(surf,surfOther)) continue;
				List<SynDependency> path = SynDependency.findDependencyPath(maybeMislabeled, surf, surfOther, false);
				if (path == null) continue;
				if (path.size() > 0) {
					SynDependency sd = path.get(path.size()-1);
					if (ConjunctionDetection.potentiallyCoordinated(surf,surfOther)) {
						if (remove.contains(sd)) continue;
						remove.add(sd);
						removeConjDep.add(sd);
						SynDependency nsd = new SynDependency("TCC_" + sd.getId(),"conj_and",surf,surfOther);
						add.add(nsd);
						addedAsConjunct.add(surfOther);
						addedConjDep.add(nsd);
					} else if (addedAsConjunct.size() > 0) {
						SurfaceElement last = addedAsConjunct.get(addedAsConjunct.size()-1);	
						if (ConjunctionDetection.conjunctsByDependencyPath(last,surfOther) || ConjunctionDetection.potentiallyCoordinated(last,surfOther)) {
							if (remove.contains(sd)) continue;
							remove.add(sd);
							removeConjDep.add(sd);
							SynDependency nsd = new SynDependency("TCC_" + sd.getId(),"conj_and",surf,surfOther);
							add.add(nsd);
							addedAsConjunct.add(surfOther);
							addedConjDep.add(nsd);
						} 
					} 
				}
			}
			// Ignore potential additions if no indication of coordination (probably an appositive instead)
			if (addedAsConjunct.size() == 1) {
				SurfaceElement a = addedAsConjunct.get(0);
				List<SynDependency> surfOtherConjs = SynDependency.dependenciesWithType(surf, embeddings, "conj", false);
				List<SynDependency> aOtherConjs = SynDependency.dependenciesWithType(a, embeddings, "conj", false);
				// This check causes a slight lowering in Appositive/Anaphora and a slight increase on Cataphora training set
				if (surfOtherConjs.size()  == 0 && aOtherConjs.size() == 0) {
					List<Word> between = sent.getDocument().getInterveningWords(a, surf);
					if (between.size() == 1 && between.get(0).getText().equals(",")) {
						remove.removeAll(removeConjDep);
						add.removeAll(addedConjDep);
					}
				}
			}
		}
		for (SynDependency r: remove) 
			log.log(Level.FINE, "Removing dependency due to term coordination correction: {0}.", new Object[]{r.toString()});
		for (SynDependency a: add) 
			log.log(Level.FINE, "Adding dependency due to term coordination correction: {0}.", new Object[]{a.toString()});
		sent.synchEmbeddings(remove, add);
		sent.addTransformation(this.getClass());
	}

}
