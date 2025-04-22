package proyectospersonales.fbrefscraper.config;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;
import proyectospersonales.fbrefscraper.util.PopupHandler;

import java.nio.file.Paths;
import java.util.List;

@Service
public class WebDriverManager {
    private static WebDriver driver;

    public WebDriverManager() {
        if (driver == null) {  // 🔹 Evitar múltiples instancias
            String driverPath = Paths.get("drivers/chromedriver.exe").toAbsolutePath().toString();
            System.setProperty("webdriver.chrome.driver", driverPath);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));

            driver = new ChromeDriver(options);
            driver.get("https://fbref.com/es/"); // 🔹 Abrimos directamente la URL
            System.out.println("✅ WebDriver inicializado correctamente.");



            PopupHandler.closePopupIfExist(
                    driver,
                    By.cssSelector("button.osano-cm-dialog__close.osano-cm-close"),
                    5,
                    "banner de cookies"
            );


        }
    }

    public WebDriver getDriver() {
        return driver;
    }

    public void closeDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
}
