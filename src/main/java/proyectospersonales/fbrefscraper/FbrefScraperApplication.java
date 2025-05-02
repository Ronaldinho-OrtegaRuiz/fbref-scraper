package proyectospersonales.fbrefscraper;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import proyectospersonales.fbrefscraper.scraper.LeagueStatsScraper;
import proyectospersonales.fbrefscraper.scraper.MatchStatsScraper;

@SpringBootApplication
public class FbrefScraperApplication implements CommandLineRunner {
    private final MatchStatsScraper matchStatsScraper;
    private final LeagueStatsScraper leagueStatsScraper;

    public FbrefScraperApplication(MatchStatsScraper matchStatsScraper, LeagueStatsScraper leagueStatsScraper) {
        this.matchStatsScraper = matchStatsScraper;
        this.leagueStatsScraper = leagueStatsScraper;

    }

    public static void main(String[] args) {
        SpringApplication.run(FbrefScraperApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        String localTeam = "Real Valladolid";
        String awayTeam = "Barcelona";
        String localLeague = "Spanish La Liga";
        String awayLeague = "Spanish La Liga";

        //leagueStatsScraper.scrape(localLeague, awayLeague);
        matchStatsScraper.scrape(localTeam, localLeague, awayTeam, awayLeague);

    }
}
