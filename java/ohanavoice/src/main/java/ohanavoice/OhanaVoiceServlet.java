package ohanavoice;

import com.amazon.speech.speechlet.servlet.SpeechletServlet;

public class OhanaVoiceServlet extends SpeechletServlet{
	  public OhanaVoiceServlet() { 
		    this.setSpeechlet(new OhanaVoiceSpeechlet());
	  }
}
