package it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt;

import com.google.gson.Gson;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.logger.Logger;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.access.AccessCollector;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.chlorine.ChlorineCollector;
import it.unipi.dii.inginf.iot.SmartWellnessCollector.mqtt.nodes.humidity.HumidityCollector;
import org.eclipse.paho.client.mqttv3.*;


/**
 * MQTT handler for incoming messages from the MQTT sensors in the network
 */
public class MqttHandler implements MqttCallback {

    private final String BROKER = "tcp://localhost:1883";
    private final String CLIENT_ID = "SmartWellnessCollector";
    private final int RECONNECTION_INTERVAL = 5;
    private final int MAX_NUMBER_OF_RECONNECTION_TIMES = 7;

    private MqttClient mqttClient = null;
    private Gson parser;
    private final ChlorineCollector chlorineCollector;
    private final HumidityCollector humidityCollector;
    private final AccessCollector accessCollector;

    private Logger logger;

    /**
     * Basic constructor, initializes the stubs for all the MQTT devices
     */
    public MqttHandler () {
        parser = new Gson();
        logger = Logger.getInstance();
        chlorineCollector = new ChlorineCollector();
        humidityCollector = new HumidityCollector();
        accessCollector = new AccessCollector();
        do {
            try {
                mqttClient = new MqttClient(BROKER, CLIENT_ID);
                mqttClient.setCallback(this);
                connectToBroker();
                System.out.println("Connected to the broker: " + BROKER);
            }
            catch(MqttException me) {
                System.out.println("Could not connect, retrying ...");
            }
        }while(!mqttClient.isConnected());
    }

    /**
     * Try to connect to the broker
     */
    private void connectToBroker () throws MqttException {
        mqttClient.connect();
        mqttClient.subscribe(chlorineCollector.getSENSOR_TOPIC());
        mqttClient.subscribe(humidityCollector.getSENSOR_TOPIC());
        mqttClient.subscribe(accessCollector.getSENSOR_TOPIC());
        System.out.println("Subscribed to topic: " + chlorineCollector.getSENSOR_TOPIC());
        System.out.println("Subscribed to topic: " + accessCollector.getSENSOR_TOPIC());
        System.out.println("Subscribed to topic: " + humidityCollector.getSENSOR_TOPIC());
    }

    /**
     * Publish a message
     * @param topic     topic of the message
     * @param message   message to send
     */
    public void publishMessage (final String topic, final String message) {
        try {
            mqttClient.publish(topic, new MqttMessage(message.getBytes()));
        }
        catch(MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * If the connection has been lost, try to reconnect after waiting some time
     */
    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("WARNING: connection lost with the broker");
        int times = 0;
        do {
            times++;
            if (times > MAX_NUMBER_OF_RECONNECTION_TIMES) {
                System.err.println("ERROR: cannot reconnect with the broker");
                System.exit(-1);
            }
            try {
                Thread.sleep(RECONNECTION_INTERVAL * 1000);
                System.out.println("WARNING: reconnecting to the broker..");
                connectToBroker();
            }
            catch (MqttException | InterruptedException e) {
                e.printStackTrace();
            }
        } while (!this.mqttClient.isConnected());
        System.out.println("INFO: connection with the broker restored");
    }

    /**
     * Handles an incoming new message and saves the payload in the logs.
     * Depending on the topic of the message, the payload is handled by a specific device stub class
     * @param topic the topic of the message
     */
    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String payload = new String(mqttMessage.getPayload());
        logger.logInfo(payload);
        if (topic.equals(chlorineCollector.getSENSOR_TOPIC())) {
            boolean updated = chlorineCollector.processMessage(payload);
            if(updated){
                publishMessage(chlorineCollector.getACTUATOR_TOPIC(), (chlorineCollector.getChlorineRegulator() ? "ON" : "OFF"));
                logger.logStatus("Chlorine regulator: " + (chlorineCollector.getChlorineRegulator() ? "ON" : "OFF"));
            }
        }

        if (topic.equals(humidityCollector.getSENSOR_TOPIC())) {
            boolean updated = humidityCollector.processMessage(payload);
            if (updated) {
                publishMessage(humidityCollector.getACTUATOR_TOPIC(), (humidityCollector.getHumidifierStatus() ? "ON":"OFF"));
                logger.logStatus("Humidifier: " + (humidityCollector.getHumidifierStatus() ? "ON":"OFF"));
            }
        }

        if (topic.equals(accessCollector.getSENSOR_TOPIC())) {
            boolean updated = accessCollector.processMessage(payload);
            if(updated){
                publishMessage(accessCollector.getACTUATOR_TOPIC(), accessCollector.getActuatorOn().toString());
                logger.logStatus("Light color: " + accessCollector.getLightColour());
                logger.logStatus("Entrance door: " + accessCollector.getEntranceLock());
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {}

    /*--------------------POOL CHLORINE-------------------*/
    public float getPoolChlorineLevel() {
        return chlorineCollector.getSensedData();
    }

    public void setPoolMinPPM(float minPPM){
        chlorineCollector.setMinPPM(minPPM);
    }

    public void setPoolMaxPPM(float maxPPM){
        chlorineCollector.setMaxPPM(maxPPM);
    }

    /*----------------STEAM BATH TEMPERATURE---------------*/
    public int getSteamBathNumberOfPeople() {
        return accessCollector.getSensedData();
    }

    public void setSteamBathIntermediateNumber(int intermediateNumber){
        accessCollector.setIntermediateNumber(intermediateNumber);
    }

    public void setSteamBathMaxNumber(int maxNumber){
        accessCollector.setMaxNumber(maxNumber);
    }

    /*-----------------STEAM BATH HUMIDITY----------------*/
    public int getSteamBathHumidityLevel() {
        return humidityCollector.getSensedData();
    }

    public void setSteamBathMinHumidity(int minHumidity){
        humidityCollector.setMinHumidity(minHumidity);
    }

    public void setSteamBathMaxHumidity(int maxHumidity){
        humidityCollector.setMaxHumidity(maxHumidity);
    }


}
