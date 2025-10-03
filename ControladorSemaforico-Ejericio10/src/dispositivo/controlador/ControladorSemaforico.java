package dispositivo.controlador;
// Ejercicio 10 - Controlador Semaforico
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import dispositivo.utils.MySimpleLogger;

public class ControladorSemaforico {
    
    private MqttClient mqttClient;
    private String dispositivo1Id;
    private String dispositivo2Id;
    private String loggerId;
    
    // Tiempos configurables (en segundos)
    private int tiempoVerde = 30;
    private int tiempoAmarillo = 5;
    private int tiempoRojo = 3;
    
    public ControladorSemaforico(String mqttBroker, String dispositivo1Id, String dispositivo2Id) {
        this.dispositivo1Id = dispositivo1Id;
        this.dispositivo2Id = dispositivo2Id;
        this.loggerId = "ControladorSemaforico";
        
        try {
            // Crear cliente MQTT
            this.mqttClient = new MqttClient(mqttBroker, "ControladorSemaforico", new MemoryPersistence());
            
            // Configurar opciones de conexión
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(60);
            
            // Conectar al broker
            this.mqttClient.connect(connOpts);
            MySimpleLogger.info(loggerId, "Conectado al broker MQTT: " + mqttBroker);
            
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al conectar con MQTT: " + e.getMessage());
            throw new RuntimeException("No se pudo conectar al broker MQTT", e);
        }
    }
    
    /**
     * Inicia el controlador semafórico
     */
    public void iniciar() {
        MySimpleLogger.info(loggerId, "=== INICIANDO CONTROLADOR SEMAFÓRICO ===");
        MySimpleLogger.info(loggerId, "Dispositivo 1 (Calle A): " + dispositivo1Id);
        MySimpleLogger.info(loggerId, "Dispositivo 2 (Calle B): " + dispositivo2Id);
        
        try {
            // 1. Inicializar ambos semáforos en rojo
            MySimpleLogger.info(loggerId, "Paso 1: Inicializando ambos semáforos en ROJO");
            ponerRojo(dispositivo1Id);
            ponerRojo(dispositivo2Id);
            esperar(tiempoRojo);
            
            // Bucle infinito del controlador
            while (true) {
                // 2. Verde en Calle A, Rojo en Calle B
                MySimpleLogger.info(loggerId, "Paso 2: Verde en Calle A, Rojo en Calle B");
                ponerVerde(dispositivo1Id);
                ponerRojo(dispositivo2Id);
                esperar(tiempoVerde);
                
                // 3. Amarillo en Calle A, Rojo en Calle B
                MySimpleLogger.info(loggerId, "Paso 3: Amarillo en Calle A, Rojo en Calle B");
                ponerAmarillo(dispositivo1Id);
                ponerRojo(dispositivo2Id);
                esperar(tiempoAmarillo);
                
                // 4. Rojo en Calle A, Rojo en Calle B (transición)
                MySimpleLogger.info(loggerId, "Paso 4: Rojo en Calle A, Rojo en Calle B (transición)");
                ponerRojo(dispositivo1Id);
                ponerRojo(dispositivo2Id);
                esperar(tiempoRojo);
                
                // 5. Rojo en Calle A, Verde en Calle B
                MySimpleLogger.info(loggerId, "Paso 5: Rojo en Calle A, Verde en Calle B");
                ponerRojo(dispositivo1Id);
                ponerVerde(dispositivo2Id);
                esperar(tiempoVerde);
                
                // 6. Rojo en Calle A, Amarillo en Calle B
                MySimpleLogger.info(loggerId, "Paso 6: Rojo en Calle A, Amarillo en Calle B");
                ponerRojo(dispositivo1Id);
                ponerAmarillo(dispositivo2Id);
                esperar(tiempoAmarillo);
                
                // 7. Rojo en Calle A, Rojo en Calle B (transición)
                MySimpleLogger.info(loggerId, "Paso 7: Rojo en Calle A, Rojo en Calle B (transición)");
                ponerRojo(dispositivo1Id);
                ponerRojo(dispositivo2Id);
                esperar(tiempoRojo);
                
                MySimpleLogger.info(loggerId, "=== CICLO COMPLETADO, INICIANDO NUEVO CICLO ===");
            }
            
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error en el controlador: " + e.getMessage());
        }
    }
    
    /**
     * Pone el semáforo en rojo (f1)
     */
    private void ponerRojo(String dispositivoId) throws MqttException {
        enviarComando(dispositivoId, "f1", "encender");
        enviarComando(dispositivoId, "f2", "apagar");
        enviarComando(dispositivoId, "f3", "apagar");
    }
    
    /**
     * Pone el semáforo en amarillo (f2)
     */
    private void ponerAmarillo(String dispositivoId) throws MqttException {
        enviarComando(dispositivoId, "f1", "apagar");
        enviarComando(dispositivoId, "f2", "parpadear");
        enviarComando(dispositivoId, "f3", "apagar");
    }
    
    /**
     * Pone el semáforo en verde (f3)
     */
    private void ponerVerde(String dispositivoId) throws MqttException {
        enviarComando(dispositivoId, "f1", "apagar");
        enviarComando(dispositivoId, "f2", "apagar");
        enviarComando(dispositivoId, "f3", "encender");
    }
    
    /**
     * Envía un comando MQTT a una función específica
     */
    private void enviarComando(String dispositivoId, String funcionId, String accion) throws MqttException {
        String topic = "ina_08/dispositivo/" + dispositivoId + "/funcion/" + funcionId + "/comandos";
        
        try {
            JSONObject mensaje = new JSONObject();
            mensaje.put("accion", accion);
            
            MqttMessage mqttMessage = new MqttMessage(mensaje.toString().getBytes());
            mqttMessage.setQos(0);
            mqttMessage.setRetained(false);
            
            mqttClient.publish(topic, mqttMessage);
            MySimpleLogger.debug(loggerId, "Enviado comando a " + dispositivoId + "/" + funcionId + ": " + accion);
        } catch (org.json.JSONException e) {
            MySimpleLogger.error(loggerId, "Error al crear mensaje JSON: " + e.getMessage());
            throw new MqttException(e);
        }
    }
    
    /**
     * Espera un tiempo determinado
     */
    private void esperar(int segundos) {
        try {
            Thread.sleep(segundos * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Cierra la conexión MQTT
     */
    public void cerrar() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                MySimpleLogger.info(loggerId, "Conexión MQTT cerrada");
            }
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al cerrar conexión MQTT: " + e.getMessage());
        }
    }
    
    /**
     * Configura los tiempos del semáforo
     */
    public void configurarTiempos(int verde, int amarillo, int rojo) {
        this.tiempoVerde = verde;
        this.tiempoAmarillo = amarillo;
        this.tiempoRojo = rojo;
        MySimpleLogger.info(loggerId, "Tiempos configurados - Verde: " + verde + "s, Amarillo: " + amarillo + "s, Rojo: " + rojo + "s");
    }
}
