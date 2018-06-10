
package ohanavoice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.LinkAccountCard;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;

public class OhanaVoiceSpeechlet implements Speechlet {

	
	private final String SESSION_TITLE = "sessionTitle";
	private final String SESSION_THEME = "sessionTheme";
	private final String SESSION_RANK = "sessionRank";
	private final String TEAMHEALTH_INTENT = "TeamHealth";
	private final String FUTUREVELOCITY_INTENT = "FutureVelocity";
	private final String CREATESTORY_INTENT = "CreateStory";
	private final String RANK_SLOT = "Rank";
	private final String THEME_SLOT = "Theme";
	private final String TITLE_SLOT = "Title";
	private final int HTTP_STATUS_UNAUTHORIZED = 401;
	private final String SF_INSTANCE_URL = "";
        private String sfInstanceAccessToken;


    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
    	logSessionAndRequestInfo("onSessionStarted", request.getRequestId(), session.getSessionId());
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
    	logSessionAndRequestInfo("onLaunch", request.getRequestId(), session.getSessionId());
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
    	logSessionAndRequestInfo("onIntent", request.getRequestId(), session.getSessionId());
    	
    	sfInstanceAccessToken = session.getUser().getAccessToken();
    	if(sfInstanceAccessToken == null){
			return getInvalidAccessTokenResponse();
    	}
    	
        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;
        System.out.println("Intent Name: " + intentName);

