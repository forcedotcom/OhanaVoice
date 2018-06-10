@RestResource(urlMapping='/api/ohanaVoice/futurevelocity/*')
global with sharing class FutureVelocityRestResource
{
    private static List<agf__ADM_Sprint__c> lastThreeSprintList;
    private static String teamName;
 
    @HttpGet
    global static FutureVelocityWrapper doGet() 
    {

        //TODO:
        //Parse URL to get the Team Id or Name
        //Set class-level TeamName/Id parameter
        teamName = 'MY_TEAM'; //Replace with actual team name

        RestRequest req = RestContext.request;
        RestResponse res = RestContext.response;      
        FutureVelocityWrapper fvw = new FutureVelocityWrapper();
      
        Integer avgVelocity = getFutureVelocity(fvw);
        getFutureVelocityWithPTO(fvw, avgVelocity);
           
        return fvw; 
    }

    
    private static Integer getFutureVelocity(FutureVelocityWrapper fvw) {
  		Date today = System.Date.today();

        //TODO: Add Team to WHERE Clause
		
        lastThreeSprintList = 
            new List<agf__ADM_Sprint__c>(
                [Select id,
                    agf__Days_Remaining__c,
                    agf__Completed_Story_Points__c,
                    agf__Start_Date__c,
                    agf__End_Date__c,
                    agf__Sprint_Timeframe__c
                    from agf__ADM_Sprint__c 
                    where agf__End_Date__c < :today order by agf__start_date__C desc limit 3]);

        Integer velocitySum = 0;
        for (agf__ADM_Sprint__c sprint : lastThreeSprintList) { 
            if(sprint.agf__Completed_Story_Points__c != null){
                velocitySum += Integer.valueOf(sprint.agf__Completed_Story_Points__c);  
            }         	     
        }

        Integer avgVelocity = 0;
        if(lastThreeSprintList.size() > 0){
            avgVelocity = (Integer)velocitySum/lastThreeSprintList.size();
        }		

        fvw.avgFutureVelocity = avgVelocity;

        return avgVelocity;
            
    }
    
    private static void getFutureVelocityWithPTO(FutureVelocityWrapper fvw, Integer avgVelocity) {
        
        agf__ADM_Sprint__c currentSprint = TeamHealthAPIClass.getCurrentSprint();
        Decimal avgStoryPointsByPTOAssignees = 0;
        List<string> teamMembersOnPTO = new List<string>();
        List<id> scrumTeamMembers = new List<id>();

        //average story points completed by each assignee
        //in last 3 sprints
        AggregateResult[] storyPointsByAssignee =
                [SELECT 
                agf__Assignee__c,
                AVG(agf__Story_Points__c)
                FROM agf__ADM_Work__c 
                WHERE agf__Sprint__c in :lastThreeSprintList
                AND agf__Story_Status__c = 'Closed'
                AND agf__Scrum_Team_Name__c = :teamName
                GROUP BY agf__Assignee__c]; 

        //Scrum Team members
        for(agf__ADM_Scrum_Team_Member__c teamMember : 
            [SELECT
            agf__Member_Name__c
            FROM agf__ADM_Scrum_Team_Member__c
            WHERE agf__Scrum_Team_Name__c = :teamName]){

            scrumTeamMembers.add(teamMember.agf__Member_Name__c);

        }      

        //Find time off logged for scrum team members for next sprint
        for(agf__ADM_Time_Off_Log__c timeOff : 
            [SELECT 
            agf__End_Date__c,
            agf__Start_Date__c,
            agf__User__c,
            agf__User__r.Name
            FROM agf__ADM_Time_Off_Log__c 
            WHERE agf__start_date__c > :currentSprint.agf__End_Date__c 
            AND agf__End_Date__c <= :currentSprint.agf__End_Date__c.addDays(14)
            AND agf__User__c in :scrumTeamMembers]){

            //Sum up the average points PTO team members contribute
            for(AggregateResult ar : storyPointsByAssignee){
                if(ar.get('agf__Assignee__c') == timeOff.agf__User__c){
                    avgStoryPointsByPTOAssignees += Integer.valueOf(ar.get('expr0'));
                    teamMembersOnPTO.add(timeOff.agf__User__r.Name);      
                }
            }

        }

        Integer avgFutureVelocityWithPTO = Integer.valueOf(avgVelocity - avgStoryPointsByPTOAssignees);
        String teamMemberNamesOnPTO = string.join(teamMembersOnPTO,',');

        if(avgFutureVelocityWithPTO <= 0){
            avgFutureVelocityWithPTO = 0;
            teamMemberNamesOnPTO = 'Everyone';
        }
        fvw.avgFutureVelocityWithPTO = avgFutureVelocityWithPTO;
        fvw.teamMemberOnPTO = teamMemberNamesOnPTO;
    }
    
    global class FutureVelocityWrapper{
        public Integer  avgFutureVelocity {get;set;}
        public String teamMemberOnPTO {get;set;}
        public Integer  avgFutureVelocityWithPTO {get;set;}           
    }

}