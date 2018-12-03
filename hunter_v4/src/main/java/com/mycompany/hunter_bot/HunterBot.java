package com.mycompany.hunter_bot;

//import com.fuzzylite.*;
import com.fuzzylite.Engine;
import com.fuzzylite.FuzzyLite;
import com.fuzzylite.activation.General;
import com.fuzzylite.defuzzifier.Centroid;
import com.fuzzylite.imex.FldExporter;
import com.fuzzylite.imex.FllImporter;
import com.fuzzylite.norm.s.Maximum;
import com.fuzzylite.norm.t.AlgebraicProduct;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Ramp;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;
import com.fuzzylite.activation.*;
import com.fuzzylite.defuzzifier.*;
import com.fuzzylite.factory.*;
import com.fuzzylite.hedge.*;
import com.fuzzylite.imex.*;
import com.fuzzylite.norm.*;
import com.fuzzylite.norm.s.*;
import com.fuzzylite.norm.t.*;
import com.fuzzylite.rule.*;
import com.fuzzylite.term.*;
import com.fuzzylite.variable.*;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import cz.cuni.amis.introspection.java.JProp;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.NavigationState;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004PathAutoFixer;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Rotate;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.StopShooting;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerKilled;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Example of Simple Pogamut bot, that randomly walks around the map searching
 * for presas shooting at everything that is in its way.
 *

 */
@AgentScoped
public class HunterBot extends UT2004BotModuleController<UT2004Bot> {

    /**
     * boolean switch to activate engage behavior
     */
    @JProp
    public boolean shouldEngage = true;
    /**
     * boolean switch to activate pursue behavior
     */
    @JProp
    public boolean shouldPursue = true;
    /**
     * boolean switch to activate rearm behavior
     */
    @JProp
    public boolean shouldRearm = true;
    /**
     * boolean switch to activate collect health behavior
     */
    @JProp
    public boolean shouldCollectHealth = true;
    /**
     * how low the health level should be to start collecting health items
     */
    @JProp
    public int healthLevel = 75;
    /**
     * how many bot the hunter killed other bots (i.e., bot has fragged them /
     * got point for killing somebody)
     */
    @JProp
    public int frags = 0;
    /**
     * how many times the hunter died
     */
    @JProp
    public int deaths = 0;

    // etat
    protected enum States {ENGAGE,HURT,SEARCH,DEAD};
   // protected state stateActuel = state.SEARCH;
    protected States state = States.SEARCH;
    // Initialisiation of Engines
   	static Engine engineAttaque = null;
    static Engine engineHurt = null;
    static Engine engineSearch= null;
    static Engine engineArme= null;
    //Initialisation of input and output variables for engines
   		 //for Attaque engine
    static InputVariable vieAttaque = null;
 	static InputVariable distanceAttaque =null;
    static OutputVariable reactionAttaque = null;
       //for Hurt engine
 	static InputVariable vieHurt = null;
 	static InputVariable distanceHurt =null;
   	static OutputVariable reactionHurt = null;
       //for search engine
    static InputVariable vieSearch = null;
    static InputVariable distanceSearch =null;
    static OutputVariable reactionSearch = null;
       //for weapon choice engine
    static InputVariable armeBot = null;
    static InputVariable distanceArme =null;
    static OutputVariable choixArme = null;
         
    /**
     * {@link PlayerKilled} listener that provides "frag" counting + is switches
     * the state of the hunter.
     *
     * @param event
     */
    @EventListener(eventClass = PlayerKilled.class)
    public void playerKilled(PlayerKilled event) {
        if (event.getKiller().equals(info.getId())) {
            ++frags;
        }
        if (enemy == null) {
            return;
        }
        if (enemy.getId().equals(event.getId())) {
            enemy = null;
        }
    }
    /**
     * Used internally to maintain the information about the bot we're currently
     * hunting, i.e., should be firing at.
     */
    protected Player enemy = null;
    /**
     * Item we're running for. 
     */
    protected Item item = null;
    /**
     * Taboo list of items that are forbidden for some time.
     */
    protected TabooSet<Item> tabooItems = null;
    
    private UT2004PathAutoFixer autoFixer;
    
	private static int instanceCount = 0;

