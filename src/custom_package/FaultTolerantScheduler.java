package custom_package;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.*;

public class FaultTolerantScheduler {
    public static void main(String[] args) {
        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            Datacenter datacenter0 = DatacenterCreator.createDatacenter("Datacenter_0");
            FaultTolerantDatacenterBroker broker = new FaultTolerantDatacenterBroker("FaultTolerantBroker");
            int brokerId = broker.getId();

            List<Vm> vmList = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
            	Vm vm = new Vm(i, brokerId, 1000, 1, 512, 5000, 100000, "Xen", new CloudletSchedulerTimeShared());
                vmList.add(vm);
            }

            List<Cloudlet> cloudletList = new ArrayList<>();
            UtilizationModel utilizationModel = new UtilizationModelFull();
            for (int i = 0; i < 10; i++) {
                Cloudlet cloudlet = new Cloudlet(i, 40000, 1, 300, 300, utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            List<Cloudlet> resultList = broker.getCloudletReceivedList();
            printCloudletList(resultList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printCloudletList(List<Cloudlet> list) {
        System.out.println("========== FAULT-TOLERANT RESULT ==========");
        for (Cloudlet cloudlet : list) {
            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                System.out.printf("Cloudlet %d: SUCCESS, VM: %d, Time: %.2f\n",
                        cloudlet.getCloudletId(),
                        cloudlet.getVmId(),
                        cloudlet.getActualCPUTime());
            }
        }
    }
}
