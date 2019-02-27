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

public class NodeQuery extends SingleQuery {


    public NodeQuery(String esperQuery, int NID, int houseNum, int timeslicenum, ClusterHeaderQuery CHQ, int tSS) {
        this.esperQuery = esperQuery;
        this.houseNum = houseNum;
        timeSliceNum  = timeslicenum;
        houseAccLoad = new double[houseNum];
        expHouseAccLoad = new double[houseNum*timeslicenum];
        timeSliceCnt = new int[houseNum];
        nodeID = NID;
        NodeAccLoad = 0;
        expNodeAccLoad = 0;
        //timeSliceCnt = 0;
        ClusterHeader = CHQ;
        timeSliceStep = tSS;
        ExeCnt = 0;
        SignalCnt = 0;
    }

    public long getExeCnt(){
        return ExeCnt;
    }

    public long getSignalCnt(){
        return SignalCnt;
    }

    @Override
    public void setUp() {
		/*
		 * Init data structures
		 */
        Configuration configuration = new Configuration();
        configuration.getEngineDefaults().getThreading()
                .setInternalTimerEnabled(false);

        defaultKey = "MLEvent";
        log.info("Registering data items as '{}' in esper queries...", defaultKey);
        configuration.addEventType(defaultKey, MLEvent.class);

        // The data types for the data items
        /*
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
        */

		/*
		 * Get the ESPER engine instance
		 */
        epService = EPServiceProviderManager.getProvider("NodeCEP",
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
                //for (EventBean theEvent : newEvents) {
                    //ClusterHeader.processData((DeviationEvent) theEvent.getUnderlying());
                    EventBean theEvent = newEvents[0];
                    int HID = (Integer) theEvent.get("HouseID");
                    double ACCload = (Double) theEvent.get("NodeAccLoad");
                    MLEvent e = (MLEvent) theEvent.get("MLevent");
                    int ts = e.timeStamp;

                    int houseAccIdx = HID;
                    houseAccLoad[houseAccIdx] += ACCload;

                    if((ts+1)%timeSliceStep == 0) {
                        //double diff = (houseAccLoad[houseAccIdx] - expHouseAccLoad[HID + houseNum * timeSliceCnt[HID]]) / expHouseAccLoad[HID + houseNum * timeSliceCnt[HID]];
                        //double diff = houseAccLoad[houseAccIdx] / expHouseAccLoad[HID + houseNum * timeSliceCnt[HID]];
                        double diff =  expHouseAccLoad[HID + houseNum * timeSliceCnt[HID]] - houseAccLoad[houseAccIdx];

                        if (diff > 0.00) {
                            //System.out.print("Diff Warning! \n");
                            //System.out.println(diff);
                            //System.out.println(ts);
                            //System.out.println(HID);

                            ClusterHeader.processData(new DeviationEvent(timeSliceCnt[HID], nodeID, HID,
                                    houseAccLoad[houseAccIdx], diff));
                            ++SignalCnt;
                        }

                        ++timeSliceCnt[HID];
                    }


                //} //for
            }
        });
    }

    //@Override
    public void processData(MLEvent e) {
        //System.out.print(epService==null);
        epService.getEPRuntime().sendEvent(e);
        ++ExeCnt;
    }

    int      houseNum;
    int      timeSliceNum;
    int[]    timeSliceCnt;
    int      nodeID;
    int      timeSliceStep;
    double[] houseAccLoad;
    double   NodeAccLoad;
    double   expNodeAccLoad;
    double[] expHouseAccLoad;
    long     ExeCnt;
    long     SignalCnt;

    ClusterHeaderQuery ClusterHeader = null;

}
