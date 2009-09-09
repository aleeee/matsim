/* *********************************************************************** *
 * project: org.matsim.*
 * KtiPtRouteTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.meisterk.kti.router;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.world.Location;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

import playground.meisterk.kti.config.KtiConfigGroup;

public class KtiPtRouteTest extends MatsimTestCase {

	private PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = null;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		Config config = super.loadConfig(null);
		KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
		ktiConfigGroup.setUsePlansCalcRouteKti(true);
		ktiConfigGroup.setPtHaltestellenFilename(this.getClassInputDirectory() + "haltestellen.txt");
		ktiConfigGroup.setPtTraveltimeMatrixFilename(this.getClassInputDirectory() + "pt_Matrix.mtx");
		ktiConfigGroup.setWorldInputFilename(this.getClassInputDirectory() + "world.xml");
		config.addModule(KtiConfigGroup.GROUP_NAME, ktiConfigGroup);
		
		NetworkLayer dummyNetwork = new NetworkLayer();
		dummyNetwork.createNode(new IdImpl("1000"), new CoordImpl(900.0, 900.0));
		dummyNetwork.createNode(new IdImpl("1001"), new CoordImpl(1300.0, 1300.0));
		
		this.plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo();
		this.plansCalcRouteKtiInfo.prepare(ktiConfigGroup, dummyNetwork);

	}
	
	@Override
	protected void tearDown() throws Exception {
		this.plansCalcRouteKtiInfo = null;
		super.tearDown();
	}

	public void testInitializationLinks() {
		
		Link link1 = new FakeLink(new IdImpl(1));
		Link link2 = new FakeLink(new IdImpl(2));
		KtiPtRoute testee = new KtiPtRoute(link1, link2, this.plansCalcRouteKtiInfo);
		assertEquals(link1, testee.getStartLink());
		assertEquals(link2, testee.getEndLink());
		assertEquals(null, testee.getFromStop());
		assertEquals(null, testee.getFromMunicipality());
		assertEquals(null, testee.getToMunicipality());
		assertEquals(null, testee.getToStop());
		
	}
	
	public void testFullInitialization() {
		
		Link link1 = new FakeLink(new IdImpl(1));
		Link link2 = new FakeLink(new IdImpl(2));
		SwissHaltestelle fromStop = new SwissHaltestelle(new IdImpl("123"), new CoordImpl(1000.0, 1000.0));
		SwissHaltestelle toStop = new SwissHaltestelle(new IdImpl("456"), new CoordImpl(2000.0, 2000.0));
		
		World world = new World();
		ZoneLayer municipalities = (ZoneLayer) world.createLayer(new IdImpl("municipality"), "municipalities");
		
		Coord dummyMuniCoord = new CoordImpl(1001.0, 1001.0);
		Location fromMunicipality = new Zone(municipalities, new IdImpl("30000"), dummyMuniCoord, dummyMuniCoord, dummyMuniCoord, 0.0, "Ixwil");
		dummyMuniCoord.setXY(2002.0, 2002.0);
		Location toMunicipality = new Zone(municipalities, new IdImpl("30001"), dummyMuniCoord, dummyMuniCoord, dummyMuniCoord, 0.0, "Ypslikon");
		
		KtiPtRoute testee = new KtiPtRoute(link1, link2, this.plansCalcRouteKtiInfo, fromStop, fromMunicipality, toMunicipality, toStop);
		assertEquals(link1, testee.getStartLink());
		assertEquals(link2, testee.getEndLink());
		assertEquals(fromStop, testee.getFromStop());
		assertEquals(toStop, testee.getToStop());
		assertEquals(fromMunicipality, testee.getFromMunicipality());
		assertEquals(toMunicipality, testee.getToMunicipality());
		
	}
	
	public void testRouteDescription() {
		
		String expectedRouteDescription = "321=40000=40001=654";
		KtiPtRoute testee = new KtiPtRoute(null, null, this.plansCalcRouteKtiInfo);
		testee.setRouteDescription(null, expectedRouteDescription, null);
		assertEquals(new IdImpl("321"), testee.getFromStop().getId());
		assertEquals(new IdImpl("654"), testee.getToStop().getId());
		assertEquals(new IdImpl("40000"), testee.getFromMunicipality().getId());
		assertEquals(new IdImpl("40001"), testee.getToMunicipality().getId());
		assertEquals(expectedRouteDescription, testee.getRouteDescription());
		
	}
	
}
