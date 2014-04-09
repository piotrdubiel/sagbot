package pl.sag;

/** structure holding trust updates applicable to different situations */
public class TrustModifiers {
	public int aggression = -100;
	public int aggressedOurAlly = -20;
	public int peace = 10;
	
	public int alliance = 50;
	public int refusedAlliance = -20;
	public int alliedAgainstUs = -50;
	public int alliedAgainstOurAlly = -10;
	
	public int militarisedBorder = -10;
	public int refusedDemilitariseBorder = -10;
	
	public int refusedInformationalQuestion = -5;
}