package functions;

import objects.ExtendedConfiguration;
import objects.ScenarioSet;


public interface ObjectiveFunction {

	public Float objective(ExtendedConfiguration configuration, ScenarioSet set); 
}
