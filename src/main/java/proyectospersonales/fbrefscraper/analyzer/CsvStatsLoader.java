package proyectospersonales.fbrefscraper.analyzer;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvStatsLoader {

    public List<double[]> loadAllStats(String teamName, String mode) {
        String path = buildCsvPath(teamName, mode);
        List<double[]> stats = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
            String header = reader.readLine(); // omitir encabezado
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);

                double[] row = new double[] {
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
        return "src/main/java/data/" + fileSafeName + "-" + mode + ".csv";
    }
}
