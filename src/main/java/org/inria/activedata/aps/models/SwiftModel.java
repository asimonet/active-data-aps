package org.inria.activedata.aps.models;

import org.inria.activedata.model.LifeCycleModel;
import org.inria.activedata.model.Place;
import org.inria.activedata.model.Transition;

public class SwiftModel extends LifeCycleModel {
	private static final long serialVersionUID = 5209209078331065134L;

	public SwiftModel() {
		super("swift");
		
		Place created = getStartPlace();
		Transition initialize = addTransition("initialize");
		addArc(created, initialize);
		
		Place set = addPlace("set");
		addArc(initialize, set);

		Transition end = addTransition("end");
		Place terminated = getEndPlace();
		addArc(set, end);
		addArc(end, terminated);
		
		addCompositionTransition("derive", set, this);
		
		Transition failure = addTransition("failure");
		addArc(set, failure);
		addArc(failure, set);
	}
}
