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

public class GlobusTransferTracker extends TimerTask {
	/**
	 * Number of results to get from the REST API (paging)
	 */
	public static int LIMIT = 50;

	JSONTransferAPIClient globusClient;

	ActiveDataClient adClient;

	private Date lastCheck;

	private DateFormat dateFormat;

	private Transition successTransition;

	private Transition failureTransition;
	
	private Place successPlace;

	private CompositionTransition endTransferTransition;
	
	Pattern pattern;

	public GlobusTransferTracker(JSONTransferAPIClient globusClient, ActiveDataClient adClient) {
		this.globusClient = globusClient;
		this.adClient = adClient;

		lastCheck = new Date();
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

		APSModel model = new APSModel();
		successTransition = (Transition) model.getTransition("globus.success");
		failureTransition = (Transition) model.getTransition("globus.failure");
		endTransferTransition = (CompositionTransition) model.getTransition("globus.end transfer");
		successPlace = model.getPlace("globus.success");
		
		// Prepare the regex for file path matching
		pattern = Pattern.compile("^(\\/~\\/[^\\/]+\\/[^\\/]+)\\/.*$");
	}

	public void run() {
		// Get all the transfers of the current user that terminated after the last check
		String lastCheckString = dateFormat.format(lastCheck);
		lastCheck = new Date();

		// Paging
		int offset = 0;

		// Stats
		int errors = 0;
		int success = 0;
		int failure = 0;

		while(true) {
			// Make the query
			JSONTransferAPIClient.Result r = null;

			try {
				Map<String, String> params = new HashMap<String, String>();
				params.put("filter", "status:SUCCEEDED,FAILED/completion_time:" + lastCheckString + ",");
				params.put("offset", String.valueOf(offset));
				params.put("limit", String.valueOf(LIMIT));

				r = globusClient.getResult("/task_list", params);
			} catch (Exception e) {
				System.err.println("Could not make REST API call: " + e);
				break;
			}

			// Check the newly terminated transfers one by one
			if(r != null) {
				try {
					int length = r.document.getInt("length");
					if (length == 0)
						break;

					JSONArray tasksArray = r.document.getJSONArray("DATA");

					for (int i=0; i < tasksArray.length(); i++) {
						// Get the task_id and status
						JSONObject taskObject = tasksArray.getJSONObject(i);
						String taskId = taskObject.getString("task_id");
						String status = taskObject.getString("status");

						// Get the life cycle for that task
						LifeCycle lc = adClient.getLifeCycle("globus", taskId);
						if(lc == null) {
							System.err.println("Unknown life cycle for task " + taskId);
							errors++;
							continue;
						}

						try {
							if(status.equals("FAILED")) {
								adClient.publishTransition(failureTransition, lc);
								failure++;
								continue;
							}
							
							// Publish the success transition for the whole transfer
							adClient.publishTransition(successTransition, lc);

							// Now we publish the composition transition for each dataset in the transfer
							Map<String, String> params = new HashMap<String, String>();
							r = globusClient.getResult("task/" + taskId + "/successful_transfers", params);
							JSONArray filesArray = r.document.getJSONArray("DATA");
							
							Token t = lc.getTokens(successPlace).values().iterator().next();
							
							Set<String> paths = new HashSet<String>();
							for(int j=0; i < filesArray.length(); i++) {
								// Get the path and publish a transition only for top-level directories
								JSONObject fileObject = filesArray.getJSONObject(j);
								String path = fileObject.getString("destination_path");
								path = pattern.matcher(path).group();
								
								if(path == null || path.equals("")) {
									errors++;
									continue;
								}
								
								if(!paths.contains(path)) {
									paths.add(path);
									
									adClient.publishTransition(endTransferTransition, lc, t, path);
									System.out.println("Published composition transition for " + path);
								}
							}

							success++;
						} catch (TransitionNotEnabledException e) {
							errors++;
							continue;
						} catch (InvalidTransitionException e) {
							System.err.println(e);
							System.exit(1);
						} catch (Exception e) {
							System.err.println(e);
							errors++;
						}
					}
				}
				catch(JSONException e) {
					System.err.println("Error while parsing JSON: " + e.getMessage());
				}
			}

			offset += LIMIT;
		}
		System.out.println(String.format("Published (success/failure/errors): %d/%d/%d",
				success, failure, errors));
	}
}