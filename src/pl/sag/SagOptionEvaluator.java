package pl.sag;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import es.csic.iiia.fabregues.bot.OptionEvaluator;
import es.csic.iiia.fabregues.bot.options.Option;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;
import es.csic.iiia.fabregues.dip.orders.SUPOrder;


public class SagOptionEvaluator implements OptionEvaluator{
	private KnowledgeBase knowledgeBase;
	private SagNegotiator negotiator;
	
	public SagOptionEvaluator(){
		
	}

	public void setNegotiator(SagNegotiator negotiator) {
		this.negotiator = negotiator;
		negotiator.registerEvaluator(this);
	}
	
	public void setKnowledgeBase(KnowledgeBase base) {
		this.knowledgeBase = base;
	}

	@Override
	public void evaluate(Option option, Game game, Power power) {
		if (validate(option)) {			
			float sum = 0.0f;
			for (Order o : option.getOrders())
				sum += o.getValue();
			option.setValue(sum);
			//changedHoldToSupport(option, game);
		}
		else 
			option.setValue(-Float.MAX_VALUE);
	}
	
	private boolean validate(Option option) {
		for (Order a : option.getOrders()) {	
			for (Order b : option.getOrders()) {
				if (!a.equals(b) && 
						a instanceof MTOOrder) {
					if (b instanceof MTOOrder) {
						Province destination_a = ((MTOOrder) a).getDestination().getProvince();
						Province destination_b = ((MTOOrder) b).getDestination().getProvince();
						if (destination_a.equals(destination_b)) {
							return false;
						}
						else if (destination_a.equals(b.getLocation().getProvince()) &&
								destination_b.equals(a.getLocation().getProvince())) {
							return false;
						}
					}
					else if (b instanceof HLDOrder ||b instanceof SUPMTOOrder || b instanceof SUPOrder) {
						Province destination_a = ((MTOOrder) a).getDestination().getProvince();
						Province destination_b = b.getLocation().getProvince();
						if (destination_a.equals(destination_b)) {
							return false;
						}
					}
				}						
			}
			if (a instanceof SUPMTOOrder) {
				if (!option.getOrders().contains(((SUPMTOOrder) a).getSupportedOrder())) return false;
			}
			else if (a instanceof SUPOrder) {
				if (!option.getOrders().contains(((SUPOrder) a).getSupportedOrder())) return false;
			}
		}
		return true;
	}

	
	private void changedHoldToSupport(Option option, Game game) {
		List<MTOOrder> moves = getMoveOrders(option);
		List<HLDOrder> holds = getHoldOrders(option);
		
		for (MTOOrder move : moves)
			for (HLDOrder hold : holds) {
				if (hold.getLocation().getAdjacentRegions().contains(move.getDestination()) &&
						hold.getLocation().getProvince().getValue() < move.getDestination().getProvince().getValue()) {
					SUPMTOOrder support = new SUPMTOOrder(hold.getPower(), hold.getLocation(), move);
					support.setOrderValue(move.getDestination().getProvince().getValue());
					option.getOrders().add(support);
					option.getOrders().remove(hold);
				}
			}
	}
	
	private List<MTOOrder> getMoveOrders(Option option) {
		List<MTOOrder> moves = new ArrayList<MTOOrder>(); 
		for (Order o : option.getOrders())
			if (o instanceof MTOOrder) {
				moves.add((MTOOrder) o);
			}
		return moves;
	}
	
	private List<HLDOrder> getHoldOrders(Option option) {
		List<HLDOrder> holds = new ArrayList<HLDOrder>(); 
		for (Order o : option.getOrders())
			if (o instanceof HLDOrder) {
				holds.add((HLDOrder) o);
			}
		return holds;
	}
}
