package pl.sag;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.swing.JPanel;


/**
 * Displays standard knowledge about a power in game.
 * 
 * @author alex
 *
 */
public class PowerStatsPane extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1805554328691484106L;
	
	private TextPane treatiesPane;
	private TextPane provincesPane;
	private TextPane unitsPane;
	
	public PowerStatsPane(String powerName) {
		super();
		this.setName(powerName);
		
		treatiesPane = new TextPane("treaties");
		treatiesPane.setColor(Color.white);

		provincesPane = new TextPane("provinces");
		provincesPane.setColor(Color.lightGray);
		
		unitsPane = new TextPane("units");
		unitsPane.setColor(Color.white);
		
		GridLayout layout = new GridLayout(1, 0);
		layout.setHgap(5);
		this.setLayout(layout);
		this.add(treatiesPane);
		this.add(provincesPane);
		this.add(unitsPane);
	}
	
	public void updateFrom(PowerKnowledgeBase base) {
		updatePeaceTreaties(base);
		updateProvinces(base);
		updateUnits(base);
	}
	
	protected void updatePeaceTreaties(PowerKnowledgeBase base) {
		Vector<String> powerNames = base.getOtherPowerNames();
		HashMap<String, Set<String>> alliances = base.getAlliances();
		Set<String> wars = base.getWars();
		
		treatiesPane.clear();
		for (String name : powerNames) {
			if (alliances.containsKey(name)) {
				Set<String> enemies = alliances.get(name);
				if (enemies.size() > 0) { 
					String allianceInfo = name + " allied against: ";
					for (String enemy : enemies) {
						allianceInfo = allianceInfo + enemy + ", ";
					}
					treatiesPane.append(allianceInfo);
				}
			}
			if (wars.contains(name)) {
				treatiesPane.append(name + " war");
			}
		}
	}
	
	protected void updateProvinces(PowerKnowledgeBase base) {
		provincesPane.clear();
		Vector<String> homes = base.getHomes();
		Vector<String> centers = base.getSupplyCenters();
		// print controlled homes first
		for (String province : homes) {
			if (centers.contains(province)) {
				provincesPane.append(province + " (home)");
			}
		}
		for (String province : centers) {
			if (!homes.contains(province)){
				provincesPane.append(province);
			}
		}
	}
	
	protected void updateUnits(PowerKnowledgeBase base) {
		unitsPane.clear();
		for (String region : base.getRegions()) {
			unitsPane.append(region);
		}
	}
}