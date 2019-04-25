package functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import objects.ExtendedConfiguration;
import objects.InferenceResult;
import objects.ScenarioSet;
import objects.ExtendedSensor;
import objects.TimeStep;
import utilities.Point2i;
import utilities.Point3f;
import utilities.Point3i;

/**
 * Extension of Function that is not currently used by DREAM
 * @author port091
 */

public class Vortex extends Function {



	boolean firstTime = true;
	@Override
	public InferenceResult inference(ExtendedConfiguration configuration, ScenarioSet set, String scenario) {

		String sensorType = set.getSensorSettings().keySet().toArray()[0].toString();
		Set<Integer> cloudNodes = set.getSensorSettings(sensorType).getValidNodes();
		List<Integer> sensorPositions = configuration.getSensorPositions(sensorType);
		
		int totalSensors = sensorPositions.size();
		if(totalSensors < 3) // Have to have at least 3
			return new InferenceResult(false, 0.0f); 
		
		int totalArea = 0;
		for(int k = 1; k <= set.getNodeStructure().getIJKDimensions().getK(); k++) {
			List<Integer> cloudPointsOnSurface = getCloudNodesOnSurface(k, cloudNodes, set);
			if(cloudPointsOnSurface.isEmpty())
				continue; // Nothing to help us on this layer
		//	System.out.println("Testing layer: " + k);
			// The each layer
			List<Integer> surfacePoints = getPointsOnSurface(k, sensorPositions, set);
			// For every sensor in our configuration, create all unique combinations of 3
			List<Point3i> triangles = getAllTriangles(surfacePoints.size());
			int area = getAreaForLayer(surfacePoints, triangles, cloudPointsOnSurface, set);
			totalArea += area;
		//	System.out.println("\tPoints we are considering: " + surfacePoints);
		//	System.out.println("\tArea =" + area);
		}

		return new InferenceResult(true, totalArea);
	}
	
	private int getAreaForLayer(List<Integer> surfacePoints, List<Point3i> triangles, List<Integer> cloudNodes, ScenarioSet set) {
		int area = 0;
		for(int node: cloudNodes) {

			// If the node is within the triangle we'll increase the area and move on to the next node.

			Point3i ijk = set.getNodeStructure().getIJKFromNodeNumber(node);
			//Vector3D P = new Vector3D(ijk.getI(), ijk.getJ(), ijk.getK());

			Point3f p = set.getNodeStructure().getXYZEdgeFromIJK(ijk);
			Vector3D P = new Vector3D(p.getX(), p.getY(), p.getZ());
			
			for(Point3i pt: triangles) {	

				Point3i ijk_a = set.getNodeStructure().getIJKFromNodeNumber(surfacePoints.get(pt.getI()));
				Point3i ijk_b = set.getNodeStructure().getIJKFromNodeNumber(surfacePoints.get(pt.getJ()));
				Point3i ijk_c = set.getNodeStructure().getIJKFromNodeNumber(surfacePoints.get(pt.getK()));

				Point3f a = set.getNodeStructure().getXYZEdgeFromIJK(ijk_a);
				Point3f b = set.getNodeStructure().getXYZEdgeFromIJK(ijk_b);
				Point3f c = set.getNodeStructure().getXYZEdgeFromIJK(ijk_c);

				Vector3D A = new Vector3D(a.getX(), a.getY(), a.getZ());
				Vector3D B = new Vector3D(b.getX(), b.getY(), b.getZ());
				Vector3D C = new Vector3D(c.getX(), c.getY(), c.getZ());

				//Vector3D A = new Vector3D(ijk_a.getI(), ijk_a.getJ(), ijk_a.getK());
				//Vector3D B = new Vector3D(ijk_b.getI(), ijk_b.getJ(), ijk_b.getK());
				//Vector3D C = new Vector3D(ijk_c.getI(), ijk_c.getJ(), ijk_c.getK());

				Vector3D v0 = new Vector3D(C.toArray()).subtract(A);
				Vector3D v1 = new Vector3D(B.toArray()).subtract(A);
				Vector3D v2 = new Vector3D(P.toArray()).subtract(A);

				float dot00 = (float) v0.dotProduct(v0);
				float dot01 = (float)  v0.dotProduct(v1);
				float dot02 = (float)  v0.dotProduct(v2);
				float dot11 = (float)  v1.dotProduct(v1);
				float dot12 = (float)  v1.dotProduct(v2);

				// Compute barycentric coordinates
				float invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
				float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
				float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

				boolean inside = (u >= 0) && (v >= 0) && (u + v <= 1);

				if(inside) {
					area++;
					break;
				}
			}
		}
		return area;
	}
	