    /**
     * Bot's preparation - called before the bot is connected to GB2004 and
     * launched into UT2004.
     */
    @Override
    public void prepareBot(UT2004Bot bot) {
        log.info("je suis dans prpare bot");
       
      
        tabooItems = new TabooSet<Item>(bot);

        autoFixer = new UT2004PathAutoFixer(bot, navigation.getPathExecutor(), fwMap, aStar, navBuilder); // auto-removes wrong navigation links between navpoints

        // listeners        
        navigation.getState().addListener(new FlagListener<NavigationState>() {

            @Override
            public void flagChanged(NavigationState changedValue) {
                switch (changedValue) {
                    case PATH_COMPUTATION_FAILED:
                    case STUCK:
                        if (item != null) {
                            tabooItems.add(item, 10);
                        }
                        reset();
                        break;

                    case TARGET_REACHED:
                        reset();
                        break;
                }
            }
        });

        // DEFINE WEAPON PREFERENCES
        weaponPrefs.addGeneralPref(UT2004ItemType.LIGHTNING_GUN, true);                
        weaponPrefs.addGeneralPref(UT2004ItemType.SHOCK_RIFLE, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, true);        
        weaponPrefs.addGeneralPref(UT2004ItemType.ROCKET_LAUNCHER, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.ASSAULT_RIFLE, true);        
        weaponPrefs.addGeneralPref(UT2004ItemType.BIO_RIFLE, true);
        
        
    }

    /**
     * Here we can modify initializing command for our bot.
     *
     * @return
     */
    @Override
    public Initialize getInitializeCommand() {
        // just set the name of the bot and his skill level, 1 is the lowest, 7 is the highest
    	// skill level affects how well will the bot aim
        return new Initialize().setName("HunterNew-" + (++instanceCount)).setDesiredSkill(2);
    }

    /**
     * Resets the state of the Hunter.
     */
    protected void reset() {
    	item = null;
        enemy = null;
        navigation.stopNavigation();
        itemsToRunAround = null;
    }
    
    @EventListener(eventClass=PlayerDamaged.class)
    public void playerDamaged(PlayerDamaged event) {
    	//log.info("I have just hurt other bot for: " + event.getDamageType() + "[" + event.getDamage() + "]");
    }
    
    @EventListener(eventClass=BotDamaged.class)
    public void botDamaged(BotDamaged event) {
    	//log.info("I have just been hurt by other bot for: " + event.getDamageType() + "[" + event.getDamage() + "]");
    }

    /**
     * Main method that controls the bot - makes decisions what to do next. It
     * is called iteratively by Pogamut engine every time a synchronous batch
     * from the environment is received. This is usually 4 times per second - it
     * is affected by visionTime variable, that can be adjusted in GameBots ini
     * file in UT2004/System folder.
     *
     * @throws cz.cuni.amis.pogamut.base.exceptions.PogamutException
     */
    @Override
    public void logic() { 
        // arrete de tirer pour rien
        if (info.isShooting() || info.isSecondaryShooting()) {
            getAct().act(new StopShooting());
        }
        // recherche d'ennemi dans le champ de vision
        if (enemy == null || !enemy.isVisible()) {
            // pick new enemy
            enemy = players.getNearestVisiblePlayer(players.getVisibleEnemies().values());
        }
   		//    stateMachine(stateActuel);
        stateMachine(state);
        if(state == States.ENGAGE)
            stateAttack();
        else if(state == States.HURT)
            stateHurt();
        else if(state == States.SEARCH)
            stateSearch();
       }
 	   protected void stateMachine(States s){
        switch(s)
        {
            case ENGAGE : 
                    //  si tu vois encore l'ennemi -> Attack
                    /*if(shouldEngage && players.canSeeEnemies()&& weaponry.hasLoadedWeapon()) 
                        { 
                            state=States.ENGAGE;
                        //    stateAttack();
                        }
                    else{
                        if(senses.isBeingDamaged()) {
                             state=States.HURT;
                        }
                           // stateHurt();
                        else {
                            state=States.SEARCH;
                      //   stateSearch();
                        }           
                }*/
                if(enemy!=null) 
                {
                    if (enemy.getFiring() != 0) // si l'enemi nous tire dessus
                            state = States.ENGAGE;  //on riposte
                    else
                        reactionAttaqueLogic();
                }
                else 
                    state=States.SEARCH;
            break;
            case HURT :
                if(enemy!=null) 
                {
                    if (enemy.getFiring() != 0) // si l'enemi nous tire dessus
                            state = States.ENGAGE;  //on riposte
                    else
                        reactionHurtLogic();
                }
                else
                    state=States.SEARCH;
            break;
            case SEARCH :
                if(enemy!=null) 
                {
                    if (enemy.getFiring() != 0) // si l'enemi nous tire dessus
                            state = States.ENGAGE;  //on riposte
                    else
                        reactionSearchLogic();
                }
                else
                    state=States.SEARCH;
            break;
                
          case DEAD :
                state=States.SEARCH;
              break;
          default :
              state=States.ENGAGE;
              break;
        }
        
    }

