package org.inria.activedata.aps.models;

import org.inria.activedata.model.LifeCycleModel;
import org.inria.activedata.model.Place;
import org.inria.activedata.model.Transition;

public class GlobusModel extends LifeCycleModel {
	private static final long serialVersionUID = 4983478791298L;

	private Place created;
	private Place succeeded;
	private Place failed;
	private Place end;

	private Transition success;
	private Transition failure;
	private Transition successEnd;
	private Transition failureEnd;

	public GlobusModel(String name) {
		super(name);

		created = getStartPlace();
		succeeded = addPlace("Succeeded");
		failed = addPlace("Failed");
		end = getEndPlace();

		success = addTransition("success");
		failure = addTransition("failure");
		successEnd = addTransition("success end");
		failureEnd = addTransition("failure end");

		addArc(created, success);
		addArc(created, failure);
		addArc(success, succeeded);
		addArc(failure, failed);
		addArc(succeeded, successEnd);
		addArc(failed, failureEnd);
		addArc(successEnd, end);
		addArc(failureEnd, end);
		
		// Compose with the analysis
		addCompositionTransition("end transfer", succeeded, new AnalysisModel());
	}
}
