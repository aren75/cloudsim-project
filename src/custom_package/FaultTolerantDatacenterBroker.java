package custom_package;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.*;

public class FaultTolerantDatacenterBroker extends DatacenterBroker {

    private final Set<Integer> failedVmIds = new HashSet<>();
    private final Random rand = new Random();

    public FaultTolerantDatacenterBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void submitCloudlets() {
        for (Cloudlet cloudlet : getCloudletList()) {
            int vmId = getRandomHealthyVmId();
            if (vmId == -1) {
                Log.printLine("No healthy VMs available!");
                continue;
            }
            bindCloudletToVm(cloudlet.getCloudletId(), vmId);
        }
        super.submitCloudlets();
    }

    private int getRandomHealthyVmId() {
        List<Vm> healthyVms = new ArrayList<>();
        for (Vm vm : getVmList()) {
            if (!failedVmIds.contains(vm.getId())) {
                healthyVms.add(vm);
            }
        }
        if (healthyVms.isEmpty()) return -1;
        return healthyVms.get(rand.nextInt(healthyVms.size())).getId();
    }

    @Override
    protected void processCloudletReturn(SimEvent ev) {
        Cloudlet cloudlet = (Cloudlet) ev.getData();

        // Simulate a 20% chance of VM failure
        if (rand.nextDouble() < 0.2) {
            int failedVmId = cloudlet.getVmId();
            failedVmIds.add(failedVmId);

            Log.printLine("⚠️  Simulating failure of VM #" + failedVmId + " — rescheduling Cloudlet #" + cloudlet.getCloudletId());

            // Resubmit the cloudlet to a new healthy VM
            cloudlet.setVmId(-1);  // clear old VM assignment
            getCloudletList().add(cloudlet);  // re-add to the broker's queue
            submitCloudlets();  // resubmit

        } else {
            getCloudletReceivedList().add(cloudlet);
            Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId() + " completed successfully.");
        }

        cloudletsSubmitted--;
        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) {
            Log.printLine(getName() + ": All Cloudlets handled.");
            clearDatacenters();
            finishExecution();
        }
    }
}