    //////////////////
    // STATE ENGAGE //
    //////////////////
    protected boolean runningToPlayer = false;

    /**
     * Fired when bot see any enemy. <ol> <li> if enemy that was attacked last
     * time is not visible than choose new enemy <li> if enemy is reachable and the bot is far - run to him
     * <li> otherwise - stand still (kind a silly, right? :-)
     * </ol>
     */
    protected void stateAttack() {
        log.info("\n Decision is: ENGAGE");
        //config.setName("Hunter [ENGAGE]");
        bot.getBotName().setInfo("ATTAQUE");
        boolean shooting = false;
        double distance = Double.MAX_VALUE;
        //waitCount = 0;
        
   		// tu arretes de tirer s'il n'y a pas d'ennemi
        if (!enemy.isVisible()) {
	        if (info.isShooting() || info.isSecondaryShooting()) {
                // stop shooting
                getAct().act(new StopShooting());
            }
                
            runningToPlayer = false;
        // si il y a un ennemi, tu tires
        } else {
            
	        distance = info.getLocation().getDistance(enemy.getLocation());
	        if (shoot.shoot(weaponPrefs, enemy) != null) {
	            //log.info("Shooting at enemy!!!");
	            shooting = true;
                choixArme();
	        }
        }

        // tu vas vers l'ennemi
        int decentDistance = Math.round(random.nextFloat() * 800) + 200;
        if (!enemy.isVisible() || !shooting || decentDistance < distance) {
            if (!runningToPlayer) {
                navigation.navigate(enemy);
                runningToPlayer = true;
            }
        // si tu l'as perdu, tu t'arretes
        } else {
            runningToPlayer = false;
            navigation.stopNavigation();
        }
        
        item = null;
    }

    ///////////////
    // STATE HURT //
    ///////////////
    protected void stateHurt() {

        log.info("\n Decision is: HURT");
        bot.getBotName().setInfo("HURT");
        //getAct().act(new Rotate().setAmount(32000));
        Item item = items.getPathNearestSpawnedItem(ItemType.Category.HEALTH);
        if (item == null) {
        	//log.warning("NO HEALTH ITEM TO RUN TO => ITEMS");
        	stateSearch();
        } else {
        	bot.getBotName().setInfo("URGENCE");
        	navigation.navigate(item);
        	this.item = item;
        }
        /*if (navigation.isNavigating()) {
        	navigation.stopNavigation();
        	item = null;
        }*/
        //
    }
    

    ////////////////
    // STATE Wait //
    ////////////////
    /**
     * State pursue is for pursuing enemy who was for example lost behind a
     * corner. How it works?: <ol> <li> initialize properties <li> obtain path
     * to the enemy <li> follow the path - if it reaches the end - set lastEnemy
     * to null - bot would have seen him before or lost him once for all </ol>
     */
    /*protected int waitCount = 0;
    protected void stateWait() {
        log.info("Decision is: PURSUE");
        ++waitCount;
        if (waitCount > 30) {
            reset();
        }
        if (enemy != null) {
        	bot.getBotName().setInfo("PURSUE");
        	navigation.navigate(enemy);
        	item = null;
        } else {
        	reset();
        }
        
    }*/

    //////////////////
    // STATE SEARCH //
    //////////////////
    protected List<Item> itemsToRunAround = null;

