package com.energiai.energiaiapi.console;

import com.energiai.energiaiapi.dto.AnalisisRequest;
import com.energiai.energiaiapi.dto.AnalisisResponse;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.service.AnalisisService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interfaz de consola (version 1). Se activa solo con el perfil "cli":
 *   mvn spring-boot:run -Dspring-boot.run.profiles=cli
 *   (o)  java -jar target/energiai-api-*.jar --spring.profiles.active=cli
 *
 * Flujo: pide los 5 datos obligatorios, luego pregunta si se desean cargar datos
 * adicionales y, en caso afirmativo, solicita secuencialmente los 7 opcionales.
 * Reutiliza AnalisisService (misma logica que la API); no persiste (guardar=false).
 */
@Component
@Profile("cli")
public class ConsoleRunner implements CommandLineRunner {

    private final AnalisisService analisisService;
    private final Scanner scanner = new Scanner(System.in);

    public ConsoleRunner(AnalisisService analisisService) {
        this.analisisService = analisisService;
    }

    @Override
    public void run(String... args) {
        imprimirEncabezado();
        boolean continuar = true;
        while (continuar) {
            FacturaDTO factura = pedirFactura();
            AnalisisResponse respuesta = analisisService.analizar(
                    new AnalisisRequest(factura, null, false), null);
            imprimirResultado(respuesta);
            continuar = leerSiNo("\n¿Desea realizar otro analisis? (s/n): ");
        }
        System.out.println("\nGracias por usar EnergiAI. ¡Hasta luego!");
    }

    // ------------------------------------------------------------------
    // Captura del formulario
    // ------------------------------------------------------------------

    private FacturaDTO pedirFactura() {
        System.out.println("\n=================================================");
        System.out.println(" DATOS OBLIGATORIOS (5)");
        System.out.println("=================================================");

        Integer consumoKwh = leerEnteroPositivo("1) Consumo mensual (kWh): ", true);
        Boolean usoHorarioPico = leerSiNo("2) ¿Uso intensivo en horario pico? (s/n): ");
        Integer cantidadEquipos = leerEnteroPositivo("3) Cantidad de equipos electricos: ", true);
        String tipoInmueble = leerOpcion("4) Tipo de inmueble",
                List.of("Casa", "Departamento", "Local"), true);
        Double horasAltoConsumo = leerDecimalNoNegativo("5) Horas diarias de uso intensivo: ", true);

        // Opcionales
        Double areaInmueble = null;
        Integer numeroPersonas = null;
        Boolean tieneAire = null;
        Boolean tieneCalentador = null;
        Boolean tieneLed = null;
        String antiguedad = null;
        Double tarifaElectrica = null;

        if (leerSiNo("\n¿Desea brindar datos adicionales para un analisis mas preciso? (s/n): ")) {
            System.out.println("\n-------------------------------------------------");
            System.out.println(" DATOS ADICIONALES (7) - Enter para omitir cada uno");
            System.out.println("-------------------------------------------------");

            areaInmueble = leerDecimalNoNegativo("6) Area del inmueble (m2): ", false);
            numeroPersonas = leerEnteroPositivo("7) Numero de personas en el hogar: ", false);
            tieneAire = leerSiNoOpcional("8) ¿Tiene aire acondicionado? (s/n): ");
            tieneCalentador = leerSiNoOpcional("9) ¿Tiene calentador electrico? (s/n): ");
            tieneLed = leerSiNoOpcional("10) ¿Tiene iluminacion LED? (s/n): ");
            antiguedad = leerOpcion("11) Antiguedad de electrodomesticos",
                    List.of("Nueva", "Regular", "Antigua"), false);
            tarifaElectrica = leerDecimalNoNegativo("12) Tarifa individual del kWh: ", false);
        }

        return new FacturaDTO(consumoKwh, usoHorarioPico, cantidadEquipos, tipoInmueble, horasAltoConsumo,
                areaInmueble, numeroPersonas, tieneAire, tieneCalentador, tieneLed, antiguedad, tarifaElectrica);
    }

    // ------------------------------------------------------------------
    // Salida del resultado
    // ------------------------------------------------------------------

