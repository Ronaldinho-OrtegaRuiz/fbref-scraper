package proyectospersonales.fbrefscraper;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import proyectospersonales.fbrefscraper.analyzer.loader.CsvStatsLoader;
import proyectospersonales.fbrefscraper.analyzer.average.StatsAnalyzer;
import proyectospersonales.fbrefscraper.analyzer.predictors.MatchPredictor;
import proyectospersonales.fbrefscraper.scraper.LeagueStatsScraper;
import proyectospersonales.fbrefscraper.scraper.MatchStatsScraper;

import java.util.List;

@SpringBootApplication
public class FbrefScraperApplication implements CommandLineRunner {

    private final MatchStatsScraper matchStatsScraper;
    private final LeagueStatsScraper leagueStatsScraper;
    private final CsvStatsLoader csvStatsLoader;
    private final StatsAnalyzer statsAnalyzer;
    private final MatchPredictor matchPredictor;

    public FbrefScraperApplication(
            MatchStatsScraper matchStatsScraper,
            LeagueStatsScraper leagueStatsScraper,
            CsvStatsLoader csvStatsLoader,
            StatsAnalyzer statsAnalyzer,
            MatchPredictor matchPredictor
    ) {
        this.matchStatsScraper = matchStatsScraper;
        this.leagueStatsScraper = leagueStatsScraper;
        this.csvStatsLoader = csvStatsLoader;
        this.statsAnalyzer = statsAnalyzer;
        this.matchPredictor = matchPredictor;
    }

    public static void main(String[] args) {
        SpringApplication.run(FbrefScraperApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String localTeam = "Las Palmas";
        String awayTeam = "Rayo Vallecano";
        String localLeague = "Spanish La Liga";
        String awayLeague = "Spanish La Liga";

        leagueStatsScraper.scrape(localLeague, awayLeague);
        //matchStatsScraper.scrape(localTeam, localLeague, awayTeam, awayLeague);

        //List<double[]> localStats = csvStatsLoader.loadAllStats(localTeam, "local");
        //List<double[]> awayStats = csvStatsLoader.loadAllStats(awayTeam, "visitante");
        //statsAnalyzer.analyzeAndPrintStats(localStats, localTeam + " (local)");
        //statsAnalyzer.analyzeAndPrintStats(awayStats, awayTeam + " (visitante)");
        //matchPredictor.predictors(localTeam, awayTeam);

    }
}