    protected void stateSearch() {
        log.info("\n Decision is: SEARCH");

        //config.setName("Hunter [ITEMS]");
        if (navigation.isNavigatingToItem()) return;
        
        List<Item> interesting = new ArrayList<Item>();
        
        // ADD WEAPONS
        for (ItemType itemType : ItemType.Category.WEAPON.getTypes()) {
        	if (!weaponry.hasLoadedWeapon(itemType)) interesting.addAll(items.getSpawnedItems(itemType).values());
        }
        // ADD ARMORS
        for (ItemType itemType : ItemType.Category.ARMOR.getTypes()) {
        	interesting.addAll(items.getSpawnedItems(itemType).values());
        }
        // ADD QUADS
        interesting.addAll(items.getSpawnedItems(UT2004ItemType.U_DAMAGE_PACK).values());
        // ADD HEALTHS
        if (info.getHealth() < 100) {
        	interesting.addAll(items.getSpawnedItems(UT2004ItemType.HEALTH_PACK).values());
        }
        
        Item item = MyCollections.getRandom(tabooItems.filter(interesting));
        if (item == null) {
        	//log.warning("NO ITEM TO RUN FOR!");
        	if (navigation.isNavigating()) return;
        	bot.getBotName().setInfo("SEARCH");
        	navigation.navigate(navPoints.getRandomNavPoint());
        } else {
        	this.item = item;
        	//log.info("RUNNING FOR: " + item.getType().getName());
        	bot.getBotName().setInfo("ITEM: " + item.getType().getName() + "");
        	navigation.navigate(item);        	
        }        
    }

    ////////////////
    // STATE DEAD //
    ////////////////
    
   @Override
    public void botKilled(BotKilled event) {
        log.info("\n Decision is: KILLED" );
        state = state.DEAD;
    	reset();
    }
   
    public void reactionAttaqueLogic() {
    
        log.info("\n reaction engage");
        log.info("boooooooot: "+info.getHealth());
        double distance= info.getLocation().getDistance(enemy.getLocation());
        log.info("distance boot" + distance);
 		vieAttaque.setValue(info.getHealth()/100);
 		distanceAttaque.setValue(info.getLocation().getDistance(enemy.getLocation())/1000);
 		engineAttaque.process();
 		double reactionValue= reactionAttaque.getValue();
        log.info("reactionvalue"+reactionValue);

 		if(reactionValue>0.5){
 			  state=States.ENGAGE;
 		}
 		else {
			  state=States.HURT;
 		}
    
    }
    public void reactionHurtLogic() {
    
        log.info("\n reaction hurt");  
        log.info("boooooooot: "+info.getHealth());
        double distance= info.getLocation().getDistance(enemy.getLocation());
        log.info("distance boot" + distance);
 		vieHurt.setValue(info.getHealth()/100);
 		distanceHurt.setValue(info.getLocation().getDistance(enemy.getLocation())/1000);
 		engineHurt.process();
 		double reactionValue= reactionHurt.getValue();
        log.info("reactionvalue"+reactionValue);

 		if(reactionValue>0.5){
 			  state=States.ENGAGE;
 		}
 		else {
			  state=States.HURT;
 		}

    }
    public void reactionSearchLogic() {
        log.info("\n reaction search"); 
   
        double distance= info.getLocation().getDistance(enemy.getLocation());
        log.info("boooooooot: "+info.getHealth());
        vieSearch.setValue(info.getHealth()/100);
        log.info("distance boot" + distance);
 	
 		distanceSearch.setValue(info.getLocation().getDistance(enemy.getLocation())/1000);
 		engineSearch.process();
 		double reactionValue= reactionSearch.getValue();
        log.info("reactionvalue"+reactionValue);
 		
 		if(reactionValue>0.5){
 			  state=States.ENGAGE;
 		}
 		else {
			  state=States.HURT;
 		}
        }
         
         
    public void choixArme() {
        log.info("\n choix arme"); 
   
        double distance= info.getLocation().getDistance(enemy.getLocation());
        log.info("distance boot" + distance);
 		distanceArme.setValue(info.getLocation().getDistance(enemy.getLocation())/1000);
        if((info.getCurrentWeaponName() == "LIGHTNING_GUN" )||(info.getCurrentWeaponName() == "SHOCK_RIFLE" )||(info.getCurrentWeaponName() == "ROCKET_LAUNCHER" ))
            armeBot.setValue(0.6); //arme longue
        else 
            armeBot.setValue(0.2);
        
 		engineArme.process();
 		double reactionValue= choixArme.getValue();
        log.info("reactionvalue"+reactionValue);
        //si l'arme n'est pas adaptée à la distance, le bot change l'arme courante par une arme adéquate à la distance
	 	if(reactionValue<0.5){
	 		weaponry.changeWeapon(weaponPrefs.getWeaponPreference(distance).getWeapon()); 
	        log.info("j'ai changé mon arme");
	 	}
 	
        
     }
    
