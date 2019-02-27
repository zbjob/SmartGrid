import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

/**
 * Created by bo on 05.11.18.
 */
public class DistributedNodeQuery extends SingleQuery {


    public DistributedNodeQuery(String esperQuery, int NID, int houseNum, int timeslicenum, DistributedClusterHeaderQuery CHQ, int tSS) {
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
        runningFlag = true;
        AfterSignal = 0;
    }

    public DistributedNodeQuery(){
        ExeCnt = 0;
        SignalCnt = 0;
        runningFlag = true;

    }

    public long getExeCnt(){
        return ExeCnt;
    }

    public long getSignalCnt(){
        return SignalCnt;
    }

    public boolean getRunningFlag() {return runningFlag;}

    public void siwtchOff() {runningFlag = false;}

    public void siwtchOn() {runningFlag = true;}

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
        epService = EPServiceProviderManager.getProvider(esperEngineURL,
                configuration);
        //System.out.print("epSerive is null\n");
        //System.out.print(epService == null);

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
                if (newEvents == null ) {
                    // we don't care about events leaving the window (old
                    // events)
                    return;
                }

                if(runningFlag == false){
                    return;
                }
                //for (EventBean theEvent : newEvents) {
                //ClusterHeader.processData((DeviationEvent) theEvent.getUnderlying());
                EventBean theEvent = newEvents[0];
                int HID = (Integer) theEvent.get("ClusterID");
                double ACCload = (Double) theEvent.get("NodeAccLoad");
                MLEvent e = (MLEvent) theEvent.get("MLevent");
                int ts = e.timeStamp;

                int houseAccIdx = HID;
                houseAccLoad[houseAccIdx] += ACCload;

              //  if((ts)%timeSliceStep == 0) {
                    //double diff = (houseAccLoad[houseAccIdx] - expHouseAccLoad[HID + houseNum * timeSliceCnt[HID]]) / expHouseAccLoad[HID + houseNum * timeSliceCnt[HID]];
                    //double diff = houseAccLoad[houseAccIdx] / expHouseAccLoad[HID + houseNum * timeSliceCnt[HID]];
                    double diff =  expHouseAccLoad[HID + houseNum * timeSliceCnt[HID]] - houseAccLoad[houseAccIdx];

                    //if (diff > 0.00) {
                        //System.out.print("Diff Warning! \n");
                        //System.out.println(diff);
                        //System.out.println(ts);
                        //System.out.println(HID);

                        ClusterHeader.processData(new DeviationEvent(timeSliceCnt[HID], nodeID, HID,
                                houseAccLoad[houseAccIdx], diff));
                        ++SignalCnt;
                        if(ts>60)
                            ++AfterSignal;
                    //}

                    ++timeSliceCnt[HID];
              //  }


                //} //for
            }
        });
    }

    public long getAfterSignal() {return AfterSignal;}
    public long getAfterExeCnt() { return AfterExeCnt;}

    //@Override
    public void processData(MLEvent e) {
        //System.out.print(epService==null);
        if(runningFlag == false){
            return;
        }
        epService.getEPRuntime().sendEvent(e);
        ++ExeCnt;
        if(e.timeStamp > 60)
            ++AfterExeCnt;
    }

    int      houseNum;
    int      timeSliceNum;
    int[]    timeSliceCnt = null;
    int      nodeID;
    int      timeSliceStep;
    double[] houseAccLoad = null;
    double   NodeAccLoad;
    double   expNodeAccLoad;
    double[] expHouseAccLoad = null;
    long     ExeCnt;
    long     SignalCnt;
    boolean  runningFlag;

    long     AfterExeCnt = 0;

    long     AfterSignal = 0;

    DistributedClusterHeaderQuery ClusterHeader = null;
}
