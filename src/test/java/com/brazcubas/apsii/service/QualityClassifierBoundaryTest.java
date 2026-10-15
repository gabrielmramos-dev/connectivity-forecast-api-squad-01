package com.brazcubas.apsii.service;

import com.brazcubas.apsii.config.QualityRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes de borda e de caminho triste para {@link QualityClassifier}.
 * Cada teste verifica um único comportamento (Arrange-Act-Assert).
 */
class QualityClassifierBoundaryTest {

    private final QualityClassifier classifier = new QualityClassifier();

    // ---------- BORDAS (limiar inclusivo/exclusivo) ----------

    @Test
    @DisplayName("RTT exatamente no limiar moderado (60 ms) já é MODERATE (limiar inclusivo)")
    void rttExactlyAtModerateThresholdIsModerate() {
        // Arrange
        double rtt = QualityRules.MODERATE_RTT_MS;
        double packetLoss = 0.0;

        // Act
        String quality = classifier.classify(rtt, packetLoss);

        // Assert
        assertEquals("MODERATE", quality);
    }

    @Test
    @DisplayName("RTT logo abaixo do limiar moderado (59,99 ms) ainda é GOOD")
    void rttJustBelowModerateThresholdIsGood() {
        // Arrange
        double rtt = Math.nextDown(QualityRules.MODERATE_RTT_MS);

        // Act
        String quality = classifier.classify(rtt, 0.0);

        // Assert
        assertEquals("GOOD", quality);
    }

    @Test
    @DisplayName("Perda de pacotes exatamente no limiar instável (5%) já é UNSTABLE (limiar inclusivo)")
    void packetLossExactlyAtUnstableThresholdIsUnstable() {
        // Arrange
        double packetLoss = QualityRules.UNSTABLE_PACKET_LOSS;

        // Act
        String quality = classifier.classify(10.0, packetLoss);

        // Assert
        assertEquals("UNSTABLE", quality);
    }

    @Test
    @DisplayName("Valores extremos (RTT 10000 ms e perda 100%) resultam em UNSTABLE com score 0")
    void extremeValuesAreUnstableWithScoreZero() {
        // Arrange
        double rtt = 10_000.0;
        double packetLoss = 100.0;

        // Act
        QualityClassifier.QualityAssessment assessment = classifier.assess(rtt, packetLoss);

        // Assert
        assertEquals(QualityRules.Quality.UNSTABLE, assessment.quality());
        assertEquals(0, assessment.qualityScore());
    }

    @Test
    @DisplayName("Conexão perfeita (RTT 0 e perda 0) tem score 100")
    void perfectConnectionHasScoreOneHundred() {
        // Arrange / Act
        int score = classifier.score(0.0, 0.0);

        // Assert
        assertEquals(100, score);
    }

    // ---------- CAMINHOS TRISTES (entrada inválida) ----------

    @Test
    @DisplayName("RTT negativo lança IllegalArgumentException")
    void negativeRttThrowsIllegalArgumentException() {
        // Arrange
        double invalidRtt = -1.0;

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> classifier.classify(invalidRtt, 0.0));
    }

    @Test
    @DisplayName("Perda de pacotes NaN lança IllegalArgumentException")
    void nanPacketLossThrowsIllegalArgumentException() {
        // Arrange
        double invalidLoss = Double.NaN;

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> classifier.score(30.0, invalidLoss));
    }

    @Test
    @DisplayName("RTT infinito lança IllegalArgumentException")
    void infiniteRttThrowsIllegalArgumentException() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> classifier.classify(Double.POSITIVE_INFINITY, 0.0));
    }
}
