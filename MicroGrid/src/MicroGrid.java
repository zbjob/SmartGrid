import java.io.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bo on 07.03.18.
 */
public class MicroGrid {
    public static void main(String[] args){

        String nodeQueryString1 = "Select last(*) as MLevent, houseID as HouseID, sum(load) as NodeAccLoad " +
                             //"from MLEvent#groupwin(houseID)#length_batch(1) group by houseID";
                             "from MLEvent#groupwin(houseID)#keepall group by houseID";



       // String nodeQueryString1 = "Select last(*) as MLevent, houseID as HouseID, sum(load) as NodeAccLoad " +
       //         "from MLEvent group by houseID";


       // String CHQueryString1 = "Select a.houseID as HouseID, a.nodeID as NodeID, a.sliceId as aSliceID, b.sliceId as bSliceId from " +
       //         "pattern [ every (a=DeviationEvent -> b=DeviationEvent) while (a.houseID=b.houseID and a.nodeID=b.nodeID)]";
        String CHQueryString1 = "Select a.houseID as houseID, a.nodeID as nodeID, a.timeSliceID as aSliceID, b.timeSliceID as bSliceId, a.diff as aDiff, b.diff as bDiff from " +
                 "pattern [ every a=DeviationEvent ->  b=DeviationEvent(houseID=a.houseID and nodeID=a.nodeID)]";

        String UtilityString = "Select distinct houseID as HouseIDs, timeSliceID as tID,  diff as Diff, count(distinct houseID) as CntDRSuccessful from DeviationEvent group by timeSliceID";


        String DistributedCHQueryString1 = "Select distinct nodeID, sum(load) as Clusterload, timeSliceID as tID from DeviationEvent group by timeSliceID";
        String DistributednodeQueryString1 = "Select last(*) as MLevent, nodeID as ClusterID, sum(load) as NodeAccLoad " +
                "from MLEvent#groupwin(houseID)#length_batch(1) group by nodeID";


//        String UtilityString = "Select distinct a.houseID as HouseIDs, count(distinct a.houseID) as CntIDs from " +
//                "DeviationEvent";
               // String UtilityString = "Select distinct a.houseID as HouseID from " +
               // "pattern [ every a=DeviationEvent]";
                //"pattern [ every a=DeviationEvent ->  b=DeviationEvent]";
        //String CHQueryString1 = "Select * from " +
        //        "pattern [ every a=DeviationEvent ->  b=DeviationEvent(houseID=a.houseID and nodeID=a.nodeID)]";

       // String CHQueryString1 = "Select a, b from " +
        //        "pattern [ every a=DeviationEvent -> every b=DeviationEvent(houseID=a.houseID and nodeID=a.nodeID)]";


        int nodeNum = 1;
        int nodeID = 634;

        //int houseNum = 32;
        int houseNum = 654;

        int timeSlice = 120;
        int DRDuration = 120;
        int DRStartTime = 17*60;

        int timeSliceStep = DRDuration/timeSlice;

        ClusterHeaderQuery Utility = new ClusterHeaderQuery(UtilityString, nodeNum);
        //ClusterHeaderQuery ClusterHeader=null;
        NodeQuery Node634 = new NodeQuery(nodeQueryString1, nodeID, houseNum, timeSlice,  Utility, timeSliceStep);

        Utility.setUp();
        Node634.setUp();
        //String inputFile = "/home/bo/Smart_Grid/P634_allHouses_0.csv";
        //String inputFile4monitoring = "/home/bo/Smart_Grid/P634_allHouses_50.csv";

        String inputFile = "/home/bo/Smart_Grid/Data/GMpaper/DR_baseline_1.csv";
        String inputFile4monitoring = "/home/bo/Smart_Grid/Data/GMpaper/DR_data_1.csv";



     //***********************************************************
        // Centralised monitoring

        //compute expected load
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputFile), "UTF8"))) {

            // skip header details in first line
            //reader.readLine();

            //int houseCnt = 0;

            String line = null;

            for(int i=0; i<DRStartTime; ++i)
                reader.readLine();

            //int timeTicker = 1;
            int timeSliceCnt = 0;
            double [] AccLoad = new double[houseNum];

            //for(int i = 0; i<=DRDuration && (line = reader.readLine()) != null; ++i) {
            for(int i = DRStartTime; i< DRDuration+DRStartTime && (line = reader.readLine()) != null; ++i) {

                String[] loadValues =  line.split(",");
                for(int houseCnt = 0; houseCnt < houseNum; ++houseCnt) {
                    AccLoad[houseCnt] += Double.parseDouble(loadValues[houseCnt]);
                }

                //++timeTicker;

                //if(timeTicker == timeSliceStep){
                if((i+1)%timeSliceStep == 0){
                    //timeTicker = 1;
                    for(int houseCnt =0; houseCnt<houseNum; ++houseCnt) {
                        Node634.expHouseAccLoad[houseCnt + houseNum * timeSliceCnt] = AccLoad[houseCnt];
                    }
                    ++timeSliceCnt;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        //perform monitoring
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputFile4monitoring), "UTF8"))) {

            // skip header details in first line
            //reader.readLine();

            //int houseCnt = 0;

            String line = null;

            for(int i=0; i<DRStartTime; ++i)
                reader.readLine();

            int timeTicker = 1;
            int timeSliceCnt = 0;
            double [] AccLoad = new double[houseNum];

            for(int ts=DRStartTime; ts< DRStartTime+DRDuration && (line = reader.readLine()) != null; ++ts) {

                String[] loadValues =  line.split(",");
                for(int houseCnt = 0; houseCnt < houseNum; ++houseCnt) {
                    double load = Double.parseDouble(loadValues[houseCnt]);
                    Node634.processData(new MLEvent(ts, nodeID, houseCnt, load));
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(Node634.getExeCnt());
        System.out.println(Node634.getSignalCnt());
        System.out.println(Utility.getExeCnt());


        //***********************************************************
        // Distributed monitoring
        // sequential  grouping to 11 clusters
        int numCluster = 21;
        DistributedNodeQuery[] SMNodes = new DistributedNodeQuery[numCluster];
        DistributedClusterHeaderQuery[] CHNodes = new DistributedClusterHeaderQuery[numCluster];

        int iCluster = houseNum/(numCluster-1);
        int lCluster = houseNum % numCluster;
        for(int i = 0; i < numCluster-1; ++i) {
            CHNodes[i] = new DistributedClusterHeaderQuery(DistributedCHQueryString1,iCluster,timeSlice,DRStartTime);
            SMNodes[i] = new DistributedNodeQuery(DistributednodeQueryString1,i,iCluster,timeSlice,CHNodes[i],timeSliceStep);
            CHNodes[i].setNodesQuery(SMNodes[i]);

            CHNodes[i].setUp();
            SMNodes[i].setUp();

            System.out.print(CHNodes[i].toString());
            System.out.print("\n");
        }

        CHNodes[numCluster-1] = new DistributedClusterHeaderQuery(DistributedCHQueryString1,lCluster,timeSlice,DRStartTime);
        SMNodes[numCluster-1] = new DistributedNodeQuery(nodeQueryString1,numCluster-1,lCluster,timeSlice,CHNodes[numCluster-1],timeSliceStep);
        CHNodes[numCluster-1].setNodesQuery(SMNodes[numCluster-1]);

        CHNodes[numCluster-1].setUp();
        SMNodes[numCluster-1].setUp();

        //load the baseline estimation
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputFile), "UTF8"))) {

            // skip header details in first line
            //reader.readLine();

            //int houseCnt = 0;

            String line = null;

            for(int i=0; i<DRStartTime; ++i)
                reader.readLine();

            //int timeTicker = 1;
            int timeSliceCnt = 0;
            double [] AccLoad = new double[houseNum];

            //for(int i = 0; i<=DRDuration && (line = reader.readLine()) != null; ++i) {
            for(int i = DRStartTime; i< DRDuration+DRStartTime && (line = reader.readLine()) != null; ++i) {

                String[] loadValues =  line.split(",");
                for(int houseCnt = 0; houseCnt < houseNum; ++houseCnt) {
                    AccLoad[houseCnt] += Double.parseDouble(loadValues[houseCnt]);
                }




                //++timeTicker;

                //if(timeTicker == timeSliceStep){
                if((i+1)%timeSliceStep == 0){
                    //timeTicker = 1;

                   /* for(int iterC =0; iterC<numCluster-1; ++iterC){
                        for(int j = 0; j< iCluster; ++j)
                        CHNodes[iterC].ExpClusterAccLoad[timeSliceCnt] += AccLoad[j+iterC*iCluster];
                    }

                    for(int iterC=0; iterC<lCluster; ++iterC){
                        CHNodes[numCluster-1].ExpClusterAccLoad[timeSlice] += AccLoad[numCluster*iCluster+iterC];
                    }
                    */

                    for(int iterC = 0; iterC<numCluster-1; ++iterC){
                        int clusterHouseIdxBeg = iterC*iCluster;
                        for(int j = 0; j < SMNodes[iterC].houseNum; ++j) {
                            CHNodes[iterC].ExpClusterAccLoad[timeSliceCnt] += AccLoad[j + clusterHouseIdxBeg];
                            //++clusterHouseIdxBeg;
                        }

                        /*System.out.print("iterC: ");
                        System.out.println(iterC);
                        System.out.print("cluserhouseIdxbeg: ");
                        System.out.println(clusterHouseIdxBeg);
                        System.out.print("cluserhouseIdxEnd: ");
                        System.out.println(clusterHouseIdxBeg+SMNodes[iterC].houseNum-1);
                        */


                    }



                    //for(int houseCnt =0; houseCnt<houseNum; ++houseCnt) {
                    //    Node634.expHouseAccLoad[houseCnt + houseNum * timeSliceCnt] = AccLoad[houseCnt];
                    //}
                    ++timeSliceCnt;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //perform monitoring
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputFile4monitoring), "UTF8"))) {

            // skip header details in first line
            //reader.readLine();

            //int houseCnt = 0;

            String line = null;

            for(int i=0; i<DRStartTime; ++i)
                reader.readLine();

            int timeTicker = 1;
            int timeSliceCnt = 0;
            double [] AccLoad = new double[houseNum];

            for(int ts=DRStartTime; ts< DRStartTime+DRDuration && (line = reader.readLine()) != null; ++ts) {

                String[] loadValues =  line.split(",");
                for(int houseCnt = 0; houseCnt < houseNum; ++houseCnt) {
                    double load = Double.parseDouble(loadValues[houseCnt]);
                    int clusterIdx = houseCnt/(iCluster+1);
                    //System.out.println(clusterIdx);
                    //System.out.print("\n");
                    //System.out.print(SMNodes[clusterIdx] == null);
                    SMNodes[clusterIdx].processData(new MLEvent(ts, clusterIdx, houseCnt, load));
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


        long DisSMExeCnt = 0;
        long DisSMSignal = 0;
        long DisSMAfterExeCnt = 0;
        long DisSMAfterSignal = 0;
        long DisCHExeCnt = 0;
        long AfterExeCnt = 0;

        long maxDisSMECnt = 0;
        long maxSMSignal = 0;
        long maxSMAfterSignal = 0;
        long maxCHExeCnt = 0;
        long maxCHAfterExeCnt = 0;

        long MaxDisSMAfterExeCnt = 0;

        for(int i=0; i < numCluster; ++i){
            DisCHExeCnt += CHNodes[i].getExeCnt();
            DisSMExeCnt += SMNodes[i].getExeCnt();
            DisSMSignal += SMNodes[i].getSignalCnt();
            AfterExeCnt += CHNodes[i].getAfterExeCnt();
            DisSMAfterSignal += SMNodes[i].getAfterSignal();
            DisSMAfterExeCnt += SMNodes[i].getAfterExeCnt();

            if(SMNodes[i].getExeCnt() > maxDisSMECnt)
                maxDisSMECnt = SMNodes[i].getExeCnt();

            if(SMNodes[i].getSignalCnt() > maxSMSignal)
                maxSMSignal = SMNodes[i].getSignalCnt();

            if(CHNodes[i].getExeCnt() > maxCHExeCnt)
                maxCHExeCnt = CHNodes[i].getExeCnt();

            if(CHNodes[i].getAfterExeCnt() > maxCHAfterExeCnt)
                maxCHAfterExeCnt = CHNodes[i].getAfterExeCnt();

            if(SMNodes[i].getAfterSignal() > maxSMAfterSignal)
                maxSMAfterSignal = SMNodes[i].getAfterSignal();

            if(SMNodes[i].getAfterExeCnt() > MaxDisSMAfterExeCnt)
                MaxDisSMAfterExeCnt = SMNodes[i].getAfterExeCnt();

            System.out.println(SMNodes[i].getRunningFlag());
        }

        System.out.println(DisSMExeCnt);
        System.out.println(DisSMSignal);
        System.out.println(DisCHExeCnt);
        System.out.println(DisSMAfterExeCnt);
        System.out.println(DisSMAfterSignal);
        System.out.println(AfterExeCnt);

        System.out.print("\n");

        System.out.println(maxDisSMECnt);
        System.out.println(maxSMSignal);
        System.out.println(maxCHExeCnt);
        System.out.println(maxSMAfterSignal);
        System.out.println(MaxDisSMAfterExeCnt);
        System.out.println(maxCHAfterExeCnt);






    }
}
