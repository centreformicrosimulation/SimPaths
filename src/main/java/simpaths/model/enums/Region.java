package simpaths.model.enums;

public enum Region implements IntegerValuedEnum {
	
	//Uses NUTS Level 1 system for each country
	
	//Italy					//Name										//EUROMOD drgn1 value
	ITC("Nord Ovest", 1),	//Nord Ovest								1
	ITF("Sud", 4),			//Sud										4
	ITG("Isole", 5),		//Isole										5
	ITH("Nord Est", 2),		//Nord Est (formerly ITD)					2
	ITI("Centro", 3),		//Centro (formerly ITE)						3
	//See https://www.euromod.ac.uk/sites/default/files/country-reports/year6/Y7_CR_IT_Final.pdf page 41 for definition EUROMOD variable drgn2 which corresponds to NUTS level 2, from which can be imputed the NUTS level 1 and therefore meaning of drgn1 values. 
	
	//UK									//Name							//EUROMOD drgn1 value
	UKC("North East", 1),					//North East					//1
	UKD("North West", 2),					//North West					//2
	UKE("Yorkshire and the Humber", 4),	//Yorkshire and the Humber		//4		//Note, there is no 3 in the definition!
	UKF("East Midlands", 5),				//East Midlands					//5
	UKG("West Midlands", 6),				//West Midlands					//6
	UKH("East of England", 7),				//East of England				//7
	UKI("London", 8),						//London						//8
	UKJ("South East", 9),					//South East					//9
	UKK("South West", 10),					//South West					//10
	UKL("Wales", 11),						//Wales							//11
	UKM("Scotland", 12),					//Scotland						//12
	UKN("Northern Ireland", 13);			//Northern Ireland				//13?
//	UKmissing,		//Included by Ambra in estimates where people lacked regional information.  For the input we use in our simulations, everyone has a region, so there is no need to include the UKmissing region here (in fact, if we did, we would have to check for UKmissing when looping through regions so as not to include it in the method to prevent null pointer exceptions). 
//	See https://www.iser.essex.ac.uk/files/euromod/country-reports/CR_UK2006-11_final_13-12-11.pdf, table on pg 59 for EUROMOD variable drgn1 definition for UK:
		
	
//	//Uses NUTS Level 1 codes for each country
//	ES1,				//Spain
//	ES2,
//	ES3,
//	ES4,
//	ES5,
//	ES6,
//	ES7,
//	GR1,				//Greece
//	GR2,
//	GR3,
//	GR4,
//	HU1,				//Hungary
//	HU2,
//	HU3,
//	IE0,				//Ireland
//	SE1,				//Sweden
//	SE2,
//	SE3,

	
	private final String name;
	private final int value;

    Region(String name, int drgn1EUROMODvariable)
    {
        this.name = name;
        this.value = drgn1EUROMODvariable;
    }

    public String getName() {
        return name;
    }

	@Override
    public int getValue()
    {
        return value;
    }
}
