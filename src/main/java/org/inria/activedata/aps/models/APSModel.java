package org.inria.activedata.aps.models;

import org.inria.activedata.model.*;

public class APSModel extends LifeCycleModel {
	private static final long serialVersionUID = 494098209840981L;
	
	private CompositionTransition startTransfer;

	public APSModel() {
		super("start");

		// Make a minimal life cycle
		Place start = getStartPlace();
		Transition end = addTransition("end");

		addArc(start, end);
		addArc(end, getEndPlace());

		// Connect to Globus
		GlobusModel globus = new GlobusModel("globus");
		startTransfer = addCompositionTransition("start globus", start, globus);
	}
	
	public CompositionTransition getStartTransferTransition() {
		return startTransfer;
	}
}
