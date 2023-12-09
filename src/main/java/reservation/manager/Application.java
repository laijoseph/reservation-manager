package reservation.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import reservation.manager.controllers.ReservationController;
import reservation.manager.service.ReservationService;

@SpringBootApplication
@ComponentScan(basePackageClasses = { ReservationController.class, ReservationService.class })
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
