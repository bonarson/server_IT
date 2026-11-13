package com.itremotesupport.it_agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itremotesupport.it_agent.dto.RemoteTaskResponse;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AgentTaskExecutionService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\." +
                    "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\." +
                    "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\." +
                    "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$"
    );

    public String execute(RemoteTaskResponse task) {
        try {
            JsonNode parameters =
                    objectMapper.readTree(task.parametersJson());

//            listPrinters();

            return switch (task.taskType()) {
                case "PING_PRINTER" -> executePing(parameters);
                case "CHECK_PRINTER_PORT" -> checkPrinterPort(parameters);
                case "GET_SYSTEM_INFO" -> getSystemInfo(parameters);
                case "TEST_PRINT" -> executeTestPrint(parameters);
                case "INSTALL_DEFAULT_PRINTER" -> installDefaultPrinter(parameters);
                case "GET_PRINTERS" -> getInstalledPrinters();
                default -> "FAILED : Type de tâche non pris en charge.";
            };

        } catch (Exception e) {
            return "FAILED : " + e.getMessage();
        }
    }

    // 1. PING PRINTER
    private String executePing(JsonNode parameters)
            throws IOException, InterruptedException {

        if (!hasExactFields(parameters, "ipAddress"))
            return "FAILED : Paramètres PING invalides.";

        String ipAddress = parameters.get("ipAddress").asText();

        if (!isValidIpv4(ipAddress))
            return "FAILED : Adresse IPv4 invalide.";

        System.out.println("Exécution PING vers : " + ipAddress);

        ProcessBuilder builder =
                new ProcessBuilder("ping", "-n", "4", ipAddress);

        builder.redirectErrorStream(true);

        Process process = builder.start();

        boolean completed = process.waitFor(15, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            return "FAILED : Délai du Ping dépassé.";
        }

        String output = new String(
                process.getInputStream().readAllBytes(),
                Charset.forName("CP850")
        );

        if (process.exitValue() == 0)
            return "SUCCESS : Imprimante accessible.\n" + output;

        return "FAILED : Imprimante inaccessible.\n" + output;
    }

    // 2. CHECK PRINTER PORT
    private String checkPrinterPort(JsonNode parameters)
            throws IOException {

        if (!hasExactFields(parameters, "ipAddress", "port"))
            return "FAILED : Paramètres du port invalides.";

        JsonNode ipNode = parameters.get("ipAddress");
        JsonNode portNode = parameters.get("port");

        if (!ipNode.isTextual()
                || !portNode.isIntegralNumber()
                || !portNode.canConvertToInt()) {
            return "FAILED : Adresse IP ou port invalide.";
        }

        String ipAddress = ipNode.asText();
        int port = portNode.asInt();

        if (!isValidIpv4(ipAddress))
            return "FAILED : Adresse IPv4 invalide.";

        if (port != 9100 && port != 515 && port != 631)
            return "FAILED : Port non autorisé.";

        System.out.println(
                "Vérification du port TCP " + port + " sur " + ipAddress
        );

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(ipAddress, port), 3000
            );

            return "SUCCESS : Port TCP " + port
                    + " accessible sur " + ipAddress;

        } catch (IOException e) {
            return "FAILED : Port TCP " + port
                    + " inaccessible sur " + ipAddress;
        }
    }

    // 3. GET SYSTEM INFO
    private String getSystemInfo(JsonNode parameters)
            throws Exception {

        if (!hasExactFields(parameters))
            return "FAILED : Paramètres GET_SYSTEM_INFO invalides.";

        String hostname = InetAddress.getLocalHost().getHostName();

        String ipAddress = getLocalIpv4Address();

        String os = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String username = System.getProperty("user.name");

        long totalRam = 0;
        long freeRam = 0;

        java.lang.management.OperatingSystemMXBean bean =
                ManagementFactory.getOperatingSystemMXBean();

        if (bean instanceof com.sun.management.OperatingSystemMXBean osBean) {
            totalRam = osBean.getTotalMemorySize();
            freeRam = osBean.getFreeMemorySize();
        }

        long totalRamMB = totalRam / (1024 * 1024);
        long freeRamMB = freeRam / (1024 * 1024);

        return """
                SUCCESS : Informations système récupérées.
                
                Nom du PC : %s
                Adresse IP : %s
                Système : %s
                Version : %s
                Utilisateur : %s
                RAM totale : %d MB
                RAM disponible : %d MB
                """.formatted(
                hostname,
                ipAddress,
                os,
                osVersion,
                username,
                totalRamMB,
                freeRamMB
        );
    }

    // Recherche IP : Ethernet prioritaire, puis Wi-Fi.
    private String getLocalIpv4Address() throws SocketException {

        String ip = findIpv4ByInterface("ethernet", "eth", "en");

        if (ip != null)
            return ip;

        ip = findIpv4ByInterface(
                "wi-fi", "wifi", "wireless", "wlan"
        );

        if (ip != null)
            return ip;

        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface network = interfaces.nextElement();

            if (!network.isUp() || network.isLoopback())
                continue;

            Enumeration<InetAddress> addresses =
                    network.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();

                if (address instanceof Inet4Address
                        && !address.isLoopbackAddress()
                        && !address.isLinkLocalAddress()) {
                    return address.getHostAddress();
                }
            }
        }

        return "IP introuvable";
    }

    private String findIpv4ByInterface(String... keywords)
            throws SocketException {

        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface network = interfaces.nextElement();

            if (!network.isUp() || network.isLoopback())
                continue;

            String name = network.getName().toLowerCase();
            String displayName = network.getDisplayName().toLowerCase();

            boolean matches = false;

            for (String keyword : keywords) {
                if (name.contains(keyword)
                        || displayName.contains(keyword)) {
                    matches = true;
                    break;
                }
            }

            if (!matches)
                continue;

            Enumeration<InetAddress> addresses =
                    network.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();

                if (address instanceof Inet4Address
                        && !address.isLoopbackAddress()
                        && !address.isLinkLocalAddress()) {
                    return address.getHostAddress();
                }
            }
        }

        return null;
    }

    private String executeWindowsPrint(JsonNode parameters)
            throws PrinterException {

        if (!hasExactFields(parameters, "mode", "printerName")) {
            return "FAILED : Paramètres WINDOWS invalides.";
        }

        JsonNode printerNode = parameters.get("printerName");

        if (!printerNode.isTextual()
                || printerNode.asText().isBlank()
                || printerNode.asText().length() > 128) {
            return "FAILED : Nom de l'imprimante invalide.";
        }

        String printerName = printerNode.asText();

        PrintService selectedPrinter = null;

        PrintService[] printers =
                PrintServiceLookup.lookupPrintServices(null, null);

        for (PrintService printer : printers) {
            if (printer.getName().equalsIgnoreCase(printerName)) {
                selectedPrinter = printer;
                break;
            }
        }

        if (selectedPrinter == null) {
            return "FAILED : Imprimante introuvable : " + printerName;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(selectedPrinter);

        job.setPrintable((Graphics graphics, PageFormat format, int pageIndex) -> {

            if (pageIndex > 0) {
                return java.awt.print.Printable.NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) graphics;

            g2d.translate(
                    format.getImageableX(),
                    format.getImageableY()
            );

            g2d.setFont(new Font("Arial", Font.PLAIN, 12));

            String[] lines = {
                    "IT REMOTE SUPPORT",
                    "TEST D'IMPRESSION",
                    "",
                    "Nom du PC : " + System.getenv("COMPUTERNAME"),
                    "Utilisateur : " + System.getProperty("user.name"),
                    "Date : " + LocalDateTime.now(),
                    "",
                    "Impression Windows reussie."
            };

            int y = 40;

            for (String line : lines) {
                g2d.drawString(line, 30, y);
                y += 25;
            }

            return java.awt.print.Printable.PAGE_EXISTS;
        });

        job.print();

        return "SUCCESS : Job d'impression envoyé à l'imprimante Windows : "
                + printerName;
    }

    // 4. TEST PRINT
    private String executeTestPrint(JsonNode parameters) throws Exception {

        JsonNode modeNode = parameters.get("mode");

        if (modeNode == null || !modeNode.isTextual()) {
            return "FAILED : Mode d'impression invalide.";
        }

        return switch (modeNode.asText()) {
            case "WINDOWS" -> executeWindowsPrint(parameters);
            case "NETWORK" -> executeNetworkPrint(parameters);
            default -> "FAILED : Mode d'impression non autorisé.";
        };
    }

    private String executeNetworkPrint(JsonNode parameters)
            throws IOException {

        if (!hasExactFields(parameters, "mode", "ipAddress", "port")) {
            return "FAILED : Paramètres NETWORK invalides.";
        }

        JsonNode ipNode = parameters.get("ipAddress");
        JsonNode portNode = parameters.get("port");

        if (!ipNode.isTextual()
                || !portNode.isIntegralNumber()
                || !portNode.canConvertToInt()) {
            return "FAILED : Adresse IP ou port invalide.";
        }

        String ipAddress = ipNode.asText();
        int port = portNode.asInt();

        if (!isValidIpv4(ipAddress)) {
            return "FAILED : Adresse IPv4 invalide.";
        }

        if (port != 9100) {
            return "FAILED : Seul le port 9100 est autorisé.";
        }

        String esc = "\u001B";

        String pcl = esc + "E"
                + esc + "&l0O"
                + esc + "&l6D"
                + esc + "(s0p12h0s0b4099T"
                + "IT REMOTE SUPPORT\r\n"
                + "TEST D'IMPRESSION RESEAU\r\n"
                + "\r\n"
                + "Adresse IP : " + ipAddress + "\r\n"
                + "Date : " + LocalDateTime.now() + "\r\n"
                + "\r\n"
                + "Test d'impression termine.\r\n"
                + "\f"
                + esc + "%-12345X";

        byte[] data = pcl.getBytes(StandardCharsets.US_ASCII);

        System.out.println(
                "Envoi du test d'impression vers "
                        + ipAddress + ":" + port
        );

        try (Socket socket = new Socket()) {

            socket.connect(
                    new InetSocketAddress(ipAddress, port),
                    3000
            );

            socket.setSoTimeout(5000);

            socket.getOutputStream().write(data);
            socket.getOutputStream().flush();

            return "SUCCESS : Données d'impression envoyées vers "
                    + ipAddress + ":" + port
                    + ". Vérifiez la sortie papier.";
        }
    }


    private String installDefaultPrinter(JsonNode parameters)
            throws Exception {

        if (!hasExactFields(
                parameters,
                "printerName",
                "ipAddress",
                "port",
                "driverName")) {
            return "FAILED : Paramètres d'installation invalides.";
        }

        JsonNode printerNode = parameters.get("printerName");
        JsonNode ipNode = parameters.get("ipAddress");
        JsonNode portNode = parameters.get("port");
        JsonNode driverNode = parameters.get("driverName");

        if (!printerNode.isTextual()
                || printerNode.asText().isBlank()
                || printerNode.asText().length() > 128
                || printerNode.asText().contains("\n")
                || printerNode.asText().contains("\r")) {
            return "FAILED : Nom de l'imprimante invalide.";
        }

        if (!ipNode.isTextual() || !isValidIpv4(ipNode.asText())) {
            return "FAILED : Adresse IPv4 invalide.";
        }

        if (!portNode.isIntegralNumber()
                || !portNode.canConvertToInt()
                || portNode.asInt() != 9100) {
            return "FAILED : Seul le port 9100 est autorisé.";
        }

        if (!driverNode.isTextual()
                || driverNode.asText().isBlank()
                || driverNode.asText().length() > 128
                || driverNode.asText().contains("\n")
                || driverNode.asText().contains("\r")) {
            return "FAILED : Nom du pilote invalide.";
        }

        if (!System.getProperty("os.name")
                .toLowerCase().contains("windows")) {
            return "FAILED : Cette opération nécessite Windows.";
        }

        String printerName = printerNode.asText();
        String ipAddress = ipNode.asText();
        String driverName = driverNode.asText();

        String portName = "IP_" + ipAddress;

        // Échappement des valeurs pour les chaînes PowerShell.
        printerName = printerName.replace("'", "''");
        driverName = driverName.replace("'", "''");

        String script = """
                $ErrorActionPreference = 'Stop'
                
                $printerName = '%s'
                $ipAddress = '%s'
                $driverName = '%s'
                $portName = '%s'
                
                    # Rechercher les pilotes Brother installés
                    $driver = Get-PrinterDriver |
                        Where-Object { $_.Name -like 'Brother*' } |
                        Sort-Object Name |
                        Select-Object -First 1
                
                    if (-not $driver) {
                        throw "Aucun pilote Brother installé. Installation manuelle nécessaire."
                    }
                
                    # Utiliser le pilote trouvé
                    $driverName = $driver.Name
                
                    Write-Output "Pilote Brother sélectionné : $driverName"
                
                # Vérifier le port TCP/IP
                $port = Get-PrinterPort -Name $portName `
                    -ErrorAction SilentlyContinue
                
                if ($port) {
                    if ($port.PrinterHostAddress -ne $ipAddress) {
                        throw "Le port existe avec une autre adresse IP."
                    }
                }
                else {
                    Add-PrinterPort `
                        -Name $portName `
                        -PrinterHostAddress $ipAddress
                }
                
                # Vérifier si l'imprimante existe déjà
                $printer = Get-Printer -Name $printerName `
                    -ErrorAction SilentlyContinue
                
                if ($printer) {
                    if ($printer.PortName -ne $portName `
                        -or $printer.DriverName -ne $driverName) {
                        throw "Une imprimante de ce nom existe avec une configuration différente."
                    }
                }
                else {
                    Add-Printer `
                        -Name $printerName `
                        -DriverName $driverName `
                        -PortName $portName
                }
                
                # Définir comme imprimante par défaut
                $network = New-Object -ComObject WScript.Network
                $network.SetDefaultPrinter($printerName)
                
                # Vérifier le résultat
                $defaultPrinter = Get-CimInstance Win32_Printer |
                    Where-Object { $_.Name -eq $printerName }
                
                if (-not $defaultPrinter -or -not $defaultPrinter.Default) {
                    throw "Impossible de définir l'imprimante par défaut."
                }
                
                Write-Output "SUCCESS : Imprimante installée et définie par défaut."
                """.formatted(
                printerName,
                ipAddress,
                driverName,
                portName
        );

        // PowerShell -EncodedCommand utilise UTF-16LE.
        String encodedScript = Base64.getEncoder().encodeToString(
                script.getBytes(StandardCharsets.UTF_16LE)
        );

        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy", "Bypass",
                "-EncodedCommand", encodedScript
        );

        builder.redirectErrorStream(true);

        Process process = builder.start();

        boolean completed = process.waitFor(30, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            return "FAILED : Délai d'installation dépassé.";
        }

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        if (process.exitValue() == 0
                && output.contains("SUCCESS")) {
            return output.trim();
        }

        return "FAILED : Installation impossible.\n" + output;
    }





    private String getInstalledPrinters() throws Exception {

        if (!System.getProperty("os.name")
                .toLowerCase()
                .contains("windows")) {
            return "FAILED : Cette opération nécessite Windows.";
        }

        String script = """
        $ErrorActionPreference = 'Stop'
        $ProgressPreference = 'SilentlyContinue'
        $InformationPreference = 'SilentlyContinue'

        $printers = @(
            Get-Printer |
            Where-Object {
                $_.DriverName -like 'Brother*'
            } |
            Select-Object -First 2 @{
                Name = 'Name'
                Expression = { $_.Name }
            }, @{
                Name = 'DriverName'
                Expression = { $_.DriverName }
            }, @{
                Name = 'PortName'
                Expression = { $_.PortName }
            }
        )

        ConvertTo-Json -InputObject $printers -Depth 2 -Compress
        """;

        String encodedScript = Base64.getEncoder().encodeToString(
                script.getBytes(StandardCharsets.UTF_16LE)
        );

        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-EncodedCommand",
                encodedScript
        );

        builder.redirectErrorStream(true);

        Process process = builder.start();

        CompletableFuture<String> outputFuture =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return new String(
                                process.getInputStream().readAllBytes(),
                                StandardCharsets.UTF_8
                        ).trim();
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                });

        boolean completed = process.waitFor(30, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            process.waitFor();

            try {
                process.getInputStream().close();
            } catch (IOException ignored) {
            }

            outputFuture.cancel(true);

            return "FAILED : Délai de récupération dépassé.";
        }

        String output;

        try {
            output = outputFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "FAILED : Impossible de lire la sortie PowerShell : "
                    + e.getMessage();
        }

        if (process.exitValue() != 0) {
            return "FAILED : Erreur PowerShell : " + output;
        }

        System.out.println("=== IMPRIMANTES BROTHER ===");
        System.out.println(output);

        return output;
    }






    public void testPowerShell() throws Exception {

        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-Command",
                "Import-Module PrintManagement; Get-Printer | Select-Object Name,DriverName,PortName | ConvertTo-Json -Compress"
        );

        builder.redirectErrorStream(true);

        Process process = builder.start();

        boolean completed = process.waitFor(30, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            System.out.println("TIMEOUT POWERSHELL");
            return;
        }

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        System.out.println("EXIT CODE : " + process.exitValue());
        System.out.println("POWERSHELL OUTPUT : " + output);
    }


    // VALIDATION
    private boolean isValidIpv4(String ipAddress) {
        return ipAddress != null
                && IPV4_PATTERN.matcher(ipAddress).matches();
    }

    private boolean hasExactFields(
            JsonNode parameters, String... allowedFields) {

        if (parameters == null
                || !parameters.isObject()
                || parameters.size() != allowedFields.length) {
            return false;
        }

        Iterator<String> fields = parameters.fieldNames();

        while (fields.hasNext()) {
            String field = fields.next();

            boolean allowed = false;

            for (String expected : allowedFields) {
                if (expected.equals(field)) {
                    allowed = true;
                    break;
                }
            }

            if (!allowed)
                return false;
        }

        return true;
    }

    public void listPrinters() {
        PrintService[] printers =
                PrintServiceLookup.lookupPrintServices(null, null);

        System.out.println("=== IMPRIMANTES DISPONIBLES ===");

        for (PrintService printer : printers) {
            System.out.println("- " + printer.getName());
        }
    }
}
