import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Weapon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Amaral D.
 */

public class ChoixArme<T> 
{

	protected Map<T, Double> arme;       
    
        protected double resultat = 0;           
	    private double recompense = 1.0; 
	    private double punition = 0.5; 
        
        int alpha = 75; // apprentissage
        int lambda = 10;   // remise
        int epsilon = 100; // exploration
              
        	
	public Renforcement_Armes(ArrayList<T> list_armes) // initialisation de la liste avec tous les armes dispos
	{
		init(list_armes);
	}
	
	protected void init(ArrayList<T> list_armes)
	{
		arme = new HashMap<T, Double>();
		
		for(T w : list_armes)
		{
			arme.put(w, 0.0); 	
		}              
                for(T weaponID : arme.keySet()) 
                {
                    System.out.println("Arme : " + weaponID + " -- Score " + arme.get(weaponID));
                }
	}
	
	protected T prendre_arme() // comparaison avec le taux d'exploration et choix de l'arme
	{
		if(resultat == 0 || Math.random()*100 < epsilon)
		{
			return (T)arme.keySet().toArray()[(Math.random()*arme.size())]; 
		}
		else
		{
			double dif_probs = 0; 
			double rand =  Math.random();
			
			for(T arme_ID : arme.keySet()) 
			{
				double arme_prob = arme.get(arme_ID) / resultat;
				if(dif_probs + arme_prob > rand)
				{
					return arme_ID;
				}				
				dif_probs += arme_prob;
			}
         }		
	}
    
    
    protected void effet_punition(T arme_ID) // Algo d'aprentissage, effets de la punition (bot fait des actions incorrectes)
	{		
            if(!arme.containsKey(arme_ID)){
                arme.put(arme_ID, 0.0);
            }
            else
            {
                double arme_neg = arme.get(arme_ID); // test de valeur de l'arme interne
                if(arme_neg == 0)
                    return;
                else if(arme_neg < punition)
                {
                        resultat -= arme_neg;
                        arme.put(arme_ID, 0.0);
                }
                else
                {
                       arme.put(arme_ID, arme.get(arme_ID) - punition);
                        resultat = resultat - punition; // decrementation de la punition specifique
                }
            }
	}


	protected void effet_recompense(T arme_ID) // Algo d'aprentissage, effets de la recompense (bot realise des bonnes actions)
	{
            if(!arme.containsKey(arme_ID))
            {
                arme.put(arme_ID, recompense);
            }
            else
            {
                arme.put(arme_ID, arme.get(arme_ID) + recompense);
                resultat = resultat + recompense; // incrementation de la recompense obtenue             

                if(epsilon  > 5){
                    epsilon--;
                }
                if(epsilon < 75){ // toujours en utilisant le taux d'exploration comme facteur de contrôle
                    epsilon++;
                }
                if(epsilon > 20){
                    epsilon--;
                }
                
            }
	}
	
    void arme_update(ArrayList<T> liste_arme) // Fonction pour garantir que l'arme a une valeur par défault
     {
        for(T w : liste_arme)
        {
            if(!arme.containsKey(w))
            {
                arme.put(w, 0.0);
            }
        }
    }
    
    public List<T> arme_triee() // Arme dejà prêt a choisir, alors le bot peut poursuivre l'enemie
    {
        ArrayList<T> armes_triee = new ArrayList<T>();
        boolean chercher = false;
        
        for(T w : arme.keySet())
        {                       
            for(int i=0; i < armes_triee.size() && !chercher; i++)
            {
                if(arme.get(w) > arme.get(armes_triee.get(i)))
                {
                    armes_triee.add(i, w);
                    chercher = true;
                }
            }
            if(!chercher)
            {
                armes_triee.add(w);
            }
            chercher = false;
        }
        return armes_triee;
    }
  
    /*
    public String toString(){
        String txt = "";
        
        for(T w : weaponsValue.keySet()){
            if(w != null){
                txt += w + " : " + weaponsValue.get(w) + "\n";
            }
        }
        
        return txt;
    }
	 // POSSIBLE RETOUR A CLASS PRINCIPAL
*/  
}
         