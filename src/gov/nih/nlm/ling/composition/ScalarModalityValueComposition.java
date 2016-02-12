package gov.nih.nlm.ling.composition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.util.CollectionUtils;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Interval;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.Predication;
import gov.nih.nlm.ling.sem.ScalarModalityValue;
import gov.nih.nlm.ling.sem.ScalarModalityValue.ScaleType;
import gov.nih.nlm.ling.sem.SemanticItem;


/**
 * A class that includes static methods for scalar modality value composition.
 * It assumes that a <code>Predication</code> has already been formed, and that we need
 * to calculate its modality value.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ScalarModalityValueComposition {
	private static Logger log = Logger.getLogger(ScalarModalityValueComposition.class.getName());	
	
	// TODO Should we not just get these from the categorization?
	private enum ScaleShift {
		NEGATE, INCREASE, LOWER, HEDGE;
	}
		
	/**
	 * Finds the modality scale appropriate for the predication.
	 *  
	 * @param pred  the predication whose scale to find
	 * @return  the scale of the predication, or null if the predicate is invalid
	 */
	public static ScaleType getModalScale(Predication pred) {
		Predicate pr = pred.getPredicate();
		ScaleType scaleType = null;
		if (pr == null) 
			log.log(Level.WARNING, "Predication {0} has no valid predicate. Unable to determine its modal scale.", new Object[]{pred.getId()});
		else {
			if (EmbeddingCategorization.isEpistemicScalar(pr)) scaleType = ScaleType.EPISTEMIC;
			else if (EmbeddingCategorization.getAllDescendants(ScaleType.DEONTIC.toString()).contains(pr.getType())) scaleType = ScaleType.DEONTIC;
			else scaleType = ScaleType.valueOf(pr.getType());
		} 
		return scaleType;
	}

	/**
	 * Propagates the scalar modality value for the predications in the scope
	 * of the <var>pred</var> predication.
	 * 
	 * @param pred  the predication whose scale/value to propagate
	 */
	// Some examples to look at where this does not work well:
	// 10064103 - determine whether .. activation is evident in .. : I think generated predication is correct, though.
	public static void propagateScalarModalityValues(Predication pred) {
		if (pred.getArguments() == null || pred.getArguments().size() == 0) {
			log.log(Level.WARNING, "Predication {0} has no valid arguments. Skipping scalar modality value propagation.", new Object[]{pred.getId()});
			return;
		}
		Predicate pr = pred.getPredicate();
		if (pr == null) {
			log.log(Level.WARNING, "Predication {0} has no valid predicate. Skipping scalar modality value propagation.", new Object[]{pred.getId()});
			return;
		}
		if (pred.isAtomic() || pred.isGeneric()) {
			log.log(Level.FINE, "Predication {0} is not embedding. Skipping scalar modality value propagation.", new Object[]{pred.getId()});
			return;
		}
		boolean scaleShifter = EmbeddingCategorization.isScaleShifter(pr);
		boolean modal = EmbeddingCategorization.isModal(pr);
		if (!modal && !scaleShifter) {
			log.log(Level.FINE, "Predication {0} is not modal or scale shifter. Skipping scalar modality value propagation.", new Object[]{pred.getId()});
			return;
		}
		LinkedHashSet<SemanticItem> scopalInfluence = new LinkedHashSet<>();
		List<SemanticItem> sources = new ArrayList<>();
		sources.addAll(pred.getSources());
		getScopalInfluence(pred,pred,0,sources,scopalInfluence);
		if (scopalInfluence.size() == 0) {
			log.log(Level.FINE, "Predication {0} has no predication in scope. Skipping scalar modality value propagation.", new Object[]{pred.getId()});
			return;
		}
		log.log(Level.FINE,"Predication {0} has {1} predications in its scope.", new Object[]{pred.getId(),scopalInfluence.size()});
		Iterator<SemanticItem> effectIter = scopalInfluence.iterator();
		while (effectIter.hasNext()) {
			SemanticItem so = effectIter.next();
			if (modal)  {	
				ScaleType predicateScale = getModalScale(pred);
				double predicateScaleValue = pr.getSense().getPriorScalarValue();
				log.log(Level.FINEST,"Modal predication {0} has scale {1} and value {2}.", new Object[]{pred.getId(),predicateScale.toString(),predicateScaleValue});
				if (so instanceof Predication) {
					Predication child = (Predication)so;
					log.log(Level.FINEST,"Propagating scalar modality value from predication {0} to predication {1} in its scope.", new Object[]{pred.getId(),child.getId()});
					updateModalScalarValues(child.getScalarValues(),predicateScale,predicateScaleValue);
				} 
			} else if (scaleShifter)  {	
				String semType = pred.getType();
				if (so instanceof Predication) {
					Predication child = (Predication)so;
					List<ScalarModalityValue> scalarValues = child.getScalarValues();
					log.log(Level.FINEST,"Propagating scalar modality value from predication {0} to predication {1} in its scope.", new Object[]{pred.getId(),child.getId()});
					if (semType.equals("NEGATOR")) updateScaleValues(ScaleShift.NEGATE,scalarValues);
					else if (semType.equals("INTENSIFIER")) updateScaleValues(ScaleShift.INCREASE,scalarValues);
					else if (semType.equals("DIMINISHER")) updateScaleValues(ScaleShift.LOWER,scalarValues);
					else if (semType.equals("HEDGE")) updateScaleValues(ScaleShift.HEDGE,scalarValues);
				}
			}
		} 	
	}
	
	// Gets the semantic objects in the scope of an embedding predication recursively.
	private static void getScopalInfluence(Predication embedding, Predication current, int intervening, 
			List<SemanticItem> sources, LinkedHashSet<SemanticItem> out ) {
		List<SemanticItem> scopalArgs = getScopalArguments(current);
		if (scopalArgs == null || scopalArgs.size() == 0) {
			log.log(Level.FINEST,"Predication {0} has no scopal arguments.", new Object[]{embedding.getId()});
			return;
		}
		Predicate pr = embedding.getPredicate();
		boolean scaleShifter = EmbeddingCategorization.isScaleShifter(pr);
		boolean modal = EmbeddingCategorization.isModal(pr);
		if (scaleShifter) {
			for (int i=0; i < scopalArgs.size(); i++) {
				SemanticItem so = scopalArgs.get(i);
				if (so instanceof Predication) {
					Predication child = (Predication)so;
					Predicate childPr = child.getPredicate();
					// TODO Not sure this is right.
					if (childPr == null) {
						log.log(Level.WARNING,"Scale shifting predication {0} has child predication {1} with invalid predicate. Skipping.", new Object[]{embedding.getId(),child.getId()});
						continue;
					}
					if (sources.size() >= 2 && CollectionUtils.containsAny(sources, child.getSources()))
//					if (!sources.contains(child.getSource()) && sources.size() >= 2)
						continue;
				
					// This gives slightly better results than the following commented out loop on GENIA factuality
					sources.addAll(child.getSources());
/*					for (SemanticItem source: child.getSources()) {
						if (sources.contains(source)) continue;
						sources.add(source);
					}*/
					if (EmbeddingCategorization.isScaleShifter(childPr))  {
						out.add(child);
						getScopalInfluence(embedding,child,intervening,sources,out);
					} else if (EmbeddingCategorization.isModal(childPr) && intervening <= 1) {
						out.add(child);
						getScopalInfluence(embedding,child,intervening,sources,out);
					} else {
						if (intervening < 1) {
							out.add(child);
						}
					}
				}
			}
		} else if (modal) {
			if (EmbeddingCategorization.isEpistemicScalar(pr)) {
				for (int i=0; i < scopalArgs.size(); i++) {
					SemanticItem so = scopalArgs.get(i);
					if (so instanceof Predication) {
						Predication child = (Predication)so;
						Predicate childPr = child.getPredicate();
						// TODO: Not sure this is right.
						if (childPr == null) {
							log.log(Level.WARNING,"Epistemic scalar predication {0} has child predication {1} with invalid predicate. Skipping.", new Object[]{embedding.getId(),child.getId()});
							continue;
						}
						if (sources.size() >= 2 && CollectionUtils.containsAny(sources, child.getSources()))
//						if (!sources.contains(child.getSource()) && sources.size() >= 2)
							continue;
						sources.addAll(child.getSources());
						if (EmbeddingCategorization.isEpistemicScalar(childPr) || EmbeddingCategorization.isScaleShifter(childPr)) {
							out.add(child);
							getScopalInfluence(embedding,child,intervening,sources,out);
						}  else {
							if (intervening < 1) {
								out.add(child);
//								intervening++;
							}
						}
					}
				}
			} else {
				for (int i=0; i < scopalArgs.size(); i++) {
					SemanticItem so = scopalArgs.get(i);
					if (so instanceof Predication) {
						Predication child = (Predication)so;
						Predicate childPr = child.getPredicate();
						// TODO: Not sure this is right.
						if (childPr == null) {
							log.log(Level.WARNING,"Predication {0} has child predication {1} with invalid predicate. Skipping.", new Object[]{embedding.getId(),child.getId()});
							continue;
						}
						if (sources.size() >= 2 && CollectionUtils.containsAny(sources, child.getSources()))						
//						if (!sources.contains(child.getSource()) && sources.size() >= 2)
							continue;
						sources.addAll(child.getSources());
						if (EmbeddingCategorization.isScaleShifter(childPr)) {
							out.add(child);
							getScopalInfluence(embedding,child,intervening,sources,out);
						}
						else if (intervening < 1) {
							out.add(child);
							getScopalInfluence(embedding,child,intervening+1,sources,out);
						}
					}
				}
			}	
		}
	}
	
	private static List<SemanticItem> getScopalArguments(Predication pred) {
		if (pred.getArguments() == null) return new ArrayList<>();
		List<SemanticItem> children = Argument.getArgItems(pred.getArgs(ArgumentRule.SCOPE_ARGUMENT_TYPES));	
		List<SemanticItem> out = new ArrayList<>();
		for (int i=0; i < children.size(); i++) {
			SemanticItem so = children.get(i);
			if (so instanceof Predication) {
				Predication child = (Predication)so;
				out.add(child);
			}
		}
		return out;
	}
	
	private static void updateModalScalarValues(List<ScalarModalityValue> scalarValues, ScaleType scaleType, double priorScalarValue) {
		if (scalarValues  == null) {
			return;
		} 
		if (scalarValues.size() == 1 && scalarValues.get(0).getScaleType() == ScaleType.EPISTEMIC) {
			Interval i = scalarValues.get(0).getValue();
			if (scaleType == ScaleType.EPISTEMIC) {
				Interval newValue = computeScaleValue(priorScalarValue,i);
				scalarValues.get(0).setValue(newValue);
//				scalarValues.put("EPISTEMIC", newValue);
			} else {
//				scalarValues.remove("EPISTEMIC");
//				scalarValues.put(name,new Interval(value,value));
				scalarValues.remove(0);
				scalarValues.add(new ScalarModalityValue(scaleType,new Interval(priorScalarValue,priorScalarValue)));
			}
		}
	}
	
	private static Interval computeScaleValue(double priorScalarValue, Interval valuesToUpdate) {
		double lower = valuesToUpdate.getBegin();
		double upper = valuesToUpdate.getEnd();
		double nLower = computeSingleScaleValue(priorScalarValue,lower);
		double nUpper = computeSingleScaleValue(priorScalarValue,upper);
		if (nLower > nUpper) return new Interval(nUpper,nLower);
		return new Interval(nLower,nUpper);
	}
	
	// TODO Needs to be tested against real examples
	private static double computeSingleScaleValue(double priorScalarValue, double initValue) {
		log.log(Level.FINEST,"Combining scalar value " + initValue + " with the prior value " + priorScalarValue);
		// may X = > X (0.5)
		if (initValue == 1.0) return priorScalarValue;
		// probably not X => X (0.25)
		else if (initValue == 0.0) return 1.0 - priorScalarValue;
		// probably might X => X (0.7)
		else if (initValue >= 0.5 && priorScalarValue > initValue ) 
			return Math.min(0.9,initValue + 0.2);
		// might probably X => X (0.55)
		else if (initValue > 0.5 && priorScalarValue <= initValue  && priorScalarValue >= 0.5) 
			return Math.min(0.5, initValue-0.2);
		// improbably probably X => X (0.25)
		else if (initValue > 0.5 && priorScalarValue < 0.5) return 1.0-initValue;
		// probably doubt  X =>  X (0.25)
		else if (initValue < 0.5 && priorScalarValue >= 0.5) return initValue;
		// improbably doubt X 
		else if (initValue < 0.5 && priorScalarValue < 0.5) return 1.0 - initValue;
		return initValue;
	}
	
	private static void updateScaleValues(ScaleShift shiftType, List<ScalarModalityValue> scalarValues) {
		if (scalarValues == null) return;
		for (ScalarModalityValue smv : scalarValues) {
			Interval i = smv.getValue();
			if (shiftType == ScaleShift.NEGATE) // scalarValues.add(new ScalarModalityValue(sc,negateScaleValue(i)));
				smv.setValue(negateScaleValue(i));
			else if (shiftType == ScaleShift.INCREASE) //scalarValues.add(new ScalarModalityValue(sc,increaseScaleValue(i)));
				smv.setValue(increaseScaleValue(i));
			else if (shiftType == ScaleShift.LOWER) //scalarValues.add(new ScalarModalityValue(sc,lowerScaleValue(i)));
				smv.setValue(lowerScaleValue(i));
			else if (shiftType == ScaleShift.HEDGE) //scalarValues.add(new ScalarModalityValue(sc,hedgeScaleValue(i)));
				smv.setValue(hedgeScaleValue(i));
		}
	}
	
	private static Interval negateScaleValue(Interval interval) {
		double lower = interval.getBegin();
		double upper = interval.getEnd();
		double nLower = negateValue(lower);
		double nUpper = negateValue(upper);
		if (nLower > nUpper) return new Interval(nUpper,nLower);
		return new Interval(nLower,nUpper);
	}
	
	// not sure here
	private static double negateValue(double val) {
		if (val > 0.0 && val <= 1.0) return 1.0-val;
		if (val == 0.0) return 0.5;
// 		if (val == 1.0 || val == 0.5 || val == 0.0) return 0.5;
//		if (val > 0.5 && val < 1.0) return 1.0 - val;
//		if (val < 0.5 && val > 0.0) return 1.0 - val;
		return 0.0;
	}
	
	private static Interval increaseScaleValue(Interval interval) {
		double lower = interval.getBegin();
		double upper = interval.getEnd();
		double nLower = increaseValue(lower);
		double nUpper = increaseValue(upper);
		if (nLower > nUpper) return new Interval(nUpper,nLower);
		return new Interval(nLower,nUpper);
		
	}
	
	private static double increaseValue(double val) {
		if (val == 1.0 || val == 0.0) return val;
		if (val >= 0.5) return Math.min(0.9, val+0.2);
		if (val < 0.5) return Math.max(0.1, val-0.2);
		return val;
	}
	
	private static Interval lowerScaleValue(Interval interval) {
		double lower = interval.getBegin();
		double upper = interval.getEnd();
		double nLower = lowerValue(lower);
		double nUpper = lowerValue(upper);
		if (nLower > nUpper) return new Interval(nUpper,nLower);
		return new Interval(nLower,nUpper);
		
	}
	
	private static double lowerValue(double val) {
		if (val == 1.0 || val == 0.0) return val;
		if (val >= 0.5) return Math.max(0.5, val-0.2);
		if (val < 0.5) return Math.min(0.4, val+0.2);
		return val;
	}
	
	private static Interval hedgeScaleValue(Interval interval) {
		double lower = interval.getBegin();
		double upper = interval.getEnd();
		double nLower = hedgeValue(lower);
		double nUpper = hedgeValue(upper);
		if (nLower > nUpper) return new Interval(nUpper,nLower);
		return new Interval(nLower,nUpper);
		
	}
	
	private static double hedgeValue(double val) {
		if (val == 1.0) return 0.8;
		if (val == 0.0) return 0.2;
		return val;
	}
}
