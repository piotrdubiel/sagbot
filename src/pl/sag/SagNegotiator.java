package pl.sag;


import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.dipgame.dipNego.language.illocs.*;
import org.dipgame.dipNego.language.infos.*;
import org.dipgame.dipNego.language.offers.Alliance;
import org.dipgame.dipNego.language.offers.Do;
import org.dipgame.dipNego.language.offers.Offer;
import org.dipgame.negoClient.DipNegoClient;
import org.dipgame.negoClient.Negotiator;
import org.dipgame.negoClient.simple.DipNegoClientHandler;
import org.dipgame.negoClient.simple.DipNegoClientImpl;
import org.json.JSONException;

import es.csic.iiia.fabregues.dip.Player;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;
import es.csic.iiia.fabregues.utilities.Log;

/**
 * Based on RandomNegotiator
 */
public class SagNegotiator implements Negotiator{
	
	private Player player;
	private Log dipLog;
	
	//Negotiation variables
	private int negotiationPort;
	private InetAddress negotiationServer;
	private DipNegoClient chat;
	private KnowledgeBase knowledgeBase;
	private BotObserver observer;
	
	private Boolean nowIsTheTimeOfWords = new Boolean(false);
	
	/**
	 * No idea why occupied was here anyway and why is it used by framework through isOccupied, but retain it just in case.
	 * It is probably connected with some dirty hacks or something :P
	 */
	private AtomicBoolean occupied = new AtomicBoolean(false);
	
	/**
	 * Bot will wait no longer than MAX_NEGOTIATION_TIME_TOTAL ms for negotiation replies.
	 */
	private static final long MAX_NEGOTIATION_TIME_TOTAL /* in millis */ = 60 * 1000; 
	
	public SagNegotiator(InetAddress negotiationIp, int negotiationPort, Player player){
		this.negotiationServer = negotiationIp;
		this.negotiationPort = negotiationPort;
		this.player = player;
		this.dipLog = player.log.getLog();
		//this.log = new Interface(name+"_"+System.currentTimeMillis());
		nowIsTheTimeOfWords = false;
		occupied.set(false);
	}

	public void setKnowledgeBase(KnowledgeBase base) {
		this.knowledgeBase = base;
	}
	
	public void setGuiObserver(BotObserver observer) {
		this.observer = observer;
	}
	
	protected void log(String string) {
		observer.log(string);
	}

	private SagProvinceEvaluator sagProvinceEvaluator;
	
	private SagOrderEvaluator sagOrderEvaluator;
	
	private SagOptionEvaluator sagOptionEvaluator;

	public void registerEvaluator(SagProvinceEvaluator sagProvinceEvaluator) {
		this.sagProvinceEvaluator = sagProvinceEvaluator;
	}

	public void registerEvaluator(SagOrderEvaluator sagOrderEvaluator) {
		this.sagOrderEvaluator = sagOrderEvaluator;
	}

	public void registerEvaluator(SagOptionEvaluator sagOptionEvaluator) {
		this.sagOptionEvaluator = sagOptionEvaluator;
	}

	private Game game;
	
	public void setGame(Game game) {
		this.game = game;
	}
	
	public boolean evaluateSUPMTOOrder(SUPMTOOrder order) {
		Order current_order = knowledgeBase.getRegionOrder(((SUPMTOOrder) order).getLocation());
		Power sending_power = order.getSupportedOrder().getPower();

		// we'll fight for most valuable province
		if (current_order == null || knowledgeBase.getProvinceStat(current_order.getLocation().getProvince().getName()).getValue() < knowledgeBase.getProvinceStat(order.getLocation().getProvince().getName()).getValue()) {
			if (current_order instanceof SUPMTOOrder) {
				// we will support player that we trust more in the first place
				return knowledgeBase.getTrust(sending_power.getName()) > knowledgeBase.getTrust(order.getSupportedOrder().getPower().getName());
			} else
				return knowledgeBase.getTrust(sending_power.getName()) > 0;
		}
		
		return false;
	}
	