    private void imprimirResultado(AnalisisResponse r) {
        System.out.println("\n=================================================");
        System.out.println(" RESULTADO DEL ANALISIS");
        System.out.println("=================================================");
        System.out.println(" Categoria         : " + r.categoria());
        if (r.probabilidades() != null && !r.probabilidades().isEmpty()) {
            System.out.println(" Probabilidades    :");
            for (Map.Entry<String, Double> e : r.probabilidades().entrySet()) {
                System.out.printf("     - %-12s %.1f%%%n", e.getKey(), e.getValue() * 100);
            }
        }
        System.out.printf(" Costo mensual est.: %.2f%n", r.costoEstimadoMensual());
        if (r.indiceEficiencia() != null) {
            System.out.printf(" Indice eficiencia : %.3f%n", r.indiceEficiencia());
        } else {
            System.out.println(" Indice eficiencia : (requiere area y numero de personas)");
        }
        System.out.println(" Fuente            : " + r.fuenteClasificacion() + " (modelo " + r.modeloVersion() + ")");
        System.out.println(" Recomendaciones   :");
        for (String rec : r.recomendaciones()) {
            System.out.println("     * " + rec);
        }
        System.out.println("=================================================");
    }

    private void imprimirEncabezado() {
        System.out.println();
        System.out.println("#################################################");
        System.out.println("#            EnergiAI - Consola v1              #");
        System.out.println("#   Analisis de eficiencia energetica (ONE G9)  #");
        System.out.println("#################################################");
    }

    // ------------------------------------------------------------------
    // Helpers de lectura con validacion
    // ------------------------------------------------------------------

    /** Entero positivo. Si obligatorio=false, Enter vacio devuelve null (omitir). */
    private Integer leerEnteroPositivo(String prompt, boolean obligatorio) {
        while (true) {
            System.out.print(prompt);
            String entrada = scanner.nextLine().trim();
            if (entrada.isEmpty()) {
                if (!obligatorio) {
                    return null;
                }
                System.out.println("   [!] Este dato es obligatorio.");
                continue;
            }
            try {
                int valor = Integer.parseInt(entrada);
                if (valor <= 0) {
                    System.out.println("   [!] Debe ser un numero mayor a 0.");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                System.out.println("   [!] Ingrese un numero entero valido.");
            }
        }
    }

    /** Decimal >= 0. Acepta coma o punto. Si obligatorio=false, Enter vacio devuelve null. */
    private Double leerDecimalNoNegativo(String prompt, boolean obligatorio) {
        while (true) {
            System.out.print(prompt);
            String entrada = scanner.nextLine().trim().replace(',', '.');
            if (entrada.isEmpty()) {
                if (!obligatorio) {
                    return null;
                }
                System.out.println("   [!] Este dato es obligatorio.");
                continue;
            }
            try {
                double valor = Double.parseDouble(entrada);
                if (valor < 0) {
                    System.out.println("   [!] No puede ser negativo.");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                System.out.println("   [!] Ingrese un numero valido (ej: 4.5).");
            }
        }
    }

    /** Pregunta si/no obligatoria. */
    private boolean leerSiNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String e = scanner.nextLine().trim().toLowerCase();
            if (e.equals("s") || e.equals("si") || e.equals("sí")) {
                return true;
            }
            if (e.equals("n") || e.equals("no")) {
                return false;
            }
            System.out.println("   [!] Responda 's' o 'n'.");
        }
    }

    /** Pregunta si/no opcional: Enter vacio devuelve null. */
    private Boolean leerSiNoOpcional(String prompt) {
        while (true) {
            System.out.print(prompt);
            String e = scanner.nextLine().trim().toLowerCase();
            if (e.isEmpty()) {
                return null;
            }
            if (e.equals("s") || e.equals("si") || e.equals("sí")) {
                return true;
            }
            if (e.equals("n") || e.equals("no")) {
                return false;
            }
            System.out.println("   [!] Responda 's', 'n' o Enter para omitir.");
        }
    }

    /** Menu de opciones. Acepta el numero o el texto. Si obligatorio=false, Enter devuelve null. */
    private String leerOpcion(String titulo, List<String> opciones, boolean obligatorio) {
        while (true) {
            System.out.println(titulo + ":");
            for (int i = 0; i < opciones.size(); i++) {
                System.out.printf("     %d) %s%n", i + 1, opciones.get(i));
            }
            System.out.print("   Opcion: ");
            String entrada = scanner.nextLine().trim();
            if (entrada.isEmpty()) {
                if (!obligatorio) {
                    return null;
                }
                System.out.println("   [!] Debe elegir una opcion.");
                continue;
            }
            try {
                int idx = Integer.parseInt(entrada);
                if (idx >= 1 && idx <= opciones.size()) {
                    return opciones.get(idx - 1);
                }
            } catch (NumberFormatException ignored) {
                for (String op : opciones) {
                    if (op.equalsIgnoreCase(entrada)) {
                        return op;
                    }
                }
            }
            System.out.println("   [!] Opcion invalida.");
        }
    }
}
