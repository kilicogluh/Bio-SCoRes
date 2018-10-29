package tasks.coref.i2b2;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.bioscores.exp.ExpressionFilter;
import gov.nih.nlm.bioscores.exp.ExpressionFilterImpl;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.AppositiveDetection;

/** 
 * A slightly different version of the anaphoricity filter, originally
 * implemented in {@link ExpressionFilterImpl.AnaphoricityFilter}. <p>
 * 
 * The main different is that this implementation does not treat coreferential 
 * mentions any different than plain entities when considering them for
 * rigid designators. This works slightly better on i2b2/VA dataset.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO Find a way to merge AnaphoricityFilters without a performance hit.
public class I2B2AnaphoricityFilter implements ExpressionFilter {
	
	@Override
	public boolean filter(CoreferenceType corefType, Expression exp) {
		ExpressionType type = ExpressionType.valueOf(exp.getType());
		if (ExpressionType.NOMINAL_TYPES.contains(type) == false) return false;
		SurfaceElement su = exp.getSurfaceElement();
		if (Expression.isCataphoric(type,su)) return false;
		if (CoreferenceUtils.isModified(type,su,false)) return false;
		return (AppositiveDetection.hasSyntacticAppositives(su) == false);
	}
}
