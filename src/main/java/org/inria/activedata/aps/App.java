package org.inria.activedata.aps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Timer;

import org.globusonline.transfer.APIError;
import org.globusonline.transfer.Authenticator;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.GoauthAuthenticator;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.inria.activedata.aps.models.APSModel;
import org.inria.activedata.model.LifeCycle;
import org.inria.activedata.model.Token;
import org.inria.activedata.runtime.client.ActiveDataClient;
import org.inria.activedata.runtime.communication.rmi.RMIDriver;
import org.json.JSONException;
import org.json.JSONObject;

public class App {
	/**
	 * Delay in seconds between successive executions of the
	 * task that publishes Active Data transitions for Globus
	 * transfers.
	 */
	public static final int DELAY = 60;

	public static final String LOCAL_PATH = "/tmp/globus_xp/";

	public static final String SOURCE_PATH = "/~/tmp/globus_xp/";

	public static final String SOURCE_ENDPOINT = "asimonet#mbp";
	
	public static final String DESTINATION_PATH = "/~/aps/";

	public static final String DESTINATION_ENDPOINT = "asimonet#aps";

	/**
	 * Size in bytes of the random files for test transfers
	 */
	private static int SOURCE_SIZE = 1024 * 5;

	/**
	 * Just a counter for naming the randomly generated files
	 */
	private static int i = 0;

	private ActiveDataClient adClient;

	private JSONTransferAPIClient globusClient;

	private APSModel model;

	public App(JSONTransferAPIClient globusClient) {
		adClient = ActiveDataClient.getInstance();
		model = new APSModel();
		this.globusClient = globusClient;
		
		// Auto-activate the endpoints
		try {
			autoActivate(SOURCE_ENDPOINT);
			autoActivate(DESTINATION_ENDPOINT);
		} catch (Exception e) {
			System.err.println("Error auto-activating endpoints: " + e);
			System.exit(17);
		}
	}

	public void startGlobusTransfer() throws Exception {
		// Generate a random file
		byte[] rand = new byte[SOURCE_SIZE];
		new Random().nextBytes(rand);

		File f = new File(LOCAL_PATH + "rand_" + (++i));
		if(f.exists())
			f.delete();
		f.deleteOnExit();

		FileOutputStream out = new FileOutputStream(f);
		out.write(rand);
		out.close();

		// Publish the life cycle
		LifeCycle lc = adClient.createAndPublishLifeCycle(model, f.getName());
		Token t = lc.getTokens(model.getStartPlace()).values().iterator().next();

		// Make a transfer submission
		String srcPath = SOURCE_PATH + f.getName();
		String destPath = DESTINATION_PATH + f.getName();
		String taskId = submitTransfer(SOURCE_ENDPOINT, srcPath, DESTINATION_ENDPOINT, destPath);

		// Now compose with the transfer model using the task id
		adClient.publishTransition(model.getStartTransferTransition(), lc, t, taskId);
	}

	private String submitTransfer(String srcEndpoint, String srcPath, String destEP, String destPath) throws Exception {
		JSONTransferAPIClient.Result r;

		System.out.println("Starting transfer  " + SOURCE_ENDPOINT + " path " + srcPath
				+ " -> " + DESTINATION_ENDPOINT + " path " + destPath);

		// Ask for a submission id
		r = globusClient.getResult("/transfer/submission_id");
		String submissionId = r.document.getString("value");

		// Construct a query
		JSONObject transfer = new JSONObject();
		transfer.put("DATA_TYPE", "transfer");
		transfer.put("submission_id", submissionId);
		JSONObject item = new JSONObject();
		item.put("DATA_TYPE", "transfer_item");
		item.put("source_endpoint", SOURCE_ENDPOINT);
		item.put("source_path", srcPath);
		item.put("destination_endpoint", DESTINATION_ENDPOINT);
		item.put("destination_path", destPath);
		item.put("recursive", false);
		transfer.append("DATA", item);

		// Make the actual submission
		r = globusClient.postResult("/transfer", transfer, null);

		return r.document.getString("task_id");
	}

	public boolean autoActivate(String endpointName)
			throws IOException, JSONException, GeneralSecurityException, APIError {
		String resource = BaseTransferAPIClient.endpointPath(endpointName)
				+ "/autoactivate";
		
		JSONTransferAPIClient.Result r = globusClient.postResult(resource, null,
				null);
		String code = r.document.getString("code");
		return !code.startsWith("AutoActivationFailed");
	}

	public static void usage() {
		System.err.println("Usage: java " + App.class.getName() + " <AD RMI host> <AD RMI port>");
		System.exit(13);
	}

	public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException {
		// Command line arguments
		if(args.length != 2)
			usage();

		String adRMIHost = args[0];
		int adRMIPort = -1;
		try {
			adRMIPort = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException e) {
			System.err.println("Invalid port for Active Data RMI server");
			System.exit(14);
		}

		String globusAuthToken = System.getenv("GLOBUS_OAUTH_TOKEN");
		String globusUsername = System.getenv("GLOBUS_USERNAME");
		
		if(globusAuthToken == null || globusUsername == null) {
			System.out.println("Environment variables GLOBUS_OAUTH_TOKEN and GLOBUS_USERNAME " +
					"must be defined.");
			System.exit(15);
		}

		// Connect to the Active Data service
		try {
			RMIDriver driver = new RMIDriver(adRMIHost, adRMIPort);
			driver.connect();
			ActiveDataClient.init(driver);
		} catch (Exception e) {
			System.err.println("Could not connect to Active Data service on " + args[0] + ':' +
					args[1] + ": " + e.getMessage());
			System.exit(16);
		}

		// Connect a globus client
		Authenticator authenticator = new GoauthAuthenticator(globusAuthToken);
		JSONTransferAPIClient globusClient = new JSONTransferAPIClient(globusUsername);
		globusClient.setAuthenticator(authenticator);

		new Timer().schedule(new GlobusTransferTracker(globusClient, ActiveDataClient.getInstance()),
				DELAY * 1000, DELAY * 1000);

		App app = new App(globusClient);

		// Start some Globus transfers
		File tmp = new File(LOCAL_PATH);
		if(!tmp.exists())
			tmp.mkdirs();

		for(int i = 0; i < 1; i++) {
			try {
				app.startGlobusTransfer();
				
				Thread.sleep(200);
			} catch (Exception e) {
				System.err.println("Error with Globus transfer: " + e);
			}
		}
	}
}
