package com.itremotesupport.it_agent.service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class SystemInfoService {

    public String getComputerName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public void displayNetworkInfo() {
        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface network =
                        interfaces.nextElement();

                if (!network.isUp() || network.isLoopback()) {
                    continue;
                }

                var addresses = network.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    if (address instanceof java.net.Inet4Address) {
                        System.out.println(
                                "Interface : " + network.getName()
                        );

                        System.out.println(
                                "Adresse IP : " + address.getHostAddress()
                        );
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(
                    "Erreur réseau : " + e.getMessage()
            );
        }
    }
}
