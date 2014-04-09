package pl.sag;
import es.csic.iiia.fabregues.dip.orders.Order;


public class DeferredDealResult {

	private final float onSuccessValueChange;
	
	private final float onFailureValueChange;
	
	private final Order order;
	
	private boolean accepted;
	
	private boolean answered;
	
	public DeferredDealResult(Order order, float onSuccessValueChange, float onFailureValueChange) {
		this.onSuccessValueChange = onSuccessValueChange;
		this.onFailureValueChange = onFailureValueChange;
		this.order = order;
		accepted = false;
		answered = false;
	}

	public boolean answered() {
		return answered;
	}

	public void setAccepted() {
		accepted = true;
		answered = true;
	}
	
	public void setRejected() {
		accepted = false;
		answered = true;
	}

	public void updateOrder() {
		order.setOrderValue(order.getValue() + (accepted ? onSuccessValueChange : onFailureValueChange));
	}
	
	
	
}
