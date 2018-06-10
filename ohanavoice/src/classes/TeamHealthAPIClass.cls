@RestResource(urlMapping='/api/ohanaVoice/*')
global with sharing class TeamHealthAPIClass
{

    final static integer SPRINT_TOTAL_DAYS = 14;
    private static agf__ADM_Sprint__c currentSprint;
    private static String teamId; 
    
    @HttpGet
    global static TeamHealthWrapper doGet() 
    {

      RestRequest req = RestContext.request;
      RestResponse res = RestContext.response;       
      TeamHealthWrapper thw = new TeamHealthWrapper();

      teamId = req.params.get('teamId');
      currentSprint = getCurrentSprint();
      System.debug(teamId);
                   
      thw.themeName = getMostUsedTheme();
      thw.teamPoints = getAverageThreeSprintVelocity();       
       
      Set<String> epicNames = getEpicTotals();

      if(epicNames != null){
        thw.epicTotal = String.valueOf(epicNames.size());
        thw.epicNames = String.join(new List<String>(epicNames),',');
      }

      return thw;
    }

    
    private static String getMostUsedTheme(){
      System.debug(teamId);
    
       Date threeSprintStartDate = currentSprint.agf__Start_Date__c.addDays(-(SPRINT_TOTAL_DAYS * 3));
       
       Map<String,Integer> themeUsageMap = new Map<String,Integer>();
       
       Integer maxUsageCount = 0;
       String maxThemeName = '';
              
        List<agf__ADM_Work__c> workList = [
          SELECT id, Name , agf__Theme__r.Name
          FROM agf__ADM_Work__c 
          WHERE agf__Sprint__c IN 
            (SELECT id 
            FROM agf__ADM_Sprint__c 
            WHERE agf__Start_Date__c >:threeSprintStartDate OR agf__Start_Date__c < TODAY ) AND agf__Theme__r.Name != null and agf__Scrum_Team__c = :teamId ];
        
                                        
       for(agf__ADM_Work__c adm : workList){

          Integer themeUsageCount = themeUsageMap.get(adm.agf__Theme__r.Name);

           if(themeUsageCount != null){     
              themeUsageMap.put(adm.agf__Theme__r.Name , ++themeUsageCount);
           }
           else{
              //initialize at 1
              themeUsageMap.put(adm.agf__Theme__r.Name, 1);
           }
       }
           
           System.debug(themeUsageMap);
           for(String themeName : themeUsageMap.keyset()){
           
               if(themeUsageMap.get(themeName) > maxUsageCount){
                   maxUsageCount = themeUsageMap.get(themeName);
                   maxThemeName = themeName;
                   
               }
               
           
           }
           
           return maxThemeName;
    }
    
    
    private static Set<String> getEpicTotals(){
    
        Map<String,Integer> epicToStoryMap = new Map<String, Integer>();
        Map<String,Integer> epicToClosedStoriesMap = new Map<String,Integer>();
        Map<String,Decimal> epicCompleteMap = new Map<String,Decimal>();
        Map<String,String> returnMap = new Map<String,String>();
    
        //get all stories with an assigned epic



        List<agf__ADM_Work__c> works = [
          SELECT id,
          agf__Epic__r.Name,
          agf__Status__c
          FROM
          agf__ADM_Work__c WHERE
          agf__Epic__c != null and agf__Scrum_Team__c = :teamId ]; 
        
        Integer epicClosedStoryCount = 0;
        Integer epicStoryCount = 0;

        for(agf__ADM_Work__c work : works){ 

            string epicName = work.agf__Epic__r.name;

            if(epicToStoryMap.get(epicName) != null)
            {
                epicToStoryMap.put(epicName, ++epicStoryCount);
            }
            else{
              //initialize epic to story count &
              //epic to closed story count maps
              epicToStoryMap.put(epicName, 1);
              epicToClosedStoriesMap.put(epicName, 0);
            }

                
            if(work.agf__Status__c == 'Closed')
            {                   
                 epicToClosedStoriesMap.put(epicName, ++epicClosedStoryCount);
            }

        }

        System.Debug('epicToStoryMap ' + epicToStoryMap);
        System.Debug('epicToClosedStoriesMap' + epicToClosedStoriesMap);

        if(epicToStoryMap != null &&  epicToClosedStoriesMap != null){

          for (String epicName : epicToStoryMap.keySet()){

              if(epicToStoryMap.get(epicName) > 0 && epicToClosedStoriesMap.get(epicName) > 0){
                  
                  Decimal totalStories = Decimal.valueOf(epicToStoryMap.get(epicName));
                  Decimal closedStories = Decimal.valueOf(epicToClosedStoriesMap.get(epicName));
                  Decimal percent = ((closedStories / totalStories) * 100).SetScale(2);
                  System.debug('percent >>>>>>> '+percent);
                  if(percent > 70){
                      epicCompleteMap.put(epicName,percent);
                      
                  }
              }
          }   
        }

         System.Debug('epicCompleteMap ' + epicCompleteMap);  

         return epicCompleteMap.keySet();
   
    }
    
   
    private Static String getAverageThreeSprintVelocity(){
    
    
        Date startDate = currentSprint.agf__Start_Date__c.addDays(-(SPRINT_TOTAL_DAYS * 3));
        
        Integer totalCompletedPoints = 0;
        
        List<agf__ADM_Sprint__c> sprintList = [SELECT id,
          agf__Completed_Story_Points__c
          FROM agf__ADM_Sprint__c 
          WHERE (agf__Start_Date__c <:startDate OR agf__Start_Date__c < TODAY)
          AND  agf__End_Date__c < TODAY
          AND  agf__Scrum_Team__c = :teamId
          ORDER BY agf__End_Date__c desc LIMIT 3];
        
        if(sprintList.size() > 0 && sprintList != null ){
            for(agf__ADM_Sprint__c sprint : sprintList){
            
                if(sprint.agf__Completed_Story_Points__c != null){
                  totalCompletedPoints +=  (Integer) sprint.agf__Completed_Story_Points__c;
                }
            }
        
        }
        
        String averageVelocity = String.ValueOf(totalCompletedPoints / sprintList.size());
        return averageVelocity;
    
    }
       
    
    public static agf__ADM_Sprint__c getCurrentSprint(){     
    
      agf__ADM_Sprint__c thisSprint = [SELECT id,
        name,
        agf__End_Date__c,
        agf__Start_Date__c
        FROM agf__ADM_Sprint__c 
        WHERE agf__Start_Date__c <= TODAY 
        AND agf__Scrum_Team__c = :teamId
        AND agf__End_Date__c >= LAST_N_DAYS:14
        AND agf__Days_Remaining__c NOT IN ('Closed', 'Not Started')];
      
      return thisSprint;
    
    
    }
    
    global class TeamHealthWrapper{
        public String themeName {get;set;}
        public String teamPoints {get;set;}
        public String epicTotal {get;set;}
        public String epicNames {get;set;}
        
    
    }

}