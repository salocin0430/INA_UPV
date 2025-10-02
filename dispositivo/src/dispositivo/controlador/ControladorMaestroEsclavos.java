package dispositivo.controlador;
// Ejercicio 11 - Controlador Maestro-Esclavos

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import dispositivo.utils.MySimpleLogger;

public class ControladorMaestroEsclavos implements MqttCallback {
    
    private MqttClient mqttClientSubscriber;  // Para suscribirse (recibir)
    private MqttClient mqttClientPublisher;   // Para publicar
    private String maestroId;
    private String[] esclavosIds;
    private String loggerId;
    private String topicBase;
    
    public ControladorMaestroEsclavos(String mqttBroker, String maestroId, String[] esclavosIds) {
        this.maestroId = maestroId;
        this.esclavosIds = esclavosIds;
        this.loggerId = "ControladorMaestroEsclavos";
        this.topicBase = "ina_08"; // Usar el ID del maestro para el topic base
        
        try {
            // Crear cliente MQTT para suscripciones (recibir)
            this.mqttClientSubscriber = new MqttClient(mqttBroker, "ControladorMaestroEsclavos_Sub", new MemoryPersistence());
            
            // Crear cliente MQTT para publicaciones (enviar)
            this.mqttClientPublisher = new MqttClient(mqttBroker, "ControladorMaestroEsclavos_Pub", new MemoryPersistence());
            
            // Configurar opciones de conexión
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(60);
            
            // Configurar callback solo en el subscriber
            this.mqttClientSubscriber.setCallback(this);
            
            // Conectar ambos clientes al broker
            this.mqttClientSubscriber.connect(connOpts);
            this.mqttClientPublisher.connect(connOpts);
            MySimpleLogger.info(loggerId, "Conectado al broker MQTT: " + mqttBroker);
            
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al conectar con MQTT: " + e.getMessage());
            throw new RuntimeException("No se pudo conectar al broker MQTT", e);
        }
    }
    
    /**
     * Inicia el controlador maestro-esclavos
     */
    public void iniciar() throws MqttException {
        MySimpleLogger.info(loggerId, "=== INICIANDO CONTROLADOR MAESTRO-ESCLAVOS ===");
        MySimpleLogger.info(loggerId, "Maestro: " + maestroId);
        MySimpleLogger.info(loggerId, "Esclavos: " + String.join(", ", esclavosIds));
        
        // Suscribirse al topic de información de la función f1 del maestro
        String topicInfo = topicBase + "/dispositivo/" + maestroId + "/funcion/f1/info";
        mqttClientSubscriber.subscribe(topicInfo, 0);
        MySimpleLogger.info(loggerId, "Suscrito al topic: " + topicInfo);
        
        MySimpleLogger.info(loggerId, "Controlador iniciado. Esperando cambios del maestro...");
    }
    
