package br.com.hubfaturamento;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RegraDeDependenciaTest {

    private static final Path FONTES = Path.of(
        "src/main/java/br/com/hubfaturamento"
    );
    private static final String BASE = "br.com.hubfaturamento.";

    @Test
    void dominioSoConheceASiMesmo() {
        assertThat(importsDa("dominio"))
            .noneMatch(i -> i.startsWith(BASE + "aplicacao"))
            .noneMatch(i -> i.startsWith(BASE + "infraestrutura"))
            .noneMatch(RegraDeDependenciaTest::ehTecnologia);
    }

    @Test
    void aplicacaoConheceSoODominio() {
        assertThat(importsDa("aplicacao"))
            .noneMatch(i -> i.startsWith(BASE + "infraestrutura"))
            .noneMatch(RegraDeDependenciaTest::ehTecnologia);
    }

    private static boolean ehTecnologia(String importacao) {
        return (
            importacao.startsWith("org.springframework") ||
            importacao.startsWith("jakarta.") ||
            importacao.startsWith("org.hibernate") ||
            importacao.startsWith("lombok")
        );
    }

    private static List<String> importsDa(String camada) {
        try (Stream<Path> arquivos = Files.walk(FONTES.resolve(camada))) {
            return arquivos
                .filter(p -> p.toString().endsWith(".java"))
                .flatMap(RegraDeDependenciaTest::linhas)
                .map(String::trim)
                .filter(l -> l.startsWith("import "))
                .map(l ->
                    l.replaceFirst("import (static )?", "").replace(";", "")
                )
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<String> linhas(Path arquivo) {
        try {
            return Files.readAllLines(arquivo).stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
