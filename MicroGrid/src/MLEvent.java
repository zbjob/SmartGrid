/**
 * Created by bo on 06.03.18.
 */
import java.lang.*;
public class MLEvent extends Event{
    int     timeStamp;
    int     nodeID;
    int     houseID;
    double  load;

    public MLEvent(int ts, int NID, int HID, double l) {
        timeStamp = ts;
        nodeID = NID;
        houseID = HID;
        load = l;
    }

    public int getHouseID() {
        return houseID;
    }

    public int getNodeID() {
        return nodeID;
    }

    public double getLoad() {
        return load;
    }

    public int getTimeStamp() {
        return timeStamp;
    }


    @Override
    public String toString() {
        return "Time: " + Integer.toString(timeStamp)
                + ". NodeID: " + Integer.toString(nodeID)
                + ". HouseID: " + Integer.toString(houseID)
                + ". Measured Load: " + Double.toString(load);
    }
}
