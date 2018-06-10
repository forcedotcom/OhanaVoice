@RestResource(urlMapping='/Team')
global with sharing class TeamResource  {

    @HttpGet
    global static agf__ADM_Scrum_Team__c show() {
	    String scrumTeamName = RestContext.request.params.get('scrumTeamName');
	    agf__ADM_Scrum_Team__c scrumTeam = [ select Id, Name from agf__ADM_Scrum_Team__c where Name = :scrumTeamName and agf__Active__c = true limit 1];

        return scrumTeam;
    }

    @HttpPost
    global static agf__ADM_Scrum_Team__c create() {

        agf__ADM_Scrum_Team__c obj;

        return obj;

    }

    @HttpPut
    global static agf__ADM_Scrum_Team__c updateObject() {

        agf__ADM_Scrum_Team__c obj;

        return obj;

    }

    @HttpDelete
    global static void remove() {

    }

}
