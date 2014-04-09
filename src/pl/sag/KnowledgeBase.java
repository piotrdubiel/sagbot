package pl.sag;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;


/** Represents a certain agents knowledge about gameplay and other powers */
public class KnowledgeBase extends PowerKnowledgeBase {
	protected HashMap<String, Integer> trust;	/**< MIN_TRUST, MAX_TRUST how much we trust a power? */
	protected HashMap<String, Integer> strength;	/**< how strong we believe a power to be? */
	protected HashMap<String, PowerKnowledgeBase> powers;	/** knowledge about other powers knowledge */	
	
	protected final TrustModifiers trustModifiers;	/** event->trust update */
	protected final StrengthModifiers strengthModifiers; /** event->strength modifier */
	protected final int MAX_TRUST = 200;
	protected final int MIN_TRUST = -200;
	
	protected HashMap<String, ProvinceStat> provinces;
	
	public static Map<String, Integer> sortByValue(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(map.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {

            public int compare(Map.Entry<String, Integer> m1, Map.Entry<String, Integer> m2) {
                return (m2.getValue()).compareTo(m1.getValue());
            }
        });

        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
	
	
	public KnowledgeBase(String powerName, Game game) {
		super(powerName, game.getPower(powerName));
		trust = new HashMap<String, Integer>();
		strength = new HashMap<String, Integer>();
		powers = new HashMap<String, PowerKnowledgeBase>();
		for (String power : this.otherPowerNames) {
			powers.put(power, new PowerKnowledgeBase(power, game.getPower(power)));
			trust.put(power, 0);
		}
		trustModifiers = new TrustModifiers();
		strengthModifiers = new StrengthModifiers();
		recalculateStrengths();
		
		provinces = new HashMap<String, ProvinceStat>();
	}
	
	protected void updateTrust(String power, int update) {
		assert(otherPowerNames.contains(power));
		int newTrust = this.trust.get(power) + update;
		if (newTrust > MAX_TRUST) {
			newTrust = MAX_TRUST;
		} else if (newTrust < MIN_TRUST) {
			newTrust = MIN_TRUST;
		}
		this.trust.put(power, newTrust);
		stateChanged();
	}

	public final PowerKnowledgeBase getPowerKnowledge(String power) {
		if (power.equals(this.powerName)) {
			return this;
		} else {
			return powers.get(power);
		}
	}
	
	public int getTrust(String power) {
		return trust.get(power);
	}
	
	public Map<String, Integer> getSortedTrust() {
		return sortByValue(trust);
	}
	
	public int getStrength(String power) {
		return strength.get(power);
	}
		
	public Map<String, Integer> getSortedStrength() {
		return sortByValue(strength);
	}
	
	public void recalculateStrengths() {
		for (String powerName : this.allPowerNames) {
			PowerKnowledgeBase power = getPowerKnowledge(powerName);
			Vector<String> supplyCenters = power.getSupplyCenters();
			Vector<String> homes = power.getHomes();
			int powerStrength = 0;
			// include owned supply centers
			powerStrength += supplyCenters.size() * strengthModifiers.ownedSupplyCenter;
			// include owned homes
			for (String home : homes) {
				if (supplyCenters.contains(home)) {
					powerStrength += strengthModifiers.ownedSupplyCenterIsHome;
				}
			}
			// include controlled units (number may be different than number of SCs)
			powerStrength += power.getRegions().size() * strengthModifiers.ownedUnit;
			strength.put(powerName, powerStrength);
		}
	}
	
	public void stateChanged() {
		recalculateStrengths();
		this.setChanged();
		this.notifyObservers();
	}
	
	@Override
	public void addPeace(String power) {
		super.addPeace(power);
		updateTrust(power, trustModifiers.peace);
		stateChanged();
	}
	
	public void addPeace(String power1, String power2) {
		if (power1.equals(powerName)) {
			addPeace(power2);
		} else if (power2.equals(powerName)) {
			addPeace(power1);
		} else {
			powers.get(power1).addPeace(power2);
			powers.get(power2).addPeace(power1);
			stateChanged();
		}
	}
	
	@Override
	public void addAlliance(String ally, String enemy) {
		super.addAlliance(ally, enemy);
		powers.get(ally).addAlliance(powerName, enemy);
		updateTrust(ally, trustModifiers.alliance);
		stateChanged();
	}
	
	public void addAlliance(String ally1, String ally2, String enemy) {
		if (ally1.equals(powerName)) {
			addAlliance(ally2, enemy);
		} else if (ally2.equals(powerName)) {
			addAlliance(ally1, enemy);
		} else {
			powers.get(ally1).addAlliance(ally2, enemy);
			powers.get(ally2).addAlliance(ally1, enemy);
			if (enemy.equals(powerName)) {
				// doesn't break alliances with us, aggression will..
				updateTrust(ally1, trustModifiers.alliedAgainstUs);
				updateTrust(ally2, trustModifiers.alliedAgainstUs);
			} else if (getAllies().contains(enemy)) {
				updateTrust(ally1, trustModifiers.alliedAgainstOurAlly);
				updateTrust(ally2, trustModifiers.alliedAgainstOurAlly);
			}
			stateChanged();
		}
	}
	
	public void refusedAlliance(String power) {
		updateTrust(power, trustModifiers.refusedAlliance);
		stateChanged();
	}
	
	@Override
	public void addAggression(String enemy) {
		super.addAggression(enemy);
		powers.get(enemy).addAggression(powerName);
		updateTrust(enemy, trustModifiers.aggression);
		stateChanged();
	}

	public void addAggression(String aggressor, String victim) {
		if (victim.equals(powerName)) {
			this.addAggression(aggressor);
		} else if (aggressor.equals(powerName)) {
			powers.get(victim).addAggression(powerName);
			// no trust update, we still trust them :)
			stateChanged();
		} else {
			if (this.getAllies().contains(victim)) {
				updateTrust(aggressor, trustModifiers.aggressedOurAlly);
			}
			powers.get(victim).addAggression(aggressor);
			powers.get(aggressor).addAggression(victim);
			stateChanged();
		}
	}	
	
	/** border militarisation */
	public void militarisedBorder(String power) {
		updateTrust(power, trustModifiers.militarisedBorder);
		stateChanged();
	}
	
	public void refusedDemilitariseBorder(String power) {
		updateTrust(power, trustModifiers.refusedDemilitariseBorder);
		stateChanged();
	}
	
	/** power refused to answer our question */
	public void refusedInformationalQuestion(String power) {
		updateTrust(power, trustModifiers.refusedInformationalQuestion);
		stateChanged();
	}
	/* we don't trust a power more if it has answered out question */
	
	public void addProvinceStat(String name, ProvinceStat stat) {
		provinces.put(name, stat);
	}
	
	public ProvinceStat getProvinceStat(String name) {
		return provinces.get(name);
	}

/*===========================================================================*/
/*= Nego-zone:                                                              =*/
/*===========================================================================*/

	/**
	 * Orders associated with regions.
	 * 
	 * This will hold either move order for our unit that we want supported by
	 * other player, or a support order that we promised to other player. This
	 * hash allows to track what deals we have made and stop promising/asking
	 * when we have plan for a unit in the province (region).
	 * 
	 * If our proposal fails, associated move order is removed from the hash.
	 * 
	 * When normal evaluation starts this hash is used to boost values of orders
	 * that were successfully negotiated.
	 */
	HashMap<Region, Order> regionAgreements = new HashMap<Region, Order>();
	
	public Order getRegionOrder(Region region) {
		return regionAgreements.get(region);
	}

	public void setRegionOrder (Region region, Order order) {
		regionAgreements.put(region, order);
	}
	
	public void clearNegotiationsData() {
		regionAgreements.clear();
	}

	public void removeRegionOrder(Region region) {
		regionAgreements.remove(region);		
	}
}
