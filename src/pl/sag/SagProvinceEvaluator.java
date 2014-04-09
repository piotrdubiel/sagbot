package pl.sag;
import es.csic.iiia.fabregues.bot.ProvinceEvaluator;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;

public class SagProvinceEvaluator implements ProvinceEvaluator {
	private KnowledgeBase knowledgeBase;
	private SagNegotiator negotiator;

	public SagProvinceEvaluator() {}

	public void setNegotiator(SagNegotiator negotiator) {
		this.negotiator = negotiator;
		negotiator.registerEvaluator(this);
	}

	public void setKnowledgeBase(KnowledgeBase base) {
		this.knowledgeBase = base;
	}

	@Override
	public void evaluate(Province province, Game game, Power power) {
		if (!negotiator.negotiatedThisTurn()) {
			negotiator.setNegotiatedThisTurn (true);
			negotiator.negotiate();
		}
		
		ProvinceStat stat = new ProvinceStat(province, power, game, knowledgeBase);
		if (province.getValue() == null) province.setValue(0.0f);

		knowledgeBase.addProvinceStat(province.getName(), stat);
		province.setValue(stat.defence + stat.attack);
	}
}
