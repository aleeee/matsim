package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordImpl;

// Gruss! balmi
//System.out.println("  reading facilities xml file... ");
//Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
//new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
//System.out.println("  done.");
//
//Iterator<IdI> facid_it =  facilities.getFacilities().keySet().iterator();
//int id = Integer.MIN_VALUE;
//while (facid_it.hasNext()) {
//	int currid = Integer.parseInt(facid_it.next().toString());
//	if (currid > id) { id = currid; }
//}
//id++;
//Facility a_fac = facilities.createFacility(new Id(id),new Coord(0.0,0.0));
//Activity act = a_fac.createActivity("shop");
//act.setCapacity(123);
//act.createOpentime("wkday","08:00:00","18:00:00");
//
//Retailer r = Retailers.getSingleton().createRetailer(new Id(1),123);
//r.setFacility(a_fac,new Double(123.4));


public class Retailers {

	private static Retailers singleton = new Retailers();
	private String name = null; // probably unnecessary, or include it into schema
	private String desc = null;
	private final TreeMap<Id, Retailer> Retailers = new TreeMap<Id, Retailer>();
	private final ArrayList<RetailersAlgorithm> algorithms = new ArrayList<RetailersAlgorithm>();
	

	private Retailers() {
	}

	public static final Retailers getSingleton() {
		return singleton;
	}

	public final void addAlgorithm(final RetailersAlgorithm algo) {
		this.algorithms.add(algo);
	}

	public final void runAlgorithms() {
		for (int i = 0; i < this.algorithms.size(); i++) {
			RetailersAlgorithm algo = this.algorithms.get(i);
			algo.run(this);
		}
	}

	public final void clearAlgorithms() {
		this.algorithms.clear();
	}

	/**
	 * @param id the id of the retailer
	 * @param cust_spm the numbers of costumers to be served
	 * @return the created Retailer object, or null if it could not be created (maybe because it already exists)
	 */
	public final Retailer createRetailer(final Id id, final int cust_sqm ) {
		// check id string for uniqueness
		if (this.Retailers.containsKey(id)) {
			return null;
		}
		Retailer c = new Retailer(id, cust_sqm);
		this.Retailers.put(id, c);
		return c;
	}

	public final void setName(final String name) {
		this.name = name;
	}

	public final void setDescription(final String desc) {
		this.desc = desc;
	}

	public ArrayList<RetailersAlgorithm> getAlgorithms() {
		return this.algorithms;
	}

	public final String getName() {
		return this.name;
	}

	public final String getDescription() {
		return this.desc;
	}

	public final TreeMap<Id, Retailer> getRetailers() {
		return this.Retailers;
	}

	public final Retailer getRetailer(final Id locId) {
		return this.Retailers.get(locId);
	}

	//needed for testing
	public static final void reset() {
		singleton = new Retailers();
	}

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" +
				"[nof_Retailers=" + this.Retailers.size() + "]" +
				"[nof_algorithms=" + this.algorithms.size() + "]";
	}

}
