package custom_package;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.core.SimEvent;
//import utils.Constants;
//import utils.DatacenterCreator;
//import utils.GenerateMatrices;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;


public class FCFSDatacenterBroker extends DatacenterBroker {

    public FCFSDatacenterBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void processCloudletReturn(SimEvent ev) {
        Cloudlet cloudlet = (Cloudlet) ev.getData();
        getCloudletReceivedList().add(cloudlet);
        Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
                + " received");
        cloudletsSubmitted--;
        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) {
            Log.printLine(getName() + ": All Cloudlets executed.");
            clearDatacenters();
            finishExecution();
        }
    }

    @Override
    protected void submitCloudlets() {
        int vmIndex = 0;
        for (Cloudlet cloudlet : getCloudletList()) {
            Vm vm = getVmList().get(vmIndex);
            bindCloudletToVm(cloudlet.getCloudletId(), vm.getId());

            vmIndex = (vmIndex + 1) % getVmList().size(); // round-robin
        }

        super.submitCloudlets();
    }
}