    public static void initAttaqueLogic()
    {
        engineAttaque = new Engine();
        engineAttaque.setName("engineAttaque");
        engineAttaque.setDescription("");
        
        vieAttaque = new InputVariable();
        vieAttaque.setName("vieAttaque");
        vieAttaque.setDescription("");
        vieAttaque.setEnabled(true);
        vieAttaque.setRange(0.000, 1.000);
        vieAttaque.setLockValueInRange(false);
        //Définition des fonctions d'appartenance
        //On choisit 2 variables linguistiques faible et forte

        vieAttaque.addTerm(new Ramp("faible", 0.400, 0.200));
        vieAttaque.addTerm(new Ramp("forte", 0.200, 0.400));
        engineAttaque.addInputVariable(vieAttaque);
        
        distanceAttaque = new InputVariable();
        distanceAttaque.setName("distanceAttaque");
        distanceAttaque.setDescription("");
        distanceAttaque.setEnabled(true);
        distanceAttaque.setRange(0.000, 1.000);
        distanceAttaque.setLockValueInRange(false);
        //Définition des fonctions d'appartenance
        //On choisit 2 variables linguistiques proche et loin
        distanceAttaque.addTerm(new Ramp("proche", 0.600, 0.400));
        distanceAttaque.addTerm(new Ramp("loin", 0.400, 0.600));
        engineAttaque.addInputVariable(distanceAttaque);
        
        
        reactionAttaque = new OutputVariable();
        reactionAttaque.setName("reactionAttaque");
        reactionAttaque.setDescription("");
        reactionAttaque.setEnabled(true);
        reactionAttaque.setRange(0.000, 1.000);
        reactionAttaque.setLockValueInRange(false);
        reactionAttaque.setAggregation(new Maximum());
        reactionAttaque.setDefuzzifier(new LargestOfMaximum());
        reactionAttaque.setDefaultValue(Double.NaN);
        reactionAttaque.setLockPreviousValue(false);
        //Définition des fonctions d'appartenance de la sortie
        //On choisit 2 variables linguistiques proche et loin

        reactionAttaque.addTerm(new Triangle("fuite", 0.000 , 0.100, 0.200));
        reactionAttaque.addTerm(new Triangle("attaque", 0.800, 0.900, 1.000));
        engineAttaque.addOutputVariable(reactionAttaque);
        
        RuleBlock mamdani = new RuleBlock();
        mamdani.setName("mamdani");
        mamdani.setDescription("");
        mamdani.setEnabled(true);
        mamdani.setConjunction(new Minimum());
        mamdani.setDisjunction(new Maximum());
        mamdani.setImplication(new Minimum());
        mamdani.setActivation(new General());
        mamdani.addRule(Rule.parse("if distanceAttaque is very proche then reactionAttaque is attaque", engineAttaque));
        mamdani.addRule(Rule.parse("if vieAttaque is faible and distanceAttaque is proche then reactionAttaque is attaque", engineAttaque));
        mamdani.addRule(Rule.parse("if vieAttaque is forte then reactionAttaque is attaque", engineAttaque));
        mamdani.addRule(Rule.parse("if vieAttaque is faible and distanceAttaque is loin then reactionAttaque is fuite", engineAttaque));
        engineAttaque.addRuleBlock(mamdani);


    }
    
