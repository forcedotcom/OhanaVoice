# Alexa interface to Salesforce Agile Accelerator 

## Overview

Integrates with Saleforce-connected Heroku app to create/retrieve data to assist Sprint planning.

## Code Example

```java
/**
 * Creates a SpeechletResponse for the create a GUS story intent.
 *
 * @return SpeechletResponse spoken and visual response for the given intent
 */
private SpeechletResponse getNextCreateStoryDialog(Session session){

    String speechOutput = null;
    String repromptText = null;
    
    if(!session.getAttributes().containsKey(SESSION_TITLE)){
    	speechOutput = "What should the story title be?";
    	repromptText = "What should the title be for the new story be?";
    	
    	return newAskResponse(speechOutput, repromptText); 
}
```

## Installation

### Amazon Echo
Create Custom Skill (See AWS Documentation)

### Java 
* Install Ohana Voice Servlet to Heroku (or other host)
* Update the [SF_INSTANCE_URL](java/ohanavoice/src/main/java/ohanavoice/OhanaVoiceSpeechlet.java#L51)


### Salesforce
* Create developer Salesforce Org
* Install [Agile Accelerator](https://appexchange.salesforce.com/listingDetail?listingId=a0N30000000ps3jEAA)
* Create new [Connected App](https://help.salesforce.com/articleView?id=connected_app_create.htm&type=0) in Salesforce Org
* Install Force.com App
* Update REST resource class `teamName` property for each resource you will use (ex. [FutureVelocityRestResource.cls](https://github.com/forcedotcom/OhanaVoice/blob/master/ohanavoice/src/classes/FutureVelocityRestResource.cls#L14))

## Contributors
* Anuj Gudivada
* Archit Jain
* Paramesh Marina
* Paul Rodibaugh
* Raghunath Polu
* Seth Anderson
