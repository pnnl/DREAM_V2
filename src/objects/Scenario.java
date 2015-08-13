package objects;

public class Scenario implements Comparable<Scenario> {

	private String scenario;
	
	public Scenario(String scenario) {
		this.setScenario(scenario);
	}

	public String getScenario() {
		return scenario;
	}

	public void setScenario(String scenario) {
		this.scenario = scenario;
	}

	@Override
	public String toString() {
		return scenario;
	}

	@Override
	public int compareTo(Scenario o) {
		return this.toString().compareTo(o.toString());
	}
}
