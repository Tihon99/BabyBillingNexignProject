package ru.anger.BRT.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.anger.BRT.DTO.DataForRatingDTO;
import ru.anger.BRT.DTO.ChargeResultDTO;

import java.math.BigDecimal;

@Service
public class HrsClientService {

    @Autowired
    private RestClient restClient;

    /*public ChargeResultDTO calculateCharge(DataForRatingDTO dto) {
        return restClient.post()
                .uri("/calculate")
                .body(dto)
                .retrieve()
                .body(ChargeResultDTO.class);
    }*/

    public ChargeResultDTO calculateCharge(DataForRatingDTO dto) {
        // Заглушка: возвращаем фиксированный результат
        return new ChargeResultDTO(
                BigDecimal.valueOf(5.00), // chargeAmount
                5,                        // minutesSpent
                false                     // shouldUpdateLastPaymentDate
        );
    }
}