	private List<Integer> getCloudNodesOnSurface(int surface, Set<Integer> sensorPositions, ScenarioSet set) {
		List<Integer> pointsOnSurface = new ArrayList<Integer>();
		// Any technology on the layer will become a point
		for(Integer position: sensorPositions) {
			if(set.getNodeStructure().getIJKFromNodeNumber(position).getK() == surface) {
				pointsOnSurface.add(position);
			}
		}
		return pointsOnSurface;
	}	
	private List<Integer> getPointsOnSurface(Integer surface, List<Integer> sensorPositions, ScenarioSet set) {
		List<Integer> pointsOnSurface = new ArrayList<Integer>();
		List<Integer> pointsAboveSurface = new ArrayList<Integer>();
		List<Integer> pointsBelowSurface = new ArrayList<Integer>();
		// Any technology on the layer will become a point
		for(Integer position: sensorPositions) {
			int location = surface.compareTo(set.getNodeStructure().getIJKFromNodeNumber(position).getK());
			if(location == 0) {
				pointsOnSurface.add(position);
			} else if(location > 0) {
				pointsBelowSurface.add(position);
			} else {
				pointsAboveSurface.add(position);
			}
		}
		
	//	System.out.println("\tPoints on the surface: " + pointsOnSurface);
		
		List<Point2i> lines = getAllLines(pointsAboveSurface.size(), pointsBelowSurface.size());
		
		
		// Calculate the intersection for each line pair
		for(Point2i pt: lines) {
			int z = surface;
			Point3i p0 = set.getNodeStructure().getIJKFromNodeNumber(pointsAboveSurface.get(pt.getI()));
			Point3i p1 = set.getNodeStructure().getIJKFromNodeNumber(pointsBelowSurface.get(pt.getJ()));
			
			int a = p0.getI();
			int b = p0.getJ();
			int c = p0.getK();
			
			int i = p1.getI();
			int j = p1.getJ();
			int k = p1.getK();
			
			float t = ((float)(z-c))/((float)(k-c));
			float x = ((1-t)*a)+(i*t);
			float y = ((1-t)*b)+(j*t);
			
			// Get the node number
			int intersectingNode = set.getNodeStructure().getNodeNumber(new Point3f(x, y ,z));
			//Point3i pi = set.getNodeStructure().getIJKFromNodeNumber(intersectingNode);
			if(!pointsOnSurface.contains(intersectingNode)) {
				pointsOnSurface.add(intersectingNode);
			}
		}
		
		return pointsOnSurface;
	}
	
	private List<Point2i>  getAllLines(int top, int bottom) {
		List<Point2i> lines = new ArrayList<Point2i>();
		for(int x = 0; x < top; x++) {
			for(int y = 0; y < bottom; y++) {	
				lines.add(new Point2i(x,y));
			}		
		}
		return lines;
	}

	private List<Point3i> getAllTriangles(int totalSensors) {
		List<Point3i> triangles = new ArrayList<Point3i>();
		for(int x = 0; x < totalSensors; x++) {
			for(int y = 0; y < totalSensors; y++) {
				for(int z = 0; z < totalSensors; z++) {
					if(x == y || x == z || y == z)
						continue;						
					List<Integer> xyz = new ArrayList<Integer>();
					xyz.add(x);
					xyz.add(y);
					xyz.add(z);
					Collections.sort(xyz);
					Point3i triangle = new Point3i(xyz.get(0), xyz.get(1), xyz.get(2));
					if(!triangles.contains(triangle))
						triangles.add(triangle);
				}
			}		
		}
		return triangles;
	}

	@Override
	public boolean mutate(ExtendedConfiguration configuration, ScenarioSet set) {
		return configuration.mutateSensorToEdgeOnly(set);
	}


