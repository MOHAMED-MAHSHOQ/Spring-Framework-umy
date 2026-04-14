package guru.springframework.spring6restmvc.services;

import guru.springframework.spring6restmvc.model.BeerCSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BeerCsvServiceImplTest {

    BeerCsvService beerCsvService = new BeerCsvServiceImpl();

    @Test
    void convertCSV() throws IOException {
        ClassPathResource csvResource = new ClassPathResource("csvdata/beers.csv");
        List<BeerCSVRecord> recs;
        try (Reader csvReader = new InputStreamReader(csvResource.getInputStream(), StandardCharsets.UTF_8)) {
            recs = beerCsvService.convertCSV(csvReader);
        }

        System.out.println(recs.size());

        assertThat(recs.size()).isGreaterThan(0);
    }
}