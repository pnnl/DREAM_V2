package gravity;

/**
 * Class is designed to calculate the wet bulk density of our input file. TODO:
 * Think about how we're going to interpolate the data
 * 
 * @author huan482
 *
 */
//Class to work in would be parse raw files, we should check if all the required elements are checked.
//Shared variables  
public class CalculateWetBulkDensity {

	private double CO2_saturation;

	private double porosity;

	private double timeLapseChange;

	private double singleTimeWBD;

	private double brineDensity;

	public CalculateWetBulkDensity(final double SCO2, final double thePorosity, final double brineD) {
		CO2_saturation = SCO2;
		porosity = thePorosity;
		brineDensity = brineD;
		timeLapseChange = 0;
		singleTimeWBD = 0;
	}
	/**
	 * Method that calculates the different of 2 wet bulk densities between 2 different time steps.
	 * @param baselineCO2Sat - The baseline CO2 Saturation we're going to compare with.
	 * @param CO2D - CO2 Density
	 * @return - The  time lapse change of the Wet Bulk Density
	 */
	public double calculateTimeLapseWBD(final double baselineCO2Sat, final double CO2D) {
		// Formula del_P = delSC02 ro(pc02 - pbrine)
		timeLapseChange = (CO2_saturation - baselineCO2Sat) * porosity * (CO2D - brineDensity);
		return timeLapseChange;
	}
	/**
	 * Method to calculate the wet bulk density in a single time step.
	 * @param rockDensity - Rock Density
	 * @param brineSaturation - Brine Saturation
	 * @param co2BrineDensity - CO2 Brine Density
	 * @param co2Saturation - CO2 Saturation
	 * @return - Wet Bulk Density
	 */
	public double calculateSingleTimeWBD(final double rockDensity, final double brineSaturation,
			final double co2BrineDensity, final double co2Saturation) {
		// Formula: Wet_Bulk_Density = ((1 - porosity) * Rock_Density) + (porosity *
		// Brine_saturation * Brine_density) + (porosity * CO2_brine_Density *
		// CO2_Saturation)
		singleTimeWBD = ((1 - porosity) * rockDensity) + (porosity * brineSaturation * brineDensity)
				+ (porosity * co2BrineDensity * co2Saturation);
		return singleTimeWBD;

	}

}
