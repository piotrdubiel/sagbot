package pl.sag.test;

import static org.junit.Assert.*;

import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import pl.sag.SagOptionEvaluator;

import es.csic.iiia.fabregues.bot.options.Option;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;

public class SagOptionEvaluatorSpec {
	SagOptionEvaluator sagOptionEvaluator;

	@Before
	public void before() {
		sagOptionEvaluator = new SagOptionEvaluator();
	}

	@Test
	public void shouldEvaluateOptionWithoutOrders() {
		// given
		Option option = new Option(new Vector<Order>());

		// when
		sagOptionEvaluator.evaluate(option, null, null);

		// then
		assertEquals(0.0, option.getValue(), 0.0);
	}

	@Test
	public void shouldPenalizeMovesToTheSameProvince() {
		// given
		Vector<Order> orders = new Vector<Order>();
		Region region = new Region("Test region");
		region.setProvince(new Province("Test province"));
		orders.add(new MTOOrder(new Power("A"), new Region("Start A"), region));
		orders.add(new MTOOrder(new Power("B"), new Region("Start B"), region));

		Option option = new Option(orders);

		// when
		sagOptionEvaluator.evaluate(option, null, null);

		// then
		assertEquals(-Float.MAX_VALUE, option.getValue(), 0.0);
	}
	
	@Test
	public void shouldPenalizeSwapMoves() {
		// given
		Vector<Order> orders = new Vector<Order>();
		Region origin = new Region("Region A");
		origin.setProvince(new Province("Province A"));
		Region destination = new Region("Region B");
		destination.setProvince(new Province("Province B"));
		
		orders.add(new MTOOrder(new Power("A"), origin, destination));
		orders.add(new MTOOrder(new Power("B"), destination, origin));

		Option option = new Option(orders);

		// when
		sagOptionEvaluator.evaluate(option, null, null);

		// then
		assertEquals(-Float.MAX_VALUE, option.getValue(), 0.0);
	}
	
	@Test
	public void shouldPenalizeOrderCollison() {
		// given
		Vector<Order> orders = new Vector<Order>();
		Region origin = new Region("Region A");
		origin.setProvince(new Province("Province A"));
		Region destination = new Region("Region B");
		destination.setProvince(new Province("Province B"));
		
		orders.add(new MTOOrder(new Power("A"), origin, destination));
		orders.add(new HLDOrder(new Power("B"), destination));

		Option option = new Option(orders);

		// when
		sagOptionEvaluator.evaluate(option, null, null);

		// then
		assertEquals(-Float.MAX_VALUE, option.getValue(), 0.0);
	}
	
	@Test
	public void shouldPenalizeSupportOrdersWithoutSupportedOrder() {
		// given
		Vector<Order> orders = new Vector<Order>();
		Region origin = new Region("Region A");
		origin.setProvince(new Province("Province A"));
		Region destination = new Region("Region B");
		destination.setProvince(new Province("Province B"));
		
		orders.add(new SUPMTOOrder(new Power("A"), origin, new MTOOrder(new Power("B"), destination, origin)));

		Option option = new Option(orders);

		// when
		sagOptionEvaluator.evaluate(option, null, null);

		// then
		assertEquals(-Float.MAX_VALUE, option.getValue(), 0.0);
	}
}
