package custom_package;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadBalancingDatacenterBroker extends DatacenterBroker {

    public LoadBalancingDatacenterBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void submitCloudlets() {
        // Map to track estimated load on each VM
        Map<Integer, Double> vmLoad = new HashMap<>();
        
        // Initialize load for all VMs
        for (Vm vm : getVmsCreatedList()) {
            vmLoad.put(vm.getId(), 0.0);
        }
        
        // If some VMs failed to create, handle gracefully
        if (vmLoad.isEmpty()) {
            System.out.println("No VMs were created successfully. Cannot schedule cloudlets.");
            return;
        }
        
        // Sort cloudlets by length (longest first to better balance)
        List<Cloudlet> sortedCloudlets = new ArrayList<>(getCloudletList());
        sortedCloudlets.sort((c1, c2) -> Long.compare(c2.getCloudletLength(), c1.getCloudletLength()));
        
        for (Cloudlet cloudlet : sortedCloudlets) {
            // Get VM with lowest estimated workload
            int selectedVmId = getLeastLoadedVm(vmLoad);
            
            if (selectedVmId != -1) {
                bindCloudletToVm(cloudlet.getCloudletId(), selectedVmId);
                
                // Find the actual VM object by ID
                Vm selectedVm = null;
                for (Vm vm : getVmsCreatedList()) {
                    if (vm.getId() == selectedVmId) {
                        selectedVm = vm;
                        break;
                    }
                }
                
                if (selectedVm != null) {
                    // Estimate load based on cloudlet length and VM processing power (MIPS)
                    double estimatedProcessingTime = cloudlet.getCloudletLength() / 
                            (selectedVm.getMips() * selectedVm.getNumberOfPes());
                    
                    // Update VM's estimated load
                    vmLoad.put(selectedVmId, vmLoad.get(selectedVmId) + estimatedProcessingTime);
                    
                    System.out.println("Cloudlet " + cloudlet.getCloudletId() + 
                            " assigned to VM " + selectedVmId + 
                            " (current load: " + vmLoad.get(selectedVmId) + ")");
                } else {
                    System.out.println("Error: Could not find VM with ID " + selectedVmId);
                }
            } else {
                System.out.println("No suitable VM found for Cloudlet " + cloudlet.getCloudletId());
            }
        }
        
        // Print estimated load distribution after scheduling
        System.out.println("\nEstimated load distribution after scheduling:");
        for (Map.Entry<Integer, Double> entry : vmLoad.entrySet()) {
            System.out.println("VM " + entry.getKey() + ": " + entry.getValue());
        }
        
        // Submit all cloudlets at once
        super.submitCloudlets();
    }

    private int getLeastLoadedVm(Map<Integer, Double> vmLoad) {
        double minLoad = Double.MAX_VALUE;
        int selectedVmId = -1;
        
        for (Map.Entry<Integer, Double> entry : vmLoad.entrySet()) {
            if (entry.getValue() < minLoad) {
                minLoad = entry.getValue();
                selectedVmId = entry.getKey();
            }
        }
        
        return selectedVmId;
    }

    @Override
    protected void processCloudletReturn(SimEvent ev) {
        Cloudlet cloudlet = (Cloudlet) ev.getData();
        getCloudletReceivedList().add(cloudlet);
        Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId() + " received");

        cloudletsSubmitted--;
        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) {
            Log.printLine(getName() + ": All Cloudlets executed.");
            clearDatacenters();
            finishExecution();
        }
    }
    
    @Override
    protected void processVmCreate(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        int result = data[2];

        if (result == CloudSimTags.TRUE) {
            getVmsToDatacentersMap().put(vmId, datacenterId);
            getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
            Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vmId +
                    " has been created in Datacenter #" + datacenterId);
        } else {
            Log.printLine(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId +
                    " failed in Datacenter #" + datacenterId);
        }

        incrementVmsAcks();
        
        // If all VMs are created or failed, submit cloudlets
        if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
            submitCloudlets();
        } else if (getVmsRequested() == getVmsAcks()) {
            // Some VMs were failed to be created
            if (getVmsCreatedList().size() > 0) { // If some were created
                submitCloudlets();
            } else { // No VMs created, abort
                Log.printLine(CloudSim.clock() + ": " + getName() +
                        ": None of the required VMs could be created. Aborting");
                finishExecution();
            }
        }
    }
}