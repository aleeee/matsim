package playground.southafrica.population.census2011;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.population.census2011.containers.Gender2011;
import playground.southafrica.population.census2011.containers.HousingType2011;
import playground.southafrica.population.census2011.containers.Income2011;
import playground.southafrica.population.census2011.containers.MainDwellingType2011;
import playground.southafrica.population.census2011.containers.PopulationGroup2011;
import playground.southafrica.population.census2011.containers.Relationship2011;
import playground.southafrica.population.census2011.containers.School2011;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

import com.vividsolutions.jts.geom.Point;

/**
 * Class to build a MATSim population from the sampled output resulting from
 * the Multi-Level Iterative Proportional Fitting (ml_ipf) population synthesis.
 * 
 * @author jwjoubert
 */
public class PopulationBuilder2011 {
	private final static Logger LOG = Logger.getLogger(PopulationBuilder2011.class);

	public static void main(String[] args) {
		Header.printHeader(PopulationBuilder2011.class.toString(), args);
		
		String folder = args[0];
		String shapefile = args[1];
		int idField = Integer.parseInt(args[2]);
		
		/* Read subplace shapefile. */
		MyMultiFeatureReader mmfr = new MyMultiFeatureReader();
		try {
			mmfr.readMultizoneShapefile(shapefile, idField);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read subplace shapefile from " + shapefile);
		}
		List<MyZone> allZones = mmfr.getAllZones();
		
		/* Get all the subplace files with synthetic populations. */
		List<File> files = FileUtils.sampleFiles(new File(folder), Integer.MAX_VALUE, FileUtils.getFileFilter("_persons.dat"));
		LOG.info("Total number of subplaces to run: " + files.size());
		
		/* Check that for each subplace in synthesized populatiuon, there is a
		 * shapefile. TODO: Can possibly remove this in the end, I guess. */
		LOG.info("Checking that each subplace population has a shapefile associated with it...");
		for(File file: files){
			/* Get subplace Id. */
			Id zoneId = new IdImpl(file.getName().substring(0, file.getName().indexOf("_")));
			MyZone thisZone = mmfr.getZone(zoneId);
		}
		LOG.info("Done.");
		
		/* Set up the population infrastructure. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory pf = sc.getPopulation().getFactory();
		ObjectAttributes personAttributes = sc.getPopulation().getPersonAttributes();
		Households households = new HouseholdsImpl();
		HouseholdsFactory hhf = households.getFactory();
		ObjectAttributes householdAttributes = households.getHouseholdAttributes();
		
		/* Now generate the individuals and their households for each of the 
		 * subplaces for which a population has been synthesized. */
		LOG.info("Building population per subplace...");
		Counter counter = new Counter("   subplaces # ");
		int personId = 0;
		int householdId = 0;
		for(File file : files){
			/* Parse the zone from the filename. */
			Id zoneId = new IdImpl(file.getName().substring(0, file.getName().indexOf("_")));
			MyZone thisZone = mmfr.getZone(zoneId);
			
			BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
			try{
				String line = br.readLine(); /* Header */
				
				/* Since the synthetic population was generated through 
				 * iterative proportional fitting, households are repeated, and
				 * we can hence not merely use the household id from the file
				 * as unique identifier for the household. Consequently, and
				 * assuming the members of the household are grouped together,
				 * we check for consecutive records' household Ids.
				 * FIXME There remains the possibility, albeit small, that the
				 * same household may have been repeated in the IPF stage. That
				 * will not be picked up here! (JWJ, June 2014) */
				Id currentHouseholdId = null;
				String previousHhid = null;
				
				while((line=br.readLine()) != null){
					String[] sa = line.split(" ");

					/* Parse the details from the synthesized population. */
					String hhid = sa[0];
					int housingType = Integer.parseInt(sa[3]);
					int mainDwellingType = Integer.parseInt(sa[4]);
					int population = Integer.parseInt(sa[5]);
					int householdIncome = Integer.parseInt(sa[6]);
					int age = Integer.parseInt(sa[8]); 
					int gender = Integer.parseInt(sa[9]);
					int relation = Integer.parseInt(sa[10]);
					int employment = Integer.parseInt(sa[11]);
					int school = Integer.parseInt(sa[12]);
					
					/* Establish the household if it doesn't exist yet. */
					Household household = null;
					if(previousHhid == null || !hhid.equalsIgnoreCase(previousHhid) ){
						household = hhf.createHousehold(new IdImpl(householdId++));
						household.setIncome(Income2011.getIncome(Income2011.getIncome2011(householdIncome)));
						
						/* Give it a home coordinate. Currently this does not 
						 * take facilities and their building types into account. */
						Point homePoint = thisZone.sampleRandomInteriorPoint();
						Coord homeCoord = new CoordImpl(homePoint.getX(), homePoint.getY());
						householdAttributes.putAttribute(household.getId().toString(), "homeCoord", homeCoord);
						
						/* Add the household attributes. */
						householdAttributes.putAttribute(household.getId().toString(), "housingType", HousingType2011.getHousingType(housingType).toString() );
						householdAttributes.putAttribute(household.getId().toString(), "mainDwellingType", MainDwellingType2011.getMainDwellingType(mainDwellingType).toString());
						
						/* Add the household to the container of households. */
						households.getHouseholds().put(household.getId(), household);
						
						currentHouseholdId = household.getId();
						previousHhid = hhid;
					} else{
						household = households.getHouseholds().get(currentHouseholdId);
					}
					
					/* Create the person. */
					Person person = pf.createPerson(new IdImpl(personId++));
					
					/* Set the hardcoded attributes. This will, however, be 
					 * duplicated in the person attributes to be more 
					 * consistent with the direction of future MATSim work. */
					((PersonImpl)person).setAge(age);
					((PersonImpl)person).setSex(Gender2011.getMatsimGender(Gender2011.getGender(gender)));
					((PersonImpl)person).setEmployed(Boolean.parseBoolean(String.valueOf(employment)));
					
					/* Set person attributes. */
					personAttributes.putAttribute(person.getId().toString(), "population", PopulationGroup2011.getType(population).toString());
					personAttributes.putAttribute(person.getId().toString(), "age", age);
					personAttributes.putAttribute(person.getId().toString(), "gender", Gender2011.getGender(gender).toString());
					personAttributes.putAttribute(person.getId().toString(), "relationship", Relationship2011.getRelationship(relation).toString());
					personAttributes.putAttribute(person.getId().toString(), "school", School2011.getSchool(school).toString());
					
					/* Create a plan that only contains the home activity. */
					Plan plan = pf.createPlan();
					Activity home = pf.createActivityFromCoord("home", (Coord) householdAttributes.getAttribute(household.getId().toString(), "homeCoord"));
					plan.addActivity(home);
					person.addPlan(plan);
					
					/* Link the person to its household. */
					household.getMemberIds().add(person.getId());
					personAttributes.putAttribute(person.getId().toString(), "householdId", household.getId().toString());
					
					/* Finally, add the person to the population. */
					sc.getPopulation().addPerson(person);
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot read from " + file.getAbsolutePath());
			} finally{
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close " + file.getAbsolutePath());
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done building population.");		
		
		/* Write all to file. */
		LOG.info("Writing the comprehensive population to file...");
		PopulationWriter pw = new PopulationWriter(sc.getPopulation());
		pw.write(folder + (folder.endsWith("/") ? "" : "/") + "population.xml.gz");
		ObjectAttributesXmlWriter personWriter = new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes());
		personWriter.writeFile(folder + (folder.endsWith("/") ? "" : "/") + "populationAttributes.xml.gz");
		HouseholdsWriterV10 hw = new HouseholdsWriterV10(households);
		hw.writeFile(folder + (folder.endsWith("/") ? "" : "/") + "households.xml.gz");
		ObjectAttributesXmlWriter householdsAttributeWriter = new ObjectAttributesXmlWriter(householdAttributes);
		householdsAttributeWriter.putAttributeConverter(CoordImpl.class, new CoordConverter());
		householdsAttributeWriter.writeFile(folder + (folder.endsWith("/") ? "" : "/") + "householdAttributes.xml.gz");

		Header.printFooter();
	}
	
	
	
	

}
