package pl.sag;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import es.csic.iiia.fabregues.bot.Bot;
import es.csic.iiia.fabregues.bot.options.OptionBoard;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.comm.IComm;
import es.csic.iiia.fabregues.dip.comm.daide.DaideComm;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;

/**
 * Bot based on a merge of RandomBot and RandomNegotiatorRandomBot
 */
public class SagBot extends Bot {

	//Negotiation variables
	private int negotiationPort;
	private InetAddress negotiationServer;
	private SagNegotiator negotiator;
	
	private static String name = "SagBot";
	protected Random rand;
	private BotObserver botObserver;
	private String powerName;
	private KnowledgeBase knowledgeBase;
	private Semaphore nextStepSemaphore;
	
	/**
	 *
	 */
	public SagBot(InetAddress negotiationIp, int negotiationPort) {
		super(new SagProvinceEvaluator(), new SagOrderEvaluator(), new SagOptionEvaluator());
		this.negotiationServer = negotiationIp;
		this.negotiationPort = negotiationPort;
		this.nextStepSemaphore = new Semaphore(0);
	}

	@Override
	/**
	 * Sets the number of options to preselect during the search of best options
	 */
	protected int getNumberOfBestOptions() {
		return 10;
	}

	@Override
	/**
	 * Sets the number of orders per unit to preselect during the search of best orders
	 */
	protected int getNumberOfBestOrdersPerUnit() {
		return 5;
	}
	
	protected void log(String string) {
		botObserver.log(string);
	}

	@Override
	public void init() {
		rand = new Random(System.currentTimeMillis());
	}
	
	@Override
	public void start() {
		powerName = getMe().getName();
		knowledgeBase = new KnowledgeBase(powerName, game);
		
		botObserver = new BotObserver(knowledgeBase, nextStepSemaphore);
		knowledgeBase.addObserver(botObserver);
		knowledgeBase.stateChanged();

		log("We are " + powerName);
		log("Map " + mapName);
		log("'->' stands for attack (agressor -> victim)");
		log("\n");
		
		this.negotiator = new SagNegotiator(negotiationServer, negotiationPort, this);
		negotiator.init();
		negotiator.setKnowledgeBase(knowledgeBase);
		negotiator.setGuiObserver(botObserver);

		negotiator.setGame(getGame());
		
		((SagOrderEvaluator) this.orderEvaluator).setNegotiator(negotiator);
		((SagOptionEvaluator) this.optionEvaluator).setNegotiator(negotiator);
		((SagProvinceEvaluator) this.provinceEvaluator).setNegotiator(negotiator);
		((SagOrderEvaluator) this.orderEvaluator).setKnowledgeBase(knowledgeBase);
		((SagOptionEvaluator) this.optionEvaluator).setKnowledgeBase(knowledgeBase);
		((SagProvinceEvaluator) this.provinceEvaluator).setKnowledgeBase(knowledgeBase);

	}
	
	public HashMap<String, String> getRegionControllers() {
		HashMap<String, String> controllers = new HashMap<String, String>();
		for (Power power : game.getPowers()) {
			for (Region region : power.getControlledRegions()) {
				controllers.put(region.getName(), power.getName());
			}
		}
		return controllers;
	}
	
	public HashMap<String, String> getScOwners() {
		HashMap<String, String> owners = new HashMap<String, String>();
		for (Power power : game.getPowers()) {
			for (Province province : power.getOwnedSCs()) {
				owners.put(province.getName(), power.getName());
			}
		}
		return owners;
	}

	@Override
	/**
	 * Receive all orders made by all players, including myself
	 */
	public void receivedOrder(Order order) {
		//log("receivedOrder: " + order.toString());
		HashMap<String, String> controllers = getRegionControllers();
		HashMap<String, String> owners = getScOwners();
		
		// check if an attack on an army or SC is made
		if (order instanceof MTOOrder) {
			MTOOrder mtoOrder = (MTOOrder) order;
			String orderMaker = order.getPower().getName();
			String regionName = mtoOrder.getDestination().getName();
			String provinceName = mtoOrder.getDestination().getProvince().getName();
			if (controllers.containsKey(regionName)) {
				String ownerName = controllers.get(regionName);
				if (!ownerName.equals(orderMaker)) {
					addAggression(orderMaker, ownerName);
				}
			} else if (owners.containsKey(provinceName)) {
				String ownerName = owners.get(provinceName);
				if (!ownerName.equals(orderMaker)) {
					addAggression(orderMaker, ownerName);
				}
			}
		}
		negotiator.negotiate();
	}
	
