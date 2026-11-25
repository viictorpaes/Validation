package com.cesar.validation.service.ai;

import com.cesar.validation.dto.ExtractedDocumentDto;
import com.cesar.validation.util.TextNormalizationUtil;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Extrator determinístico baseado em expressões regulares para documentos brasileiros.
 * Utilizado exclusivamente como contingência caso a chave GEMINI_API_KEY não esteja configurada
 * ou quando a API externa estiver temporariamente indisponível, sem jamais inventar dados.
 */
@Component
public class HeuristicDocumentExtractor {

    private static final Pattern CPF_PATTERN = Pattern.compile("\\b(\\d{3}[\\s.]?\\d{3}[\\s.]?\\d{3}[\\s\\-/]?\\d{2})\\b");
    private static final Pattern CEP_PATTERN = Pattern.compile("\\b(\\d{5}-?\\d{3})\\b");
    private static final Pattern NAME_LINE_PATTERN = Pattern.compile(
        "(?im)^\\s*(?:nome(?:\\s+do\\s+cliente|\\s+do\\s+titular)?|titular|cliente|consumidor|sacado)\\s*[:\\-]\\s*([^\\r\\n]{3,120})$"
    );
    private static final Pattern ADDRESS_LABEL_PATTERN = Pattern.compile(
        "(?im)^\\s*(?:endere[cç]o(?:\\s+de\\s+entrega|\\s+da\\s+instala[cç][aã]o)?|logradouro|local)\\s*[:\\-]\\s*([^\\r\\n]{5,160})$"
    );
    private static final Pattern STREET_LINE_PATTERN = Pattern.compile(
        "(?im)^\\s*((?:rua|av\\.?|avenida|travessa|alameda|pra[cç]a|estrada|rodovia|quadra)\\s+[^\\r\\n]{4,140})$"
    );
    private static final Pattern CITY_STATE_PATTERN = Pattern.compile(
        "(?im)(?:cidade\\s*[:\\-]\\s*)?([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s]{2,45})\\s*[-/]\\s*(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)\\b"
    );
    private static final Pattern LABELED_DATE_PATTERN = Pattern.compile(
        "(?im)(?:data\\s+de\\s+emiss[aã]o|emiss[aã]o|emitido\\s+em|apresenta[cç][aã]o)\\s*[:\\-]?\\s*(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2})"
    );
    private static final Pattern ANY_DATE_PATTERN = Pattern.compile(
        "\\b(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})\\b"
    );

