package dispositivo.controlador;
// Ejercicio 10 - Controlador Semaforico

import dispositivo.utils.MySimpleLogger;

public class ControladorSemaforicoIniciador {
    
    public static void main(String[] args) {
        MySimpleLogger.info("ControladorSemaforicoIniciador", "=== INICIANDO CONTROLADOR SEMAFÓRICO ===");
        
        // Verificar argumentos
        if (args.length < 3) {
            MySimpleLogger.error("ControladorSemaforicoIniciador", 
                "Uso: java ControladorSemaforicoIniciador <mqtt-broker> <dispositivo1-id> <dispositivo2-id> [verde] [amarillo] [rojo]");
            MySimpleLogger.error("ControladorSemaforicoIniciador", 
                "Ejemplo: java ControladorSemaforicoIniciador tcp://tambori.dsic.upv.es:1883 ttmi050 ttmi051 30 5 3");
            System.exit(1);
        }
        
        String mqttBroker = args[0];
        String dispositivo1Id = args[1];
        String dispositivo2Id = args[2];
        
        // Tiempos opcionales (por defecto: verde=30s, amarillo=5s, rojo=3s)
        int tiempoVerde = 30;
        int tiempoAmarillo = 5;
        int tiempoRojo = 3;
        
        if (args.length >= 4) {
            try {
                tiempoVerde = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                MySimpleLogger.warn("ControladorSemaforicoIniciador", "Tiempo verde inválido, usando valor por defecto: 30s");
            }
        }
        
        if (args.length >= 5) {
            try {
                tiempoAmarillo = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                MySimpleLogger.warn("ControladorSemaforicoIniciador", "Tiempo amarillo inválido, usando valor por defecto: 5s");
            }
        }
        
        if (args.length >= 6) {
            try {
                tiempoRojo = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                MySimpleLogger.warn("ControladorSemaforicoIniciador", "Tiempo rojo inválido, usando valor por defecto: 3s");
            }
        }
        
        // Mostrar configuración
        MySimpleLogger.info("ControladorSemaforicoIniciador", "Configuración:");
        MySimpleLogger.info("ControladorSemaforicoIniciador", "  MQTT Broker: " + mqttBroker);
        MySimpleLogger.info("ControladorSemaforicoIniciador", "  Dispositivo 1 (Calle A): " + dispositivo1Id);
        MySimpleLogger.info("ControladorSemaforicoIniciador", "  Dispositivo 2 (Calle B): " + dispositivo2Id);
        MySimpleLogger.info("ControladorSemaforicoIniciador", "  Tiempo Verde: " + tiempoVerde + "s");
        MySimpleLogger.info("ControladorSemaforicoIniciador", "  Tiempo Amarillo: " + tiempoAmarillo + "s");
        MySimpleLogger.info("ControladorSemaforicoIniciador", "  Tiempo Rojo: " + tiempoRojo + "s");
        
        // Crear y configurar el controlador
        final ControladorSemaforico controlador;
        
        try {
            controlador = new ControladorSemaforico(mqttBroker, dispositivo1Id, dispositivo2Id);
            controlador.configurarTiempos(tiempoVerde, tiempoAmarillo, tiempoRojo);
            
            // Agregar shutdown hook para cerrar correctamente
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MySimpleLogger.info("ControladorSemaforicoIniciador", "Cerrando controlador...");
                controlador.cerrar();
            }));
            
            // Iniciar el controlador
            controlador.iniciar();
            
        } catch (Exception e) {
            MySimpleLogger.error("ControladorSemaforicoIniciador", "Error fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
