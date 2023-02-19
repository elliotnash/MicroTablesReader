package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazecast.jSerialComm.*;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

class TeensyJson {
    public HashMap<String, SensorJson> sensors;
    public List<CommandJson> commands;
}

class SensorJson {
    public String name;
    public String type;
    public JsonNode data;
}

class CommandJson {
    public String command;
    public JsonNode data;
}

class MicrocontrollerConnection {
    private Reader reader = new Reader();
    private SerialPort port;
    MicrocontrollerConnection(SerialPort port) {
        // Configure serial port
        this.port = port;
        this.port.openPort();
        this.port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        // Start reader thread
        reader.start();

        // add shutdown hook to nicely stop reader thread
        Runtime.getRuntime().addShutdownHook(new Thread(() -> reader.interrupt()));
    }
    private static class Event {
        public static final byte NULL = 0x00;
        public static final byte STX = 0x02;
        public static final byte ETX = 0x03;
    }
    private class Reader extends Thread {
        public synchronized void run() {
            try {
                while (!interrupted()) {
                    // read until start of event
                    while (port.getInputStream().read() != Event.STX) ;

                    // read data to ETX
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    var read = port.getInputStream().read();
                    while (read != Event.ETX) {
                        out.write(read);
                        read = port.getInputStream().read();
                    }

                    onPacket(out.toString());
                }
            } catch (Exception e) {e.printStackTrace();}
            port.closePort();
        }
    }
    private ObjectMapper objectMapper = new ObjectMapper();
    int test = 0;
    long start = 0;

    private synchronized void onPacket(String json) {
        test++;
        if (test == 1) {
            start = System.currentTimeMillis();
        }
        if (test == 101) {
            long elapsed = System.currentTimeMillis()-start;
            System.out.println("Took "+elapsed+"ms");
        }
        // System.out.println(test);
//        try {
//            System.out.println(json);
//            TeensyJson packet = objectMapper.readValue(json, TeensyJson.class);
//            System.out.println(packet.sensors.get("temp_1").data.get("temperature"));
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
    }
}

public class Main {
    public static void main(String[] args) {
        var ports = Arrays.stream(SerialPort.getCommPorts()).filter(port ->
                port.getSystemPortName().startsWith("cu.usbmodem")).toList();
        // first port should be correct I'm going to cry if it's not
        if (ports.isEmpty()) {
            System.out.println("No teensy connected");
        }
        var connection = new MicrocontrollerConnection(ports.get(0));
    }
}
