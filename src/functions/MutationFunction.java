package functions;

import objects.ExtendedConfiguration;
import objects.ScenarioSet;

public interface MutationFunction {

	public static enum MUTATE {
		SENSOR,
		WELL
	}
	
	public boolean mutate(ExtendedConfiguration configruation, ScenarioSet set);
}
