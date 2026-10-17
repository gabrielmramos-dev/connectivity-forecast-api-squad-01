package com.brazcubas.apsii.repository;

import com.brazcubas.apsii.model.Forecast;
import com.brazcubas.apsii.model.PredictionModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvForecastRepositoryTest {
    @Mock
    private ModelRepository repository;

    private CsvForecastRepository csvRepository;

    @BeforeEach
    void setUp() {
        when(repository.findAll()).thenReturn(List.of(model("model-a"), model("model-b")));
        csvRepository = new CsvForecastRepository(
                new DefaultResourceLoader(), repository, "classpath:data-test/predictions-11-2.csv");
        csvRepository.load();
    }

    @Test
    void blankConfidenceLoadsAsNull() {
        assertNull(byId("mock-a-2").modelConfidence());
    }

    @Test
    void boundaryConfidenceValuesZeroAndOneAreAccepted() {
        assertEquals(0.0, byId("mock-a-3").modelConfidence());
        assertEquals(1.0, byId("mock-a-4").modelConfidence());
    }

    @Test
    void validConfidenceIsKeptAsIs() {
        assertEquals(0.82, byId("mock-a-1").modelConfidence());
    }

    @Test
    void rowsWithOutOfRangeOrNonNumericConfidenceAreDiscarded() {
        List<String> ids = csvRepository.findByModelAndProbe("model-a", 900001)
                .stream().map(Forecast::predictionId).toList();

        // descartadas: mock-a-5 (1.5), mock-a-6 ("abc"), mock-a-7 (-0.1)
        assertEquals(List.of("mock-a-1", "mock-a-2", "mock-a-3", "mock-a-4"), ids);
    }

    @Test
    void findAllKeepsOnlyValidRowsOfKnownModels() {
        // 4 de model-a + 1 de model-b; model-zz é desconhecido e fica de fora
        assertEquals(5, csvRepository.findAll().size());
    }

    private Forecast byId(String id) {
        return csvRepository.findByModelAndProbe("model-a", 900001).stream()
                .filter(forecast -> forecast.predictionId().equals(id))
                .findFirst().orElseThrow();
    }

    private static PredictionModel model(String id) {
        return new PredictionModel(id, id, "Descrição de exemplo", "Grupo", "Teste", "1.0", true, null);
    }
}
