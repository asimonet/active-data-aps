package org.inria.activedata.aps.models;

import org.inria.activedata.model.*;

public class AnalysisModel extends LifeCycleModel {
	private static final long serialVersionUID = 5635409123645L;

	private Place created;
	private Place pads;
	private Place terminated;
	
	private Transition transfer;
	private Transition end;
	
	public AnalysisModel() {
		super("APS");
		
		// The places
		created = getStartPlace();
		pads = addPlace("pads");
		terminated = getEndPlace();
		
		// The transitions
		transfer = addTransition("transfer");
		addArc(created, transfer);
		addArc(transfer, pads);

		end = addTransition("end");
		addArc(pads, end);
		addArc(end, terminated);

		// Compose with metadata
		addCompositionTransition("extract", created, new MetadataModel());
	}
}
