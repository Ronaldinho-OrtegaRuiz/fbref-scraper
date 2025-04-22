package proyectospersonales.fbrefscraper.util;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PopupHandler {

    public static void closePopupIfExist(WebDriver driver, By selector, int timeoutSegundos, String descripcion) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSegundos));
            WebElement botonCerrar = wait.until(ExpectedConditions.elementToBeClickable(selector));
            botonCerrar.click();
            System.out.println(descripcion + " cerrado.");
        } catch (TimeoutException e) {
            System.out.println("No apareció " + descripcion + ". Continuando.");
        } catch (Exception e) {
            System.out.println("Error al cerrar " + descripcion + ": " + e.getMessage());
        }
    }
}
