@RestResource(urlMapping='/api/ohanaVoice/createstory/*')
global with sharing class CreateGusStoryRestResource
{
    @HttpPost
    global static String doPost(String title, String rank, String theme, String team) 
    {

        //TODO:
        //Query Product Tag based on Team
        //Remove hardcoded Product Tag Reference

        System.debug('inside post method >> '+title + ' , ' + rank + ' , ' +theme);
        try {                 
            List<agf__ADM_Theme__c> themes = [Select id from agf__ADM_Theme__c where name = :theme limit 1];
            Decimal thisRank = Decimal.valueOf(rank);
            agf__ADM_Work__c story = new agf__ADM_Work__c(agf__Subject__c  = title, 
                                                          agf__Priority_Rank__c = thisRank,
                                                          agf__Theme__c = themes[0].id,                                                      
                                                          agf__Product_Tag__c = 'a0Z46000000geq9EAA',
                                                          agf__Type__c = 'User Story'
                                                         );
            insert story;
            return story.id;
            
        } catch (Exception e){
            System.debug('Create Story - Something went wrong, OOPS..');
            
            return '';
        } 
        return ''; 
    }
    
}