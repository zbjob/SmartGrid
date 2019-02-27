/**
 * Created by bo on 05.11.18.
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

public class DistributedClusterHeaderQuery extends SingleQuery {
    public DistributedClusterHeaderQuery(String Query, int nodenumber, int tCnt, int DRbeg) {
        esperQuery = Query;
        nodeNum = nodenumber;
        timeSliceCnt = tCnt;
        ExpClusterAccLoad = new double[tCnt];
        nodeAccLoad = new double[nodenumber];
        HouseIDs = new int[nodenumber];
        DRStartTime = DRbeg;
        CHAccLoad = 0;
        ExeCnt = 0;
        AfterExeCnt = 0;
    }

    public DistributedClusterHeaderQuery(){}

    public void setNodesQuery(DistributedNodeQuery DNQ) {NodesQ=DNQ;}
    public void switchOffClusterMonitoring(){NodesQ.siwtchOff();}

    @Override
    public void setUp() {
        Configuration configuration = new Configuration();
        configuration.getEngineDefaults().getThreading()
                .setInternalTimerEnabled(false);

        defaultKey = "DeviationEvent";
        log.info("Registering data items as '{}' in esper queries...", defaultKey);
        configuration.addEventType(defaultKey, DeviationEvent.class);

        epService = EPServiceProviderManager.getProvider(esperEngineURL,
                configuration);

        log.info("Creating ESPER query...");

        statement = epService.getEPAdministrator().createEPL(this.esperQuery);

        statement.addListener(new UpdateListener() {
            @Override
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                //System.out.print("int CH update \n");
                if (newEvents == null) {
                    // we don't care about events leaving the window (old
                    // events)
                    System.out.print("In ClusterHeaderQuery, none output event. \n");
                    return;
                }

                for (EventBean theEvent : newEvents) {
                    //ClusterHeader.processData((DeviationEvent) theEvent.getUnderlying());
                    //System.out.print("In ClusterHeaderQuery, 1 output event \n");
                    //System.out.print((DeviationEvent) theEvent.getUnderlying());

                    //System.out.print("Warning! Two consecutive sub-intervals failed  to meet expected DR ");

                    double Clusterload = (Double) theEvent.get("Clusterload");
                    int Idx=(Integer) theEvent.get("tID");
                    System.out.print("tID = ");
                    System.out.println(Idx);
                    System.out.println(Clusterload - ExpClusterAccLoad[Idx]);

                    if( Idx > 60 && 0.7*ExpClusterAccLoad[Idx]-Clusterload > 0){
                        NodesQ.siwtchOff();
                        //System.out.print("!!!!!!!! \n");
                        //System.out.print("switch off \n");
                    }

                    //System.out.print(theEvent.getUnderlying());
                    //System.out.print("\n");
                    //String str = theEvent.getUnderlying().toString();
                    //str = str.replaceAll("\\D+",",");
                   // System.out.print(str);
                    //System.out.print("\n");
                    //System.out.print("\n");


                    //System.out.print("In ClusterHeaderQuery, 2 output event \n");
                    //System.out.print(e==null);
                    // System.out.print("\n");



                   // Integer NID = (Integer) theEvent.get("nodeID");
                   // Integer HID = (Integer) theEvent.get("houseID");
                   // Integer aSID = (Integer) theEvent.get("aSliceID");
                   // Integer count = (Integer) theEvent.get("CntIDs");
                    //System.out.println(count);

                    //Integer bSID = (Integer) theEvent.get("bSliceID");



                   // // System.out.print(NID);
                   // System.out.print(NID == null);
                   // System.out.print(NID);
                   // System.out.print(HID);
                   // System.out.print(aSID);
                    //System.out.print(bSID);

                   // if ( count < 240) {
                   //     String output = "WARNING! " +
                    //            " Node: " + NID.toString() +
                    //            " House: " + HID.toString() +
                    //            " in sub duration: " + aSID.toString() +
                     //           "count : " + count.toString() +
                   // //            //" and " + bSID.toString() +
                    //            " failed to satisfy DR !";


                     //   System.out.print(output);
                        //System.out.println(count);
                    //    System.out.print("\n");
                    //    System.out.print("\n");
                    //}
                }
            }
        });
    }

    //@Override
    public void processData(DeviationEvent e) {
        //System.out.print("In ClusterHeaderQuery, processData \n");
        //System.out.print(e);
        //System.out.print("\n");

        epService.getEPRuntime().sendEvent(e);
        ++ExeCnt;
        if(e.timeSliceID>60)
            ++AfterExeCnt;
    }

    public long getAfterExeCnt() {return AfterExeCnt;}

    public long getExeCnt(){
        return ExeCnt;
    }

    int         nodeNum;
    int         ClusterID;
    int         timeSliceNum;
    int         timeSliceCnt;
    int         DRStartTime;
    int[]       HouseIDs;


    double[]    nodeAccLoad;
    double[]    ExpClusterAccLoad;
    double      CHAccLoad;

    long        ExeCnt;
    long        AfterExeCnt;
    DistributedNodeQuery NodesQ = null;

}
