package pl.sag;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.io.*;

import es.csic.iiia.fabregues.dip.Player;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.comm.CommException;
import es.csic.iiia.fabregues.dip.comm.IComm;
import es.csic.iiia.fabregues.dip.comm.daide.DaideComm;
import es.csic.iiia.fabregues.dip.orders.DSBOrder;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.REMOrder;
import es.csic.iiia.fabregues.dip.orders.WVEOrder;

/**
 * SagSimpleBot is a skeleton to support the development of a bot capable to play DipGame negotiating about peace, alliances and/or orders.
 * 
 * @author Angela Fabregues, IIIA-CSIC, fabregues@iiia.csic.es
 */
public class SagSimpleBot extends Player {

	private InetAddress negoServerIp;
	private int negoServerPort;
	private SagSimpleNegotiator negotiator;
	private boolean debugMode;
	private BotObserver botObserver;
	 
	public SagSimpleBot(InetAddress negoServerIp, int negoServerPort) {
		super();
		this.negoServerIp = negoServerIp;
		this.negoServerPort = negoServerPort;
		this.debugMode = false;
		this.botObserver = new BotObserver(new KnowledgeBase("aaa", game), new Semaphore(0));
	}

	public void setDebug(boolean debugMode) {
		this.debugMode = debugMode;
		System.out.println("Setting debug mode to: " + debugMode);
	}

	/**
	 * Handling being accepted in a game
	 */
	@Override
	public void init() {
		System.out.println("Initializing..");
		negotiator = new SagSimpleNegotiator(negoServerIp, negoServerPort, this);
	}

	/**
	 * Handling the game starting
	 */
	@Override
	public void start() {
		System.out.println("Starting game..");
		negotiator.init();
	}
	
	/**
	 * Decide what orders to send to your units
	 */
	@Override
	public List<Order> play() {
		negotiator.negotiate();	// negotiations should probably be made after the tactic planning phase

		/* HoldBot code */
		List<Order> orders = new Vector<Order>();
		switch (game.getPhase()) {
		case SPR:
		case FAL:
			//Holding all controlled units
			for (Region unit: me.getControlledRegions()) {
				HLDOrder hldOrder = new HLDOrder(me, unit);
				orders.add(hldOrder);
			}
			break;
		case SUM:
		case AUT:
			//Disbanding all dislodged units
			for (Region dislodgedUnit: game.getDislodgedRegions(me)) {
				DSBOrder dsbOrder = new DSBOrder(dislodgedUnit, me);
				orders.add(dsbOrder);
			}
			break;
		default:
			//That's WIN
			int nBuilds = me.getOwnedSCs().size() - me.getControlledRegions().size ();
			if (nBuilds > 0) {
				//Waiving nBuilds times
				for (int i = 0; i < nBuilds; i++) {
					WVEOrder wveOrder = new WVEOrder(me);
					orders.add(wveOrder);
				}
			} else if (nBuilds < 0) {
				//Removing nBuilds units
				int nRemovals = -nBuilds;
				for (int i = 0; i < nRemovals; i++) {
					Region remUnit = me.getControlledRegions().get (i);
					REMOrder remOrder = new REMOrder(me, remUnit);
					orders.add(remOrder);
				}
			}
			break;
		}

		if (this.debugMode) {
			System.out.println("round finished. press Enter to continue or type 'auto' to go on without prompting..");
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String command;
			try {
				command = reader.readLine();
				if (command.equals("auto")) {
					setDebug(false);
				}
			} catch (IOException ioe) {
				System.out.println("IO error waiting for enter!");
			}
		}

		return orders;
	}

	/**
	 * Handling the reception of previous phase performed orders
	 */
	@Override
	public void receivedOrder(Order arg0) {
		
	}
	
	/**
	 * Program that runs SagSimpleBot connected to a gameManager (gameServer + negoServer)
	 * @param args
	 */
	public static void main(String[] args) {
		InetAddress negoServerIp;
		int negoServerPort;
		InetAddress gameServerIp;
		int gameServerPort;
		String name;
		boolean debugMode = false;
		final String usageString = "Usage:\n SagSimpleBot [<gameServerIp> <gameServerPort> <negoServerIp> <negoServerPort> <name> (<debug>)]";
		try {
			if (args.length == 0) {
				gameServerIp = InetAddress.getLocalHost();
				gameServerPort = 16713;
				negoServerIp = InetAddress.getLocalHost();
				negoServerPort = 16714;
				name = "SagSimpleBot";
			} else if (args.length >= 5) {
				gameServerIp = InetAddress.getByName(args[0]);
				gameServerPort = Integer.valueOf(args[1]);
				negoServerIp = InetAddress.getByName(args[2]);
				negoServerPort = Integer.valueOf(args[3]);
				name = args[4];
				if (args.length == 6) {
					debugMode = Boolean.valueOf(args[5]);
				}
			} else {
				System.err.println(usageString);
				return;
			}
		} catch (UnknownHostException e) {
			System.err.println(usageString);
			return;
		} catch (NumberFormatException e) {
			System.err.println(usageString);
			return;
		}
		
	  try {
		  	System.out.println("Connecting to: " + gameServerIp + ":" + gameServerPort);
			IComm comm = new DaideComm(gameServerIp, gameServerPort, name);
			SagSimpleBot SagSimpleBot = new SagSimpleBot(negoServerIp, negoServerPort);
			SagSimpleBot.setDebug(debugMode);
			SagSimpleBot.start(comm);
	  } catch (CommException e) {
	  	System.err.println("Cannot connect to the server.");
	  }
	}
}
