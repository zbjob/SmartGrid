/**
 * Created by bo on 07.03.18.
 */

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


public abstract class SingleQuery {

    final static Logger log = LoggerFactory.getLogger(EsperSingleQuery.class);

    // This map contains a list of key,class mappings, which will be
    // registered to the esper engine
    // Map<String, Object> types = new LinkedHashMap<String, Object>();

    /*
     * URL of the ESPER engine instancegit s
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

    String defaultKey = null;


    public abstract void setUp();
    //public abstract void processData(Event e);
}
