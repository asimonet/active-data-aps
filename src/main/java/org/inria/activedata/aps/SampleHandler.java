package org.inria.activedata.aps;

import org.inria.activedata.aps.models.APSModel;
import org.inria.activedata.model.AbstractTransition;
import org.inria.activedata.model.LifeCycle;
import org.inria.activedata.model.LifeCycleModel;
import org.inria.activedata.model.Token;
import org.inria.activedata.runtime.client.ActiveDataClient;
import org.inria.activedata.runtime.client.TransitionHandler;
import org.inria.activedata.runtime.communication.rmi.RMIDriver;

public class SampleHandler implements TransitionHandler {

	static ActiveDataClient client;

	public static void usage() {
		System.err.println("Usage: SampleHandler ad_host ad_port");
		System.exit(1);
	}
	
	public static void main(String args[]) throws Exception {
		if(args.length != 2)
			usage();
		
		RMIDriver driver = new RMIDriver(args[0], Integer.parseInt(args[1]));
		ActiveDataClient.init(driver);
		client = ActiveDataClient.getInstance();
		
		LifeCycleModel model = new APSModel();
		TransitionHandler handler = new SampleHandler();
		
		client.subscribeTo(model.getTransition("globus.success"), handler);
		client.subscribeTo(model.getTransition("globus.failure"), handler);
		client.subscribeTo(model.getTransition("filesystem B.extract"), handler);
		client.subscribeTo(model.getTransition("filesystem B.start swift"), handler);
		client.subscribeTo(model.getTransition("swift.initialize"), handler);
		client.subscribeTo(model.getTransition("swift.derive"), handler);
		client.subscribeTo(model.getTransition("swift.failure"), handler);
	}

	@Override
	public void handler(AbstractTransition transition, boolean isLocal, Token[] inTokens, Token[] outTokens) {
		Token t = inTokens[0];

		if(transition.getFullName().equals("globus.success")) {
			System.out.println("Globus transfer successful: " + t.getUid());
		}

		else if(transition.getFullName().equals("globus.failure")) {
			System.out.println("Globus transfer failed: " + t.getUid());
		}

		else if(transition.getFullName().equals("filesystem B.extract")) {
			System.out.println("Metadata extracted for dataset " + t.getUid());
		}

		else if(transition.getFullName().equals("filesystem B.start swift")) {
			System.out.println(outTokens[0].getUid() + " entering Swift workflow");
		}

		else if(transition.getFullName().equals("swift.initialize")) {
			System.out.println(t.getUid() + " assigned to a variable in Swift");
		}

		else if(transition.getFullName().equals("swift.derive")) {
			System.out.println(t.getUid() + " derived in " + outTokens[0].getUid());
		}

		else if(transition.getFullName().equals("swift.failure")) {
			System.out.println("Swift job failed on " + t.getUid());
			for(String tag: t.getTags())
				System.out.println("\t" + tag);
		}
	}
}
