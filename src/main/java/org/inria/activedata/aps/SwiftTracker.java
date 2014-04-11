package org.inria.activedata.aps;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.globusonline.transfer.APIError;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.inria.activedata.aps.models.APSModel;
import org.inria.activedata.model.CompositionTransition;
import org.inria.activedata.model.InvalidTransitionException;
import org.inria.activedata.model.LifeCycle;
import org.inria.activedata.model.Place;
import org.inria.activedata.model.Token;
import org.inria.activedata.model.Transition;
import org.inria.activedata.model.TransitionNotEnabledException;
import org.inria.activedata.runtime.client.ActiveDataClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SwiftTracker extends TimerTask {
	// TRACE VARIABLE, line 9, thread 1-9, f WAIT swift#mapper#17000
	private static final String NEW_PATTERN = "TRACE\\sVARIABLE,\\s(line\\s\\d+),\\s(thread \\d+\\-\\d+), (\\w+)\\sWAIT\\s.+";
	
	// TRACE VARIABLE, line 8, thread main, fin INITIALIZED <single_file_mapper; input = true, file = input.txt>
	private static final String INITIALIZE_PATTERN = "TRACE\\sVARIABLE,\\s(line\\s\\d+),\\sthread\\s([\\w\\-]+), (\\w+)\\sINITIALIZED\\s.+";

	/**
	 * Number of results to get from the REST API (paging)
	 */
	ActiveDataClient adClient;


	private CompositionTransition endTransferTransition;

	Pattern pattern;

	public SwiftTracker(ActiveDataClient adClient) {
		this.adClient = adClient;


		// Prepare the regex for file path matching
		pattern = Pattern.compile("^(\\/~\\/[^\\/]+\\/[^\\/]+)\\/.*$");
	}

	public void run() {
	}
}