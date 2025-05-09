package proyectospersonales.fbrefscraper.analyzer.predictors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import proyectospersonales.fbrefscraper.analyzer.average.StatsAnalyzer;
import proyectospersonales.fbrefscraper.analyzer.loader.CsvStatsLoader;

import java.util.List;

@Service
public class MatchPredictor {

    private final CsvStatsLoader csvStatsLoader;
    private final StatsAnalyzer statsAnalyzer;

    @Autowired
    public MatchPredictor(CsvStatsLoader csvStatsLoader, StatsAnalyzer statsAnalyzer) {
        this.csvStatsLoader = csvStatsLoader;
        this.statsAnalyzer = statsAnalyzer;
    }

    public void predictors(String equipoLocal, String equipoVisitante) {
        predecirEstadisticasPartido(equipoLocal, equipoVisitante);
    }

    private void predecirEstadisticasPartido(String equipoLocal, String equipoVisitante) {
        List<double[]> statsLocal = csvStatsLoader.loadAllStats(equipoLocal, "local");
        List<double[]> statsVisitante = csvStatsLoader.loadAllStats(equipoVisitante, "visitante");

        double[] promediosLocal = statsAnalyzer.analyzeAndPrintStats(statsLocal, equipoLocal + " (local)");
        double[] promediosVisitante = statsAnalyzer.analyzeAndPrintStats(statsVisitante, equipoVisitante + " (visitante)");

        double precisionLocal = promediosLocal[5] / 100.0;
        double precisionVisitante = promediosVisitante[5] / 100.0;

        double dificultadRivalLocal = promediosVisitante[10]; // Dificultad del rival para el local (xG del visitante)
        double dificultadRivalVisitante = promediosLocal[10]; // Dificultad del rival para el visitante (xG del local)

        // Predicción de goles
        double golesLocal = estimarGoles(promediosLocal[0], promediosLocal[1], dificultadRivalVisitante, precisionLocal);
        double golesVisitante = estimarGoles(promediosVisitante[0], promediosVisitante[1], dificultadRivalLocal, precisionVisitante);

        // Predicción de tarjetas amarillas
        double tarjetasLocal = estimarTarjetas(
                promediosLocal[6], promediosLocal[8],
                promediosVisitante[12], promediosVisitante[13],
                dificultadRivalVisitante
        );

        double tarjetasVisitante = estimarTarjetas(
                promediosVisitante[6], promediosVisitante[8],
                promediosLocal[12], promediosLocal[13],
                dificultadRivalLocal
        );

        // Predicción de tiros de esquina
        double cornersLocal = estimarTirosDeEsquina(
                promediosLocal[7], promediosLocal[3],
                promediosVisitante[14], promediosVisitante[3],
                dificultadRivalVisitante
        );

        double cornersVisitante = estimarTirosDeEsquina(
                promediosVisitante[7], promediosVisitante[3],
                promediosLocal[14], promediosLocal[3],
                dificultadRivalLocal
        );

        // Predicción de disparos a puerta
        double disparosPuertaLocal = estimarDisparosAPuerta(
                promediosLocal[4], promediosLocal[3],
                promediosVisitante[4], promediosVisitante[3],
                dificultadRivalVisitante
        );

        double disparosPuertaVisitante = estimarDisparosAPuerta(
                promediosVisitante[4], promediosVisitante[3],
                promediosLocal[4], promediosLocal[3],
                dificultadRivalLocal
        );

        // Imprimir resultados
        System.out.println("Predicción de goles:");
        System.out.printf("- %s: %.2f goles%n", equipoLocal, golesLocal);
        System.out.printf("- %s: %.2f goles%n", equipoVisitante, golesVisitante);
        System.out.println("¿Ambos equipos marcan?: " + ((golesLocal >= 1.0 && golesVisitante >= 1.0) ? "Sí" : "No"));

        System.out.println("\nPredicción de tarjetas amarillas:");
        System.out.printf("- %s: %.2f tarjetas%n", equipoLocal, tarjetasLocal);
        System.out.printf("- %s: %.2f tarjetas%n", equipoVisitante, tarjetasVisitante);
        double totalTarjetas = tarjetasLocal + tarjetasVisitante;
        System.out.printf("Total estimado de tarjetas: %.2f%n", totalTarjetas);
        System.out.println("¿Más de 4.5 tarjetas en el partido?: " + (totalTarjetas > 4.5 ? "Sí" : "No"));

        System.out.println("\nPredicción de tiros de esquina:");
        System.out.printf("- %s: %.2f tiros de esquina%n", equipoLocal, cornersLocal);
        System.out.printf("- %s: %.2f tiros de esquina%n", equipoVisitante, cornersVisitante);
        double totalCorners = cornersLocal + cornersVisitante;
        System.out.printf("Total estimado de tiros de esquina: %.2f%n", totalCorners);
        System.out.println("¿Más de 8.5 tiros de esquina en el partido?: " + (totalCorners > 8.5 ? "Sí" : "No"));

        System.out.println("\nPredicción de disparos a puerta:");
        System.out.printf("- %s: %.2f disparos a puerta%n", equipoLocal, disparosPuertaLocal);
        System.out.printf("- %s: %.2f disparos a puerta%n", equipoVisitante, disparosPuertaVisitante);
        double totalDisparosPuerta = disparosPuertaLocal + disparosPuertaVisitante;
        System.out.printf("Total estimado de disparos a puerta: %.2f%n", totalDisparosPuerta);
        System.out.println("¿Más de 7.5 disparos a puerta en el partido?: " + (totalDisparosPuerta > 7.5 ? "Sí" : "No"));
    }

