package org.inria.activedata.aps.models;

import org.inria.activedata.model.*;

public class AnalysisModel extends LifeCycleModel {
	private static final long serialVersionUID = 5635409123645L;

	private Place created;
	private Place terminated;
	
	private Transition end;
	
	public AnalysisModel() {
		super("filesystem B");
		
		// The places
		created = getStartPlace();
		terminated = getEndPlace();
		
		// Add a single transition
		end = addTransition("end");
		addArc(created, end);
		addArc(end, terminated);

		// Compose with metadata
		addCompositionTransition("extract", created, new MetadataModel());
		
		// Compose with Globus
		//addCompositionTransition("start transfer", created, new GlobusModel("globus-2"));
		
		// Compose with Swift
		addCompositionTransition("start swift", created, new SwiftModel());
	}
}