    private static final Set<String> VALID_UFS = Set.of(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
        "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    public ExtractedDocumentDto extractFromText(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return new ExtractedDocumentDto(
                null, null, null, null, null, null, null, null,
                List.of("nomeTitular", "cpf", "endereco", "cidade", "estado", "dataEmissao", "tipoDocumento")
            );
        }

        String nome = matchFirstGroup(NAME_LINE_PATTERN, ocrText);
        String cpf = extractCpf(ocrText);
        String cep = matchFirstGroup(CEP_PATTERN, ocrText);
        String endereco = matchFirstGroup(ADDRESS_LABEL_PATTERN, ocrText);
        if (endereco == null) {
            endereco = matchFirstGroup(STREET_LINE_PATTERN, ocrText);
        }

        String cidade = null;
        String estado = null;
        Matcher cityStateMatcher = CITY_STATE_PATTERN.matcher(ocrText);
        while (cityStateMatcher.find()) {
            String candidateCity = cleanField(cityStateMatcher.group(1));
            String candidateUf = cityStateMatcher.group(2).toUpperCase(Locale.ROOT);
            if (candidateCity != null && VALID_UFS.contains(candidateUf)
                && !candidateCity.toLowerCase(Locale.ROOT).startsWith("rua ")
                && !candidateCity.toLowerCase(Locale.ROOT).startsWith("avenida ")) {
                cidade = candidateCity;
                estado = candidateUf;
                break;
            }
        }

        String dataEmissao = extractIsoDate(ocrText);
        String tipoDocumento = inferDocumentType(ocrText);

        List<String> missing = computeMissingFields(nome, cpf, endereco, cidade, estado, dataEmissao, tipoDocumento);

        return new ExtractedDocumentDto(
            nome,
            cpf,
            endereco,
            cidade,
            estado,
            cep != null ? cep.replaceAll("\\D", "") : null,
            dataEmissao,
            tipoDocumento,
            missing
        );
    }

    public static List<String> computeMissingFields(
        String nome,
        String cpf,
        String endereco,
        String cidade,
        String estado,
        String dataEmissao,
        String tipoDocumento
    ) {
        List<String> missing = new ArrayList<>();
        if (isBlank(nome)) missing.add("nomeTitular");
        if (isBlank(cpf)) missing.add("cpf");
        if (isBlank(endereco)) missing.add("endereco");
        if (isBlank(cidade)) missing.add("cidade");
        if (isBlank(estado)) missing.add("estado");
        if (isBlank(dataEmissao)) missing.add("dataEmissao");
        if (isBlank(tipoDocumento)) missing.add("tipoDocumento");
        return missing;
    }

    private String extractCpf(String text) {
        Matcher matcher = CPF_PATTERN.matcher(text);
        while (matcher.find()) {
            String digits = matcher.group(1).replaceAll("\\D", "");
            if (digits.length() == 11) {
                return digits;
            }
        }
        return null;
    }

    private String extractIsoDate(String text) {
        String raw = matchFirstGroup(LABELED_DATE_PATTERN, text);
        if (raw != null) {
            Optional<LocalDate> parsed = TextNormalizationUtil.parseFlexibleDate(raw);
            if (parsed.isPresent()) {
                return parsed.get().toString();
            }
        }
        Matcher fallback = ANY_DATE_PATTERN.matcher(text);
        if (fallback.find()) {
            Optional<LocalDate> parsed = TextNormalizationUtil.parseFlexibleDate(fallback.group(1));
            if (parsed.isPresent()) {
                return parsed.get().toString();
            }
        }
        return null;
    }

    private String inferDocumentType(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("SABESP") || upper.contains("COMPESA") || upper.contains("COPASA")
            || upper.contains("CAERN") || upper.contains("EMBASA") || upper.contains("SANEPAR")
            || upper.contains("CEDAE") || upper.contains("CAGECE") || upper.contains("SANEAMENTO")
            || upper.contains("CONTA DE ÁGUA") || upper.contains("CONTA DE AGUA")
            || upper.contains("FORNECIMENTO DE AGUA") || upper.contains("FORNECIMENTO DE ÁGUA")) {
            return "CONTA_DE_AGUA";
        }
        if (upper.contains("ENEL") || upper.contains("NEOENERGIA") || upper.contains("CELPE")
            || upper.contains("CEMIG") || upper.contains("CPFL") || upper.contains("EQUATORIAL")
            || upper.contains("COELBA") || upper.contains("COPEL") || upper.contains("LIGHT")
            || upper.contains("CONTA DE LUZ") || upper.contains("CONTA DE ENERGIA")
            || upper.contains("ENERGIA ELÉTRICA") || upper.contains("ENERGIA ELETRICA")
            || upper.contains("KWH")) {
            return "CONTA_DE_ENERGIA";
        }
        if (upper.contains("VIVO") || upper.contains("CLARO") || upper.contains("TIM ")
            || upper.contains("TIM ULTRA") || upper.contains("OI FIBRA")
            || upper.contains("TELECOM") || upper.contains("TELEFONE") || upper.contains("BANDA LARGA")) {
            return "CONTA_DE_TELEFONE";
        }
        if (upper.contains("FATURA") && upper.contains("CART")) {
            return "FATURA_CARTAO";
        }
        if (upper.contains("CONTRATO DE ALUGUEL") || upper.contains("CONTRATO DE LOCA")) {
            return "CONTRATO_ALUGUEL";
        }
        if (upper.contains("COMPROVANTE DE RESID")) {
            return "OUTROS";
        }
        return null;
    }

    private String matchFirstGroup(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return cleanField(m.group(1));
        }
        return null;
    }

    private String cleanField(String value) {
        if (value == null) return null;
        String trimmed = value.replaceAll("\\s+", " ").trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