    /**
     * Callback cuando llega un mensaje MQTT
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        MySimpleLogger.info(loggerId, "Mensaje recibido en topic " + topic + ": " + payload);
        MySimpleLogger.info(loggerId, "Cliente MQTT Subscriber conectado: " + (mqttClientSubscriber != null && mqttClientSubscriber.isConnected()));
        MySimpleLogger.info(loggerId, "Cliente MQTT Publisher conectado: " + (mqttClientPublisher != null && mqttClientPublisher.isConnected()));
        
        try {
            // Parsear el mensaje JSON
            JSONObject statusMessage = new JSONObject(payload);
            String funcionId = statusMessage.getString("id");
            String estado = statusMessage.getString("estado");
            
            // Solo procesar la función f1
            if ("f1".equals(funcionId)) {
                MySimpleLogger.info(loggerId, "Cambio detectado en f1 del maestro: " + estado);
                
                // Verificar conexión antes de replicar
                if (mqttClientPublisher != null && mqttClientPublisher.isConnected()) {
                    // Replicar el cambio a todos los esclavos
                    replicarCambioAEsclavos(funcionId, estado);
                } else {
                    MySimpleLogger.warn(loggerId, "Cliente MQTT Publisher no conectado, no se puede replicar cambio");
                }
            }
            
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error al procesar mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Replica el cambio de estado a todos los dispositivos esclavos
     */
    private void replicarCambioAEsclavos(String funcionId, String estado) {
        MySimpleLogger.info(loggerId, "Replicando cambio a " + esclavosIds.length + " esclavos...");
        
        for (String esclavoId : esclavosIds) {
            try {
                // Determinar la acción basada en el estado
                String accion = determinarAccion(estado);
                
                MySimpleLogger.info(loggerId, "Enviando comando a esclavo " + esclavoId + ": f1 -> " + accion);
                
                // Enviar comando al esclavo
                enviarComandoEsclavo(esclavoId, funcionId, accion);
                
                MySimpleLogger.info(loggerId, "Comando procesado para " + esclavoId);
                
            } catch (Exception e) {
                MySimpleLogger.error(loggerId, "Error al procesar esclavo " + esclavoId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        MySimpleLogger.info(loggerId, "Replicación completada para todos los esclavos");
    }
    
    /**
     * Determina la acción basada en el estado
     */
    private String determinarAccion(String estado) {
        switch (estado.toUpperCase()) {
            case "ON":
                return "encender";
            case "OFF":
                return "apagar";
            case "BLINK":
                return "parpadear";
            default:
                MySimpleLogger.warn(loggerId, "Estado desconocido: " + estado + ", usando 'apagar'");
                return "apagar";
        }
    }
    
    /**
     * Envía un comando a un dispositivo esclavo
     */
    private void enviarComandoEsclavo(String esclavoId, String funcionId, String accion) throws MqttException {
        // Verificar conexión antes de enviar
        if (mqttClientPublisher == null || !mqttClientPublisher.isConnected()) {
            MySimpleLogger.warn(loggerId, "Cliente MQTT Publisher no conectado, no se puede enviar comando a " + esclavoId);
            return;
        }
        
        // Construir el topic de comando del esclavo
        String topicComando = "ina_08/dispositivo/" + esclavoId + "/funcion/" + funcionId + "/comandos";
        
        // Crear mensaje JSON
        try {
            JSONObject comando = new JSONObject();
            comando.put("accion", accion);
            
            // Enviar mensaje
            MqttMessage message = new MqttMessage(comando.toString().getBytes());
            message.setQos(0);
            message.setRetained(false);
            MySimpleLogger.info(loggerId, "before sending " + topicComando + ": " + comando.toString());
            mqttClientPublisher.publish(topicComando, message);
            MySimpleLogger.info(loggerId, "Comando enviado a " + topicComando + ": " + comando.toString());
        } catch (org.json.JSONException e) {
            MySimpleLogger.error(loggerId, "Error al crear mensaje JSON: " + e.getMessage());
            throw new MqttException(e);
        }
    }
    
    /**
     * Cierra la conexión MQTT
     */
    public void cerrar() {
        try {
            if (mqttClientSubscriber != null && mqttClientSubscriber.isConnected()) {
                mqttClientSubscriber.disconnect();
                mqttClientSubscriber.close();
                MySimpleLogger.info(loggerId, "Conexión MQTT Subscriber cerrada");
            }
            if (mqttClientPublisher != null && mqttClientPublisher.isConnected()) {
                mqttClientPublisher.disconnect();
                mqttClientPublisher.close();
                MySimpleLogger.info(loggerId, "Conexión MQTT Publisher cerrada");
            }
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al cerrar conexiones MQTT: " + e.getMessage());
        }
    }
    
    // Métodos de callback requeridos por MqttCallback
    @Override
    public void connectionLost(Throwable cause) {
        MySimpleLogger.error(loggerId, "Conexión MQTT perdida: " + (cause != null ? cause.getMessage() : "Desconocido"));
        if (cause != null) {
            cause.printStackTrace();
        }
        
        // Intentar reconectar automáticamente con retry
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                MySimpleLogger.info(loggerId, "Intentando reconectar... (intento " + (retryCount + 1) + "/" + maxRetries + ")");
                
                if (mqttClientSubscriber != null && !mqttClientSubscriber.isConnected()) {
                    // Configurar opciones de conexión
                    MqttConnectOptions connOpts = new MqttConnectOptions();
                    connOpts.setCleanSession(true);
                    connOpts.setKeepAliveInterval(60);
                    
                    mqttClientSubscriber.connect(connOpts);
                    
                    // Re-suscribirse al topic
                    String topicInfo = topicBase + "/dispositivo/" + maestroId + "/funcion/f1/info";
                    mqttClientSubscriber.subscribe(topicInfo, 0);
                    MySimpleLogger.info(loggerId, "Reconectado y re-suscrito al topic: " + topicInfo);
                    return; // Éxito, salir del bucle
                }
            } catch (MqttException e) {
                retryCount++;
                MySimpleLogger.error(loggerId, "Error al reconectar (intento " + retryCount + "): " + e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(2000); // Esperar 2 segundos antes del siguiente intento
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        MySimpleLogger.error(loggerId, "No se pudo reconectar después de " + maxRetries + " intentos");
    }
    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No necesitamos hacer nada aquí
    }
}
