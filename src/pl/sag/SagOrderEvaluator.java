package pl.sag;
import es.csic.iiia.fabregues.bot.OrderEvaluator;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.orders.*;

public class SagOrderEvaluator implements OrderEvaluator{
	private KnowledgeBase knowledgeBase;
	private SagNegotiator negotiator;
	
	private final static float SUPPORT_MOD = 1.2f;
	
	public SagOrderEvaluator() {
	}

	public void setNegotiator(SagNegotiator negotiator) {
		this.negotiator = negotiator;
		negotiator.registerEvaluator(this);
	}
	
	public void setKnowledgeBase(KnowledgeBase base) {
		this.knowledgeBase = base;
	}

	@Override
	/**
	 * Sets a random value to the orders that evaluates
	 */
	public void evaluate(Order order, Game game, Power power) {
		if (negotiator.negotiatedThisTurn())
			negotiator.setNegotiatedThisTurn(false);
		
		if (order instanceof MTOOrder) {
			order.setOrderValue(evaluateMoveOrder((MTOOrder) order));
		}
		else if (order instanceof SUPMTOOrder) {
			order.setOrderValue(evaluateSupportMoveOrder((SUPMTOOrder) order));
		}
		else if (order instanceof WVEOrder) {
			order.setOrderValue(-200.0f);
		}
		else if (order instanceof BLDOrder) {
			order.setOrderValue(evaluateBuildOrder((BLDOrder) order));
		}
		else if (order instanceof HLDOrder) {
			order.setOrderValue(evaluateHoldOrder((HLDOrder) order));
		}
		else if (order instanceof RTOOrder) {
			order.setOrderValue(evaluateRetreatOrder((RTOOrder) order));
		}
		else {
			order.setOrderValue(order.getLocation().getProvince().getValue());
		}
	}
	
	private float evaluateMoveOrder(MTOOrder order) {
		return knowledgeBase.getProvinceStat(order.getDestination().getProvince().getName()).getValue();
	}
	
	private float evaluateSupportMoveOrder(SUPMTOOrder order) {
		return knowledgeBase.getProvinceStat(order.getDestination().getName()).getValue() * SUPPORT_MOD;
	}
	
	private float evaluateHoldOrder(HLDOrder order) {
		return knowledgeBase.getProvinceStat(order.getLocation().getProvince().getName()).getValue();
	}
	
	private float evaluateRetreatOrder(RTOOrder order) {
		return knowledgeBase.getProvinceStat(order.getDestination().getProvince().getName()).defence;
	}
	
	private float evaluateBuildOrder(BLDOrder order) {
		return 0;	
	}
}
