package proyectospersonales.fbrefscraper;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import proyectospersonales.fbrefscraper.analyzer.CsvStatsLoader;
import proyectospersonales.fbrefscraper.analyzer.StatsAnalyzer;
import proyectospersonales.fbrefscraper.scraper.LeagueStatsScraper;
import proyectospersonales.fbrefscraper.scraper.MatchStatsScraper;

import java.util.List;

@SpringBootApplication
public class FbrefScraperApplication implements CommandLineRunner {

    private final MatchStatsScraper matchStatsScraper;
    private final LeagueStatsScraper leagueStatsScraper;
    private final CsvStatsLoader csvStatsLoader;
    private final StatsAnalyzer statsAnalyzer;

    public FbrefScraperApplication(
            MatchStatsScraper matchStatsScraper,
            LeagueStatsScraper leagueStatsScraper,
            CsvStatsLoader csvStatsLoader,
            StatsAnalyzer statsAnalyzer
    ) {
        this.matchStatsScraper = matchStatsScraper;
        this.leagueStatsScraper = leagueStatsScraper;
        this.csvStatsLoader = csvStatsLoader;
        this.statsAnalyzer = statsAnalyzer;
    }

    public static void main(String[] args) {
        SpringApplication.run(FbrefScraperApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String localTeam = "Valladolid";
        String awayTeam = "Barcelona";
        String localLeague = "Spanish La Liga";
        String awayLeague = "Spanish La Liga";
        String competition = "Spanish La Liga";

        List<double[]> localStats = csvStatsLoader.loadAllStats(localTeam, "local");
        List<double[]> awayStats = csvStatsLoader.loadAllStats(awayTeam, "visitante");

        statsAnalyzer.analyzeAndPrintStats(localStats, localTeam + " (local)");

        statsAnalyzer.analyzeAndPrintStats(awayStats, awayTeam + " (visitante)");

    }
}