	/*** Objective is same as CCS9_1 ***/

	@Override
	public Float objective(final ExtendedConfiguration configuration, final ScenarioSet set, boolean runThreaded) {
		List<Thread> threads = new ArrayList<Thread>();
		// Clear out previous information
		for (ExtendedSensor sensor : configuration.getExtendedSensors()) {
			sensor.clearScenariosUsed();
		}
		final int cores = Runtime.getRuntime().availableProcessors() - 1; //Use all but one core
		for (final String scenario: set.getScenarios()) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						innerLoopParallel(configuration, set, scenario);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			if(threads.size() < cores) {
				thread.start();
				threads.add(thread);
			}
		}
		for (Thread thread: threads)
		{
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return configuration.getObjectiveValue();
	}


	/**					**\
	 * Helper Methods	 *
	 * 					 *
	\*					 */

	public void innerLoopParallel(ExtendedConfiguration con, ScenarioSet set, String scenario) throws Exception
	{
		if (set.getScenarioWeights().get(scenario) > 0)
		{
			InferenceResult inferenceResult = null;
			for (TimeStep timeStep: set.getNodeStructure().getTimeSteps())
			{
				inferenceResult = runOneTime(con, set, timeStep, scenario, true);
				if (inferenceResult.isInferred())
					break;
			}

			// maxTime is an index, we want the value there
			float area = 1;		 // Bigger is better this time	
			if (inferenceResult.isInferred()) {
				area = -inferenceResult.getGoodness();
				con.addTimeToDetection(scenario, area);				
			}
			
			con.addObjectiveValue(scenario, area * set.getGloballyNormalizedScenarioWeight(scenario));
			con.addInferenceResult(scenario, inferenceResult);
		}
	}

	private InferenceResult runOneTime(ExtendedConfiguration con, ScenarioSet set, TimeStep timeStep, String scenario, boolean usedSensors) throws Exception
	{
		/* TODO: We need to update this to match the history query
		int time = timeStep.getTimeStep();
		for (ExtendedSensor sensor : con.getExtendedSensors())
		{
			// Make sure the sensor is in the cloud...
			if (sensor.isInCloud(set))
			{
				Float value = getHistory(scenario, sensor.getNodeNumber(), time, sensor.getSensorType());
				if(value == null) {				
					String query = "SELECT data_value from " + scenario + "_" + sensor.getSensorType() + " WHERE node_position=" + sensor.getNodeNumber() + " AND time_step_index=" + time;
					Constants.log(Level.FINEST, "CCS9_1 - objective QUERY", query);
					Map<String, Object> results = DatabaseUtilities.queryDatabase(query);
					if(results.containsKey("data_value") && results.get("data_value") instanceof Float) {
						value = (Float) results.get("data_value");
						storeHistory(scenario, sensor.getNodeNumber(), time, sensor.getSensorType(), value);
						Constants.log(Level.FINEST, "CCS9_1 - objective STORE HISTORY:\t@scenario=" + scenario.getScenario()
								+"\t@node=" + sensor.getNodeNumber() + "\t@time=" + time + 
								"\t@type="+sensor.getSensorType() +"\tVALUE=" + value, null);
					}
				} else {
					Constants.log(Level.FINEST, "CCS9_1 - objective GET HISTORY:\t@scenario=" + scenario.getScenario()
							+"\t@node=" + sensor.getNodeNumber() + "\t@time=" + time + 
							"\t@type="+sensor.getSensorType() +"\tVALUE=" + value, null);
				}

				SensorSetting temp = set.getSensorSettings().get(sensor.getSensorType());
				boolean triggered = value > temp.getThreshold();

				// Check the history
				if (!triggered && temp.isUsingHistory() && time > temp.getHistoryLength())
				{
					int previous_timestep_index = (int) (time - temp.getHistoryLength());
					float prev_value = sensor.getHistory(scenario, previous_timestep_index);
					triggered = ((value / prev_value) >= (1 + temp.getChangeOverHistory()));
				}

				sensor.setTriggered(triggered, scenario, timeStep, value);
			}
		}
		*/
		return inference(con, set, scenario);
	}

}
