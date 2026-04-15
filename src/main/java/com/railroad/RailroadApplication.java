package com.railroad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главное приложение "Система железнодорожная касса"
 * 
 * Функционал:
 * - Пассажир: поиск рейсов, выбор поезда, оформление заявки, оплата
 * - Администратор: управление поездами, станциями, пользователями, просмотр заявок
 * 
 * @author Railroad Team
 * @version 1.0
 */
@SpringBootApplication
public class RailroadApplication {

    public static void main(String[] args) {
        SpringApplication.run(RailroadApplication.class, args);
    }
}
