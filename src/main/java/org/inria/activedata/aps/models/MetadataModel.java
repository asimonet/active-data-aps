package org.inria.activedata.aps.models;

import org.inria.activedata.model.LifeCycleModel;
import org.inria.activedata.model.Place;
import org.inria.activedata.model.Transition;

public class MetadataModel extends LifeCycleModel {
	private static final long serialVersionUID = 6530558952708546375L;

	public MetadataModel() {
		super("metadata");
		
		Place created = getStartPlace();
		Place terminated = addPlace("terminated");

		Transition add = addTransition("add");
		addArc(created, add);
		addArc(add, created);

		Transition update = addTransition("update");
		addArc(update, created);
		addArc(created, update);
		
		Transition delete = addTransition("delete");
		addArc(delete, created);
		addArc(created, delete);

		Transition remove = addTransition("remove all");
		addArc(created, remove);
		addArc(remove, terminated);
	}
}