    public static void initHurtLogic()
    {
        engineHurt = new Engine();
        engineHurt.setName("engineHurt");
        engineHurt.setDescription("");
        
        vieHurt = new InputVariable();
        vieHurt.setName("vieHurt");
        vieHurt.setDescription("");
        vieHurt.setEnabled(true);
        vieHurt.setRange(0.000, 1.000);
        vieHurt.setLockValueInRange(false);
        //Définition des fonctions d'appartenance
        //On choisit 2 variables linguistiques faible et forte
        vieHurt.addTerm(new Ramp("faible", 0.800, 0.600));
        vieHurt.addTerm(new Ramp("forte", 0.600, 0.800));
        engineHurt.addInputVariable(vieHurt);
        
        distanceHurt = new InputVariable();
        distanceHurt.setName("distanceHurt");
        distanceHurt.setDescription("");
        distanceHurt.setEnabled(true);
        distanceHurt.setRange(0.000, 1.000);
        distanceHurt.setLockValueInRange(false);
        //Définition des fonctions d'appartenance
        //On choisit 2 variables linguistiques proche et loin        
        distanceHurt.addTerm(new Ramp("proche", 0.600, 0.400));
        distanceHurt.addTerm(new Ramp("loin", 0.400, 0.600));
        engineHurt.addInputVariable(distanceHurt);
        
        
        
        reactionHurt = new OutputVariable();
        reactionHurt.setName("reactionHurt");
        reactionHurt.setDescription("");
        reactionHurt.setEnabled(true);
        reactionHurt.setRange(0.000, 1.000);
        reactionHurt.setLockValueInRange(false);
        reactionHurt.setAggregation(new Maximum());
        reactionHurt.setDefuzzifier(new LargestOfMaximum());
        reactionHurt.setDefaultValue(Double.NaN);
        reactionHurt.setLockPreviousValue(false);
        //Définition des fonctions d'appartenance
        //On choisit 2 variables linguistiques fuite et attaque
        reactionHurt.addTerm(new Triangle("fuite", 0.000 , 0.100, 0.200));
        reactionHurt.addTerm(new Triangle("attaque", 0.800, 0.900, 1.000));
        engineHurt.addOutputVariable(reactionHurt);
        
        RuleBlock mamdani = new RuleBlock();
        mamdani.setName("mamdani");
        mamdani.setDescription("");
        mamdani.setEnabled(true);
        mamdani.setConjunction(new Minimum());
        mamdani.setDisjunction(new Maximum());
        mamdani.setImplication(new Minimum());
        mamdani.setActivation(new General());
        mamdani.addRule(Rule.parse("if distanceHurt is very proche then reactionHurt is attaque", engineHurt));
        mamdani.addRule(Rule.parse("if vieHurt is faible and distanceHurt is proche then reactionHurt is attaque", engineHurt));
        mamdani.addRule(Rule.parse("if vieHurt is forte then reactionHurt is attaque", engineHurt));
        mamdani.addRule(Rule.parse("if vieHurt is faible and distanceHurt is loin then reactionHurt is fuite", engineHurt));
        engineHurt.addRuleBlock(mamdani);

    }
    
     public static void  initSearchLogic()
    {
        engineSearch = new Engine();
        engineSearch.setName("engineSearch");
        engineSearch.setDescription("");
        
        vieSearch = new InputVariable();
        vieSearch.setName("vieSearch");
        vieSearch.setDescription("");
        vieSearch.setEnabled(true);
        vieSearch.setRange(0.000, 1.000);
        vieSearch.setLockValueInRange(false);
        //Définition des fonctions d'appartenance
        //On choisit 2 variables linguistiques faible et forte
        vieSearch.addTerm(new Ramp("faible", 0.600, 0.400));
        vieSearch.addTerm(new Ramp("forte", 0.400, 0.600));
        engineSearch.addInputVariable(vieSearch);
        
        distanceSearch = new InputVariable();
        distanceSearch.setName("distanceSearch");
        distanceSearch.setDescription("");
        distanceSearch.setEnabled(true);
        distanceSearch.setRange(0.000, 1.000);
        distanceSearch.setLockValueInRange(false);
        //Définition des fonctions d'appartenance
        //On choisit 2 variables linguistiques proche et loin
        distanceSearch.addTerm(new Ramp("proche", 0.600, 0.400));
        distanceSearch.addTerm(new Ramp("loin", 0.400, 0.600));
        engineSearch.addInputVariable(distanceSearch);
        
        
        reactionSearch = new OutputVariable();
        reactionSearch.setName("reactionSearch");
        reactionSearch.setDescription("");
        reactionSearch.setEnabled(true);
        reactionSearch.setRange(0.000, 1.000);
        reactionSearch.setLockValueInRange(false);
        reactionSearch.setAggregation(new Maximum());
        reactionSearch.setDefuzzifier(new LargestOfMaximum());
        reactionSearch.setDefaultValue(Double.NaN);
        reactionSearch.setLockPreviousValue(false);
        //Définition des fonctions d'appartenance de sortie
        //On choisit 2 variables linguistiques fuite et attaque        
        reactionSearch.addTerm(new Triangle("fuite", 0.000 , 0.100, 0.200));
        reactionSearch.addTerm(new Triangle("attaque", 0.800, 0.900, 1.000));
        engineSearch.addOutputVariable(reactionSearch);
        
        RuleBlock mamdani = new RuleBlock();
        mamdani.setName("mamdani");
        mamdani.setDescription("");
        mamdani.setEnabled(true);
        mamdani.setConjunction(new Minimum());
        mamdani.setDisjunction(new Maximum());
        mamdani.setImplication(new Minimum());
        mamdani.setActivation(new General());
        mamdani.addRule(Rule.parse("if distanceSearch is very proche then reactionSearch is attaque", engineSearch));
        mamdani.addRule(Rule.parse("if vieSearch is faible and distanceSearch is proche then reactionSearch is attaque", engineSearch));
        mamdani.addRule(Rule.parse("if vieSearch is forte then reactionSearch is attaque", engineSearch));
        mamdani.addRule(Rule.parse("if vieSearch is faible and distanceSearch is loin then reactionSearch is fuite", engineSearch));
        engineSearch.addRuleBlock(mamdani);

    }
    
