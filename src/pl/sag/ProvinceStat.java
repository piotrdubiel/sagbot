package pl.sag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;

public class ProvinceStat {
	Province province;
	float defence = 0.0f;
	float attack = 0.0f;
	int strength = 0;
	int competition = 0;
	ArrayList<Float> proximity = new ArrayList<Float>();
	Power owner;
	Game game;
	private KnowledgeBase knowledgeBase;

	private final static float ATTACK_MOD = 1.0f;
	private final static float DEFENCE_MOD = 1.0f;

	private final static int DEPTH = 4;
	
	public static class AttackSupporter {
		private final Power power;
		private final Vector<Region> from = new Vector<Region>();
		
		AttackSupporter (Power power) {
			this.power = power;
		}

		public Power getPower() {
			return power;
		}

		public Vector<Region> getAttackSources() {
			return from;
		}
		
		/**
		 * What is the possible impact of the power on strength value?
		 */
		public int getImpact() {
			return from.size();
		}
		
		public void addAttackSource(Region province) {
			from.add(province);
		}
		
	} /* class AttackSupporter */

	
	
	final Map<String, AttackSupporter> possibleAllyAttackSupporters = new TreeMap<String, AttackSupporter>();
	
	Collection<AttackSupporter> getPossibleAllyAttackSupporters() {
		return possibleAllyAttackSupporters.values();
	}
	
	final Map<String, AttackSupporter> possibleAttackSupporters = new TreeMap<String, AttackSupporter>();
	
	Collection<AttackSupporter> getPossibleAttackSupporters() {
		return possibleAttackSupporters.values();
	}
	
	public ProvinceStat(Province p, Power power, Game g, KnowledgeBase knowledge) {
		province = p;
		game = g;
		knowledgeBase = knowledge;
		if (province.isSC()) owner = game.getOwner(province);
		else owner = game.getController(province);

		// attack value
		if (owner != null && !owner.equals(power) && province.isSC()) {
			attack = power(owner.getOwnedSCs().size());
		}

		// defence value
		if (owner != null && owner.equals(power) && province.isSC()) {
			for (Region r : game.getAdjacentUnits(province)) {
				Power controller = game.getController(r);
				if (controller != null && knowledge.getStrength(controller.getName()) > defence) defence = power(controller.getOwnedSCs().size());
			}
		}
		
		// strength
		for (Region unit : game.getAdjacentUnits(province)) {
			Power controller = game.getController(unit);
			if (controller != null) {
				if (controller.equals(power))
					strength++;
				// doing things with friends is more fun, so why not conquer some lands together? ^^ 
				else if (knowledgeBase.getAllies().contains(controller.getName()) && !game.getController(province).equals(controller)) {
					final AttackSupporter as;
					if (!possibleAllyAttackSupporters.containsKey(controller.getName())) {
						as = new AttackSupporter(controller);
						possibleAllyAttackSupporters.put(controller.getName(), as);
					} else {
						as = possibleAllyAttackSupporters.get(controller.getName());
					}
					as.addAttackSource(unit);
				}
				// last but not least, we may want to make new alliances if it would support our case and we trust another player
				else if (knowledgeBase.getTrust(controller.getName()) > 0 && !game.getController(province).equals(controller)) {
					final AttackSupporter as;
					if (!possibleAttackSupporters.containsKey(controller.getName())) {
						as = new AttackSupporter(controller);
						possibleAttackSupporters.put(controller.getName(), as);
					} else {
						as = possibleAttackSupporters.get(controller.getName());
					}
					as.addAttackSource(unit);
				}
			}
		}

		// competition
		for (Power competitor : game.getPowers()) {
			int power_competition = 0;
			if (!competitor.equals(power)) {
				for (Region unit : game.getAdjacentUnits(province)) {
					Power controller = game.getController(unit);
					if (controller != null && controller.equals(competitor)) power_competition++;
				}
			}
			if (power_competition > competition) competition = power_competition;
		}
	}

	public String toString() {
		if (owner != null) return province.getName() + "(" + owner.getName() + ") Def: " + defence + " Att: " + attack + " Str: " + strength + " C: " + competition;
		else return province.getName() + " Def: " + defence + " Att: " + attack + " Str: " + strength + " C: " + competition;
	}

	public float getValue() {
		calculateProximity();
		float sum = 0.0f;
		for (float f : proximity) {
			sum += f;
		}
		return sum + strength - competition;
	}

	private void calculateProximity() {
		// init
		for (int i = 0; i < DEPTH; ++i) {
			proximity.add(0.0f);
		}
		
		// proximity
		// zero
		ProvinceStat stat = knowledgeBase.getProvinceStat(province.getName());
		proximity.set(0, ATTACK_MOD * stat.attack + DEFENCE_MOD * stat.defence);
		
		calculateNextProximity(province, 1);
	}

	private void calculateNextProximity(Province province, int depth) {
		if (depth >= DEPTH) return;
		float previous = proximity.get(depth);
		float sum = 0.0f;
		for (Province p : game.getAdjacentProvinces(province)) {
			ProvinceStat stat = knowledgeBase.getProvinceStat(p.getName());
			sum += ATTACK_MOD * stat.attack + DEFENCE_MOD * stat.defence;
		}
		proximity.set(depth, (previous + sum) / 5.0f);
		for (Province p : game.getAdjacentProvinces(province)) {
			calculateNextProximity(p, depth + 1);
		}
	}

	private float power(int scs) {
		return scs * scs;
	}

/*===========================================================================*/
/*= Comparators:                                                            =*/
/*===========================================================================*/

	public static final Comparator<ProvinceStat> AtDefComparator = new Comparator<ProvinceStat>() {

		@Override
		public int compare(ProvinceStat o1, ProvinceStat o2) {
			return Float.compare(o1.attack + o1.defence, o2.attack + o2.defence);
		}
		
	};
}
