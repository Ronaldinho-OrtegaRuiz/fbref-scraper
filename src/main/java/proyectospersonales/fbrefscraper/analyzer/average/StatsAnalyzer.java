package proyectospersonales.fbrefscraper.analyzer.average;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatsAnalyzer {

    public double[] analyzeAndPrintStats(List<double[]> stats, String teamName) {
        double[] averages = calculateAverages(stats);
        System.out.println("Promedios " + teamName + ":");
        printAverages(averages);
        return averages;
    }


    private double[] calculateAverages(List<double[]> list) {
        if (list.isEmpty()) return new double[0];

        int cols = list.get(0).length;
        double[] sum = new double[cols];

        for (double[] row : list) {
            for (int i = 0; i < cols; i++) {
                sum[i] += row[i];
            }
        }

        for (int i = 0; i < cols; i++) {
            sum[i] /= list.size();
        }

        return sum;
    }

    private void printAverages(double[] avg) {
        String[] labels = {
                "Goles", "xG", "Posesión", "Disparos Totales", "Disparos a Puerta", "Precisión (%)",
                "Tarjetas Amarillas", "Tiros de Esquina", "Faltas", "Offside",
                "Goles Recibidos", "xG Rival", "T. Amarillas Rival", "Faltas Rival",
                "Tiros Esquina Rival", "Offside Rival"
        };

        for (int i = 0; i < avg.length; i++) {
            System.out.printf("%-25s: %.2f%n", labels[i], avg[i]);
        }
    }
}
