package playground.mzilske.pt.queuesim;

import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;

public interface UmlaufStueckI {

	TransitLine getLine();

	TransitRoute getRoute();

	Departure getDeparture();

	boolean isFahrt();

	NetworkRouteWRefs getCarRoute();

}
