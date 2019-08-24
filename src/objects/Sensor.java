package objects;

import java.util.Map;

import utilities.Point3f;
import utilities.Point3i;

/**
 * Basic monitoring technology class
 * @author port091
 * @author rodr144
 */

public class Sensor {
	
	//Static functions for accessing the aliases
	static public Map<String, String> sensorAliases;
	
	// About the sensors location
	protected Point3i node;
	protected Point3f point;
	protected int nodeNumber;
	
	// What type of sensor
	protected String type;
    
    public Sensor(int i, int j, int k, String type, NodeStructure domain) {
    	
    	node =  new Point3i(i,j,k);
    	nodeNumber = domain.getNodeNumberFromIJK(node);
    	point = domain.getXYZFromIJK(node);
    	
    	this.type = type;
    }
    
    public Sensor(int nodeNumber, String type, NodeStructure domain) {
    	
    	this.nodeNumber = nodeNumber;
    	node = domain.getIJKFromNodeNumber(nodeNumber);
    	point = domain.getXYZFromIJK(node);
    	
    	this.type = type;
    }
    
    public Sensor(Sensor toCopy) {
    	
    	this.nodeNumber = toCopy.nodeNumber;
    	this.node = new Point3i(toCopy.node);
    	this.point = new Point3f(toCopy.point);
    	
    	this.type = toCopy.type;    	
    }
	
	public Integer getNodeNumber() {
		return nodeNumber;
	}
	
	public void setLocation(int i, int j, int k, NodeStructure domain) {
		node.setI(i);
		node.setJ(j);
		node.setK(k);
		node =  new Point3i(i,j,k);
    	nodeNumber = domain.getNodeNumberFromIJK(node);
    	point = domain.getXYZFromIJK(node);
	}
	
	public String getSensorType() {
		return type;
	}
	
	public Point3i getIJK() {
		return node;
	}
	
	public Point3f getXYZ() {
		return point;
	}
	
    @Override
    public int hashCode() {
    	return type != null ? type.hashCode() * 37 + nodeNumber : nodeNumber;
    }

}
