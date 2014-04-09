package pl.sag;
import java.io.IOException;
import java.net.InetAddress;

import org.dipgame.negoClient.DipNegoClient;
import org.dipgame.negoClient.Negotiator;
import org.dipgame.negoClient.simple.DipNegoClientHandler;
import org.dipgame.negoClient.simple.DipNegoClientImpl;
import org.json.JSONException;

/**
 * SagNegotiator is a negotiator about peace, alliances and/or orders.
 * 
 * @author Angela Fabregues, IIIA-CSIC, fabregues@iiia.csic.es
 */
public class SagSimpleNegotiator implements Negotiator {

	private SagSimpleBot player;
	private InetAddress negoServerIp;
	private int negoServerPort;
	private DipNegoClientHandler handler;
	private DipNegoClient client;
	private Boolean isDisconnecting = new Boolean(false);
	
	public SagSimpleNegotiator(InetAddress negoServerIp, int negoServerPort, SagSimpleBot player) {
		this.negoServerIp = negoServerIp;
		this.negoServerPort = negoServerPort;
		this.player = player;
	}
	
	/**
	 * Handling the game starting (because SagBot call this method from its start method)
	 */
	@Override
	public void init() {
		this.handler = new SagSimpleNegotiationHandler(this);
		client = new DipNegoClientImpl(negoServerIp, negoServerPort, player.getName(), handler);
		try {
			client.init();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			player.exit();
		} catch (JSONException e) {
			// It won't happen
			e.printStackTrace();
		}
	}

	/**
	 * Handling negotiation phase
	 */
	@Override
	public void negotiate() {
		//Decide how the negotiation should be
	}

	@Override
	public boolean isOccupied() {
		return false;
	}
	
	/**
	 * Disconnecting from the negotiation server
	 */
	@Override
	public void disconnect() {
		//checking whether it was disconnecting avoiding death locks
		boolean wasDisconnecting = false;
		synchronized (isDisconnecting) {
			wasDisconnecting = isDisconnecting;
			isDisconnecting = true;
		}
		
		if (!wasDisconnecting) {
			//Disconnecting from the negotiation server
			try {
				client.disconnect();
			} catch(Exception e) {
				System.err.println(e.getMessage());
			}
			//Disconnecting from the game server
			try {
				player.exit();
			} catch(Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}
}
