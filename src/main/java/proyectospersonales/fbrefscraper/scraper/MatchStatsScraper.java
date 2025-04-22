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
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MatchStatsScraper {
    private final WebDriver driver;

    public MatchStatsScraper(WebDriverManager webDriverManager) {
        this.driver = webDriverManager.getDriver();
    }

    public void scrape(String equipoLocal, String equipoVisitante) {
        System.out.println("Página actual: " + driver.getCurrentUrl());

        buscarEquipo(equipoLocal, "Local", "fpc");
        System.out.println("Búsqueda realizada para los partidos de local del: " + equipoLocal);

        buscarEquipo(equipoVisitante, "Visitante", "fpc");
        System.out.println("Búsqueda realizada para los partidos de visitantes del: " + equipoVisitante);
    }

    private void buscarEquipo(String equipo, String condicionSede, String liga) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
        WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("search")));

        searchInput.clear();
        searchInput.sendKeys(equipo);
        searchInput.sendKeys(Keys.ENTER);
        seleccionarPrimerEquipoEnLaBusqueda(condicionSede, liga);
    }

    private void seleccionarPrimerEquipoEnLaBusqueda(String condicionSede, String liga) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        WebElement primerEquipo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".search-item-name a")));

        String equipoURL = primerEquipo.getAttribute("href");
        System.out.println("Primer equipo encontrado: " + primerEquipo.getText());
        System.out.println("URL del equipo: " + equipoURL);

        driver.get(equipoURL);

        extraerInformesDePartido(condicionSede, liga);
    }


    public void extraerInformesDePartido(String condicionSede, String liga) {
        System.out.println("Iniciando extracción de informes de partido para la sede: " + condicionSede);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        System.out.println("Buscando la tabla de partidos...");
        WebElement tablaPartidos = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table#matchlogs_for.stats_table")));
        System.out.println("Tabla de partidos encontrada.");

        List<WebElement> filas = tablaPartidos.findElements(By.cssSelector("tbody > tr"))
                .stream()
                .filter(fila -> !fila.getAttribute("class").contains("thead")) // Ignora encabezados intermedios
                .filter(fila -> !fila.findElements(By.cssSelector("td")).isEmpty()) // Ignora filas sin datos
                .collect(Collectors.toList());

        System.out.println("Filas de la tabla con datos reales encontradas: " + filas.size());

        int contador = 0;

        for (int i = 0; i < filas.size(); i++) {
            WebElement fila = filas.get(i);
            System.out.println("Procesando fila #" + (i + 1));

            try {
                System.out.println("Buscando celda de sede en la fila...");
                WebElement sedeTd = fila.findElement(By.cssSelector("td[data-stat='venue']"));
                String sede = sedeTd.getText().trim();
                System.out.println("Sede encontrada: " + sede);

                if (!sede.equalsIgnoreCase(condicionSede)) {
                    System.out.println("Sede no coincide con la condición. Saltando fila.");
                    continue;
                }

                System.out.println("Buscando enlace al informe del partido en la fila...");
                WebElement enlaceInforme = fila.findElement(By.xpath(".//td[@data-stat='match_report']/a"));

                // Verificar que sea un informe y no "Cara a cara"
                String textoEnlace = enlaceInforme.getText().trim();
                if (!textoEnlace.equalsIgnoreCase("Informe del partido")) {
                    System.out.println("El enlace no es un informe de partido (es: '" + textoEnlace + "'). Saltando fila.");
                    continue;
                }

                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'center'});", enlaceInforme);
                Thread.sleep(2000);
                wait.until(ExpectedConditions.elementToBeClickable(enlaceInforme));

                try {
                    Actions actions = new Actions(driver);
                    actions.moveToElement(enlaceInforme).click().perform();
                    System.out.println("Enlace clickeado con éxito.");
                } catch (Exception e) {
                    System.err.println("No se pudo hacer clic en el enlace: " + e.getMessage());
                    continue;
                }

                System.out.println("Entrando en el informe del partido: " + driver.getCurrentUrl());

                PopupHandler.closePopupIfExist(
                        driver,
                        By.cssSelector("div#modal-close"),
                        3,
                        "modal del informe"
                );


                extraerDatosDelInforme(condicionSede, liga);
                Thread.sleep(5000);
                contador++;
                System.out.println("Informe #" + contador + " extraído.");

                System.out.println("Volviendo a la tabla de partidos...");
                driver.navigate().back();
                Thread.sleep(2000);

                tablaPartidos = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table#matchlogs_for.stats_table")));
                filas = tablaPartidos.findElements(By.cssSelector("tbody > tr"))
                        .stream()
                        .filter(f -> !f.getAttribute("class").contains("thead"))
                        .filter(f -> !f.findElements(By.cssSelector("td")).isEmpty())
                        .collect(Collectors.toList());

                System.out.println("Reubicada la tabla de partidos. Filas encontradas: " + filas.size());

            } catch (NoSuchElementException e) {
                System.out.println("Fila sin dato de sede o enlace. Saltando.");
            } catch (Exception e) {
                System.err.println("Error procesando una fila: " + e.getMessage());
            }
        }

        System.out.println("Proceso de extracción de informes completado. Total extraídos: " + contador);
    }

    private void extraerDatosDelInforme(String condicionSede, String liga) {
        try {
            WebElement scorebox = driver.findElement(By.cssSelector("div.scorebox"));

            List<WebElement> scoresDivs = scorebox.findElements(By.cssSelector("div.scores"));
            List<WebElement> teamNames = scorebox.findElements(By.cssSelector("strong > a"));

            if (scoresDivs.size() < 2 || teamNames.size() < 2) {
                System.err.println("No se encontraron los divs necesarios para extraer la info.");
                return;
            }

            int indexEquipo = condicionSede.equalsIgnoreCase("Local") ? 0 : 1;
            int indexRival = 1 - indexEquipo;

            String equipo = teamNames.get(indexEquipo).getText().trim();
            String rival = teamNames.get(indexRival).getText().trim();

            int golesHechos = Integer.parseInt(scoresDivs.get(indexEquipo).findElement(By.cssSelector("div.score")).getText().trim());
            int golesRecibidos = Integer.parseInt(scoresDivs.get(indexRival).findElement(By.cssSelector("div.score")).getText().trim());
            String ambosMarcaron = (golesHechos > 0 && golesRecibidos > 0) ? "sí" : "no";

            // xG
            List<WebElement> xgDivs = driver.findElements(By.cssSelector("div.score_xg"));
            double xgHechos = xgDivs.size() > indexEquipo ? Double.parseDouble(xgDivs.get(indexEquipo).getText().trim()) : -1;
            double xgRecibidos = xgDivs.size() > indexRival ? Double.parseDouble(xgDivs.get(indexRival).getText().trim()) : -1;

            // Posesión
            List<WebElement> posesionDivs = driver.findElements(By.xpath("//tr[th[contains(text(),'Posesión del balón')]]/following-sibling::tr[1]/td/div/div/strong"));
            double posesion = posesionDivs.size() > indexEquipo ? Double.parseDouble(posesionDivs.get(indexEquipo).getText().replace("%", "").trim()) : -1;

            // Disparos (corregido para funcionar con visitante)
            int disparosTotales = -1;
            int disparosPuerta = -1;
            int porcentajePrecision = -1;
            try {
                WebElement filaDisparos = driver.findElement(By.xpath("//tr[th[contains(text(),'Disparos a puerta')]]/following-sibling::tr[1]"));
                List<WebElement> tds = filaDisparos.findElements(By.tagName("td"));

                if (tds.size() >= 2) {
                    String textoDisparo = tds.get(indexEquipo).findElement(By.xpath(".//div[1]/div[1]")).getText();

                    // Primer patrón: "5 of 13 — 38%" (formato antiguo)
                    Pattern pattern1 = Pattern.compile("(\\d+)\\s+of\\s+(\\d+)\\s+—\\s+(\\d+)%");

                    // Segundo patrón: "38% — 5 of 13" (formato nuevo)
                    Pattern pattern2 = Pattern.compile("(\\d+)%\\s+—\\s+(\\d+)\\s+of\\s+(\\d+)");

                    Matcher matcher1 = pattern1.matcher(textoDisparo);
                    Matcher matcher2 = pattern2.matcher(textoDisparo);

                    if (matcher1.find()) {
                        disparosPuerta = Integer.parseInt(matcher1.group(1));
                        disparosTotales = Integer.parseInt(matcher1.group(2));
                        porcentajePrecision = Integer.parseInt(matcher1.group(3));
                    } else if (matcher2.find()) {
                        porcentajePrecision = Integer.parseInt(matcher2.group(1));
                        disparosPuerta = Integer.parseInt(matcher2.group(2));
                        disparosTotales = Integer.parseInt(matcher2.group(3));
                    } else {
                        System.err.println("No se pudo parsear los disparos: " + textoDisparo);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error extrayendo disparos: " + e.getMessage());
            }


            // Tarjetas amarillas
            List<WebElement> tarjetasTd = driver.findElements(By.xpath("//tr[th[contains(text(),'Tarjetas')]]/following-sibling::tr[1]/td"));
            int tarjetasAmarillas = -1;
            int tarjetasAmarillasRival = -1;

            if (tarjetasTd.size() >= 2) {
                tarjetasAmarillas = tarjetasTd.get(indexEquipo).findElements(By.cssSelector("span.yellow_card")).size();
                tarjetasAmarillasRival = tarjetasTd.get(indexRival).findElements(By.cssSelector("span.yellow_card")).size();
            }

            // Faltas, Tiros de esquina y Offside
            int faltas = -1;
            int faltasRival = -1;
            int tirosEsquina = -1;
            int tirosEsquinaRival = -1;
            int offside = -1;
            int offsideRival = -1;

            WebElement extraStats = driver.findElement(By.id("team_stats_extra"));
            List<WebElement> bloques = extraStats.findElements(By.xpath("./div"));

            for (WebElement bloque : bloques) {
                List<WebElement> divs = bloque.findElements(By.tagName("div"));
                for (int i = 3; i + 2 < divs.size(); i += 3) {
                    String nombreEstadistica = divs.get(i + 1).getText().trim();

                    switch (nombreEstadistica) {
                        case "Faltas":
                            faltas = Integer.parseInt(divs.get(i).getText().trim());
                            faltasRival = Integer.parseInt(divs.get(i + 2).getText().trim());
                            break;
                        case "Tiro de esquina":
                            tirosEsquina = Integer.parseInt(divs.get(i).getText().trim());
                            tirosEsquinaRival = Integer.parseInt(divs.get(i + 2).getText().trim());
                            break;
                        case "Posición adelantada":
                            offside = Integer.parseInt(divs.get(i).getText().trim());
                            offsideRival = Integer.parseInt(divs.get(i + 2).getText().trim());
                            break;
                    }
                }
            }

            // Guardar en CSV
            String nombreArchivo = "src/main/java/data/" + equipo.toLowerCase().replace(" ", "_") + "-" + condicionSede.toLowerCase() + ".csv";
            System.out.println("---- Datos recolectados ----");
            System.out.println("Equipo: " + equipo);
            System.out.println("Condición: " + condicionSede);
            System.out.println("Goles Hechos: " + golesHechos);
            System.out.println("xG Hechos: " + xgHechos);
            System.out.println("Posesión: " + posesion);
            System.out.println("Disparos Totales: " + disparosTotales);
            System.out.println("Disparos a Puerta: " + disparosPuerta);
            System.out.println("Precisión de Disparos: " + porcentajePrecision + "%");
            System.out.println("Tarjetas Amarillas: " + tarjetasAmarillas);
            System.out.println("Tiros de Esquina: " + tirosEsquina);
            System.out.println("Faltas: " + faltas);
            System.out.println("Offside: " + offside);
            System.out.println("Rival: " + rival);
            System.out.println("Goles Recibidos: " + golesRecibidos);
            System.out.println("xG Rival: " + xgRecibidos);
            System.out.println("Tarjetas Amarillas Rival: " + tarjetasAmarillasRival);
            System.out.println("Ambos Marcaron: " + ambosMarcaron);
            System.out.println("Faltas Rival: " + faltasRival);
            System.out.println("Tiros de Esquina Rival: " + tirosEsquinaRival);
            System.out.println("Offside Rival: " + offsideRival);
            System.out.println("-----------------------------");

            guardarEnCSV(
                    nombreArchivo,
                    golesHechos, xgHechos, posesion,
                    disparosTotales, disparosPuerta, porcentajePrecision,
                    tarjetasAmarillas, tirosEsquina, faltas, offside,
                    rival, golesRecibidos, xgRecibidos,
                    tarjetasAmarillasRival, ambosMarcaron,
                    faltasRival, tirosEsquinaRival, offsideRival, liga
            );

        } catch (Exception e) {
            System.err.println("Error extrayendo datos del informe: " + e.getMessage());
        }
    }



    private void guardarEnCSV(String nombreArchivo,
                              int golesHechos, double xgHechos, double posesion,
                              int disparosTotales, int disparosPuerta, int porcentajePrecision,
                              int tarjetasAmarillas, int tirosEsquina, int faltas, int offside,
                              String rival, int golesRecibidos, double xgRecibidos,
                              int tarjetasAmarillasRival, String ambosMarcaron,
                              int faltasRival, int tirosEsquinaRival, int offsideRival, String liga) {

        boolean archivoExiste = Files.exists(Paths.get(nombreArchivo));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nombreArchivo, true))) {

            if (!archivoExiste) {
                writer.write(String.join(",",
                        "Goles", "xG", "Posesión",
                        "Disparos Totales", "Disparos a Puerta", "Precisión (%)",
                        "Tarjetas Amarillas", "Tiros de Esquina", "Faltas", "Offside",
                        "Rival", "Goles Recibidos", "xG Rival",
                        "Tarjetas Amarillas Rival", "Ambos Marcaron",
                        "Faltas Rival", "Tiros de Esquina Rival", "Offside Rival"
                ));
                writer.newLine();
            }

            writer.write(String.join(",",
                    String.valueOf(golesHechos), String.valueOf(xgHechos), String.valueOf(posesion),
                    String.valueOf(disparosTotales), String.valueOf(disparosPuerta), String.valueOf(porcentajePrecision),
                    String.valueOf(tarjetasAmarillas), String.valueOf(tirosEsquina), String.valueOf(faltas), String.valueOf(offside),
                    rival, String.valueOf(golesRecibidos), String.valueOf(xgRecibidos),
                    String.valueOf(tarjetasAmarillasRival), ambosMarcaron,
                    String.valueOf(faltasRival), String.valueOf(tirosEsquinaRival), String.valueOf(offsideRival)
            ));
            writer.newLine();

        } catch (IOException e) {
            System.err.println("Error escribiendo en el archivo CSV: " + e.getMessage());
        }
    }



}