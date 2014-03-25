package org.inria.activedata.aps;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import org.globusonline.transfer.JSONTransferAPIClient;
import org.inria.activedata.aps.models.StartModel;
import org.inria.activedata.model.InvalidTransitionException;
import org.inria.activedata.model.LifeCycle;
import org.inria.activedata.model.Transition;
import org.inria.activedata.model.TransitionNotEnabledException;
import org.inria.activedata.runtime.client.ActiveDataClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GlobusTransferTracker extends TimerTask {
	JSONTransferAPIClient globusClient;

	ActiveDataClient adClient;

	private Date lastCheck;

	private DateFormat dateFormat;
	
	private Transition successTransition;
	
	private Transition failureTransition;

	public GlobusTransferTracker(JSONTransferAPIClient globusClient, ActiveDataClient adClient) {
		this.globusClient = globusClient;
		this.adClient = adClient;

		lastCheck = new Date();
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		
		StartModel model = new StartModel();
		successTransition = (Transition) model.getTransition("globus.success");
		failureTransition = (Transition) model.getTransition("globus.failure");
	}

	public void run() {
		// Get all the transfers of the current user that terminated after the last check
		String lastCheckString = dateFormat.format(lastCheck);
		lastCheck = new Date();

		// TODO care of the page size
		JSONTransferAPIClient.Result r = null;

		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("filter", "status:SUCCEEDED,FAILED/completion_time:" + lastCheckString + ",");

			r = globusClient.getResult("/task_list", params);
		} catch (Exception e) {
			System.err.println("Exception while getting task list: " + e);
			e.printStackTrace();
		}

		// Check the newly terminated transfers one by one
		if(r != null) {
			try {
				int length = r.document.getInt("length");
				if (length == 0) {
					System.out.println("No transfer done this time");
					return;
				}

				JSONArray tasksArray = r.document.getJSONArray("DATA");
				int errors = 0;
				int success = 0;
				int failure = 0;
				
				for (int i=0; i < tasksArray.length(); i++) {
					// Get the task_id and status
					JSONObject taskObject = tasksArray.getJSONObject(i);
					String taskId = taskObject.getString("task_id");
					String status = taskObject.getString("status");
					
					System.out.println("Task " + taskId + " is done with status " + status);
					
					// Get the life cycle for that task
					LifeCycle lc = adClient.getLifeCycle("globus", taskId);
					if(lc == null) {
						System.err.println("Unknown life cycle for task " + taskId);
						errors++;
						continue;
					}
					
					try {
						if(status.equals("SUCCEEDED")) {
							adClient.publishTransition(successTransition, lc);
							success++;
						}
						else {
							adClient.publishTransition(failureTransition, lc);
							failure++;
						}
					} catch (TransitionNotEnabledException e) {
						errors++;
						continue;
					} catch (InvalidTransitionException e) {
						System.err.println(e);
						System.exit(1);
					}
				}
				
				System.out.println(String.format("Published (success/failure/errors): %d/%d/%d",
						success, failure, errors));
			}
			catch(JSONException e) {
				System.err.println("Error while parsing JSON: " + e.getMessage());
			}
		}
	}
}