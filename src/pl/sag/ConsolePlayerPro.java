package pl.sag;


import java.net.InetAddress;
import java.util.List;
import java.util.Vector;

import es.csic.iiia.fabregues.dip.Player;
import es.csic.iiia.fabregues.dip.board.Dislodgement;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.comm.CommException;
import es.csic.iiia.fabregues.dip.comm.IComm;
import es.csic.iiia.fabregues.dip.comm.Order2StringA;
import es.csic.iiia.fabregues.dip.comm.ParseService;
import es.csic.iiia.fabregues.dip.comm.Parser;
import es.csic.iiia.fabregues.dip.comm.StringA2Order;
import es.csic.iiia.fabregues.dip.comm.daide.DaideComm;
import es.csic.iiia.fabregues.dip.orders.DSBOrder;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.REMOrder;
import es.csic.iiia.fabregues.dip.orders.RTOOrder;
import es.csic.iiia.fabregues.dip.orders.WVEOrder;
import es.csic.iiia.fabregues.utilities.Console;

/**
 * Play Diplomacy from a console application. Check {@link http
 * ://www.dipgame.org} for a better way to play. A log file is generated.
 * 
 * @author Angela Fabregues, IIIA-CSIC, fabregues@iiia.csic.es
 */
public class ConsolePlayerPro extends Player {

	private static String name = "ConsolePlayerPro";
	private StringA2Order orderParser;
	private List<Order> orders;

	public ConsolePlayerPro() {
		super();
	}

	/**
	 * Informs about the map type before starting the game
	 */
	public void init() {
		orderParser = new StringA2Order(game);
		System.out.println("Map " + mapName);
	}

	/**
	 * Starts the player when the powers are assigned 
	 */
	public void start(){
		System.out.println("We are " + getMe().getName());
	}
	
	/**
	 * Plays sending the orders introduced by the user
	 * 
	 * @return list of orders
	 */
	public List<Order> play() {

		System.out.print("\n\nCurrent phase: ");
		printStringA(Parser.getNOW(game));
		System.out.println();

		orders = new Vector<Order>();
		
		switch (game.getPhase()) {
		case SPR:
		case FAL:
			if (0 < me.getControlledRegions().size()) {
				readOrders(me.getControlledRegions().size());
				movementPhase(orders);
			}
			break;
		case AUT:
		case SUM:
			if (game.getDislodgedRegions(me).size() > 0) {
				readOrders(game.getDislodgedRegions(me).size());
				retreatementPhase(orders);
			}
			break;
		case WIN:
			if (me.getControlledRegions().size() > me.getOwnedSCs().size()) { // REMOVE
				readOrders(me.getControlledRegions().size() - me.getOwnedSCs().size());
				removingPhase(orders);
			} else if (me.getControlledRegions().size() < me.getOwnedSCs().size()) { // BUILD
				readOrders(me.getOwnedSCs().size() - me.getControlledRegions().size());
				buildingPhase(orders);
			}
			break;
		}

		showOrders();
		return orders;
	}

	/**
	 * Reads the a set of orders introduced by the user. Checks the syntaxis of
	 * them.
	 * 
	 * @param count
	 *            . Use -1 for unlimited orders or a positive value to fix the
	 *            number of orders to ask for.
	 */
	private void readOrders(int count) {
		String msg = Console.readString("  PRESS INTRO TO WRITE ORDERS\nEnter an order... \"exemple of order: ( ENG AMY LVP ) MTO NTH\"\n").toUpperCase();
		while (msg.length() > 0) {
			try {
				Order order = orderParser.processOrder(ParseService.toStringArr(msg));
				Order2StringA.getOrderMsg(order); // to check if it is correctly
				// written
				orders.add(order);
				count--;
			} catch (Exception e) {
				System.out.print("Impossible to understand order\n");
				System.out.print(msg);
				msg = Console.readString("\n  PRESS INTRO TO WRITE ORDERS\nEnter an order again... \"exemple: ( ENG AMY LVP ) MTO NTH\"\n").toUpperCase();
				continue;
			}
			if (count == 0) {
				break;
			}
			msg = Console.readString("\n  PRESS INTRO TO WRITE ORDERS\nEnter an order... \"exemple: ( ENG AMY LVP ) MTO NTH\"\n").toUpperCase();
		}
	}

	/**
	 * Complete orders for SPR and FAL phases
	 * 
	 * @param orders
	 */
	private void movementPhase(List<Order> orders) {
		// Create a list with all ordered regions
		List<Region> orderedRegions = new Vector<Region>(me.getControlledRegions().size());
		for (Order order : orders) {
			orderedRegions.add(order.getLocation());
		}

		// Create a hold order for each controlled and not ordered region
		for (Region region : me.getControlledRegions()) {
			if (!orderedRegions.contains(region)) {
				orders.add(new HLDOrder(me, region));
				orderedRegions.add(region);
			}
			if (orderedRegions.size() == me.getControlledRegions().size()) {
				break;
			}
		}
	}