	/**
	 * Inits the negotiation
	 */
	public void init() {
		
/*===========================================================================*/
/*==                            DipNegoHandler:                            ==*/
/*===========================================================================*/
		
		DipNegoClientHandler handler = new DipNegoClientHandler() {
			
			@Override
			public void handleServerOff() {
				
			}
			
			@Override
			public void handleErrorMessage(String arg0) {
				
			}

			@Override
			public void handleClientAccepted() {

				
			}

			@Override
			public void handleFirstGamePhase() {

				
			}

			/**
			 * L1 message: we have received a deal proposal
			 */
			public void handleProposal(Propose propose) {
				final Deal deal = propose.getDeal();
				final Power from = propose.getSender();
				Offer offer;
				
				switch (deal.getType()) {
				case AGREE:
					offer = ((Agree) deal).getOffer();
					switch (offer.getType()) {
					case ALLIANCE:
						Alliance offer_alliance = (Alliance) offer;
						
						boolean reject_alliance = knowledgeBase.getTrust(from.getName()) < 0;
						
						if (!reject_alliance) for (Power enemy : offer_alliance.getEnemyPowers()) {
							// we reject to have alliance against our ally
							if (knowledgeBase.getAlliances().get(enemy.toString()) != null) {
								reject_alliance = true;
								log ("Received Proposal->Agree->Alliance from " + from.getName() + ": " + offer_alliance.toString() + ". Rejecting because " + enemy + " is our ally.");
								break;
							}
						} else {
							log ("Received Proposal->Agree->Alliance from " + from.getName() + ": " + offer_alliance.toString() + ". Rejecting because we do not trust " + from.getName() + " (trust is " + knowledgeBase.getTrust(from.getName()) + ").");
						}
						
						if (!reject_alliance) {
							log ("Received Proposal->Agree->Alliance from " + from.getName() + ": " + offer_alliance.toString() + ". Alliance was made.");
							for (Power enemy : offer_alliance.getEnemyPowers()) {
								knowledgeBase.addAlliance(from.getName(), enemy.getName());
							}
							sendAccept (from, deal);
						} else {
							sendReject (from, deal);							
						}
						break;
					case DO:
						Do offer_do = (Do) offer;
						Order requested_order = offer_do.getOrder();
								
						synchronized (nowIsTheTimeOfWords) {
							if (nowIsTheTimeOfWords) {
								sagOrderEvaluator.evaluate(requested_order, game, knowledgeBase.getPower());
								if (requested_order instanceof SUPMTOOrder) {
									if (evaluateSUPMTOOrder((SUPMTOOrder) requested_order)) {
										log("Received Propsal->Agree->Do from " + from.getName() + " with requested order:" + requested_order.toString() + ". Agreed!");
										sendAccept(from, deal);
									} else {
										log("Received Propsal->Agree->Do from " + from.getName() + " with requested order:" + requested_order.toString() + ". Rejecting.");
										sendReject(from, deal);
									}								
								} else {
									log("Received Propsal->Agree->Do from " + from.getName() + " with requested order:" + requested_order.toString() + ". Only SUPMTOOrders are negotiated, rejecting.");
									sendAccept(from, deal);
								}
							} else {
								// A bad news is that we are not negotiating ATM. 
								log("Received Propsal->Agree->Do from " + from.getName() + " with requested order:" + requested_order.toString() + ". Not negotiationg moves now, auto rejecting.");
								sendAccept(from, deal);
							}
						}
						break;
					}
					break;
				case COMMIT:
					//if (offer == null)
					// Cannot cast from Deal to Commit - srsly, dipgame?
					//	offer = ((org.dipgame.dipNego.language.infos.Commit) deal).getOffer();
					break;
				default:
					break;
				
				}
			}

			/**
			 * L1 message: our deal has been accepted
			 */
			public void handleAccept(Accept accept) {
				log ("Our deal was accepted: " + accept.getDeal().toString());
				final Power from = accept.getSender();
				final Deal deal = accept.getDeal();
				final Offer offer;
				switch (deal.getType()) {
				case AGREE:
					offer = ((Agree) deal).getOffer();
					switch(offer.getType()) {
					
					case ALLIANCE:
						Alliance offer_alliance = (Alliance) offer;
						for (Power enemy : offer_alliance.getEnemyPowers()) {
							knowledgeBase.addAlliance(from.getName(), enemy.getName());
						}
						break;
					
					case DO:
						Do offer_do = (Do) offer;
						Order requested_order = offer_do.getOrder();
						
						// if our deal was accepted we don't have to do anything
						// special, order is already in knowledge base. Only thing left
						// is to notify main thread that one of the proposals was
						// answered.						
						synchronized (nowIsTheTimeOfWords) {
							if (nowIsTheTimeOfWords) {
								defferedDealsMutex.lock();
								if (--dealsOffered_ == 0) {
									defferedDealsCondition.signalAll();
								}
								defferedDealsMutex.unlock();
							}
						}
						
						break;
					
					} /* switch offer.getType() */
					break;
				}
			}

			/**
			 * L1 message: our deal has been rejected
			 */
			public void handleReject(Reject reject) {
				log ("Our deal was rejected: " + reject.toString() + ". Let's pity the fool who dare to ignore our will, as he shall find himself lost to our overwhelming might.");
				final Power from = reject.getSender();
				final Deal deal = reject.getDeal();
				final Offer offer;
				switch (deal.getType()) {
				case AGREE:
					offer = ((Agree) deal).getOffer();
					switch(offer.getType()) {
					
					case ALLIANCE:
						knowledgeBase.refusedAlliance(from.getName());
						break;
					
					case DO:
						Do offer_do = (Do) offer;
						Order requested_order = offer_do.getOrder();
						
						synchronized (nowIsTheTimeOfWords) {
							if (nowIsTheTimeOfWords) {
								if (requested_order instanceof SUPMTOOrder) {
									SUPMTOOrder sup_order = (SUPMTOOrder) requested_order;
									if (knowledgeBase.getRegionOrder(sup_order.getSupportedOrder().getLocation()).equals(sup_order.getSupportedOrder())) {
										knowledgeBase.removeRegionOrder(sup_order.getSupportedOrder().getLocation());
									}								
								}
								defferedDealsMutex.lock();
								if (--dealsOffered_ == 0) {
									defferedDealsCondition.signalAll();
								}
								defferedDealsMutex.unlock();
							}
						}
						
						break;
					
					} /* switch offer.getType() */
					break;
				}
			}

			/**
			 * L2 message: we have received a piece of information
			 */
			public void handleInform(Inform inform) {
				handleInform(inform.getInformation(), inform.getSender());
			}

			@SuppressWarnings("incomplete-switch") // complete for stage L4
			public void handleInform(Information i, Power p) {
				switch (i.getType()) {
				case AND: // L2 info
					org.dipgame.dipNego.language.infos.And i_and = (org.dipgame.dipNego.language.infos.And) i;
					// TODO: implement
					break;
				case BELIEF: // L2 info
					org.dipgame.dipNego.language.infos.Belief i_belief = (org.dipgame.dipNego.language.infos.Belief) i;
					// TODO: implement
					break;
				case DESIRE: // L2 info
					org.dipgame.dipNego.language.infos.Desire i_desire = (org.dipgame.dipNego.language.infos.Desire) i;
					// TODO: implement
					break;
				case NOT: // L2 info
					org.dipgame.dipNego.language.infos.Not i_not = (org.dipgame.dipNego.language.infos.Not) i;
					// TODO: implement
					break;
				case OBSERV: // L2 info
					org.dipgame.dipNego.language.infos.Observ i_observ = (org.dipgame.dipNego.language.infos.Observ) i;
					// TODO: implement
					break;
				case UNKNOWN: // L3 info
					org.dipgame.dipNego.language.infos.Unknown i_unk = (org.dipgame.dipNego.language.infos.Unknown) i;
					// TODO: implement
					break;
				}
			}
			
			/**
			 * L>=3 message: we have been queried
			 */
			public void handleQuery(Query query) {
				handleQuery(query.getInformation(), query.getSender());				
			}

			@SuppressWarnings("incomplete-switch") // complete for stage L4
			void handleQuery(Information i, Power p) {
				switch (i.getType()) {
				case AND: // L2 info
					org.dipgame.dipNego.language.infos.And i_and = (org.dipgame.dipNego.language.infos.And) i;
					// TODO: implement
					break;
				case BELIEF: // L2 info
					org.dipgame.dipNego.language.infos.Belief i_belief = (org.dipgame.dipNego.language.infos.Belief) i;
					// TODO: implement
					break;
				case DESIRE: // L2 info
					org.dipgame.dipNego.language.infos.Desire i_desire = (org.dipgame.dipNego.language.infos.Desire) i;
					// TODO: implement
					break;
				case NOT: // L2 info
					org.dipgame.dipNego.language.infos.Not i_not = (org.dipgame.dipNego.language.infos.Not) i;
					// TODO: implement
					break;
				case OBSERV: // L2 info
					org.dipgame.dipNego.language.infos.Observ i_observ = (org.dipgame.dipNego.language.infos.Observ) i;
					// TODO: implement
					break;
				case UNKNOWN: // L3 info
					org.dipgame.dipNego.language.infos.Unknown i_unk = (org.dipgame.dipNego.language.infos.Unknown) i;
					// TODO: implement
					break;
				}
			}
			
			/**
			 * L>=3 message: our deal has been accepted
			 */
			private void handleAnswer (Answer answer) {
				handleAnswer(answer.getInformation(), answer.getSender());
			}
			
			@SuppressWarnings("incomplete-switch") // complete for stage L4
			private void handleAnswer (Information i, Power p) {
				switch (i.getType()) {
				case AND: // L2 info
					org.dipgame.dipNego.language.infos.And i_and = (org.dipgame.dipNego.language.infos.And) i;
					// TODO: implement
					break;
				case BELIEF: // L2 info
					org.dipgame.dipNego.language.infos.Belief i_belief = (org.dipgame.dipNego.language.infos.Belief) i;
					// TODO: implement
					break;
				case DESIRE: // L2 info
					org.dipgame.dipNego.language.infos.Desire i_desire = (org.dipgame.dipNego.language.infos.Desire) i;
					// TODO: implement
					break;
				case NOT: // L2 info
					org.dipgame.dipNego.language.infos.Not i_not = (org.dipgame.dipNego.language.infos.Not) i;
					// TODO: implement
					break;
				case OBSERV: // L2 info
					org.dipgame.dipNego.language.infos.Observ i_observ = (org.dipgame.dipNego.language.infos.Observ) i;
					// TODO: implement
					break;
				case UNKNOWN: // L3 info
					org.dipgame.dipNego.language.infos.Unknown i_unk = (org.dipgame.dipNego.language.infos.Unknown) i;
					// TODO: implement
					break;
				}
			}
			
			/** Receives only messages sent directly to me - no other recipients */
			@Override
			public void handleNegotiationMessage(Power from, List<Power> to, Illocution illocution) {
				String recipients = "";
				for (Power power : to) {
					recipients = recipients + ", " + power.getName();
				}
				//System.out.println("received msg from: " + from.getName()
				//		+ " to: " + recipients
				//		+ " text: " + illocution.getString());
				occupied.set(true);
				if (illocution instanceof Propose) {
					handleProposal((Propose) illocution);
				} else if (illocution instanceof Accept) {
					handleAccept((Accept) illocution);
				} else if (illocution instanceof Reject) {
					handleReject((Reject) illocution);
				} else if (illocution instanceof Query) {
					handleQuery((Query) illocution);
				} else if (illocution instanceof Answer) {
					handleAnswer((Answer) illocution);
				} else if (illocution instanceof Inform) {
					handleInform((Inform) illocution);
				}
				occupied.set(false);
			}

			@Override
			public void handleNewGamePhase() {

				
			}
		}; /* DipNegoHandler */

/*===========================================================================*/
/*==                        End of DipNegoHandler!                         ==*/
/*===========================================================================*/
		
		chat = new DipNegoClientImpl(negotiationServer, negotiationPort, player.getMe().getName(), handler, dipLog);
		try {
			chat.init();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		
		dipLog.print("Negotiation module initiated.");
	}
	
	
	/**
	 * Sends a negotiation message
	 * @param illoc
	 */
	private void sendDialecticalAction(Illocution illoc) {
		try {
			Vector<String> recvs = new Vector<String>();
			for(Power rec : illoc.getReceivers()){
				recvs.add(rec.getName());
			}
			chat.send(recvs, illoc);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void disconnect() {
		chat.disconnect();
	}

	@Override
	public boolean isOccupied() {
		return occupied.get();
	}
	
	static final class PendingDeal {
		
		private final Deal deal;
		
		private final Power power;
		
		PendingDeal (Deal deal, Power power) {
			this.deal = deal;
			this.power = power;
		}

		public Deal getDeal() {
			return deal;
		}

		public Power getPower() {
			return power;
		}
		
	}; /* class PendingDeal */
	
	int dealsOffered_ = 0;
	
	final Lock defferedDealsMutex = new ReentrantLock();
	
	final Condition defferedDealsCondition = defferedDealsMutex.newCondition();
	
	void offerAlliance (Power power, Power against) {
		if (!knowledgeBase.getAllies().contains(power)) {
			List<Power> target = new Vector<Power>(1);
			target.add(player.getMe());
			
			List<Power> alliance_against = new Vector<Power>(1);
			alliance_against.add(against);
			
			List<Power> between = new Vector<Power>(2);
			between.add(power);
			between.add(player.getMe());

			log ("Offering alliance against " + against + " to " + power + ".");
			
			final Illocution alliance_illoc = new Propose(player.getMe(), power, new Agree(target, new Alliance(between, alliance_against)));
			sendDialecticalAction(alliance_illoc);			
		}
	}
	
	void offerDeal (Deal deal, String power) {
		final Illocution illoc = new Propose(player.getMe(), player.getGame().getPower(power), deal);
		
		defferedDealsMutex.lock();
		dealsOffered_++;
		defferedDealsMutex.unlock();
		
		log ("Sending deal offer to " + power + ": " + deal.toString());
		sendDialecticalAction(illoc);
	}

	/**
	 * Wait until all of deals we offered are answered or timeout happens
	 */
	private void waitForResponses () {
		defferedDealsMutex.lock();
		try {
			long wait_start = System.currentTimeMillis();
			while (dealsOffered_ != 0) {
				long wait_for = MAX_NEGOTIATION_TIME_TOTAL - System.currentTimeMillis() + wait_start;
				if (wait_for > 0)
					defferedDealsCondition.await(wait_for, TimeUnit.MILLISECONDS);
				else break;
			}
			dealsOffered_ = 0;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		defferedDealsMutex.unlock();
	}

/*===========================================================================*/
/*= Parallel negotiations handling:                                         =*/
/*===========================================================================*/

	private final static int MAX_PROVINCES_TO_NEGOTIATE = 5;
	
	private final static float PROPOSE_COMMON_ATTACK_TRESHOLD = 1.0f;
	
	@Override
	public void negotiate() {
		synchronized (nowIsTheTimeOfWords) {
			nowIsTheTimeOfWords = true;
			knowledgeBase.clearNegotiationsData();
			
			Power I = knowledgeBase.getPower();
			
			Vector<ProvinceStat> stats = new Vector<ProvinceStat>();
			
			// make initial evaluation of the provinces
			for (Province p : game.getProvinces()) {
				sagProvinceEvaluator.evaluate(p, game, I);
				stats.add(knowledgeBase.getProvinceStat(p.getName()));
			}
			
			Collections.sort(stats, ProvinceStat.AtDefComparator);
			
			int negos_left = MAX_PROVINCES_TO_NEGOTIATE;
			
			// beginning with possibly most valuable provinces we...
			for (ProvinceStat ps : stats) {
				if (negos_left-- == 0) break;
				
				final Region target_region = game.getRegion(ps.province.getName());
				
				// ... may request for attack support
				if (ps.attack != 0) {
				
					LinkedList<MTOOrder> move_orders = new LinkedList<MTOOrder>();
					
					// search for valuable attack orders
					for (Province p : game.getAdjacentProvinces (ps.province)) {
						Region source_region = game.getRegion(p.getName());	
						if (game.getController(source_region).equals(knowledgeBase.getPower())) {
							MTOOrder mto = new MTOOrder(knowledgeBase.getPower(), source_region, target_region);
							sagOrderEvaluator.evaluate(mto, game, knowledgeBase.getPower());
							if (mto.getValue() > PROPOSE_COMMON_ATTACK_TRESHOLD) {
								move_orders.add(mto);
							}
						}
					}
					
					if (move_orders.isEmpty()) continue;
					
					// we are interested mainly in the best attack option(s), so sort!
					Collections.sort(move_orders, new Comparator<Order>() {
						@Override
						public int compare(Order o1, Order o2) {
							return o1.getValue().compareTo(o2.getValue());
						}
					});
					
					Order region_order = knowledgeBase.getRegionOrder(move_orders.peekFirst().getLocation());
					if (region_order == null || region_order.getValue() < move_orders.peekFirst().getValue()) {
						knowledgeBase.setRegionOrder(move_orders.peekFirst().getLocation(), move_orders.peekFirst());
					} else {
						continue;
					}
					
					if (!ps.getPossibleAllyAttackSupporters().isEmpty() || !ps.getPossibleAttackSupporters().isEmpty()) {
						// attack supporters should be sorted - from the best ally (best may mean the one who we trust the most or the one who is most likely to support our case here), or from the player who is most likely to become our new ally
						ProvinceStat.AttackSupporter as = !ps.getPossibleAllyAttackSupporters().isEmpty() ? ps.getPossibleAllyAttackSupporters().iterator().next() : ps.getPossibleAttackSupporters().iterator().next();
						
						for (Region region : as.getAttackSources()) {
							SUPMTOOrder ally_sup = new SUPMTOOrder(as.getPower(), region, move_orders.getFirst());
							org.dipgame.dipNego.language.offers.Do offer = new org.dipgame.dipNego.language.offers.Do(ally_sup);
							Agree agree = new Agree ();
							agree.addPower(as.getPower());
							agree.setOffer(offer);
							offerAlliance (as.getPower(), game.getController(ally_sup.getSupportedOrder().getDestination()));
							offerDeal (agree, as.getPower().getName());
						}
						
					}
				}
				
				// ... or ask for help!
				if (ps.defence != 0) {
					// if province is in danger and we have allies around, or we have non-allied players who might want to help us because common foe may attack
				}
				
			}
			
		// we block execution of responses to proposals until dry run evaluation finishes
		} /* synchronized (nowIsTheTimeOfWords) */
		
		waitForResponses ();
		synchronized (nowIsTheTimeOfWords) {
			nowIsTheTimeOfWords = false;
		}
	}


	private void sendReject(Power to, Deal deal) {
		Vector<Power> target = new Vector<Power>(1);
		target.add(to);
		Reject reject = new Reject(player.getMe(), target, deal);
		sendDialecticalAction(reject);
	}

	private void sendAccept(Power to, Deal deal) {
		Vector<Power> target = new Vector<Power>(1);
		target.add(to);
		Accept accept = new Accept(player.getMe(), target, deal);
		sendDialecticalAction(accept);
	}
	
	/*
	 * 	/**
	 * Sends negotiation proposals randomly
	public void negotiate(){
		if( rand.nextInt(70) == 0){
			List<Power> available = new Vector<Power>(7);
			for(Power power: player.getGame().getPowers()){
				if(power.getControlledRegions().size()>0){
					available.add(power);
				}
			}
			Power receiver = available.get(rand.nextInt(available.size()));
			if(receiver.equals(player.getMe())){
				return;
			}
			Deal deal = null;
			if(rand.nextBoolean()){
				List<Power> peace = new Vector<Power>(2);
				peace.add(player.getMe());
				peace.add(receiver);
				deal = new Agree(peace, new Peace(peace));
			}else{
				List<Power> peace = new Vector<Power>(2);
				peace.add(player.getMe());
				peace.add(receiver);
				
				Power againstPower = available.get(rand.nextInt(available.size()));
				if(againstPower.equals(receiver) || againstPower.equals(player.getMe())){
					return;
				}
				List<Power> against = new Vector<Power>(1);
				against.add(againstPower);
				deal = new Agree(peace, new Alliance(peace, against));
			}
			Illocution illoc = new Propose(player.getMe(), receiver, deal);
			sendDialecticalAction(illoc);
			//TODO update knowledgeBase

			//System.out.println("sending msg to: " + receiver.getName()
			//		+ " text: " + illoc.getString());
		}
	}
*/

	// negotiations in evaluator: variable for the nasty hack, required to assure negotiations are called once per turn
	private boolean negotiatedThisTurn = false;
	
	void setNegotiatedThisTurn (boolean negotiatedThisTurn) {
		this.negotiatedThisTurn = negotiatedThisTurn;
	}
	
	boolean negotiatedThisTurn () {
		return negotiatedThisTurn;
	}
	
}
