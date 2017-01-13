package functions;

import objects.ExtendedConfiguration;
import objects.ScenarioSet;

/**
 * Enum for what mutate logic to use in the simulated annealing algorithm
 * @author port091
 */

public interface MutationFunction {

	public static enum MUTATE {
		SENSOR,
		WELL
	}
	
	public boolean mutate(ExtendedConfiguration configruation, ScenarioSet set);
}