	/**
	 * Complete orders for SUM and AUT phases
	 * 
	 * @param orders
	 */
	private void retreatementPhase(List<Order> orders) {
		// Create a list with all ordered regions
		List<Region> orderedRegions = new Vector<Region>(me.getControlledRegions().size());
		for (Order order : orders) {
			orderedRegions.add(order.getLocation());
		}

		// Create a retreat for each dislodged unit or a disband if it is not
		// possible to do a retreat.
		for (Region region : game.getDislodgedRegions(me)) {
			if (!orderedRegions.contains(region)) {
				Dislodgement dislodgement = game.getDislodgedRegions().get(region);
				if (dislodgement.getRetreateTo().size() > 0) {
					orders.add(new RTOOrder(region, me, dislodgement.getRetreateTo().firstElement()));
				} else {
					orders.add(new DSBOrder(region, me));
				}
				orderedRegions.add(region);
			}
		}

	}

	/**
	 * Complete removals for WIN phase
	 * 
	 * @param orders
	 */
	private void removingPhase(List<Order> orders) {
		if (me.getControlledRegions().size() - me.getOwnedSCs().size() > orders.size()) {
			// Create a list with all ordered regions
			List<Region> orderedRegions = new Vector<Region>(me.getControlledRegions().size());
			for (Order order : orders) {
				orderedRegions.add(order.getLocation());
			}

			// Create a remove for each extra unit
			int i = 0;
			while (me.getControlledRegions().size() - me.getOwnedSCs().size() > orders.size()) {
				Region region = me.getControlledRegions().get(i);
				if (!orderedRegions.contains(region)) {
					orders.add(new REMOrder(me, region));
					orderedRegions.add(region);
				}
			}
		}
	}

	/**
	 * Complete builds for WIN phase
	 * 
	 * @param orders
	 */
	private void buildingPhase(List<Order> orders) {
		// Create a waive for each unit that we can build
		while (me.getOwnedSCs().size() - me.getControlledRegions().size() > orders.size()) {
			orders.add(new WVEOrder(me));
		}
	}

	/**
	 * Informs about submitted orders
	 */
	public void receivedOrder(Order order) {
		System.out.print("Submitted order: ");
		printStringA(Order2StringA.getOrderMsg(order));
	}

	/**
	 * Informs about the winner of the game and exits
	 */
	public void handleSlo(String winner) {
		System.out.println("\nWinner: " + winner);
	}

	/**
	 * Informs about an error and allows to resubmit a new order
	 */
	public void submissionError(String[] message) {
		super.submissionError(message);
		System.out.print("There is a problem with the following order: ");
		printStringA(message);
		System.out.println("You should write it again");
		resubmitOrder();
	}

	/**
	 * Informs about a missing order and allows to submit it
	 */
	public void missingOrder(String[] message) {
		super.missingOrder(message);
		System.out.print("The following order is missing: ");
		printStringA(message);
		System.out.println("You should write it");
		resubmitOrder();
	}

	/**
	 * Reads an order
	 */
	private void resubmitOrder() {
		readOrders(1);
		switch (game.getPhase()) {
		case SPR:
		case FAL:
			movementPhase(orders);
			break;
		case AUT:
		case SUM:
			retreatementPhase(orders);
			break;
		case WIN:
			if (me.getControlledRegions().size() > me.getOwnedSCs().size()) { // remove
				removingPhase(orders);
			} else if (me.getControlledRegions().size() < me.getOwnedSCs().size()) { // build
				buildingPhase(orders);
			}
			break;
		}

		showOrders();
		try {
			sendOrders(orders);
		} catch (CommException e) {
			log.printError(e.getLocalizedMessage());
			exit();
		}
	}

	/**
	 * Shows the current orders
	 */
	private void showOrders() {
		if (orders.size() > 0) {
			System.out.print("\nORDERS:\n");
			for (Order order : orders) {
				printStringA(Order2StringA.getOrderMsg(order));
			}
			System.out.print("\n");
		}
	}

	/**
	 * Prints a string array in a line with strings separated by single spaces
	 * 
	 * @param message
	 */
	private void printStringA(String[] message) {
		String outcome = "";
		for (String str : message) {
			outcome += str + " ";
		}
		System.out.println(outcome.substring(0, outcome.length() - 1));
	}

	/**
	 * Main method. It allows to connect and reconnect to a game.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ConsolePlayerPro con = null;
		try {
			if (args.length == 0) {
				InetAddress gameServerIp;
				int gameServerPort;
				String name;
				gameServerIp = InetAddress.getLocalHost();
				gameServerPort = 16713;
				name = "ConsolePlayer";
				System.out.println("Console player connecting to: " + gameServerIp + ":" + gameServerPort);
				IComm comm = new DaideComm(gameServerIp, gameServerPort, name);
				con = new ConsolePlayerPro();
				con.start(comm);
			}  else if (args.length == 3) {
				con = new ConsolePlayerPro();
				IComm comm = new DaideComm(InetAddress.getByName(args[0]), Integer.parseInt(args[1]), name + "(" + args[2] + ")");
				con.start(comm);
			} else if (args.length == 5) {
				con = new ConsolePlayerPro();
				IComm comm = new DaideComm(InetAddress.getByName(args[0]), Integer.parseInt(args[1]), name + "(" + args[2] + ")");
				con.restart(comm, args[3], args[4]);
			} else {
				System.err.println("Usage:\n  " + name + " <ip> <port> <name>");
			}
		} catch (Exception e) {
			System.err.println("Usage:\n  " + name + " <ip> <port> <name>");
		}
	}
}
