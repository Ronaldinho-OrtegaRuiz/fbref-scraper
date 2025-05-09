package proyectospersonales.fbrefscraper.analyzer.loader;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CsvStatsLoader {

    public List<double[]> loadAllStats(String teamName, String mode) {
        String path = buildCsvPath(teamName, mode);
        List<double[]> stats = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
            String header = reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                boolean hasXg = parts.length == 20;

                double[] row;
                if (hasXg) {
                    row = new double[] {
                            Double.parseDouble(parts[0]),   // Goles
                            Double.parseDouble(parts[1]),   // xG
                            Double.parseDouble(parts[2]),   // Posesión
                            Double.parseDouble(parts[3]),   // Disparos Totales
                            Double.parseDouble(parts[4]),   // Disparos a Puerta
                            Double.parseDouble(parts[5]),   // Precisión
                            Double.parseDouble(parts[6]),   // Tarjetas Amarillas
                            Double.parseDouble(parts[7]),   // Tiros de Esquina
                            Double.parseDouble(parts[8]),   // Faltas
                            Double.parseDouble(parts[9]),   // Offside
                            Double.parseDouble(parts[11]),  // Goles Recibidos
                            Double.parseDouble(parts[12]),  // xG Rival
                            Double.parseDouble(parts[13]),  // Tarjetas Amarillas Rival
                            Double.parseDouble(parts[15]),  // Faltas Rival
                            Double.parseDouble(parts[16]),  // Tiros de Esquina Rival
                            Double.parseDouble(parts[17])   // Offside Rival
                    };
                } else {
                    row = new double[] {
                            Double.parseDouble(parts[0]),   // Goles
                            0.0,                            // xG no disponible
                            Double.parseDouble(parts[1]),   // Posesión
                            Double.parseDouble(parts[2]),   // Disparos Totales
                            Double.parseDouble(parts[3]),   // Disparos a Puerta
                            Double.parseDouble(parts[4]),   // Precisión
                            Double.parseDouble(parts[5]),   // Tarjetas Amarillas
                            Double.parseDouble(parts[6]),   // Tiros de Esquina
                            Double.parseDouble(parts[7]),   // Faltas
                            Double.parseDouble(parts[8]),   // Offside
                            Double.parseDouble(parts[10]),  // Goles Recibidos
                            0.0,                            // xG Rival no disponible
                            Double.parseDouble(parts[11]),  // Tarjetas Amarillas Rival
                            Double.parseDouble(parts[13]),  // Faltas Rival
                            Double.parseDouble(parts[14]),  // Tiros de Esquina Rival
                            Double.parseDouble(parts[15])   // Offside Rival
                    };
                }

                stats.add(row);
            }

        } catch (IOException e) {
            System.err.println("Error leyendo CSV: " + path);
            e.printStackTrace();
        }

        return stats;
    }


    private String buildCsvPath(String teamName, String mode) {
        String fileSafeName = teamName.toLowerCase().replace(" ", "_");
        String baseDir = "data/";

        String[] palabras = teamName.toLowerCase().split(" ");
        String primera = palabras[0];
        String ultima = palabras[palabras.length - 1];

        Set<String> variantesSet = new LinkedHashSet<>();

        // Variante completa
        variantesSet.add(fileSafeName + "-" + mode + ".csv");

        // Primera palabra
        variantesSet.add(primera + "-" + mode + ".csv");
        variantesSet.add("ad_" + primera + "-" + mode + ".csv");

        // Última palabra
        variantesSet.add(ultima + "-" + mode + ".csv");
        variantesSet.add("ad_" + ultima + "-" + mode + ".csv");

        for (String nombreArchivo : variantesSet) {
            File archivo = new File(baseDir + nombreArchivo);
            if (archivo.exists()) {
                return archivo.getPath();
            }
        }

        throw new RuntimeException("No se encontró archivo CSV para " + teamName + " (" + mode + ")");
    }


}
