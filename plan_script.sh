sed -i 's/    @InjectMocks//g' src/test/java/com/sayai/record/fantasy/service/FantasyDraftServiceAutoPickTest.java
sed -i 's/    private FantasyDraftService fantasyDraftService;/    @InjectMocks\n    private FantasyDraftService fantasyDraftService;/g' src/test/java/com/sayai/record/fantasy/service/FantasyDraftServiceAutoPickTest.java
