package pl.sag;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Observable;
import java.util.Set;
import java.util.Vector;

import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;


/** Represents a certain agents knowledge about his treaties */
public class PowerKnowledgeBase extends Observable {
	protected HashMap<String, Set<String>> alliances;	/**< with whom (key) against what powers? */
	protected Set<String> peaceTreaties;	/**< with whom peace treaties where signed? */
	final protected HashSet<String> otherPowerNames;		/**< other powers existing in the game */
	final protected HashSet<String> allPowerNames;
	final protected String powerName;		/**< name of the power this knowledge base is associated with */
	final protected Power power;
	
	public PowerKnowledgeBase(String powerName, Power power) {
		this.alliances = new HashMap<String, Set<String>>();
		
		this.otherPowerNames = new HashSet<String>(Arrays.asList("AUS", "ENG",
				"FRA","GER", "ITA", "RUS", "TUR"));
		this.allPowerNames = new HashSet<String>();
		allPowerNames.addAll(otherPowerNames);

		this.otherPowerNames.remove(powerName);
		assert(!otherPowerNames.contains(powerName));
		
		this.peaceTreaties = new HashSet<String>();
		for (String currPowerName : otherPowerNames) {
			peaceTreaties.add(currPowerName);
		}
		
		for (String name: this.otherPowerNames) {
			alliances.put(name, new HashSet<String>());
		}
		this.powerName = powerName;
		this.power = power;
	}
	
	public String getPowerName() {
		return powerName;
	}
	
	public Power getPower() {
		return power;
	}
	
	public Vector<String> getSupplyCenters() {
		Vector<String> centers = new Vector<String>();
		for (Province supplyCenter: power.getOwnedSCs()) {
			centers.add(supplyCenter.getName());
		}
		Collections.sort(centers);
		return centers;
	}
	
	public Vector<String> getHomes() {
		Vector<String> centers = new Vector<String>();
		for (Province supplyCenter: power.getHomes()) {
			centers.add(supplyCenter.getName());
		}
		Collections.sort(centers);
		return centers;
	}
	
	public Vector<String> getRegions() {
		Vector<String> regions = new Vector<String>();
		for (Region region: power.getControlledRegions()) {
			regions.add(region.getName());
		}
		Collections.sort(regions);
		return regions;
	}
	
	public Vector<String> getOtherPowerNames() {
		Vector<String> powerNames = new Vector<String>();
		for (String power : otherPowerNames) {
			powerNames.add(power);
		}
		Collections.sort(powerNames);
		return powerNames;
	}
	
	public Set<String> getWars() {
		HashSet<String> wars = new HashSet<String>();
		for (String power : otherPowerNames) {
			if (!peaceTreaties.contains(power)) {
				wars.add(power);
			}
		}
		return wars;
	}
	
	public Set<String> getAllies() {
		Set<String> allies = new HashSet<String>();
		for (String power : otherPowerNames) {
			if (alliances.get(power).size() > 0) {
				allies.add(power);
			}
		}
		return allies;
	}
	
	public HashMap<String, Set<String>> getAlliances() {
		return alliances;
	}

	public void addPeace(String power) {
		assert(otherPowerNames.contains(power));
		this.peaceTreaties.add(power);
	}
	
	public void addAlliance(String ally, String enemy) {
		assert(otherPowerNames.contains(ally));
		assert(otherPowerNames.contains(enemy));
		this.alliances.get(ally).add(enemy);
		addPeace(ally);	// alliance means peace
	}
	
	public void addAggression(String enemy) {
		assert(otherPowerNames.contains(enemy));
		this.peaceTreaties.remove(enemy);
		this.alliances.get(enemy).clear();
	}
}