     public static void initChoixArmeLogic()
    {
        engineArme = new Engine();
        engineArme.setName("engineArme");
        engineArme.setDescription("");
          
        armeBot = new InputVariable(); // protée de notre arme
        armeBot.setName("armeBot");
        armeBot.setDescription("");
        armeBot.setEnabled(true);
        armeBot.setRange(0.000, 1.000);
        armeBot.setLockValueInRange(false);
        //Définition des fonctions d'appartenance
        //On choisit 2 variables linguistiques faible et longue
        armeBot.addTerm(new Ramp("faible", 0.500, 0.300)); 
        armeBot.addTerm(new Ramp("longue", 0.300, 0.500));
        engineArme.addInputVariable(armeBot);
        
        distanceArme = new InputVariable();
        distanceArme.setName("distanceArme");
        distanceArme.setDescription("");
        distanceArme.setEnabled(true);
        distanceArme.setRange(0.000, 1.000);
        distanceArme.setLockValueInRange(false);
        //Définition des fonctions d'appartenance
        //On choisit 2 variables linguistiques proche et loin
        distanceArme.addTerm(new Ramp("proche", 0.600, 0.400));
        distanceArme.addTerm(new Ramp("loin", 0.400, 0.600));
        engineArme.addInputVariable(distanceArme);
        
        
        choixArme = new OutputVariable();
        choixArme.setName("choixArme");
        choixArme.setDescription("");
        choixArme.setEnabled(true);
        choixArme.setRange(0.000, 1.000);
        choixArme.setLockValueInRange(false);
        choixArme.setAggregation(new Maximum());
        choixArme.setDefuzzifier(new LargestOfMaximum());
        choixArme.setDefaultValue(Double.NaN);
        choixArme.setLockPreviousValue(false);
        //Définition des fonctions d'appartenance
        //On choisit 2 variables linguistiques change (pour changer d'arme) et notChange
        choixArme.addTerm(new Triangle("change", 0.000 , 0.100, 0.200));
        choixArme.addTerm(new Triangle("NotChange", 0.800, 0.900, 1.000));
        engineArme.addOutputVariable(choixArme);
        
        RuleBlock mamdani = new RuleBlock();
        mamdani.setName("mamdani");
        mamdani.setDescription("");
        mamdani.setEnabled(true);
        mamdani.setConjunction(new Minimum());
        mamdani.setDisjunction(new Maximum());
        mamdani.setImplication(new Minimum());
        mamdani.setActivation(new General());
        mamdani.addRule(Rule.parse("if armeBot is faible and distanceArme is proche then choixArme is NotChange", engineArme));
        mamdani.addRule(Rule.parse("if armeBot is faible and distanceArme is loin then choixArme is change", engineArme));
        mamdani.addRule(Rule.parse("if armeBot is longue and distanceArme is proche then choixArme is change", engineArme));
        mamdani.addRule(Rule.parse("if armeBot is longue and distanceArme is loin then choixArme is NotChange", engineArme));
        engineArme.addRuleBlock(mamdani);
        
        
    }
    
   

    ///////////////////////////////////
    public static void main(String args[]) throws PogamutException{

	     initAttaqueLogic();
	     initHurtLogic();
	     initSearchLogic();
	     initChoixArmeLogic();
        // starts 3 Hunters at once
        // note that this is the most easy way to get a bunch of (the same) bots running at the same time        
    	new UT2004BotRunner(HunterBot.class, "Hunter").setMain(true).setLogLevel(Level.INFO).startAgents(2);
     
      
        
     }
}

