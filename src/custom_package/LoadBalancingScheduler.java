package custom_package;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class LoadBalancingScheduler {
    public static void main(String[] args) {
        try {
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;

            // Initialize CloudSim
            CloudSim.init(numUsers, calendar, traceFlag);

            // Create datacenter using the existing DatacenterCreator class
            Datacenter datacenter0 = DatacenterCreator.createDatacenter("Datacenter_0");

            // Create broker
            LoadBalancingDatacenterBroker broker = new LoadBalancingDatacenterBroker("Broker_1");
            int brokerId = broker.getId();

            // Create VM list - better resource configuration
            List<Vm> vmList = new ArrayList<>();
            int vmCount = 5; // Create more VMs to better demonstrate load balancing
            
            for (int i = 0; i < vmCount; i++) {
                // Create VMs with different configurations to better simulate heterogeneous environment
                int mips = 1000 + (i * 200); // Different processing capabilities
                int pesNumber = 1 + (i % 2); // Either 1 or 2 processing elements
                int ram = 1024;
                
                Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, 1000, 10000, "Xen", 
                        new CloudletSchedulerTimeShared());
                vmList.add(vm);
            }

            // Create cloudlet list with different lengths to demonstrate load balancing
            List<Cloudlet> cloudletList = new ArrayList<>();
            int cloudletCount = 10; // More cloudlets to better observe load balancing
            UtilizationModel utilizationModel = new UtilizationModelFull();

            for (int i = 0; i < cloudletCount; i++) {
                // Vary cloudlet lengths to better demonstrate load balancing
                long length = 10000 + (i * 5000); // Different lengths
                int pesNumber = 1; // Single core tasks
                
                Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, 300, 300,
                        utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }

            // Submit VM list and cloudlet list
            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            // Start simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // Get results
            List<Cloudlet> results = broker.getCloudletReceivedList();
            printCloudletList(results);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "    ";
        DecimalFormat dft = new DecimalFormat("###.##");
        
        System.out.println("========== IMPROVED LOAD BALANCING RESULT ==========");
        System.out.println("Cloudlet ID" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + "Time" + indent +
                "Start Time" + indent + "Finish Time");

        // Calculate VM load statistics
        Map<Integer, Integer> vmTaskCount = new HashMap<>();
        Map<Integer, Double> vmTotalTime = new HashMap<>();
        double totalExecutionTime = 0;
        double maxFinishTime = 0;
        
        for (Cloudlet cloudlet : list) {
            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                // Gather stats
                int vmId = cloudlet.getVmId();
                vmTaskCount.put(vmId, vmTaskCount.getOrDefault(vmId, 0) + 1);
                vmTotalTime.put(vmId, vmTotalTime.getOrDefault(vmId, 0.0) + cloudlet.getActualCPUTime());
                
                totalExecutionTime += cloudlet.getActualCPUTime();
                if (cloudlet.getFinishTime() > maxFinishTime) {
                    maxFinishTime = cloudlet.getFinishTime();
                }
                
                // Print details
                System.out.println(indent + cloudlet.getCloudletId() + indent + "SUCCESS" + indent +
                        cloudlet.getResourceId() + indent + cloudlet.getVmId() + indent +
                        dft.format(cloudlet.getActualCPUTime()) + indent +
                        dft.format(cloudlet.getExecStartTime()) + indent + 
                        dft.format(cloudlet.getFinishTime()));
            }
        }
        
        // Print load balancing statistics
        System.out.println("\n========== LOAD BALANCING STATISTICS ==========");
        System.out.println("VM ID" + indent + "Tasks" + indent + "Total Time" + indent + "Avg Time per Task");
        
        double totalVmTime = 0;
        for (Integer vmId : vmTaskCount.keySet()) {
            int tasks = vmTaskCount.get(vmId);
            double totalTime = vmTotalTime.get(vmId);
            double avgTime = tasks > 0 ? totalTime / tasks : 0;
            totalVmTime += totalTime;
            
            System.out.println(vmId + indent + tasks + indent + 
                    dft.format(totalTime) + indent + dft.format(avgTime));
        }
        
        System.out.println("\nTotal execution time (sum of all cloudlets): " + dft.format(totalExecutionTime));
        System.out.println("Total VM processing time: " + dft.format(totalVmTime));
        System.out.println("Makespan (completion time): " + dft.format(maxFinishTime));
        
        // Calculate load balancing metrics
        double avgVmLoad = vmTaskCount.isEmpty() ? 0 : totalVmTime / vmTaskCount.size();
        double loadVariance = 0.0;
        double maxDeviation = 0.0;
        
        for (Double vmLoad : vmTotalTime.values()) {
            double deviation = Math.abs(vmLoad - avgVmLoad);
            loadVariance += Math.pow(deviation, 2);
            maxDeviation = Math.max(maxDeviation, deviation);
        }
        
        if (!vmTaskCount.isEmpty()) {
            loadVariance /= vmTaskCount.size();
        }
        
        double stdDeviation = Math.sqrt(loadVariance);
        
        System.out.println("Average VM load: " + dft.format(avgVmLoad));
        System.out.println("Load balancing variance: " + dft.format(loadVariance));
        System.out.println("Standard deviation: " + dft.format(stdDeviation));
        System.out.println("Maximum load deviation: " + dft.format(maxDeviation));
        System.out.println("Lower variance and deviation values indicate better load balancing");
        
        // Calculate resource utilization efficiency
        System.out.println("\n========== RESOURCE UTILIZATION ==========");
        double utilizationEfficiency = totalExecutionTime / (maxFinishTime * vmTaskCount.size()) * 100;
        System.out.println("Resource utilization efficiency: " + dft.format(utilizationEfficiency) + "%");
    }
}