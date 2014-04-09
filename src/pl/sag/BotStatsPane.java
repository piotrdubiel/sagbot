package pl.sag;
import java.awt.Color;
import java.util.Map;

/**
 * Displays additional knowledge for the power played by bot.
 * 
 * @author alex
 *
 */
public class BotStatsPane extends PowerStatsPane {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8146740730352391595L;
	TextPane trustPane;
	TextPane strengthPane;
	
	public BotStatsPane(String powerName) {
		super(powerName);
		trustPane = new TextPane("trust");
		trustPane.setColor(Color.lightGray);
		this.add(trustPane);
		
		strengthPane = new TextPane("strength");
		strengthPane.setColor(Color.white);
		this.add(strengthPane);
	}
	
	public void updateFrom(KnowledgeBase base) {
		updateFrom((PowerKnowledgeBase) base);
		updateTrust(base);
		updateStrength(base);
	}
	
	protected void updateTrust(KnowledgeBase base) {
		trustPane.clear();
		Map<String, Integer> trust = base.getSortedTrust();
		for (String power : trust.keySet()) {
			int value = trust.get(power);
			trustPane.append(power + '\t' + value);
		}
	}
	
	protected void updateStrength(KnowledgeBase base) {
		strengthPane.clear();
		Map<String, Integer> strength = base.getSortedStrength();
		for (String power : strength.keySet()) {
			int value = strength.get(power);
			strengthPane.append(power + '\t' + value);
		}
	}
}
