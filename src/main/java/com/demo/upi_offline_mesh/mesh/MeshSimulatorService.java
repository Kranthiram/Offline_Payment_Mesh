package com.demo.upi_offline_mesh.mesh;

import com.demo.upi_offline_mesh.model.MeshPacket;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MeshSimulatorService {

    private final List<VirtualDevice> devices = new ArrayList<>();
    private final Random random = new Random();

    public void registerDevice(VirtualDevice device) {
        devices.add(device);
    }

    public List<VirtualDevice> getDevices() {
        return devices;
    }

    public void runGossipRound() {
        for (VirtualDevice sender : devices) {
            for (MeshPacket packet : new ArrayList<>(sender.getInbox())) {

                if (packet.getTtl() <= 0) {
                    continue;  // dead packet, drop it — don't forward further
                }

                // pick 1-2 random neighbors (excluding self) to forward to
                int neighborsToPick = 1 + random.nextInt(2);
                for (int i = 0; i < neighborsToPick; i++) {
                    VirtualDevice neighbor = devices.get(random.nextInt(devices.size()));
                    if (neighbor == sender) {
                        continue;
                    }

                    MeshPacket forwarded = new MeshPacket(
                            packet.getPacketId(),
                            packet.getTtl() - 1,          // TTL decrements on each hop
                            packet.getCreatedAt(),
                            packet.getCiphertext()
                    );
                    neighbor.receive(forwarded);
                }
            }
        }
    }

    public List<MeshPacket> collectFromBridges() {
        List<MeshPacket> collected = new ArrayList<>();
        for (VirtualDevice device : devices) {
            if (device.hasInternet()) {
                collected.addAll(device.getInbox());
                device.clearInbox();   // simulate "uploaded, cleared from local storage"
            }
        }
        return collected;
    }
}