	public void addAggression(String aggressor, String victim) {
		String string = aggressor + " -> " + victim;	
		if (!knowledgeBase.getPowerKnowledge(aggressor).getWars().contains(victim)) {
			string += ", declaring war";
		}
		
		if (knowledgeBase.getPowerKnowledge(aggressor).getAllies().contains(victim)) {
			string += ", breaking alliance";
		}
		log(string);
		this.knowledgeBase.addAggression(aggressor, victim);
	}

	@Override
	/**
	 * Selects the orders to send from the preselected ones that are stored in optionBoard
	 */
	protected List<Order> selectOption(OptionBoard scenarios) {
		if (this.botObserver.getAutoMode()) {
			botObserver.log("starting round... waiting for GUI nextStep");
			try {
				nextStepSemaphore.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	
		HashMap<String, Float> values = new HashMap<String, Float>();
		for (Province p : game.getProvinces()) {
			values.put(p.getName(), knowledgeBase.getProvinceStat(p.getName()).getValue());
		}
		Map<String, Float> sorted = sortByValues(values);
		sorted.putAll(values);
		
		for (String s : sorted.keySet()) {
			System.out.println(s + " => "+sorted.get(s) + " " + knowledgeBase.getProvinceStat(s));
		}
		
		if(scenarios.getOptions().size() >= 2){
			System.out.println("Second: " + scenarios.getOptions().get(0).getValue());
			System.out.println("First: " + scenarios.getOptions().get(1).getValue());
			if (rand.nextFloat() <= 0.2) {
				scenarios.selectOption(scenarios.getOptions().get(0));
				System.out.println("Selected SECOND => " + scenarios.getSelectedOption().getOrders());
			}
			else {
				scenarios.selectOption(scenarios.getOptions().get(1));
				System.out.println("Selected FIRST => " + scenarios.getSelectedOption().getOrders());
			}
			return scenarios.getSelectedOrders();
		}
		else if (scenarios.getOptions().size() > 0) {
			scenarios.selectOption(scenarios.getOptions().get(0));
			System.out.println("Selected FIRST and only => " + scenarios.getSelectedOption().getOrders());
			return scenarios.getSelectedOrders();
		}
		
		return new Vector<Order>(0);
	}
	
	public static <K extends Comparable,V extends Comparable> Map<K,V> sortByValues(Map<K,V> map){
        List<Map.Entry<K,V>> entries = new LinkedList<Map.Entry<K,V>>(map.entrySet());
      
        Collections.sort(entries, new Comparator<Map.Entry<K,V>>() {

            @Override
            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
      
        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<K,V> sortedMap = new LinkedHashMap<K,V>();
      
        for(Map.Entry<K,V> entry: entries){
            sortedMap.put(entry.getKey(), entry.getValue());
        }
      
        return sortedMap;
    }
	
	/**
	 * Informs about the winner of the game and exits
	 */
	@Override
	public void handleSlo(String winner) {
		botObserver.powerWon(winner);
	}
	
	public void exit(){
		super.exit();
		if(negotiator!=null){
			negotiator.disconnect();
		}
	}
	
	/**
	 * Main method. It allows to connect and reconnect to a game.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SagBot sagBot = null;
		String usageString = "Usage:\n  " + name + " <ip> <port> <name>  <negotiation ip> <negotiation port>";
		try {
			if (args.length == 0) {
				InetAddress negoServerIp;
				int negoServerPort;
				InetAddress gameServerIp;
				int gameServerPort;
				String name = "SagBot";
				gameServerIp = InetAddress.getLocalHost();
				gameServerPort = 16713;
				negoServerIp = InetAddress.getLocalHost();
				negoServerPort = 16714;
				System.out.println(name + " connecting to: " + gameServerIp + ":" + gameServerPort);
				sagBot = new SagBot(negoServerIp, negoServerPort);
				IComm comm = new DaideComm(gameServerIp, gameServerPort, name);
				sagBot.start(comm);
			}  else if (args.length >= 5) {
				for (String arg: args)
					System.out.println(arg);
				sagBot = new SagBot(InetAddress.getByName(args[3]), Integer.parseInt(args[4]));
				IComm comm = new DaideComm(InetAddress.getByName(args[0]), Integer.parseInt(args[1]), name + "(" + args[2] + ")");
				sagBot.start(comm);
			} else {
				System.err.println(usageString);
			}
		} catch (final ArrayIndexOutOfBoundsException be) {
			System.err.println(usageString);
		} catch (final UnknownHostException uhe) {
			System.err.println("Unknown host: " + uhe.getMessage());
		} catch (final NumberFormatException nfe) {
			System.err.println(usageString);
		} catch (Exception e) {
			System.err.println(usageString);
		}
	}
}
