package com.demo.upi_offline_mesh.mesh;

import com.demo.upi_offline_mesh.model.MeshPacket;

import java.util.ArrayList;
import java.util.List;

public class VirtualDevice {

    private final String deviceId;
    private final boolean hasInternet;   // "bridge" devices can reach the backend
    private final List<MeshPacket> inbox = new ArrayList<>();

    public VirtualDevice(String deviceId, boolean hasInternet) {
        this.deviceId = deviceId;
        this.hasInternet = hasInternet;
    }

    public void receive(MeshPacket packet) {
        // avoid storing exact duplicate packetId twice in the same device's inbox
        boolean alreadyHave = inbox.stream()
                .anyMatch(p -> p.getPacketId().equals(packet.getPacketId()));
        if (!alreadyHave) {
            inbox.add(packet);
        }
    }

    public List<MeshPacket> getInbox() {
        return inbox;
    }

    public void clearInbox() {
        inbox.clear();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean hasInternet() {
        return hasInternet;
    }
}
