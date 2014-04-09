package pl.sag;
import java.util.List;

import org.dipgame.dipNego.language.illocs.Illocution;
import org.dipgame.negoClient.simple.DipNegoClientHandler;

import es.csic.iiia.fabregues.dip.board.Power;

/**
 * SagNegotiationHandler is a negotiation handler for peace, alliance and/or order messages.
 * 
 * @author Angela Fabregues, IIIA-CSIC, fabregues@iiia.csic.es
 */
public class SagSimpleNegotiationHandler implements DipNegoClientHandler {
	
	private SagSimpleNegotiator negotiator;
	
	public SagSimpleNegotiationHandler(SagSimpleNegotiator negotiator) {
		this.negotiator = negotiator;
	}

	/**
	 * Thinks to do when the negotiator is accepted by the negotiation server
	 */
	@Override
	public void handleClientAccepted() {
		System.out.println("Negotiation handler accepted by server");

	}

	/**
	 * Handle the reception of an error message
	 */
	@Override
	public void handleErrorMessage(String arg0) {

	}

	/**
	 * Handle the beginning of a game
	 */
	@Override
	public void handleFirstGamePhase() {

	}

	/**
	 * Handle the beginning of a new phase
	 */
	@Override
	public void handleNewGamePhase() {

	}
	
	/**
	 * Handle the reception of a negotiation message
	 */
	@Override
	public void handleNegotiationMessage(Power from, List<Power> to, Illocution illoc) {

	}

	/**
	 * Handling the server switching of
	 */
	@Override
	public void handleServerOff() {
		negotiator.disconnect();
	}
}
