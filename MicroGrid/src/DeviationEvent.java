/**
 * Created by bo on 07.03.18.
 */
public class DeviationEvent extends Event {
    int     timeSliceID;
    int  nodeID;
    int  houseID;
    double  load;
    double  diff;

    public int getTimeSliceID() {
        return timeSliceID;
    }

    public int getNodeID() {
        return nodeID;
    }

    public int getHouseID() {
        return houseID;
    }

    public double getLoad() {
        return load;
    }

    public double getDiff() {
        return diff;
    }

    public DeviationEvent(int tsID, int NID, int HID, double l, double d) {
        timeSliceID = tsID;
        nodeID = NID;
        houseID = HID;
        load = l;
        diff = d;
    }

    @Override
    public String toString() {
        return "Time Slice: " + Integer.toString(timeSliceID)
                + ". NodeID: " + Integer.toString(nodeID)
                + ". HouseID: " + Integer.toString(houseID)
                + ". Measured Accumulated Load: " + Double.toString(load)
                + ". Deviation: " + Double.toString(diff);
    }

}
