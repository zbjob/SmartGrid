//package uk.ac.ic.doc.lsds.sanding;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;


public class EsperSingleQuery {

	final static Logger log = LoggerFactory.getLogger(EsperSingleQuery.class);

	// This map contains a list of key,class mappings, which will be
	// registered to the esper engine
	Map<String, Object> types = new LinkedHashMap<String, Object>();

	/*
	 * URL of the ESPER engine instance
	 */
	String esperEngineURL = "";

	/*
	 * The ESPER engine instance used by this processor, will be fetched based
	 * on the given URL
	 */
	EPServiceProvider epService = null;

	/*
	 * The actual ESPER query as String
	 */
	String esperQuery = "";

	/*
	 * The query as a statement, built from the query string
	 */
	EPStatement statement = null;

	protected String defaultKey = "input";

	
	public EsperSingleQuery(String esperQuery, Map<String, Object> types) {
		this.esperQuery = esperQuery;
		this.types = types;
	}
	
	public void setUp() {
		/*
		 * Init data structures
		 */
		Configuration configuration = new Configuration();
		configuration.getEngineDefaults().getThreading()
				.setInternalTimerEnabled(false);

		// The data types for the data items
		//
		if (types != null) {
			log.info("Registering data items as '{}' in esper queries...", defaultKey);
			for (String key : types.keySet()) {
				Class<?> clazz = (Class<?>) types.get(key);
				log.info("  * registering type '{}' for key '{}'",
						clazz.getName(), key);
			}
			configuration.addEventType(defaultKey, types);
			log.info("{} attributes registered.", types.size());
		}
		
		/*
		 * Get the ESPER engine instance
		 */
		epService = EPServiceProviderManager.getProvider(esperEngineURL,
				configuration);

		log.info("Creating ESPER query...");
		
		/*
		 * Build the ESPER statement
		 */
		statement = epService.getEPAdministrator().createEPL(this.esperQuery);

		/*
		 * Set a listener called when statement matches
		 */
		statement.addListener(new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				if (newEvents == null) {
					// we don't care about events leaving the window (old
					// events)
					return;
				}
				for (EventBean theEvent : newEvents) {
					//SandingRunner.sendOutput(theEvent);
				}
			}
		});
	}
	
	public void processData(Map<String, Object> item) {
		epService.getEPRuntime().sendEvent(item, this.defaultKey);		
	}


}