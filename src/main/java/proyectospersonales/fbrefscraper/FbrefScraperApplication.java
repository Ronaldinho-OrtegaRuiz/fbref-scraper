package proyectospersonales.fbrefscraper;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import proyectospersonales.fbrefscraper.scraper.MatchStatsScraper;

@SpringBootApplication
public class FbrefScraperApplication implements CommandLineRunner {
    private final MatchStatsScraper fbrefScraper;

    public FbrefScraperApplication(MatchStatsScraper fbrefScraper) {
        this.fbrefScraper = fbrefScraper;
    }

    public static void main(String[] args) {
        SpringApplication.run(FbrefScraperApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        String equipoLocal = "Barcelona";
        String equipoVisitante = "Real Madrid";

        fbrefScraper.scrape(equipoLocal, equipoVisitante);
    }
}