        if (TEAMHEALTH_INTENT.equals(intentName)) {
            return getTeamHealthResponse();
        } 
        else if(FUTUREVELOCITY_INTENT.equals(intentName)){
        	return getFutureVelocityResponse();
        }
        else if(CREATESTORY_INTENT.equals(intentName)){
        	
        	Slot rankSlot = intent.getSlot(RANK_SLOT);
        	Slot themeSlot = intent.getSlot(THEME_SLOT);
        	Slot titleSlot = intent.getSlot(TITLE_SLOT);
        	
        	if(themeSlot != null && themeSlot.getValue() != null){
	       		return handleThemeDialogRequest(session, themeSlot);
	    	}
        	else if(rankSlot != null && rankSlot.getValue() != null){
        		return handleRankDialogRequest(session, rankSlot);
        	}
        	else if(titleSlot != null && titleSlot.getValue() != null){
        		return handleTitleDialogRequest(session, titleSlot);
        	}
        	
        	return getNextCreateStoryDialog(session);
        }
        else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelpResponse();
        } 
        else {
            throw new SpeechletException("Invalid Intent");
        }
        
    }
    
    
    private SpeechletResponse handleThemeDialogRequest(final Session session,
    		final Slot themeSlot) {
    	String theme = themeSlot.getValue();   	
    	session.setAttribute(SESSION_THEME, theme);

    	return getNextCreateStoryDialog(session);	
	}
    
	private SpeechletResponse handleTitleDialogRequest(Session session, Slot titleSlot) {
		String title = titleSlot.getValue();   	
    	session.setAttribute(SESSION_TITLE, title);

    	return getNextCreateStoryDialog(session);			
	}

	private SpeechletResponse handleRankDialogRequest(Session session, Slot rankSlot) {
		String rank = rankSlot.getValue();   	
    	session.setAttribute(SESSION_RANK, rank);
    	     	
    	return getNextCreateStoryDialog(session);	
	}
	
	private SpeechletResponse createStory(Session session) {
		String speechText;
		String postResult = null;
		HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 

		try {

		    HttpPost request = new HttpPost(SF_INSTANCE_URL + "//services/apexrest/api/ohanaVoice/createstory");
		
		    JSONObject json = new JSONObject();
		    json.put("title", session.getAttribute(SESSION_TITLE));
		    json.put("rank", session.getAttribute(SESSION_RANK));
		    json.put("theme", session.getAttribute(SESSION_THEME));
		    
		    StringEntity params = new StringEntity(json.toString());
		    
		    request.addHeader("Content-Type","application/json");
		    request.addHeader("Authorization","Bearer " + sfInstanceAccessToken);
		    request.addHeader("Content-Type","application/json");
		    request.addHeader("accept","application/json");
		    request.setEntity(params);
		    HttpResponse response = httpClient.execute(request);

		    //handle response here...
		    
		 // verify response is HTTP OK
	        final int statusCode = response.getStatusLine().getStatusCode();
	        if (statusCode != HttpStatus.SC_OK) {
	        	
	        	if(statusCode == HTTP_STATUS_UNAUTHORIZED){
	        		return getInvalidAccessTokenResponse();
	        	}
	        	
	            String error = "There was an error creating the story in Salesforce";
	            // Error is in EntityUtils.toString(response.getEntity()) 
	            //return SpeechletResponse.newTellResponse(new PlainTextOutputSpeech().setText(error));
	        }

	        postResult = null;
	        try {
	        	postResult = EntityUtils.toString(response.getEntity());
	        } catch (IOException ioException) {
	        	 System.out.println(ioException);
	        }
	        System.out.println(postResult);

		}
		catch (Exception ex) {

			System.out.println(ex);

		} 
		
		
		speechText = "Story created in the backlog";
		
		// Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Create Story");
        card.setContent("Created story with ID: " + postResult);

        // Create the plain text output.
        SsmlOutputSpeech speech = new SsmlOutputSpeech();
        speech.setSsml("<speak>" + speechText + "</speak>");

        return SpeechletResponse.newTellResponse(speech, card);
	}
		
	private SpeechletResponse getNextCreateStoryDialog(Session session){

		String speechOutput = null;
    	String repromptText = null;

    	if(!session.getAttributes().containsKey(SESSION_TITLE)){
    		speechOutput = "What should the story title be?";
    		repromptText = "What should the title be for the new story be?";
    		
    		return newAskResponse(speechOutput, repromptText); 
    	}
    	else if(!session.getAttributes().containsKey(SESSION_RANK)){	
    	    speechOutput = "What should the story rank be?";
    	    repromptText = "What should the rank be for the new story?";
        	
        	return newAskResponse(speechOutput, repromptText);
    	}
    	else if(!session.getAttributes().containsKey(SESSION_THEME)){ 	
	    	speechOutput = "What should the story theme be?";
	    	repromptText = "What should the theme be for the new story?";
    	
	    	return newAskResponse(speechOutput, repromptText);
    	}
		
		return createStory(session);
	}

	private SpeechletResponse getFutureVelocityResponse() {
		String getResult = "";
		String speechText = "";
		
	    try {
			getResult = makeGetRequest("/services/apexrest/api/ohanaVoice/futurevelocity");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	    
		if(getResult == Integer.toString(HTTP_STATUS_UNAUTHORIZED)){
			return getInvalidAccessTokenResponse();
		}
	     
	    System.out.println("futureVelocity json response " + getResult);
	    
	     JSONObject jsonObject = null;
	     String teamMemberOnPTO = null;
	     int avgFutureVelocityWithPTO = 0;
	     int avgFutureVelocity = 0;
	     
        try {
            jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
            teamMemberOnPTO = jsonObject.getString("teamMemberOnPTO");
            avgFutureVelocityWithPTO = jsonObject.getInt("avgFutureVelocityWithPTO");
            avgFutureVelocity = jsonObject.getInt("avgFutureVelocity");
        } catch (JSONException jsonException) {
            System.out.println("Json exception occured: " + jsonException.getMessage());
        }
        
        if(teamMemberOnPTO != null){
        	
        	speechText = "<p>Your projected future velocity is <say-as interpret-as=\"unit\">" + avgFutureVelocityWithPTO + 
        			" </say-as>points because " + teamMemberOnPTO + " will be out of office</p> ";
        	
        	speechText += "<p>Your future velocity would be <say-as interpret-as=\"unit\">" + (avgFutureVelocity - avgFutureVelocityWithPTO) + "</say-as> points higher if " +
        			teamMemberOnPTO + " was not out of office</p>";
        	
        }
        else{
        	
        	speechText = "<p>Your projected future velocity is <say-as interpret-as=\"unit\">" + avgFutureVelocity + 
        			" </say-as>with no team members out of office next sprint</p>";
        }

         // Create the Simple card content.
         SimpleCard card = new SimpleCard();
         card.setTitle("Future Velocity");
         card.setContent(speechText);

         // Create the plain text output.
         SsmlOutputSpeech speech = new SsmlOutputSpeech();
         speech.setSsml("<speak>" + speechText + "</speak>");

         return SpeechletResponse.newTellResponse(speech, card);
	}

	private SpeechletResponse getTeamHealthResponse() {
		String getResult = ""; 
		
		try {
			getResult = makeGetRequest("/services/apexrest/api/ohanaVoice/teamhealth");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
		if(getResult == Integer.toString(HTTP_STATUS_UNAUTHORIZED)){
			return getInvalidAccessTokenResponse();
		}
	     
	     JSONObject jsonObject = null;
	     String themeName = null;
	     String teamPoints = null;
	     String epicTotal = null;
	     String epicNames = null;
	     
        try {
            jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
            themeName = jsonObject.getString("themeName");
            teamPoints = jsonObject.getString("teamPoints");
            epicTotal = jsonObject.getString("epicTotal");
            epicNames = jsonObject.getString("epicNames");
            
        } catch (JSONException jsonException) {
        	System.out.println(jsonException.getMessage());
        }
	
	   	 String timeSpend = "<p> During the last three sprints, you have spent most of your time in: " +
	   			 "<say-as interpret-as=\"spell-out\">"+ themeName +"</say-as> </p> ";
	   	 String averageVelocity = "<p>Your average velocity was: <say-as interpret-as=\"unit\">"+ teamPoints + 
	   			 " Points</say-as></p>";
	   	 String epicCompletion = "<p>You have: <say-as interpret-as=\"cardinal\">" + epicTotal +
	   			 "</say-as>: Epics that are greater than <say-as interpret-as=\"cardinal\">75</say-as> percent complete:" +
	   			 epicNames + "</p>";
	
	     // Create the Simple card content.
	     SimpleCard card = new SimpleCard();
	     card.setTitle("Team Health");
	     card.setContent(timeSpend + "\n" + averageVelocity + "\n" + epicCompletion);
	
	     // Create the plain text output.
	     SsmlOutputSpeech speech = new SsmlOutputSpeech();
	     speech.setSsml("<speak>" + timeSpend + averageVelocity +  epicCompletion + "</speak>");
	
	     return SpeechletResponse.newTellResponse(speech, card);
	     
		}

	@Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
    	logSessionAndRequestInfo("onSessionEnded", request.getRequestId(), session.getSessionId());
        // any cleanup logic goes here
    }
    
    private void logSessionAndRequestInfo(String theMethod, String requestId, String sessionId){
    	System.out.println(theMethod + " requestId=" + requestId + " sessionId=" + sessionId);
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getWelcomeResponse() {
        String speechText = "Welcome to the Alexa interface to Salesforce Gus";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Alexa interface to Salesforce Gus");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the help intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelpResponse() {
        String speechText = "You can ask gus to create a story";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Alexa interface to Salesforce Gus");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }
    
    private SpeechletResponse getInvalidAccessTokenResponse(){
    	
		String error = "Your session has expired. Please try again to refresh your session or link your account with Salesforce in the Alexa App before using this skill";
		SsmlOutputSpeech speech = new SsmlOutputSpeech();
        speech.setSsml("<speak>" + error + "</speak>");
		return SpeechletResponse.newTellResponse(speech, new LinkAccountCard());
    }
    /**
     * Wrapper for creating the Ask response from the input strings with
     * plain text output and reprompt speeches.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
        return newAskResponse(stringOutput, false, repromptText, false);
    }

    /**
     * Wrapper for creating the Ask response from the input strings.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param isOutputSsml
     *            whether the output text is of type SSML
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @param isRepromptSsml
     *            whether the reprompt text is of type SSML
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml,
            String repromptText, boolean isRepromptSsml) {
        OutputSpeech outputSpeech, repromptOutputSpeech;
        if (isOutputSsml) {
            outputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
        } else {
            outputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
            System.out.println("Setting outputSpeech: " + stringOutput);
        }

        if (isRepromptSsml) {
            repromptOutputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) repromptOutputSpeech).setSsml(stringOutput);
        } else {
            repromptOutputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
            System.out.println("Setting repromptOutputSpeech: " + repromptText);
        }

        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);
        
        System.out.println("speechlet response: " + SpeechletResponse.newAskResponse(outputSpeech, reprompt));
        
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }    
    
    
    private String makeGetRequest(String apiName) throws IOException{
    	HttpURLConnection connection = null; 
        URL url = new URL(SF_INSTANCE_URL + "/" + apiName);
        
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization","Bearer " + sfInstanceAccessToken);
        connection.setRequestProperty("Content-Type","application/json");
        connection.setRequestProperty("accept","application/json");

        connection.connect();
        int responseCode = connection.getResponseCode();
        if(responseCode == HTTP_STATUS_UNAUTHORIZED){
        	return "" + HTTP_STATUS_UNAUTHORIZED;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
        StringBuilder sb = new StringBuilder();
        String output;
        while ((output = br.readLine()) != null) {
        sb.append(output);
        } 
        System.out.println("json response for " + apiName + " " + sb.toString());
        return sb.toString();	
    }
    
}
