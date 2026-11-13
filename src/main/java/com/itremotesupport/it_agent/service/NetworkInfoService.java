package com.itremotesupport.it_agent.service;

import java.net.*;
import java.util.*;

public class NetworkInfoService {

    public record NetworkAddress(
            String interfaceName,
            String ipAddress
    ) {}

    public NetworkAddress getPreferredNetworkAddress()
            throws SocketException {

        List<NetworkAddress> ethernet = new ArrayList<>();
        List<NetworkAddress> wifi = new ArrayList<>();
        List<NetworkAddress> others = new ArrayList<>();

        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {

            NetworkInterface network = interfaces.nextElement();

            if (!network.isUp()
                    || network.isLoopback()
                    || network.isVirtual()
                    || isVirtualAdapter(network)) {
                continue;
            }

            String name = network.getName().toLowerCase();
            String displayName = network.getDisplayName()
                    .toLowerCase();

            Enumeration<InetAddress> addresses =
                    network.getInetAddresses();

            while (addresses.hasMoreElements()) {

                InetAddress address = addresses.nextElement();

                if (!(address instanceof Inet4Address)
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()) {
                    continue;
                }

                NetworkAddress info = new NetworkAddress(
                        network.getDisplayName(),
                        address.getHostAddress()
                );

                if (name.contains("ethernet")
                        || name.startsWith("eth")
                        || displayName.contains("ethernet")) {

                    ethernet.add(info);

                } else if (name.contains("wireless")
                        || name.contains("wi-fi")
                        || name.contains("wifi")
                        || name.contains("wlan")
                        || displayName.contains("wi-fi")
                        || displayName.contains("wireless")) {

                    wifi.add(info);

                } else {
                    others.add(info);
                }
            }
        }

        if (!ethernet.isEmpty()) {
            return ethernet.get(0);
        }

        if (!wifi.isEmpty()) {
            return wifi.get(0);
        }

        if (!others.isEmpty()) {
            return others.get(0);
        }

        throw new IllegalStateException(
                "Aucune adresse IPv4 réseau disponible"
        );
    }

    private boolean isVirtualAdapter(NetworkInterface network) {

        String description = (
                network.getName() + " "
                        + network.getDisplayName()
        ).toLowerCase();

        return description.contains("virtual")
                || description.contains("vmware")
                || description.contains("virtualbox")
                || description.contains("hyper-v")
                || description.contains("vethernet")
                || description.contains("wsl")
                || description.contains("docker")
                || description.contains("loopback")
                || description.contains("tunnel");
    }
}