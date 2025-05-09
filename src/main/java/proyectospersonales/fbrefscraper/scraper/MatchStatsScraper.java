package proyectospersonales.fbrefscraper.scraper;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import proyectospersonales.fbrefscraper.config.WebDriverManager;
import proyectospersonales.fbrefscraper.util.PopupHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MatchStatsScraper {
    private final WebDriver driver;

    public MatchStatsScraper(WebDriverManager webDriverManager) {
        this.driver = webDriverManager.getDriver();
    }

    public void scrape(String localTeam, String localLeague, String awayTeam, String awayLeague) {
        System.out.println("Página actual: " + driver.getCurrentUrl());

        searchTeam(localTeam, "Local", localLeague);
        System.out.println("Búsqueda realizada para los partidos de local del: " + localTeam);

        searchTeam(awayTeam, "Visitante", awayLeague);
        System.out.println("Búsqueda realizada para los partidos de visitantes del: " + awayTeam);
    }

    private void searchTeam(String team, String hostCondition, String league) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
        WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("search")));

        searchInput.clear();
        searchInput.sendKeys(team);
        searchInput.sendKeys(Keys.ENTER);
        selectFirstTeamInSearch(hostCondition, league);
    }

    private void selectFirstTeamInSearch(String siteCondition, String league) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        WebElement firstTeam = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".search-item-name a")));

        String teamURL = firstTeam.getAttribute("href");
        System.out.println("Primer equipo encontrado: " + firstTeam.getText());
        System.out.println("URL del equipo: " + teamURL);

        driver.get(teamURL);

        extractMatchReports(siteCondition, league);
    }


    public void extractMatchReports(String siteCondition, String league) {
        System.out.println("Iniciando extracción de informes de partido para la sede: " + siteCondition);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        System.out.println("Buscando la tabla de partidos...");
        WebElement matchsTable = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table#matchlogs_for.stats_table")));
        System.out.println("Tabla de partidos encontrada.");

        List<WebElement> rows = matchsTable.findElements(By.cssSelector("tbody > tr"))
                .stream()
                .filter(fila -> !fila.getAttribute("class").contains("thead"))
                .filter(fila -> !fila.findElements(By.cssSelector("td")).isEmpty())
                .collect(Collectors.toList());

        System.out.println("Filas de la tabla con datos reales encontradas: " + rows.size());

        int counter = 0;

        for (int i = 0; i < rows.size(); i++) {
            WebElement row = rows.get(i);
            System.out.println("Procesando row #" + (i + 1));

            try {
                System.out.println("Buscando celda de sede en la row...");
                WebElement siteTd = row.findElement(By.cssSelector("td[data-stat='venue']"));
                String site = siteTd.getText().trim();
                System.out.println("Sede encontrada: " + site);

                if (!site.equalsIgnoreCase(siteCondition)) {
                    System.out.println("Sede no coincide con la condición. Saltando row.");
                    continue;
                }

                System.out.println("Buscando enlace al informe del partido en la row...");
                WebElement linkReport = row.findElement(By.xpath(".//td[@data-stat='match_report']/a"));

                String textInLink = linkReport.getText().trim();
                if (!textInLink.equalsIgnoreCase("Informe del partido")) {
                    System.out.println("El enlace no es un informe de partido (es: '" + textInLink + "'). Saltando row.");
                    continue;
                }

                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'center'});", linkReport);
                Thread.sleep(2000);
                wait.until(ExpectedConditions.elementToBeClickable(linkReport));

                try {
                    Actions actions = new Actions(driver);
                    actions.moveToElement(linkReport).click().perform();
                    System.out.println("Enlace clickeado con éxito.");
                } catch (Exception e) {
                    System.err.println("No se pudo hacer clic en el enlace: " + e.getMessage());
                    continue;
                }

                System.out.println("Entrando en el informe del partido: " + driver.getCurrentUrl());


                extractDataFromTheReport(siteCondition, league);
                Thread.sleep(5000);
                counter++;
                System.out.println("Informe #" + counter + " extraído.");

                System.out.println("Volviendo a la tabla de partidos...");
                driver.navigate().back();
                Thread.sleep(2000);

                matchsTable = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table#matchlogs_for.stats_table")));
                rows = matchsTable.findElements(By.cssSelector("tbody > tr"))
                        .stream()
                        .filter(f -> !f.getAttribute("class").contains("thead"))
                        .filter(f -> !f.findElements(By.cssSelector("td")).isEmpty())
                        .collect(Collectors.toList());

                System.out.println("Reubicada la tabla de partidos. Filas encontradas: " + rows.size());

            } catch (NoSuchElementException e) {
                System.out.println("Fila sin dato de sede o enlace. Saltando.");
            } catch (Exception e) {
                System.err.println("Error procesando una row: " + e.getMessage());
            }
        }

        System.out.println("Proceso de extracción de informes completado. Total extraídos: " + counter);
    }

    private void extractDataFromTheReport(String siteCondition, String league) {
        try {
            WebElement scorebox = driver.findElement(By.cssSelector("div.scorebox"));

            List<WebElement> scoresDivs = scorebox.findElements(By.cssSelector("div.scores"));
            List<WebElement> teamNames = scorebox.findElements(By.cssSelector("strong > a"));

            if (scoresDivs.size() < 2 || teamNames.size() < 2) {
                System.err.println("No se encontraron los divs necesarios para extraer la info.");
                return;
            }

            int indexTeam = siteCondition.equalsIgnoreCase("Local") ? 0 : 1;
            int indexRivalTeam = 1 - indexTeam;

            String team = teamNames.get(indexTeam).getText().trim();
            String rival = teamNames.get(indexRivalTeam).getText().trim();

            int scoredGoals = Integer.parseInt(scoresDivs.get(indexTeam).findElement(By.cssSelector("div.score")).getText().trim());
            int concededGoals = Integer.parseInt(scoresDivs.get(indexRivalTeam).findElement(By.cssSelector("div.score")).getText().trim());
            String bothScored = (scoredGoals > 0 && concededGoals > 0) ? "sí" : "no";

            // xG
            List<WebElement> xgDivs = driver.findElements(By.cssSelector("div.score_xg"));
            double xgScored = xgDivs.size() > indexTeam ? Double.parseDouble(xgDivs.get(indexTeam).getText().trim()) : -1;
            double xgConceded = xgDivs.size() > indexRivalTeam ? Double.parseDouble(xgDivs.get(indexRivalTeam).getText().trim()) : -1;

            // Posesión
            List<WebElement> possessionDivs = driver.findElements(By.xpath("//tr[th[contains(text(),'Posesión del balón')]]/following-sibling::tr[1]/td/div/div/strong"));
            double possession = possessionDivs.size() > indexTeam ? Double.parseDouble(possessionDivs.get(indexTeam).getText().replace("%", "").trim()) : -1;

            // Disparos
            int totalShots = -1;
            int shotsOnTarget = -1;
            int accuracyRate = -1;
            try {
                WebElement shotsRow = driver.findElement(By.xpath("//tr[th[contains(text(),'Disparos a puerta')]]/following-sibling::tr[1]"));
                List<WebElement> tds = shotsRow.findElements(By.tagName("td"));

                if (tds.size() >= 2) {
                    String shotText = tds.get(indexTeam).findElement(By.xpath(".//div[1]/div[1]")).getText();

                    // Primer patrón: "5 of 13 — 38%" (formato antiguo)
                    Pattern pattern1 = Pattern.compile("(\\d+)\\s+of\\s+(\\d+)\\s+—\\s+(\\d+)%");

                    // Segundo patrón: "38% — 5 of 13" (formato nuevo)
                    Pattern pattern2 = Pattern.compile("(\\d+)%\\s+—\\s+(\\d+)\\s+of\\s+(\\d+)");

                    Matcher matcher1 = pattern1.matcher(shotText);
                    Matcher matcher2 = pattern2.matcher(shotText);

                    if (matcher1.find()) {
                        shotsOnTarget = Integer.parseInt(matcher1.group(1));
                        totalShots = Integer.parseInt(matcher1.group(2));
                        accuracyRate = Integer.parseInt(matcher1.group(3));
                    } else if (matcher2.find()) {
                        accuracyRate = Integer.parseInt(matcher2.group(1));
                        shotsOnTarget = Integer.parseInt(matcher2.group(2));
                        totalShots = Integer.parseInt(matcher2.group(3));
                    } else {
                        System.err.println("No se pudo parsear los disparos: " + shotText);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error extrayendo disparos: " + e.getMessage());
            }


            // Tarjetas amarillas
            List<WebElement> cardsTd = driver.findElements(By.xpath("//tr[th[contains(text(),'Tarjetas')]]/following-sibling::tr[1]/td"));
            int yellowCards = -1;
            int yellowCardsRival = -1;

            if (cardsTd.size() >= 2) {
                yellowCards = cardsTd.get(indexTeam).findElements(By.cssSelector("span.yellow_card")).size();
                yellowCardsRival = cardsTd.get(indexRivalTeam).findElements(By.cssSelector("span.yellow_card")).size();
            }

            // Faltas, Tiros de esquina y Offside
            int faults = -1;
            int faultsRival = -1;
            int cornerKicks = -1;
            int cornerKicksRival = -1;
            int offside = -1;
            int offsideRival = -1;

            WebElement extraStats = driver.findElement(By.id("team_stats_extra"));
            List<WebElement> blocks = extraStats.findElements(By.xpath("./div"));

            for (WebElement block : blocks) {
                List<WebElement> divs = block.findElements(By.tagName("div"));
                for (int i = 3; i + 2 < divs.size(); i += 3) {
                    String StatisticName = divs.get(i + 1).getText().trim();

                    switch (StatisticName) {
                        case "Faltas":
                            faults = Integer.parseInt(divs.get(i).getText().trim());
                            faultsRival = Integer.parseInt(divs.get(i + 2).getText().trim());
                            break;
                        case "Tiro de esquina":
                            cornerKicks = Integer.parseInt(divs.get(i).getText().trim());
                            cornerKicksRival = Integer.parseInt(divs.get(i + 2).getText().trim());
                            break;
                        case "Posición adelantada":
                            offside = Integer.parseInt(divs.get(i).getText().trim());
                            offsideRival = Integer.parseInt(divs.get(i + 2).getText().trim());
                            break;
                    }
                }
            }

            // Competición
            String competition = "";
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                WebElement competitionLink = driver.findElement(By.cssSelector("#content > div > a[href*='/comps/']"));

                competition = competitionLink.getText().trim();
            } catch (Exception e) {
                System.err.println("No se pudo extraer la competencia: " + e.getMessage());
                competition = "Desconocida";
            }


            String nameFile = "data/" + team.toLowerCase().replace(" ", "_") + "-" + siteCondition.toLowerCase() + ".csv";

            saveInCSV(
                    nameFile,
                    scoredGoals, xgScored, possession,
                    totalShots, shotsOnTarget, accuracyRate,
                    yellowCards, cornerKicks, faults, offside,
                    rival, concededGoals, xgConceded,
                    yellowCardsRival, bothScored,
                    faultsRival, cornerKicksRival, offsideRival, competition, league
            );

        } catch (Exception e) {
            System.err.println("Error extrayendo datos del informe: " + e.getMessage());
        }
    }


    private void saveInCSV(String nameFile,
                           int scoredGoals, double xgScored, double possession,
                           int totalShots, int shotsOnTarget, int accuracyRate,
                           int yellowCards, int cornerKicks, int faults, int offside,
                           String rivalTeam, int goalsConceded, double xgConceded,
                           int yellowCardsRival, String bothScored,
                           int faultsRival, int cornerKicksRival, int offsideRival, String competition,
                           String league) {

        boolean archivoExiste = Files.exists(Paths.get(nameFile));
        boolean incluirXG = !league.equalsIgnoreCase("Colombian Primera A");

        int hierarchy = calculateHierarchy(league, rivalTeam);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nameFile, true))) {

            if (!archivoExiste) {
                if (incluirXG) {
                    writer.write(String.join(",",
                            "Goles", "xG", "Posesión",
                            "Disparos Totales", "Disparos a Puerta", "Precisión (%)",
                            "Tarjetas Amarillas", "Tiros de Esquina", "Faltas", "Offside",
                            "Rival", "Goles Recibidos", "xG Rival",
                            "Tarjetas Amarillas Rival", "Ambos Marcaron",
                            "Faltas Rival", "Tiros de Esquina Rival", "Offside Rival", "Competición", "Jerarquía"
                    ));
                } else {
                    writer.write(String.join(",",
                            "Goles", "Posesión",
                            "Disparos Totales", "Disparos a Puerta", "Precisión (%)",
                            "Tarjetas Amarillas", "Tiros de Esquina", "Faltas", "Offside",
                            "Rival", "Goles Recibidos",
                            "Tarjetas Amarillas Rival", "Ambos Marcaron",
                            "Faltas Rival", "Tiros de Esquina Rival", "Offside Rival", "Competición", "Jerarquía"
                    ));
                }
                writer.newLine();
            }

            if (incluirXG) {
                writer.write(String.join(",",
                        String.valueOf(scoredGoals), String.valueOf(xgScored), String.valueOf(possession),
                        String.valueOf(totalShots), String.valueOf(shotsOnTarget), String.valueOf(accuracyRate),
                        String.valueOf(yellowCards), String.valueOf(cornerKicks), String.valueOf(faults), String.valueOf(offside),
                        rivalTeam, String.valueOf(goalsConceded), String.valueOf(xgConceded),
                        String.valueOf(yellowCardsRival), bothScored,
                        String.valueOf(faultsRival), String.valueOf(cornerKicksRival), String.valueOf(offsideRival), String.valueOf(competition),
                        String.valueOf(hierarchy)
                ));
            } else {
                writer.write(String.join(",",
                        String.valueOf(scoredGoals), String.valueOf(possession),
                        String.valueOf(totalShots), String.valueOf(shotsOnTarget), String.valueOf(accuracyRate),
                        String.valueOf(yellowCards), String.valueOf(cornerKicks), String.valueOf(faults), String.valueOf(offside),
                        rivalTeam, String.valueOf(goalsConceded),
                        String.valueOf(yellowCardsRival), bothScored,
                        String.valueOf(faultsRival), String.valueOf(cornerKicksRival), String.valueOf(offsideRival), String.valueOf(competition),
                        String.valueOf(hierarchy)
                ));
            }

            writer.newLine();

        } catch (IOException e) {
            System.err.println("Error escribiendo en el archivo CSV: " + e.getMessage());
        }
    }

    public int calculateHierarchy(String leagueName, String rivalTeam) {
        String[] knownLeagues = {
                "Spanish La Liga",
                "English Premier League",
                "Italian Serie A",
                "German Bundesliga",
                "Portuguese Primeira Liga",
                "Dutch Eredivisie",
                "Ligue 1",
                "Colombian Primera A",
        };


        int hierarchy = searchTeamInLeague(leagueName, rivalTeam);
        if (hierarchy > 0) {
            return hierarchy;
        }

        System.out.println("Equipo NO encontrado en la liga local: " + leagueName + ", buscando en otras ligas...");

        for (String otherLeague : knownLeagues) {
            if (!otherLeague.equalsIgnoreCase(leagueName)) {
                hierarchy = searchTeamInLeague(otherLeague, rivalTeam);
                if (hierarchy > 0) {
                    System.out.println("Equipo encontrado en liga secundaria: " + otherLeague + " con jerarquía: " + hierarchy);
                    return hierarchy;
                }
            }
        }

        System.out.println("Equipo " + rivalTeam + " no encontrado en ninguna liga conocida.");
        return 0;
    }

    private int searchTeamInLeague(String leagueName, String rivalTeam) {
        String leagueFilePath = "league_data/" + leagueName + ".csv";
        int[] hierarchyRange = getHierarchyRange(leagueName);

        try (BufferedReader reader = new BufferedReader(new FileReader(leagueFilePath))) {
            String line;
            int position = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                position++;
                String[] columns = line.split(",");
                if (columns.length > 1) {
                    String teamName = columns[1].trim();
                    String normalizedTeam = normalize(teamName);
                    String normalizedRival = normalize(rivalTeam);


                    if (normalizedTeam.equals(normalizedRival)) {

                        if (position <= hierarchyRange[0]) return 10;
                        if (position <= hierarchyRange[1]) return 8;
                        if (position <= hierarchyRange[2]) return 6;
                        return 4;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error leyendo el archivo de la liga " + leagueName + ": " + e.getMessage());
        }

        return 0;
    }

    private int[] getHierarchyRange(String leagueName) {
        switch (leagueName.toLowerCase(Locale.ROOT)) {
            case "spanish la liga":
                return new int[]{6, 10, 15};
            case "english premier league":
                return new int[]{7, 10, 16};
            case "italian serie a":
                return new int[]{8, 10, 15};
            case "german bundesliga":
                return new int[]{6, 10, 14};
            case "ligue 1":
                return new int[]{7, 10, 15};
            case "colombian primera a":
                return new int[]{8, 10, 14};
            default:
                return new int[]{4, 10, 14};
        }
    }

    private String normalize(String input) {
        if (input == null) return "";

        String cleaned = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .replace("ß", "ss")
                .replace("ø", "o")
                .replace("æ", "ae")
                .replace("œ", "oe")
                .replace("ü", "u")
                .replace("ö", "o")
                .replace("ä", "a")
                .replace("ñ", "n");

        switch (cleaned) {
            case "bayern munchen":
            case "fc bayern munchen":
                return "bayern munich";
            case "bayer leverkusen":
            case "leverkusen":
                return "leverkusen";
            case "eint frankfurt":
            case "eintracht frankfurt":
                return "eint frankfurt";
            case "real betis":
                return "betis";
            case "gladbach":
            case "borussia monchengladbach":
                return "gladbach";
            default:
                return cleaned;
        }
    }


}