    private double estimarGoles(double promedioGoles, double promedioXG, double dificultadRival, Double precisionEquipo) {
        final double PRECISION_LIGA = 0.3217;
        double ajusteDificultad = 1.0 - (dificultadRival - 1) * 0.05;

        double base;
        if (promedioXG > 0.0) {
            base = (promedioGoles + promedioXG) / 2;
        } else {
            double precision = (precisionEquipo != null) ? precisionEquipo : PRECISION_LIGA;
            base = promedioGoles * (0.5 + precision);
        }

        return Math.max(0, base * ajusteDificultad);
    }

    private double estimarTarjetas(
            double tarjetasEquipo, double faltasEquipo,
            double tarjetasRivalRecibidas, double faltasRivalRecibidas,
            double dificultadRival
    ) {
        double numerador = (tarjetasEquipo * faltasEquipo) + (tarjetasRivalRecibidas * faltasRivalRecibidas);
        double denominador = faltasEquipo + faltasRivalRecibidas;

        // Ajuste por dificultad del rival
        double ajusteDificultad = 1.0 + (dificultadRival - 1) * 0.03;
        return (denominador != 0) ? numerador / denominador * ajusteDificultad : 0.0;
    }

    private double estimarTirosDeEsquina(
            double cornersPropios, double disparosPropios,
            double cornersRival, double disparosRival,
            double dificultadRival
    ) {
        double factorPropio = cornersPropios + (disparosPropios * 0.1);
        double factorRival = cornersRival + (disparosRival * 0.05);

        // Ajuste por dificultad del rival
        double ajusteDificultad = 1.0 - (dificultadRival - 1) * 0.04;
        return (factorPropio + factorRival) / 2.0 * ajusteDificultad;
    }

    private double estimarDisparosAPuerta(
            double disparosPuertaPropios, double disparosTotalesPropios,
            double disparosPuertaRivalRecibidos, double disparosTotalesRival,
            double dificultadRival
    ) {
        double tasaPropia = disparosTotalesPropios > 0 ? disparosPuertaPropios / disparosTotalesPropios : 0;
        double tasaRival = disparosTotalesRival > 0 ? disparosPuertaRivalRecibidos / disparosTotalesRival : 0;

        double tasaPromedio = (tasaPropia + tasaRival) / 2;

        // Ajuste por dificultad del rival
        double ajusteDificultad = 1.0 - (dificultadRival - 1) * 0.05;
        return disparosTotalesPropios * tasaPromedio * ajusteDificultad;
    }
}
