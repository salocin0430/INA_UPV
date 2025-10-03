package dispositivo.controlador;
// Ejercicio 11 - Controlador Maestro-Esclavos Iniciador

import dispositivo.utils.MySimpleLogger;

public class ControladorMaestroEsclavosIniciador {
    
    public static void main(String[] args) {
        MySimpleLogger.info("ControladorMaestroEsclavosIniciador", "=== INICIANDO CONTROLADOR MAESTRO-ESCLAVOS ===");
        
        // Verificar argumentos
        if (args.length < 3) {
            MySimpleLogger.error("ControladorMaestroEsclavosIniciador", 
                "Uso: java ControladorMaestroEsclavosIniciador <mqtt-broker> <maestro-id> <esclavo1-id> [esclavo2-id] [esclavo3-id] ...");
            MySimpleLogger.error("ControladorMaestroEsclavosIniciador", 
                "Ejemplo: java ControladorMaestroEsclavosIniciador tcp://tambori.dsic.upv.es:1883 ttmi050 ttmi051 ttmi052");
            System.exit(1);
        }
        
        String mqttBroker = args[0];
        String maestroId = args[1];
        
        // Obtener IDs de esclavos (todos los argumentos después del maestro)
        String[] esclavosIds = new String[args.length - 2];
        for (int i = 2; i < args.length; i++) {
            esclavosIds[i - 2] = args[i];
        }
        
        // Mostrar configuración
        MySimpleLogger.info("ControladorMaestroEsclavosIniciador", "Configuración:");
        MySimpleLogger.info("ControladorMaestroEsclavosIniciador", "  MQTT Broker: " + mqttBroker);
        MySimpleLogger.info("ControladorMaestroEsclavosIniciador", "  Maestro: " + maestroId);
        MySimpleLogger.info("ControladorMaestroEsclavosIniciador", "  Esclavos: " + String.join(", ", esclavosIds));
        
        // Crear y configurar el controlador
        final ControladorMaestroEsclavos controlador;
        
        try {
            controlador = new ControladorMaestroEsclavos(mqttBroker, maestroId, esclavosIds);
            
            // Agregar shutdown hook para cerrar correctamente
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MySimpleLogger.info("ControladorMaestroEsclavosIniciador", "Cerrando controlador...");
                controlador.cerrar();
            }));
            
            // Iniciar el controlador
            controlador.iniciar();
            
        } catch (Exception e) {
            MySimpleLogger.error("ControladorMaestroEsclavosIniciador", "Error fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
