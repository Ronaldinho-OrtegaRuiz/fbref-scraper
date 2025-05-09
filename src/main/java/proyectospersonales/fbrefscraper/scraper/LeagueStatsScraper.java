package proyectospersonales.fbrefscraper.scraper;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import proyectospersonales.fbrefscraper.config.WebDriverManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeagueStatsScraper {

    private final WebDriver driver;

    public LeagueStatsScraper(WebDriverManager webDriverManager) {
        this.driver = webDriverManager.getDriver();
    }

    public void scrape(String localLeague, String awayLeague) {
        System.out.println("Página actual: " + driver.getCurrentUrl());

        if (localLeague.equals(awayLeague)) {
            System.out.println("Buscando liga: " + localLeague);
            searchLeague(localLeague);
        } else {
            System.out.println("Buscando liga local: " + localLeague);
            searchLeague(localLeague);
            System.out.println("Buscando liga visitante: " + awayLeague);
            searchLeague(awayLeague);
        }

    }

    private void searchLeague(String leagueName) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("search")));
        searchInput.clear();
        searchInput.sendKeys(leagueName);
        searchInput.sendKeys(Keys.ENTER);

        if (leagueName.equals("Italian Serie A") || leagueName.equals("English Premier League") || leagueName.equals("German Bundesliga") || leagueName.equals("Dutch Eredivisie")) {
            System.out.println("Buscando liga: " + leagueName);
            selectFirstLeagueInSearch(leagueName);
        }

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table[id^='results'][id$='_overall']")));

        searchTable(leagueName);
    }


    private void searchTable(String leagueName) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            System.out.println("Buscando la tabla de partidos...");

            WebElement leagueTable = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("table[id^='results'][id$='_overall']"))
            );

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", leagueTable);

            System.out.println("Tabla de partidos encontrada.");

            List<WebElement> rows = leagueTable.findElements(By.cssSelector("tbody > tr"))
                    .stream()
                    .filter(fila -> !fila.getAttribute("class").contains("thead"))
                    .filter(fila -> !fila.findElements(By.cssSelector("td")).isEmpty())
                    .collect(Collectors.toList());

            System.out.println("Filas encontradas: " + rows.size());

            extractDataLeagueTable(driver, leagueTable, leagueName);

        } catch (Exception e) {
            System.out.println("No se encontró la tabla de partidos. Verifique que la liga esté activa.");
            System.out.println("Error: " + e.getMessage());
        }
    }


    private void extractDataLeagueTable(WebDriver driver, WebElement leagueTable, String leagueName) {
        List<WebElement> filas = leagueTable.findElements(By.cssSelector("tbody > tr"));

        String nameFile = "league_data/" + leagueName + ".csv";
        clearFile(nameFile);

        for (int i = 0; i < filas.size(); i++) {
            WebElement fila = filas.get(i);
            try {
                int position = Integer.parseInt(fila.findElement(By.cssSelector("th[data-stat='rank']")).getText());
                String team = fila.findElement(By.cssSelector("td[data-stat='team'] a")).getText();
                int playedGames = Integer.parseInt(fila.findElement(By.cssSelector("td[data-stat='games']")).getText());
                int differenceGoals = Integer.parseInt(fila.findElement(By.cssSelector("td[data-stat='goal_diff']")).getText());
                int points = Integer.parseInt(fila.findElement(By.cssSelector("td[data-stat='points']")).getText());

                if (i == 0) {
                    saveInCSV(nameFile, "Posición", "Equipo", "PJ", "Dif. Goles", "Puntos");
                }

                saveInCSV(nameFile, String.valueOf(position), team, String.valueOf(playedGames),
                        String.valueOf(differenceGoals), String.valueOf(points));

            } catch (Exception e) {
                System.out.println("Fila con error al procesar, puede no ser una fila válida.");
            }
        }
    }


    private void clearFile(String nameFile) {
        try {
            Files.write(Paths.get(nameFile), new byte[0], StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.err.println("Error limpiando el archivo CSV: " + e.getMessage());
        }
    }


    private void saveInCSV(String nameFile, String... values) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nameFile, true))) {
            writer.write(String.join(",", values));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error escribiendo en el archivo CSV: " + e.getMessage());
        }
    }


    private void selectFirstLeagueInSearch(String league) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        List<WebElement> leagues = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(".search-item-name a")));

        WebElement selectedLeague;

        if ("German Bundesliga".equalsIgnoreCase(league)  || "Brazilian Série A".equalsIgnoreCase(league)) {
            selectedLeague = leagues.get(1);
        } else {
            selectedLeague = leagues.get(0);
        }

        String leagueURL = selectedLeague.getAttribute("href");
        System.out.println("Liga seleccionada: " + selectedLeague.getText());
        System.out.println("URL de la liga: " + leagueURL);

        driver.get(leagueURL);
    }

}
